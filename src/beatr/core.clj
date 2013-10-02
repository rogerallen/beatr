(ns beatr.core
  (:require [overtone.live :as o]
            [beatr.beatr :as b]
            [beatr.gui :as bg]))

;; This file just shows what you can do with beatr

;; ======================================================================
;; start the gui
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
  (b/start 4
           [16 16 16]
           [kick-s open-hihat close-hihat])

  ;; give it a beat
  (do
    ;;                                  1 1 1 1 1 1 1
    ;;                1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6
    (b/set-seq-buf 0 [1 0 0 0 1 0 0 0 1 0 0 0 1 0 0 0])
    (b/set-seq-buf 1 [0 0 1 0 0 0 1 0 0 0 1 0 0 0 1 0])
    (b/set-seq-buf 2 [0 0 0 1 0 0 0 1 0 0 0 1 0 0 0 1])
    )

  ;; change it a bit
  (b/set-seq-buf 2 [0 0 1 1 0 0 1 1 0 1 0 1 0 1 0 1])

  ;; ======================================================================

  ;; okay, let's be polyrhythmic
  (b/start 4
           [16     12     12          8]
           [kick-s kick-s close-hihat open-hihat])

  (do
    ;;                                1 1 1 1 1 1 1
    ;;              1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6
    (b/set-seq-buf 0 [1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0])
    (b/set-seq-buf 1 [1 0 1 0 1 0 1 0 1 0 1 0])
    (b/set-seq-buf 2 [0 1 0 1 0 1 0 1 0 1 0 1])
    (b/set-seq-buf 3 [0 1 0 1 0 1 0 1])
    )

  (do
    ;;                                1 1 1 1 1 1 1
    ;;              1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6
    (b/set-seq-buf 0 [1 0 0 0 1 0 0 0 1 0 0 0 1 0 0 0])
    (b/set-seq-buf 1 [1 0 0 0 1 0 0 0 1 0 0 0])
    (b/set-seq-buf 2 [0 0 0 1 0 0 0 1 0 0 0 1])
    (b/set-seq-buf 3 [0 1 0 1 0 0 0 1])
    )

  (do
    ;;                                1 1 1 1 1 1 1
    ;;              1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6
    (b/set-seq-buf 0 [0 0 1 0 0 0 1 0 0 0 1 0 0 0 1 0])
    (b/set-seq-buf 1 [1 0 1 0 1 0 1 0 1 0 1 0])
    (b/set-seq-buf 2 [0 1 0 0 0 1 0 0 0 1 0 0])
    (b/set-seq-buf 3 [0 0 1 1 0 0 1 1])
    )

  ;; adjust tempo via
  (o/ctl @b/root-trg-atom :rate 240)
  (o/ctl @b/root-trg-atom :rate 120)

  ;; adjust sound via
  (b/set-seq-ctl 1 :sample-buf clap)
  (b/set-seq-ctl 1 :amp 0.25)
  (b/set-seq-ctl 1 :sample-rate 1.5)


  ;; ======================================================================

  (b/start 4
           [12     12          8]
           [kick-s close-hihat kick-s])

  (do
    ;;                                1 1 1
    ;;              1 2 3 4 5 6 7 8 9 0 1 2
    (b/set-seq-buf 0 [1 0 1 0 1 0 1 0 1 0 1 0])
    (b/set-seq-buf 1 [0 1 0 1 0 1 0 1 0 1 0 1])
    (b/set-seq-buf 2 [1 0 1 0 1 0 1 0])
    )

  (do
    ;;                                1 1 1
    ;;              1 2 3 4 5 6 7 8 9 0 1 2
    (b/set-seq-buf 0 [1 0 0 1 1 0 1 0 1 0 0 1])
    (b/set-seq-buf 1 [0 1 0 0 1 1 0 1 0 0 1 1])
    (b/set-seq-buf 2 [1 0 1 1 0 1 0 1])
    )

  ;; what time is it?
  (b/get-root-cnt)

  ;; stop the noise!
  (b/stop)

)
