(ns beatr.core
  (:require [overtone.live :as o]
            [beatr.beatr :as b]
            [beatr.euclidian :as e]
            [beatr.gui :as bg]))

;; This file just shows what you can do with beatr

;; ======================================================================
;; start the gui (won't be very exciting until later...)
(comment
  (bg/run)
)

;; ======================================================================
;; load up a collection of samples to use
(def kick-s (o/sample (o/freesound-path 777)))
(def click-s (o/sample (o/freesound-path 406)))
(def snare (o/sample (o/freesound-path 26903)))
(def kick (o/sample (o/freesound-path 2086)))
(def close-hihat (o/sample (o/freesound-path 802)))
(def open-hihat (o/sample (o/freesound-path 26657)))
(def clap (o/sample (o/freesound-path 48310)))
(def clap2 (o/sample (o/freesound-path 132676)))
(def gshake (o/sample (o/freesound-path 113625)))

;; ======================================================================
;; doodle around...
(comment

  ;; ======================================================================
  ;; euclidian rhythms examples
  (b/restart 3 [12 12 8] [clap clap2 click-s])

  ;; play with the active beats and rotations here
  (b/beats 0 (e/euclidian-rhythm 5 12 1))
  (b/beats 1 (e/euclidian-rhythm 7 12 2))
  (b/beats 2 (e/euclidian-rhythm 3 8 3))

  (b/beats 0 (e/euclidian-rhythm 5 12 0))
  (b/beats 1 (e/euclidian-rhythm 5 12 1))
  (b/beats 2 (e/euclidian-rhythm 5 8 0))

  (b/beats 0 (e/euclidian-rhythm 7 12 0))
  (b/beats 1 (e/euclidian-rhythm 7 12 3))
  (b/beats 2 (e/euclidian-rhythm 5 8 0))

  (b/stop)

  ;; ======================================================================

  ;; Steve Reich "Clapping Music" example
  (defn rot
    "rotate a pattern by n"
    [pat n]
    (apply vector (take (count pat) (drop n (cycle pat)))))
  ;; (def pat [1 1 1 0 1 1 0 1 0 1 1 0]) the original pattern
  ;; well, how hard could this be?  :^) it's pi!
  (def pat [1 1 1 0 1 0 1 1 1 1 0 1])
  (def rotpat (partial rot pat))
  (b/restart 2 [(count pat) (count pat)] [clap clap2])
  (do
    (b/beats 0 pat)
    (b/beats 1 pat))
  ;; and change these in time...
  (b/beats 1 (rotpat 1))
  (b/beats 1 (rotpat 2))
  (b/beats 1 (rotpat 3))
  (b/beats 1 (rotpat 4))
  (b/beats 1 (rotpat 5))
  (b/beats 1 (rotpat 6))
  (b/beats 1 (rotpat 7))
  (b/beats 1 (rotpat 8))
  (b/beats 1 (rotpat 9))
  (b/beats 1 (rotpat 10))
  (b/beats 1 (rotpat 11))
  (b/beats 1 (rotpat 12))
  (b/stop)

  ;; ======================================================================

  ;; be like every other drum machine ...
  (b/restart 4
             [16 16 16]
             [kick-s open-hihat close-hihat])

  ;; give it a beat
  (do
    ;;                            1 1 1 1 1 1 1
    ;;          1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6
    (b/beats 0 [1 0 0 0 1 0 0 0 1 0 0 0 1 0 0 0])
    (b/beats 1 [0 0 1 0 0 0 1 0 0 0 1 0 0 0 1 0])
    (b/beats 2 [0 0 0 1 0 0 0 1 0 0 0 1 0 0 0 1])
    )

  ;; change it a bit
  (b/beats 2 [0 0 1 1 0 0 1 1 0 1 0 1 0 1 0 1])

  ;; ======================================================================

  ;; okay, let's be polyrhythmic
  (b/restart 4
             [16     12     12          8]
             [kick-s kick-s close-hihat open-hihat])

  (do
    (b/beats 0 [1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0])
    (b/beats 1 [1 0 1 0 1 0 1 0 1 0 1 0])
    (b/beats 2 [0 1 0 1 0 1 0 1 0 1 0 1])
    (b/beats 3 [0 1 0 1 0 1 0 1])
    )

  (do
    (b/beats 0 [1 0 0 0 1 0 0 0 1 0 0 0 1 0 0 0])
    (b/beats 1 [1 0 0 0 1 0 0 0 1 0 0 0])
    (b/beats 2 [0 0 0 1 0 0 0 1 0 0 0 1])
    (b/beats 3 [0 1 0 1 0 0 0 1])
    )

  (do
    (b/beats 0 [0 0 1 0 0 0 1 0 0 0 1 0 0 0 1 0])
    (b/beats 1 [1 0 1 0 1 0 1 0 1 0 1 0])
    (b/beats 2 [0 1 0 0 0 1 0 0 0 1 0 0])
    (b/beats 3 [0 0 1 1 0 0 1 1])
    )

  ;; adjust tempo via
  (b/tick-rate 180)
  (b/tick-rate 120) ; default

  ;; adjust sound via
  (b/ctl 1 :sample-buf clap)
  (b/ctl 1 :amp 0.25)
  (b/ctl 1 :sample-rate 1.5)

  ;; ======================================================================

  (b/restart 4
             [12     12          8]
             [kick-s close-hihat kick-s])

  (do
    (b/beats 0 [1 0 1 0 1 0 1 0 1 0 1 0])
    (b/beats 1 [0 1 0 1 0 1 0 1 0 1 0 1])
    (b/beats 2 [1 0 1 0 1 0 1 0])
    )

  (do
    (b/beats 0 [1 0 0 1 1 0 1 0 1 0 0 1])
    (b/beats 1 [0 1 0 0 1 1 0 1 0 0 1 1])
    (b/beats 2 [1 0 1 1 0 1 0 1])
    )

  ;; what time is it?
  (b/ticks)

  ;; ======================================================================

  ;; Try the new hotness...TB-303 clone
  (b/restart 4 [16 16] [b/beatr-303-synth kick-s])

  ;; give it a beat
  (do
    ;;                                     1  1  1  1  1  1  1
    ;;          1  2  3  4  5  6  7  8  9  0  1  2  3  4  5  6
    (b/beats 0 [40 44 32 42 40 47 40 42 32 44 40 42 40 47 32 42])
    (b/beats 1 [ 1  0  1  1  1  0  0  0  1  0  0  0  1  0  1  1])
    )

  (b/beats 0 [40 32 37 42 32 37  0  0 40 32 37 42 32 37  0 32])

  ;; try out the TB-303 controls
  (b/ctl 0 :wave 1)
  (b/ctl 0 :cutoff 800)
  (b/ctl 0 :env 2600)
  (b/ctl 0 :res 0.7)
  (b/ctl 0 :sus 0.1)
  (b/ctl 0 :dec 2.1)

  ;; stop the noise!
  (b/stop)

)
