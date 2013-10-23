(defproject org.clojars.dparis/dire "0.5.0"
  :description "Erlang-style supervisor error handling for Clojure"
  :url "https://github.com/MichaelDrogalis/dire"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [slingshot "0.10.3"]
                 [robert/hooke "1.3.0"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]
                                  [bultitude "0.1.7"]]}}
  :plugins [[lein-midje "3.1.1"]
            [codox "0.6.4"]]
  :aliases {"midje-test" ["with-profile" "dev" "midje"]})
