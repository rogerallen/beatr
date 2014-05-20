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
;; (get-rate-divisor 3 4 315)

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

;; TB-303 clone starting point from Dan Stowell
;; http://permalink.gmane.org/gmane.comp.audio.supercollider.user/22591
;; SynthDef("sc303", { arg out=0, freq=440, wave=0, ctf=100, res=0.2,
;; 		sus=0, dec=1.0, env=1000, gate=0, vol=0.2;
;; 	var filEnv, volEnv, waves;
;;
;; 	// can't use adsr with exp curve???
;; 	//volEnv = EnvGen.ar(Env.adsr(1, 0, 1, dec, vol, 'exp'), In.kr(bus));
;; 	volEnv = EnvGen.ar(Env.new([10e-10, 1, 1, 10e-10], [0.01, sus, dec], 'exp'), gate);
;; 	filEnv = EnvGen.ar(Env.new([10e-10, 1, 10e-10], [0.01, dec], 'exp'), gate);
;;
;; 	waves = [Saw.ar(freq, volEnv), Pulse.ar(freq, 0.5, volEnv)];
;;
;; 	Out.ar(out, RLPF.ar( Select.ar(wave, waves), ctf + (filEnv * env), res).dup * vol);
;; }).send(s);

(o/defsynth beatr-303-synth
  "Plays a TB-303 clone. http://en.wikipedia.org/wiki/Roland_TB-303"
  [beat-num     0 ; the beat number of this synth
   beat-buf     0 ; the buffer of all beats 0=off, N=on and N is midi note value
   seq-beats    8 ; the number of beats in a sequence
   beat-cnt-bus 0 ; the beat count bus
   beat-trg-bus 0 ; the beat trigger bus
   amp          1 ; output volume
   ;; TB-303 controls...
   wave         0 ; 0=tri,1=square
   cutoff       100
   env          1000
   res          0.2
   sus          0
   dec          1.0
   out-bus      0]
  (let [cnt           (o/in:kr beat-cnt-bus)
        beat-trg      (o/in:kr beat-trg-bus)
        note-val      (o/buf-rd:kr 1 beat-buf cnt)
        freq-val      (o/midicps note-val)
        this-beat-trg (and (> note-val 0)
                           (= beat-num (mod cnt seq-beats))
                           beat-trg)
        vol           (o/set-reset-ff this-beat-trg)
        ;; tb303...
        vol-env (o/env-gen (o/envelope [10e-10, 1, 1, 10e-10] [0.01, sus, dec] :exp) this-beat-trg)
        filter-env (o/env-gen (o/envelope [10e-10, 1, 10e-10] [0.01, dec] :exp) this-beat-trg)
        waves   [(* (o/saw freq-val) vol-env) (* (o/pulse freq-val 0.5) vol-env)]
        tb303   (o/rlpf (o/select wave waves) (+ cutoff (* filter-env env)) res)
        ]
    (o/out out-bus (* vol amp (o/pan2 tb303)))))

;; ======================================================================
;; control atoms
(defonce root-rate-atom (atom DEFAULT-ROOT-RATE))
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
;; Public API routines.
(defn duration
  "return beat duration in seconds, given the tempo (bpm) and number of beats in a sequence.  E.g. (duration 120 8) => 4.0 seconds."
  [tempo num-beats]
  (* num-beats (/ 60.0 tempo)))

