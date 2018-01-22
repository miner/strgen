(defproject com.velisco/strgen "0.1.6"
  :description "String generator from regular expressions, for use with Clojure test.check and spec"
  :url "https://github.com/miner/strgen"
  :deploy-repositories {"releases" :clojars}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.8"]]
  :doo {:build "sg"
        :alias {:default [:node]}}
  :cljsbuild {:builds [{:id "sg"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "target/testable.js"
                                   :output-dir "target"
                                   :target :nodejs
                                   :warnings true
                                  ;; :optimizations :whitespace
                                   :pretty-print true}}]}
;;  :hooks [leiningen.cljsbuild]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/test.check "0.10.0-alpha2"]])

