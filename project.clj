(defproject dire "0.1.3-SNAPSHOT"
  :description "Erlang-style supervisor error handling for Clojure"
  :url "https://github.com/MichaelDrogalis/dire"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"stuart" "http://stuartsierra.com/maven2"}  
  :dependencies [[org.clojure/clojure "1.5.0-RC1"]
                 [org.clojure/tools.nrepl "0.2.0-RC1"]
                 [midje "1.4.0"]
                 [com.stuartsierra/lazytest "1.2.3"]]
  :plugins [[lein-midje "2.0.3"]])

