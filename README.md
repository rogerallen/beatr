# beatr

A server-side Overtone beat-box that easily handles polyrhythms.

![Screenshot](https://github.com/rogerallen/beatr/raw/master/beatr-anim.gif)

## Usage

Check out the examples in the src/beatr/core.clj directory.  Beatr is meant to be used via the repl.

In a nutshell:

```clj
;; start 4 sequences that fit in 4 seconds
;; each sequence has a different number of beats
(b/start 4
         [16     12     12          8]
         [kick-s kick-s close-hihat open-hihat])

;; add triggers like you see in the animation above
(b/set-seq-buf 0 [0 0 1 0 0 0 1 0 0 0 1 0 0 0 1 0])
(b/set-seq-buf 1 [1 0 1 0 1 0 1 0 1 0 1 0])
(b/set-seq-buf 2 [0 1 0 0 0 1 0 0 0 1 0 0])
(b/set-seq-buf 3 [0 0 1 1 0 0 1 1])
```

## License

Copyright © 2013 Roger Allen

Distributed under the Eclipse Public License, the same as Clojure.


[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/rogerallen/beatr/trend.png)](https://bitdeli.com/free "Bitdeli Badge")
