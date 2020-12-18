(defproject graph-traversal "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.generators "1.0.0"]
                 [expound "0.8.7"]]
  :repl-options {:init-ns graph-traversal.algorithms}
  :profiles {:test {:dependencies [[org.clojure/test.check "1.1.0"]
                                   [com.gfredericks/test.chuck "0.2.10"]
                                   [lambdaisland/kaocha "1.0.732"]]}}
  :aliases {"kaocha" ["with-profile" "+test" "run" "-m" "kaocha.runner"]})
