(ns miner.strgen
  (:require [miner.strgen.impl :as impl]))

(defn string-generator
  "Returns a test.check generator that generates strings matching the given regular
  expression `regex`.  (Fancy flags and POXIX extensions are not suppored; see the doc for
  more information about the supported regular expression syntax.)  The optional
  `or-more-limit` controls the maximum numbers of elements that are generated when matching
  a potentially unbounded regex (such as #\"x*\" or #\"y+\").  The default is 9."
  
  ([regex] 
   (impl/string-generator regex))

  ([regex or-more-limit]
   (impl/string-generator regex or-more-limit)))

