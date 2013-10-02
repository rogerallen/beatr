(ns beatr.beatr
  (:require [overtone.live :as o]))

;; based on overtone/src/overtone/examples/timing/interal_sequencer.clj
;; code rearranged to fit my brain + add polyrhythm

;; Default constants to use
;; 120Hz = 7200bpm & 6000/30 = 240bpm or 4bps
;; 16 beats = 4 seconds
(def DEFAULT-ROOT-RATE     120)
(def DEFAULT-BEAT-DIVISOR   30)
(def MAX-BEAT-BUSES          8)

(defn get-rate-divisor
  [num-beats seq-len-in-seconds root-rate-in-hz]
  (let [bps (/ num-beats seq-len-in-seconds)]
    (/ root-rate-in-hz bps)))

;; put global metronome pulse on root-trg-bus
(defonce root-trg-bus (o/control-bus))
(o/defsynth root-trg-synth [rate DEFAULT-ROOT-RATE]
  (o/out:kr root-trg-bus (o/impulse:kr rate)))

;; put global metronome count on root-cnt-bus
(defonce root-cnt-bus (o/control-bus))
(o/defsynth root-cnt-synth []
  (let [cur-count (o/pulse-count:kr (o/in:kr root-trg-bus))]
    (o/tap "cur-root-cnt" 30 cur-count)
    (o/out:kr root-cnt-bus cur-count)))

(defn dovec [xs]
  "doall and convert sequence to vector"
  (apply vector (doall xs)))

;; put out polyrhythm beat pulses on beatN-trg-bus's
(defonce beat-trg-buses
  (dovec (for [i (range MAX-BEAT-BUSES)]
           (o/control-bus))))
(o/defsynth beat-trg-synth [beat-bus 0
                            div DEFAULT-BEAT-DIVISOR]
  (o/out:kr beat-bus (o/pulse-divider (o/in:kr root-trg-bus) div)))

;; put out beat counts on beatN-cnt-bus's
(defonce beat-cnt-buses
  (dovec (for [i (range MAX-BEAT-BUSES)]
           (o/control-bus))))
(o/defsynth beat-cnt-synth [cnt-bus 0 trg-bus 0]
  (o/out:kr cnt-bus (o/pulse-count (o/in:kr trg-bus))))

;; just sample playback.  took out the fancy bits Sam put in
(o/defsynth mono-sample-beat-synth
  "Plays a single channel audio buffer on a particular beat."
  [beat-num     0 ; the beat number of this synth
   beat-buf     0 ; the buffer of all beats 0=off, 1=on
   seq-beats    8 ; the number of beats in a sequence
   beat-cnt-bus 0 ; the beat count bus
   beat-trg-bus 0 ; the beat trigger bus
   sample-buf   0 ; the sample buffer to play
   sample-rate  1 ; rate of sample-buffer playback
   amp          1 ; output volume
   out-bus      0]
  (let [cnt           (o/in:kr beat-cnt-bus)
        beat-trg      (o/in:kr beat-trg-bus)
        this-beat-trg (and (o/buf-rd:kr 1 beat-buf cnt)
                           (= beat-num (mod cnt seq-beats))
                           beat-trg)
        vol           (o/set-reset-ff this-beat-trg)]
    (o/out out-bus
         (* vol amp
            (o/pan2 (o/scaled-play-buf 1 sample-buf sample-rate this-beat-trg))))))

;; ======================================================================
;; control atoms
(defonce root-trg-atom (atom nil)) ; will contain the synth driving the root-trg-bus
(defonce root-cnt-atom (atom nil)) ; will contain the synth driving the root-cnt-bus
(defonce beat-trgs-atom (atom [])) ; will contain a list of synths driving the beat-trg-buses
(defonce beat-cnts-atom (atom [])) ; will contain a list of synths driving the beat-cnt-buses
(defonce seq-secs-atom (atom 0)) ; length of the sequences in seconds
(defonce seq-beats-atom (atom [])) ; beats in each sequence
;; list of o/buffers.  one for each sequence. buffers are the length
;; of the corresponding sequence and contain either 1 or 0 to control
;; playing the sample on that beat.  Initialized to be all 0
(defonce seq-bufs-atom (atom []))
(defonce seq-vecs-atom (atom []))  ; copy of bufs for client/gui use
;; a vector of vectors of synths.  each sequence contatains one synth
;; per beat that plays the sample
(defonce synth-seqs-atom (atom []))
(defonce active-atom (atom false)) ; is the sequence ready & active?

