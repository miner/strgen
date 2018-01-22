(ns miner.test-strgen
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.generators :as gen]
            [clojure.spec.alpha :as s]
            [miner.strgen :as sg]))

(def ^:dynamic *exercise-limit* 5000)

(def regexes [#"f.o"
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
              #"[^\s/]+/[^\s/]+"
              #"^(https?|ftp)://[^\s/$.?#].[^\s]*$" ])



(defn test-re
  ([re] (test-re re *exercise-limit*))
  ([re limit]
   (doseq [x (gen/sample (sg/string-generator re) limit)]
     (is (re-matches re x)))))

           

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



