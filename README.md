# beatr

A Overtone beat-box that easily handles polyrhythms and displays the
beats in a Quil GUI.  Timing is done on the Supercollider server side
a la Sam Aaron's
[internal_sequencer](https://github.com/overtone/overtone/blob/master/src/overtone/examples/timing/internal_sequencer.clj)
example.  Note that beatr is meant to be used via the repl, not via
GUI input.

![Screenshot](https://github.com/rogerallen/beatr/raw/master/beatr-anim.gif)

## Usage

Check out the examples in the src/beatr/core.clj directory.

In a nutshell:

```clj
;; start an array of 4 sequences that fit in 4.5 seconds
;; each sequence has a different number of beats
(b/restart 4.5
           [16     12     12          8]
           [kick-s kick-s close-hihat open-hihat])

;; add triggers like you see in the animation above
(b/beats 0 [0 0 1 0 0 0 1 0 0 0 1 0 0 0 1 0])
(b/beats 1 [1 0 1 0 1 0 1 0 1 0 1 0])
(b/beats 2 [0 1 0 0 0 1 0 0 0 1 0 0])
(b/beats 3 [0 0 1 1 0 0 1 1])
```

## License

Copyright © 2013 Roger Allen

Distributed under the Eclipse Public License, the same as Clojure.


[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/rogerallen/beatr/trend.png)](https://bitdeli.com/free "Bitdeli Badge")
