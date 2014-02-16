(ns beatr.euclidian)

;; Found inspriration on this blog.
;;   http://ruinwesen.com/blog?id=216
;; which references this paper
;;   http://cgm.cs.mcgill.ca/~godfried/publications/banff.pdf
;; which describes the idea generating rhythms via an algorithm first used
;; for spallation neutron source accelerators.  This algorithm is related
;; to an algorithm first described by Euclid.  Rhythms produced via this
;; method are very similar to historic rhythms used across the world.

(defn- seq-split-remainder
  [s]
  (partition-by #(= (first s) %1) s))

(defn- interleave-seqs
  [fs rs]
  (let [int-seq (map #(concat %1 %2) fs rs)
        rem-fs (drop (count rs) fs)
        rem-rs (drop (count fs) rs)]
    (concat int-seq rem-fs rem-rs)))

(defn- bjorklund
  "Recursively distribute remainder sequence over the real sequence via algorithm
  Bjorklund used for the timing generation in neutron accelerators."
  [s]
  (loop [ss s]
    (let [ssr (seq-split-remainder ss)
          real (first ssr)
          remainder (second ssr)]
      (if (<= (count remainder) 1)
        (concat real remainder)
        (recur (interleave-seqs real remainder))))))

(defn- rotate
  "rotate a sequence s by n, returning a vector"
  [s n]
  (let [c (count s)]
    (vec (take c (drop (mod n c) (cycle s))))))

(defn euclidian-rhythm
  "distribute k 1s in a sequence of length n, distributing them in a most-even way.
  an optional rotation by r is provided."
  ([k n]
   (euclidian-rhythm k n 0))
  ([k n r]
   (let [ones (repeat k 1)
         zeros (repeat (- n k) 0)]
     (rotate (vec (flatten (bjorklund (map list (concat ones zeros))))) r))))

(comment
  ;; testing examples from the paper.
  ;; these should all return true, but the
  ;; exceptions look reasonable to me and we can rotate to fix
  (= (euclidian-rhythm 1 2)   [1 0])
  (= (euclidian-rhythm 1 3)   [1 0 0])
  (= (euclidian-rhythm 1 4)   [1 0 0 0])
  (= (euclidian-rhythm 2 3 1) [1 0 1])
  (= (euclidian-rhythm 2 5)   [1 0 1 0 0])
  (= (euclidian-rhythm 3 4 2) [1 0 1 1])
  (= (euclidian-rhythm 3 5)   [1 0 1 0 1])
  (= (euclidian-rhythm 3 7)   [1 0 1 0 1 0 0])
  (= (euclidian-rhythm 3 8)   [1 0 0 1 0 0 1 0])
  (= (euclidian-rhythm 4 7)   [1 0 1 0 1 0 1])
  (= (euclidian-rhythm 4 11)  [1 0 0 1 0 0 1 0 0 1 0])
  (= (euclidian-rhythm 4 12)  [1 0 0 1 0 0 1 0 0 1 0 0])
  (= (euclidian-rhythm 5 7)   [1 0 1 1 0 1 1])
  (= (euclidian-rhythm 5 8)   [1 0 1 1 0 1 1 0])
  (= (euclidian-rhythm 5 9)   [1 0 1 0 1 0 1 0 1])
  (= (euclidian-rhythm 5 11)  [1 0 1 0 1 0 1 0 1 0 0])
  (= (euclidian-rhythm 5 12)  [1 0 0 1 0 1 0 0 1 0 1 0])
  (= (euclidian-rhythm 7 8 6) [1 0 1 1 1 1 1 1])
  (= (euclidian-rhythm 7 12)  [1 0 1 1 0 1 0 1 1 0 1 0])
  (= (euclidian-rhythm 7 16)  [1 0 0 1 0 1 0 1 0 0 1 0 1 0 1 0])
  (= (euclidian-rhythm 9 16)  [1 0 1 1 0 1 0 1 0 1 1 0 1 0 1 0])
  (= (euclidian-rhythm 11 24) [1 0 0 1 0 1 0 1 0 1 0 1 0 0 1 0 1 0 1 0 1 0 1 0])
  (= (euclidian-rhythm 13 24) [1 0 1 1 0 1 0 1 0 1 0 1 0 1 1 0 1 0 1 0 1 0 1 0])
  )
