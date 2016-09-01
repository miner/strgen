(ns miner.test-strgen
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.generators :as gen]
            [clojure.spec :as s]
            [miner.strgen :as sg]))

(def ^:dynamic *exercise-limit* 5000)


(defn test-re
  ([re] (test-re re *exercise-limit*))
  ([re limit]
   (every? (partial re-matches re)
           (gen/sample (sg/string-generator re) limit))))

(deftest gen-regexes
  (is (test-re #"((s[a-z]*)|\d+)(x[a-j]y|y[^-A-Za-z]z|pq|PQ)\w@[^A-Zaz]"))
  ;; http://clojure.org/guides/spec example
  (is (test-re #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$"))
  (is (test-re #"\.\?\w\[\][-.?]")))


(defn test-spec-re
  ([re] (test-re re *exercise-limit*))
  ([re limit]
   (every? #(apply = %)
           (s/exercise (s/spec (s/and string? #(re-matches re %))
                               :gen #(sg/string-generator re))
                       limit))))

(deftest spec-regexes
  (is (test-re #"((s[a-z]*)|\d+)(x[a-j]y|y[^-A-Za-z]z|pq|PQ)\w@[^A-Zaz]"))
  ;; http://clojure.org/guides/spec example
  (is (test-re #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$"))
  (is (test-re #"\.\?\w\[\][-.?]")))

