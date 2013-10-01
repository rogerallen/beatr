(defproject beatr "0.1.0-SNAPSHOT"
  :description "an overtone beatbox"
  :url "http://github.com/rogerallen/beatr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [overtone "0.8.1" :exclusions [org.clojure/clojure]]
                 [quil "1.6.0" :exclusions [org.clojure/clojure]]])
