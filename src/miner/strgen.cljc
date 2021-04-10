(ns miner.strgen
  (:require [miner.strgen.impl :as impl]
            [clojure.test.check.generators :as gen]))

(defn string-generator
  "Returns a test.check generator that generates strings matching the given regular
  expression `regex`.  (Fancy flags and POSIX extensions are not suppored; see the doc for
  more information about the supported regular expression syntax.)  The optional
  `or-more-limit` controls the maximum numbers of elements that are generated when matching
  a potentially unbounded regex (such as #\"x*\" or #\"y+\").  The default is 9."
  
  ([regex] 
   (impl/string-generator regex))

  ([regex or-more-limit]
   (impl/string-generator regex or-more-limit)))


(defn case-insensitive-string-generator
  "Like `string-generator` but case-insensitive so it generates a mix of upper and lowercase
  characters for the given regex."
  
  ([regex]
   (gen/bind (string-generator regex) impl/gen-case-insensitive))

  ([regex or-more-limit]
   (gen/bind (string-generator regex or-more-limit) impl/gen-case-insensitive)))
