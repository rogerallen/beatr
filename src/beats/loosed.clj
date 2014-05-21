(ns beats.loosed
  (:require [overtone.live :as o]
            [overtone.music.pitch :as omp]
            [beatr.beatr :as b]
            [beatr.gui :as bg]))

;; ======================================================================
;; samples
(def ROOT "/Users/rallen/Music/Samples/Survival Drum Kit/")
(defn sample [filename]
  (o/sample (str ROOT filename)))

(def snare (sample "2 Snares/Survival -  Snare  (30).wav"))
(def kick  (sample "1 Kicks/Survival - Kick  (17).wav"))
(def clap1 (sample "3 Claps/Survival - Clap  (4).wav"))
(def clap2 (sample "3 Claps/Survival - Clap  (5).wav"))
(def crash (sample "6 Other/Survival - Crash  (6).wav"))

;; ======================================================================
;; state
(defn zb [n] (vec (repeat n 0)))
(defonce num-beats (atom [8 8 8 8 8 16 16]))
(defonce melody    (atom (zb 16)))
(defonce bass      (atom (zb 16)))
(defonce the-key  (atom {:tonic :d4
                         :scale :pentatonic}))
(defn adjust-melody-and-bass
  [watch-key a old-val new-val]
  (let [new-melody (vec (omp/degrees->pitches @melody
                                              (:scale @the-key)
                                              (:tonic @the-key)))
        new-melody (b/dovec  (map #(if (> %2 0) %1 0) new-melody @melody))
        new-bass   (vec (omp/degrees->pitches @bass
                                              (:scale @the-key)
                                              (:tonic @the-key)))
        new-bass   (b/dovec (map #(if (> %2 0) (- %1 12) 0) new-bass @bass))
        ]
    (b/beats 5 new-melody)
    (b/beats 6 new-bass)))
(add-watch the-key :key-change adjust-melody-and-bass)
(add-watch melody :key-change adjust-melody-and-bass)
(add-watch bass :key-change adjust-melody-and-bass)

;; ======================================================================
;; helpers
(defn m! [v] (reset! melody v))
(defn b! [v] (reset! bass v))
(defn k! [t s] (reset! the-key {:scale s :tonic t}))

(defn zbi [i] (zb (@num-beats i)))

(defn m-! [] (reset! melody (zbi 5)))
(defn b-! [] (reset! bass (zbi 6)))

(defn zap []
  (b/restart (b/duration 100 8)
             @num-beats
             [kick snare        ;; 0 1
              clap1 clap2 crash ;; 2 3 4
              b/beatr-303-synth ;; 5
              b/beatr-303-synth ;; 6
              ])
  (doall
   (dotimes [i 7]
     (b/beats i (zb (@num-beats i))))))

(defn s0 []
  (b/beats 0 [1 0 1 0 1 0 1 0])
  (b/beats 1 [0 1 0 1 0 1 0 1]))

(defn s1 []
    (b/beats 2 [0 0 0 0 0 1 0 1])
    (b/beats 3 [0 1 0 1 0 0 0 0]))

(defn s1- []
  (b/beats 2 (zbi 2))
  (b/beats 3 (zbi 3)))

(defn s2 []
  (b/beats 4 [1 0 0 0 0 0 0 0]))

(defn s2- []
  (b/beats 4 (zbi 4)))

;; ======================================================================
;; playground
(comment
  (bg/run)

  (zap)

  (s0)
  (s1)  (s1-)
  (s2)  (s2-)

  (m! [0 4 2 1 0 2 3 4 0 4 2 1 0 2 3 4])
  (m! [0 1 2 4 0 1 1 4 0 2 2 3 0 3 3 2])
  (m! [0 4 0 2 0 3 0 1 0 3 0 1 0 1 0 1])
  (m! [0 4 2 1 0 0 0 0 0 4 2 1 0 0 0 0])
  (m! [0 4 0 1 0 0 0 0 0 4 0 1 0 0 0 0])
  (m-!)

  (b! [1 0 3 4 1 0 0 2 1 0 3 4 1 0 0 2])
  (b! [1 0 4 0 1 0 2 0 1 0 4 0 4 2 3 1])
  (b! [1 0 0 0 1 0 0 0 1 0 0 0 1 0 3 1])
  (b-!)

  ;; deriving the degree
  ;; (map omp/find-note-name (omp/scale :d4 :pentatonic))
  ;; :D4 :E4 :G4 :A4 :B4 :D5 :E5 :G5
  (k! :d4 :pentatonic)       ; :i
  (k! :a3 :egyptian)         ; :iv
  (k! :b3 :jiao)             ; ;v
  (k! :e4 :minor-pentatonic) ; :ii
  (k! :g4 :major-pentatonic) ; :iii

  (b/stop)
)