;; FIXME – I think this leaks the old array of synths
(defn restart
  "Setup an array of sequences.  The seconds arg tells how long
   all the sequences in the array lasts.  Each sequence can have a
   different number of beats in order to play polyrhythmic beats.  The
   seq-beats arg is a vector of the number of beats in each sequence.
   The seq-synths arg is a vector of Overtone synths for each
   sequence.

   Ex: (start 4 [16 12] [bass-drum snare-drum])

   is an array of beats that repeats after 4 seconds.  The bass-drum
   sequence has 16 beats over 4 seconds (240bpm) and the snare-drum
   has 12 beats over 4 seconds for 180bpm."
  [seconds
   seq-beats
   seq-synths]
  (swap! active-atom (fn [_] false))
  (o/stop) ;; kill anything currently playing on the server
  (let [beat-set (apply vector (sort (set seq-beats)))]
    (assert (>= MAX-BEAT-BUSES (count beat-set)))
    (assert (= (count seq-beats) (count seq-synths)))

    (swap! seq-secs-atom (fn [_] seconds))
    (swap! seq-beats-atom (fn [_] seq-beats))
    (swap! root-trg-atom (fn [_] (root-trg-synth @root-rate-atom)))
    (swap! root-cnt-atom (fn [_] (root-cnt-synth)))
    (swap! beat-trgs-atom
           (fn [_] (dovec (for [i (range (count beat-set))]
                           (let [beat-trg-bus (nth beat-trg-buses i)
                                 num-beats    (nth beat-set i)
                                 cur-divisor  (get-rate-divisor num-beats seconds @root-rate-atom)]
                             (beat-trg-synth beat-trg-bus cur-divisor))))))
    (swap! beat-cnts-atom
           (fn [_] (dovec (for [i (range (count beat-set))]
                           (let [beat-trg-bus (nth beat-trg-buses i)
                                 beat-cnt-bus (nth beat-cnt-buses i)]
                             (beat-cnt-synth beat-cnt-bus beat-trg-bus))))))
    (swap! seq-bufs-atom
           (fn [_] (dovec (for [num-beats seq-beats]
                           (o/buffer num-beats)))))
    (swap! seq-vecs-atom
           (fn [_] (dovec (for [num-beats seq-beats]
                           (apply vector (repeat num-beats 0))))))

    (swap! synth-seqs-atom
           (fn [_] (dovec (for [[seq-index num-beats] (map-indexed vector seq-beats)]
                           (let [beat-index (.indexOf beat-set num-beats)
                                 seq-buf      (nth @seq-bufs-atom seq-index)
                                 beat-cnt-bus (nth beat-cnt-buses beat-index)
                                 beat-trg-bus (nth beat-trg-buses beat-index)
                                 synth        (nth seq-synths seq-index)
                                 play-sample? (= (type synth) overtone.sc.sample.PlayableSample)
                                                ]
                             (dovec
                              (for [k (range num-beats)]
                                (if play-sample?
                                  (mono-sample-beat-synth
                                   :beat-num     k
                                   :beat-buf     seq-buf
                                   :seq-beats    num-beats
                                   :beat-cnt-bus beat-cnt-bus
                                   :beat-trg-bus beat-trg-bus
                                   :sample-buf   synth)
                                  (synth
                                   :beat-num     k
                                   :beat-buf     seq-buf
                                   :seq-beats    num-beats
                                   :beat-cnt-bus beat-cnt-bus
                                   :beat-trg-bus beat-trg-bus))))))))))
  (swap! active-atom (fn [_] true)))

(defn ctl
  "send a ctl key value pair to the seq-index synth in the array.
   Ex: (ctl 3 :sample-buf clap)"
  [seq-index key value]
  (doall (map #(o/ctl % key value) (@synth-seqs-atom seq-index)))
  nil)

(defn beats
  "send an array of beats to the nth sequence in the array.  1
  indicates a 'beat' and 0 indicates no 'beat'
  Ex: (beats 1 [0 0 1 1 0 0 1 0])"
  [seq-index new-buf]
  (assert (< seq-index (count @seq-beats-atom)))
  (assert (= (count new-buf) (nth @seq-beats-atom seq-index)))
  (o/buffer-write! (nth @seq-bufs-atom seq-index) new-buf)
  (swap! seq-vecs-atom (fn [xs] (assoc xs seq-index new-buf)))
  nil)

(defn ticks
  "return the current value of the root-cnt-atom a/k/a the 'tick' that
  all other timing is based upon.  Returns 0.0 if nothing is running."
  []
  (if-not (nil? @root-cnt-atom)
    @(get-in @root-cnt-atom [:taps "cur-root-cnt"])
    0.0))

(defn tick-rate
  "Set the root tick rate.  The default rate is 120Hz."
  [hz]
  (swap! root-rate-atom (fn [_] hz))
  (o/ctl @root-trg-atom :rate hz))

;; FIXME -- pause instead of stop?
(defn stop
  "stop Overtone"
  []
  (o/stop))
