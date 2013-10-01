(ns beatr.core
  (:require [overtone.live :as o])
  (:require [quil.core :as q]))

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
(defonce seq-secs-atom (atom nil)) ; length of the sequences in seconds
(defonce seq-beats-atom (atom [])) ; beats in each sequence
;; list of o/buffers.  one for each sequence. buffers are the length
;; of the corresponding sequence and contain either 1 or 0 to control
;; playing the sample on that beat.  Initialized to be all 0
(defonce seq-bufs-atom (atom []))
(defonce seq-vecs-atom (atom []))  ; copy of bufs for client use
;; a vector of vectors of synths.  each sequence contatains one synth
;; per beat that plays the sample
(defonce synth-seqs-atom (atom []))

;; ======================================================================
;; "public" api
;;   restart
;;   set-seq-ctl
;;   set-seq-buf
(defn get-root-cnt []
  @(get-in @root-cnt-atom [:taps "cur-root-cnt"]))
;;(get-root-cnt)

(defn restart [seq-seconds
               beat-seq-beats
               beat-seq-synths]
  (o/stop) ;; kill anything currently going
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
                                 :sample-buf   synth))))))))
    nil))

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

;; ======================================================================
;; collection of samples
(def kick-s (o/sample (o/freesound-path 777)))
(def click-s (o/sample (o/freesound-path 406)))
(def snare (o/sample (o/freesound-path 26903)))
(def kick (o/sample (o/freesound-path 2086)))
(def close-hihat (o/sample (o/freesound-path 802)))
(def open-hihat (o/sample (o/freesound-path 26657)))
(def clap (o/sample (o/freesound-path 48310)))
(def gshake (o/sample (o/freesound-path 113625)))

;; ======================================================================
;; doodle #1 ...
(comment

  (restart 4 [16     12     12          8]
             [kick-s kick-s close-hihat open-hihat])

  (do
    ;;                                1 1 1 1 1 1 1
    ;;              1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6
    (set-seq-buf 0 [1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0])
    (set-seq-buf 1 [1 0 1 0 1 0 1 0 1 0 1 0])
    (set-seq-buf 2 [0 1 0 1 0 1 0 1 0 1 0 1])
    (set-seq-buf 3 [0 1 0 1 0 1 0 1])
    )

  (do
    ;;                                1 1 1 1 1 1 1
    ;;              1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6
    (set-seq-buf 0 [1 0 0 0 1 0 0 0 1 0 0 0 1 0 0 0])
    (set-seq-buf 1 [1 0 0 0 1 0 0 0 1 0 0 0])
    (set-seq-buf 2 [0 0 0 1 0 0 0 1 0 0 0 1])
    (set-seq-buf 3 [0 1 0 1 0 0 0 1])
    )

  (do
    ;;                                1 1 1 1 1 1 1
    ;;              1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6
    (set-seq-buf 0 [0 0 1 0 0 0 1 0 0 0 1 0 0 0 1 0])
    (set-seq-buf 1 [1 0 1 0 1 0 1 0 1 0 1 0])
    (set-seq-buf 2 [0 1 0 0 0 1 0 0 0 1 0 0])
    (set-seq-buf 3 [0 0 1 1 0 0 1 1])
    )

  ;; adjust tempo via
  (o/ctl @root-trg-atom :rate 130)

  ;; adjust sound via
  (set-seq-ctl 1 :sample-buf clap)
  (set-seq-ctl 1 :amp 0.25)
  (set-seq-ctl 1 :sample-rate 1.5)

)

;; ======================================================================
;; doodle #2 ...
(comment

  (restart 4 [12     12          8]
             [kick-s close-hihat kick-s])

  (do
    ;;                                1 1 1
    ;;              1 2 3 4 5 6 7 8 9 0 1 2
    (set-seq-buf 0 [1 0 1 0 1 0 1 0 1 0 1 0])
    (set-seq-buf 1 [0 1 0 1 0 1 0 1 0 1 0 1])
    (set-seq-buf 2 [1 0 1 0 1 0 1 0])
    )

  (do
    ;;                                1 1 1
    ;;              1 2 3 4 5 6 7 8 9 0 1 2
    (set-seq-buf 0 [1 0 0 1 1 0 1 0 1 0 0 1])
    (set-seq-buf 1 [0 1 0 0 1 1 0 1 0 0 1 1])
    (set-seq-buf 2 [1 0 1 1 0 0 1 1])
    )

  (o/stop)

  (get-root-cnt)
)

