(defproject com.velisco/strgen "0.1.8"
  :description "String generator from regular expressions, for use with Clojure test.check and spec"
  :url "https://github.com/miner/strgen"
  :deploy-repositories {"releases" :clojars}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [org.clojure/test.check "0.9.0"]])

