(ns beatr.core
  (:require [overtone.live :as o]
            [beatr.beatr :as b]
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
(def gshake (o/sample (o/freesound-path 113625)))

;; ======================================================================
;; doodle around...
(comment

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

  ;; stop the noise!
  (b/stop)

)