;; ======================================================================
;; quil for seeing what it looks like

(def colors {:background [238 173  14]
             :grid       [ 20  20  20]
             :note       [100 100 100]
             :play       [238 238 238]
             :time       [ 14 173 238]})
(def layout {:grid-offset  40
             :grid-width  400
             :cell-height  40
             :note-scale  0.7})

(defn setup []
  (q/smooth)
  (q/frame-rate 20))

(defn draw-grid []
  (apply q/stroke (colors :grid))
  (q/stroke-weight 1.5)
  (q/push-matrix)
  (q/translate (layout :grid-offset) (layout :grid-offset))
  (q/line 0 0 (layout :grid-width) 0)
  (doall
   (dotimes [i (count @seq-beats-atom)]
     (q/line 0 (layout :cell-height) (layout :grid-width) (layout :cell-height))
     (q/line 0 0 0 (layout :cell-height))
     (let [num-beats (@seq-beats-atom i)
           cell-width (/ (layout :grid-width) num-beats)]
       (doall
        (dotimes [j num-beats]
          (q/line (* (inc j) cell-width) 0 (* (inc j) cell-width) (layout :cell-height)))))
     (q/translate 0 (layout :cell-height))))
  (q/pop-matrix))

(defn draw-note-highlights []
  (apply q/stroke (colors :play))
  (q/stroke-weight 0.0)
  (apply q/fill (colors :play))
  (q/push-matrix)
  (q/translate (layout :grid-offset) (layout :grid-offset))
  (let [cur-time (get-root-cnt)
        grid-max-time (* DEFAULT-ROOT-RATE @seq-secs-atom)
        time-x (/ (mod cur-time grid-max-time) grid-max-time)
        playing-cells (apply vector (map #(int (* time-x %)) @seq-beats-atom))]
   (doall
    (dotimes [i (count @seq-beats-atom)]
      (let [num-beats (@seq-beats-atom i)
            cell-width (/ (layout :grid-width) num-beats)
            j (playing-cells i)]
        (q/rect (* j cell-width) 0 cell-width (layout :cell-height)))
      (q/translate 0 (layout :cell-height)))))
  (q/pop-matrix))

(defn draw-notes []
  (apply q/stroke (colors :note))
  (q/stroke-weight 0.0)
  (apply q/fill (colors :note))
  (q/push-matrix)
  (q/translate (layout :grid-offset) (layout :grid-offset))
  (doall
   (dotimes [i (count @seq-beats-atom)]
     (let [num-beats (@seq-beats-atom i)
           cell-width (/ (layout :grid-width) num-beats)
           note-width (* (layout :note-scale) cell-width)
           note-height (* (layout :note-scale) (layout :cell-height))
           note-size (min note-width note-height)]
       (doall
        (dotimes [j num-beats]
          (when (= 1 ((@seq-vecs-atom i) j))
            (q/ellipse (+ (* 0.5 cell-width) (* j cell-width)) (* 0.5 (layout :cell-height))
                       note-size note-size)))))
     (q/translate 0 (layout :cell-height))))
  (q/pop-matrix))

(defn draw-time []
  (let [cur-time (get-root-cnt)
        grid-max-time (* DEFAULT-ROOT-RATE @seq-secs-atom)
        time-x (* (/ (mod cur-time grid-max-time) grid-max-time)
                  (layout :grid-width))
        time-x (+ time-x (layout :grid-offset))]
  (apply q/stroke (colors :time))
  (q/stroke-weight 0.5)
  (q/line time-x (- (layout :grid-offset) 10)
          time-x (+ (* (layout :cell-height) (inc (count @seq-beats-atom))) 10))))

(defn draw []
  (apply q/background (colors :background))
  (draw-note-highlights)
  (draw-grid)
  (draw-notes)
  (draw-time)
  )

(defn run []
  (q/defsketch doodle :title "beatr" :setup setup :draw draw :size [600 600])
  nil)

;; see it go!
(comment
  (run)
)
