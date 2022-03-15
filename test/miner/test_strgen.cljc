(ns miner.test-strgen
  (:require
   #?(:cljs [cljs.test :refer-macros [deftest is testing run-tests]]
      :clj  [clojure.test :refer [deftest is testing]])
            [clojure.test.check.generators :as gen]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [miner.strgen :as sg]))

;; https://github.com/bensu/doo -- need a test runner for CLJS

(def ^:dynamic *exercise-limit* 5000)

(def regexes [#"f.o"
              #"foo/bar"
              #"f.*o+"
              #":k[a-z]o"
              #":k[a-z]/f\d*o+"
              #"s[a-z]o"
              #"s[a-z]/f\d*o+"
              #"(foo|bar|(baz+|quux?){2})+a?b+"
              #"((s[a-z]*)|\d+)(x[a-j]y|y[^-A-Za-z]z|pq|PQ)\w@[^A-Zaz]"
              ;; email example from spec guide
              #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$"
              #"\.\?\w\[\][-.?]"
              #"[^\s]"
              #"[^\S]"
              #"[a\sb]"
              #"\{\|\}"
              #"[^\s/]+/[^\s/]+"
              #"^f\^*bar\$?x$"
              #"^(https?|ftp)://[^\s/$.?#].[^\s]*$" ])



(defn test-re
  ([re] (test-re re *exercise-limit*))
  ([re limit]
   (doseq [x (gen/sample (sg/string-generator re) limit)]
     (is (re-matches re x)))))


(deftest show-info
  (testing "Show test info"
    (println)
    (println "  ** Test Info **")
    (println "  StrGen" (nth (clojure.edn/read-string (slurp  "project.clj")) 2))
    (println "  Clojure" (clojure-version))
    (println "  Java" (System/getProperty "java.version"))
    (println)
    true))

(deftest gen-regexes
  (doseq [re regexes]
    (test-re re)))


(defn test-spec-re
  ([re] (test-re re *exercise-limit*))
  ([re limit]
   (doseq [[r c] (s/exercise (s/spec (s/and string? #(re-matches re %))
                                     :gen #(sg/string-generator re))
                             limit)]
     (is (= r c)))))


(deftest spec-regexes
  (doseq [re regexes]
    (test-spec-re re)))


;; SEM FIXME: Not a complete test as we convert everything back to lowercase before
;; matching.  Could break if we changed `regexs` to require uppercase.  We should also
;; validate that the generator yields sensible mixed case results.
(defn test-spec-re-case-insensitive
  ([re] (test-re re *exercise-limit*))
  ([re limit]
   (doseq [[r c] (s/exercise (s/spec (s/and string? #(re-matches re (str/lower-case %)))
                                     :gen #(sg/case-insensitive-string-generator re))
                             limit)]
     (is (= r c)))))

(deftest spec-regexes-case-insensitive
  (doseq [re regexes]
    (test-spec-re-case-insensitive re)))

