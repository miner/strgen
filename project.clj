(defproject com.velisco/strgen "0.2.2"
  :description "String generator from regular expressions, for use with Clojure test.check and spec"
  :url "https://github.com/miner/strgen"
  :deploy-repositories {"releases" :clojars}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.844"]
                 ;;[org.clojure/spec.alpha "0.2.194"]
                 [org.clojure/test.check "1.1.0"]]
  :repositories {"sonatype-snapshot"
                 {:url "https://oss.sonatype.org/content/repositories/snapshots"}})