;; ======================================================================
;; "public" api
;;   restart
;;   set-seq-ctl
;;   set-seq-buf
(defn get-root-cnt []
  (if-not (nil? @root-cnt-atom)
    @(get-in @root-cnt-atom [:taps "cur-root-cnt"])
    0.0))
;;(get-root-cnt)

;; FIXME – this leaks the old array of synths
(defn start [seq-seconds
               beat-seq-beats
               beat-seq-synths]
  (swap! active-atom (fn [_] false))
  (o/stop) ;; kill anything currently playing on the server
  (let [beat-set (apply vector (sort (set beat-seq-beats)))]
    (assert (>= MAX-BEAT-BUSES (count beat-set)))
    (assert (= (count beat-seq-beats) (count beat-seq-synths)))

    (swap! seq-secs-atom (fn [_] seq-seconds))
    (swap! seq-beats-atom (fn [_] beat-seq-beats))
    (swap! root-trg-atom (fn [_] (root-trg-synth)))
    (swap! root-cnt-atom (fn [_] (root-cnt-synth)))
    (swap! beat-trgs-atom
           (fn [_] (dovec (for [i (range (count beat-set))]
                           (let [beat-trg-bus (nth beat-trg-buses i)
                                 num-beats    (nth beat-set i)
                                 cur-divisor  (get-rate-divisor num-beats seq-seconds DEFAULT-ROOT-RATE)]
                             (beat-trg-synth beat-trg-bus cur-divisor))))))
    (swap! beat-cnts-atom
           (fn [_] (dovec (for [i (range (count beat-set))]
                           (let [beat-trg-bus (nth beat-trg-buses i)
                                 beat-cnt-bus (nth beat-cnt-buses i)]
                             (beat-cnt-synth beat-cnt-bus beat-trg-bus))))))
    (swap! seq-bufs-atom
           (fn [_] (dovec (for [num-beats beat-seq-beats]
                           (o/buffer num-beats)))))
    (swap! seq-vecs-atom
           (fn [_] (dovec (for [num-beats beat-seq-beats]
                           (apply vector (repeat num-beats 0))))))

    (swap! synth-seqs-atom
           (fn [_] (dovec (for [[seq-index num-beats] (map-indexed vector beat-seq-beats)]
                           (let [beat-index (.indexOf beat-set num-beats)
                                 seq-buf      (nth @seq-bufs-atom seq-index)
                                 beat-cnt-bus (nth beat-cnt-buses beat-index)
                                 beat-trg-bus (nth beat-trg-buses beat-index)
                                 synth        (nth beat-seq-synths seq-index)]
                             (dovec
                              (for [k (range num-beats)]
                                (mono-sample-beat-synth
                                 :beat-num     k
                                 :beat-buf     seq-buf
                                 :seq-beats    num-beats
                                 :beat-cnt-bus beat-cnt-bus
                                 :beat-trg-bus beat-trg-bus
                                 :sample-buf   synth)))))))))
  (swap! active-atom (fn [_] true)))

(defn set-seq-ctl
  [seq-index key value]
  (doall (map #(o/ctl % key value) (@synth-seqs-atom seq-index)))
  nil)

(defn set-seq-buf
  [seq-index new-buf]
  (o/buffer-write! (nth @seq-bufs-atom seq-index) new-buf)
  (swap! seq-vecs-atom (fn [xs]
                         (assoc xs seq-index new-buf)))
  nil)

(defn stop []
  (o/stop))
