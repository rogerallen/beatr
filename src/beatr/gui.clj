(ns beatr.gui
  (:require [quil.core :as q]
            [beatr.beatr :as b]))

;; ======================================================================
;; quil for seeing what it looks like

;; a goldenrod colorscheme
(def colors {:clear-color     [128 128 128]
             :background      [205 149  12]
             :grid-background [238 173  14]
             :play            [255 193  37]
             :time            [238 221 130]
             :grid            [139  26  26]
             :note            [139  58  58]
             })

(def layout {:grid-offset  20
             :grid-width  600
             :cell-height  40
             :note-scale  0.618
             })

(defn setup []
  (q/smooth)
  (q/frame-rate 20)) ;; normally 20

(defn draw-grid []
  (apply q/stroke (colors :grid))
  (q/stroke-weight 1.5)
  (q/push-matrix)
  (q/translate (layout :grid-offset) (layout :grid-offset))
  (q/line 0 0 (layout :grid-width) 0)
  (doall
   (dotimes [i (count @b/seq-beats-atom)]
     (q/line 0 (layout :cell-height) (layout :grid-width) (layout :cell-height))
     (q/line 0 0 0 (layout :cell-height))
     (let [num-beats (@b/seq-beats-atom i)
           cell-width (/ (layout :grid-width) num-beats)]
       (doall
        (dotimes [j num-beats]
          (q/line (* (inc j) cell-width) 0 (* (inc j) cell-width) (layout :cell-height)))))
     (q/translate 0 (layout :cell-height))))
  (q/pop-matrix))

(defn draw-note-highlights []
  (let [cur-time (b/get-root-cnt)]
    (let [grid-max-time (* b/DEFAULT-ROOT-RATE @b/seq-secs-atom)
          time-x (/ (mod cur-time grid-max-time) grid-max-time)
          playing-cells (apply vector (map #(int (* time-x %)) @b/seq-beats-atom))]
      (apply q/stroke (colors :play))
      (q/stroke-weight 0.0)
      (apply q/fill (colors :play))
      (q/push-matrix)
      (q/translate (layout :grid-offset) (layout :grid-offset))
      (doall
       (dotimes [i (count @b/seq-beats-atom)]
         (let [num-beats (@b/seq-beats-atom i)
               cell-width (/ (layout :grid-width) num-beats)
               j (playing-cells i)]
           (q/rect (* j cell-width) 0 cell-width (layout :cell-height)))
         (q/translate 0 (layout :cell-height))))
      (q/pop-matrix))))

(defn draw-notes []
  (apply q/stroke (colors :note))
  (q/stroke-weight 0.0)
  (apply q/fill (colors :note))
  (q/push-matrix)
  (q/translate (layout :grid-offset) (layout :grid-offset))
  (doall
   (dotimes [i (count @b/seq-beats-atom)]
     (let [num-beats (@b/seq-beats-atom i)
           cell-width (/ (layout :grid-width) num-beats)
           note-width (* (layout :note-scale) cell-width)
           note-height (* (layout :note-scale) (layout :cell-height))
           note-size (min note-width note-height)]
       (doall
        (dotimes [j num-beats]
          (when (= 1 ((@b/seq-vecs-atom i) j))
            (q/ellipse (+ (* 0.5 cell-width) (* j cell-width)) (* 0.5 (layout :cell-height))
                       note-size note-size)))))
     (q/translate 0 (layout :cell-height))))
  (q/pop-matrix))

(defn draw-time []
  (let [cur-time (b/get-root-cnt)]
    (let [ grid-max-time (* b/DEFAULT-ROOT-RATE @b/seq-secs-atom)
          time-x (* (/ (mod cur-time grid-max-time) grid-max-time)
                    (layout :grid-width))
          time-x (+ time-x (layout :grid-offset))]
      (apply q/stroke (colors :time))
      (q/stroke-weight 0.5)
      (q/line time-x (- (layout :grid-offset) 10)
              time-x (+ (layout :grid-offset) (* (layout :cell-height) (count @b/seq-beats-atom)) 10)))))

(defn draw-background []
  (apply q/background (colors :clear-color))
  (apply q/stroke (colors :background))
  (q/stroke-weight 0.0)
  (apply q/fill (colors :background))
  ;; ideally would set this to be window width, height FIXME
  (q/rect 0 0
          (+ (* 2 (layout :grid-offset)) (layout :grid-width))
          (+ (* 2 (layout :grid-offset)) (* (layout :cell-height)
                                            (count @b/seq-beats-atom))))
  (q/push-matrix)
  (q/translate (layout :grid-offset) (layout :grid-offset))
  (apply q/fill (colors :grid-background))
  (q/rect 0 0
          (layout :grid-width)  (* (layout :cell-height)
                                   (count @b/seq-beats-atom)))
  (q/pop-matrix))

(def do-dump false)

(defn draw []
  (draw-background)
  (when @b/active-atom
    (draw-note-highlights)
    (draw-grid)
    (draw-notes)
    (draw-time))
  (if do-dump (q/save-frame "beatr-dump-###.png")))

(defn run []
  (q/defsketch doodle :title "beatr" :setup setup :draw draw :size [640 480])
  nil)
