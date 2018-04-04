(ns miner.strgen.impl
  (:require [clojure.set :as set]
            #?(:cljs cljs.reader)
            [clojure.test.check.generators :as gen]))

;; All of this is considered private

;; Minimal regex features
;; *?+ . [abc] [a-z] [^a] \n (a|b) \w \W \d \D \s \S
;; x{N} x{N,} x{N,M} -- number of matches
;; ^x$ works by ignoring ^ as first, and $ as last

;; not supported: \1, (capture group), [:alnum:], fancy flags, fancy quantifiers
;; http://en.wikipedia.org/wiki/Regular_expression

;; Try test.chuck for a better string-from-regex-generator, but it has some dependecies that
;; will not allow it to be used in a contrib library.
;; https://github.com/gfredericks/test.chuck

;; For regex * + {N,} there is technically no limit to how many items might match "or
;; more".  For purposes of generating strings, we limit the or-more items to
;; *or-more-limit*.

;; Note: the intention is to capture the value of *or-more-limit* at the time of parsing the
;; regex -- that is, when `string-generator` is called with a second argument for
;; `or-more-limit`.  Binding *or-more-limit* should not affect the results of executing a
;; saved generator at a later time.

(def ^:dynamic *or-more-limit* 9)

(declare parse-chars)

(defn slash [c]
  (case c
    \d '(:digit)
    \D '(:not-digit)
    \w '(:word)
    \W '(:not-word)
    \s '(:space)
    \S '(:not-space)
    \t '(:tab)
    \n '(:newline)
    \r '(:return)
    (\[ \] \* \+ \. \? \\ \( \) \/ \$ \^) c
    (throw (ex-info (str "Unsupported backslash char " c) {:unsupported-backslash c}))))

(defn parse-set-contents [cs result]
  ;;(println "parse-set-contents  " result "  " (first cs))
  (case (first cs)
    nil (throw (ex-info "Unterminated [set]" {:error :unterminated-set :partial result}))
    \] (if (not (seq result))
         (recur (rest cs) (conj result \]))
         [(list* :set result) (rest cs)])
    \- (if (or (not (seq result)) (= \] (second cs)))
         (recur (rest cs) (conj result \-))
         (recur (rest (rest cs)) (conj (pop result) (list :btw (peek result) (second cs)))))
    \\ (if (= \] (second cs))
         (recur (rest cs) (conj result \\))
         (recur (rest (rest cs)) (conj result (slash (second cs)))))
    (recur (rest cs) (conj result (first cs)))))

;; already consumed first [
(defn parse-set [cs]
  (case (first cs)
    \^ (let [[setexp rst] (parse-set-contents (rest cs) [])]
         [(list* :inverted (rest setexp)) rst])    
    (parse-set-contents cs [])))

(defn read-digits [ds]
  (when (seq ds)
    #?(:clj  (Long/parseLong (apply str ds))
       :cljs (cljs.reader/read-string (apply str ds)))))

;; parse {N,M} where { is already consumed
;; but ,M is optional
;; Note subtle distinction between exactly {N} no comma, and N-or-more {N,}
;; For purposes of gen, we cap unspecified N-or-more at (+ N *or-more-limit*)

;; returns vector of [N, M (possibly nil), rest-of-cs]
(defn parse-times [cs]
  (loop [digits [] n nil comma false cs cs]
    (case (first cs)
      \} (if n
           [n (when comma (or (read-digits digits) (+ n *or-more-limit*))) (rest cs)]
           [(read-digits digits) nil (rest cs)])
      \space (recur digits n comma (rest cs))
      \, (recur [] (read-digits digits) true (rest cs))
      (\0 \1 \2 \3 \4 \5 \6 \7 \8 \9) (recur (conj digits (first cs)) n comma (rest cs)))))


(defn parse-chars
  ([cs] (parse-chars cs [] []))
  ([cs group result]
   ;; (println group "  " result "  " (first cs))
   (case (first cs)
     nil (if (empty? result) group (conj result group))
     \^ (if (and (empty? result) (empty? group))
          ;; ignore, but only at start of regex
          (recur (rest cs) group result)
          (throw (ex-info "Unexpected ^ found"
                          {:error :caret
                           :partial (if (empty? result) group (conj result group))
                           :remaining (apply str cs)})))
     \$ (if (empty? (rest cs))
          ;; ignore, but only at end of regex
          (recur (rest cs) group result)
          (throw (ex-info "Unexpected $ found"
                          {:error :dollar
                           :partial (if (empty? result) group (conj result group))
                           :remaining (apply str cs)})))
     \( (recur (rest cs) [] (conj result group))
     \) (if (empty? result)
          (throw (ex-info "Missing ( for group"
                          {:error :missing-open
                           :partial group
                           :remaining (apply str cs)}))
          (recur (rest cs) (conj (peek result) group) (pop result)))
     \[ (let [[setexp rst] (parse-set (rest cs))] (recur rst (conj group setexp) result))
     \. (recur (rest cs) (conj group '(:any)) result)
     ;; :alt is temporarily inserted in place, to be regrouped later
     \| (recur (rest cs) (conj group :alt) result)
     \* (recur (rest cs) (conj (pop group) (list :* (peek group))) result)
     \+ (recur (rest cs) (conj (pop group) (list :+ (peek group))) result)
     \? (recur (rest cs) (conj (pop group) (list :? (peek group))) result)
     \{ (let [[n m rst] (parse-times (rest cs))]
          (recur rst (conj (pop group) (list :times (peek group) n m)) result))
     \\ (recur (rest (rest cs)) (conj group (slash (second cs))) result)
     (recur (rest cs) (conj group (first cs)) result))))


;; Hack alert!  regroup-alt walks throught the initial parse and figures out how to regroup
;; the infix :alt markers into sexp-style prefix notation.  Yes, it should have been done
;; that way from the start, but I couldn't figure out how to keep my groups straight in a
;; single pass.  So we have to live with this.

;; pre mid post refer to the alt groups [pre (* :alt mid) :alt post]
;; mid is (or nil [[x]+]) -- multiple groups in mid
;; post is (nil or [x]) -- nil means no :alt has been seen yet
;; keywords are used as special groupings or operators in the first position of a list (not vector)
(defn regroup-alt [coll]
  ;;DEBUG
  ;(println "REGROUP ALT:" coll)
  
  (loop [cs coll pre [] mid nil post nil]
    (if post
      (cond (empty? cs) (list* :alt pre (conj mid post))
            (coll? (first cs)) (recur (rest cs) pre mid (conj post (regroup-alt (first cs))))
            (= :alt (first cs)) (recur (rest cs) pre (conj mid post) [])
            :else (recur (rest cs) pre mid (conj post (first cs))))
      (cond (empty? cs) (if (keyword? (first pre)) (seq pre) pre)
            (coll? (first cs)) (recur (rest cs) (conj pre (regroup-alt (first cs))) nil nil)
            (= :alt (first cs)) (recur (rest cs) pre [] [])
            :else (recur (rest cs) (conj pre (first cs)) nil nil)))))

;; SEM -- probably don't need to regroup alts within (:set ...) or (:inverted ...), etc.
;; really only within vectors I think

(defn regex-seq [regex]
  (if (string? regex)
    (seq regex)
    #?(:clj (seq (str regex))
       :cljs (drop-last (rest (seq (str regex)))))))

;; regex can be either a string or a regex
(defn parse [regex]
  (try
    (regroup-alt (parse-chars (regex-seq regex)))
    #?(:clj (catch clojure.lang.ExceptionInfo e (throw e)))
    #?(:clj (catch Exception e (throw (ex-info (str "Confused by regular expression: " regex)
                                               {:failed regex}))))
    #?(:cljs (catch :default e (throw (ex-info (str "Confused by regular expression: " regex)
                                               {:failed regex}))))
    ))

(declare tree->generator)

(defn ascii [ch]
  #?(:clj  (long ch)
     :cljs (.charCodeAt ch 0)))

(defn between [ch-begin ch-end]
  (set (map char (range (ascii ch-begin) (inc (ascii ch-end))))))

;; all these char ranges need double checking
(def ^:private digits #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9})
(def ^:private atoz (between \a \z))
(def ^:private AtoZ (between \A \Z))
(def ^:private space (set (seq " \t\n\r")))
(def ^:private punctuation (set (seq ":#$%^&*()-+=!@~`;'?/.|\\[]{},<>\"")))
(def ^:private underscore (set (seq "_")))

(def ^:private word (set/union atoz AtoZ digits underscore))
(def ^:private not-word (set/union space punctuation))
(def ^:private not-digits (set/union space punctuation atoz AtoZ underscore))
(def ^:private not-space (set/union word punctuation))

(def ^:private all-chars (set/union word punctuation space))

(defn charset-seq [tree]
  (if-not (seq? tree) 
    (set tree)
    (case (first tree)
      :digit digits
      :not-digit not-digits
      :word word
      :not-word not-word
      :space space
      :not-space not-space
      :tab #{\t}
      :newline #{\n}
      :return #{\r}
      :btw (apply between (rest tree))
      (set tree))))

(defn inverted [trees]
  (apply set/difference all-chars (set (filter char? trees)) (map charset-seq (filter seq? trees))))

(defn charset [trees]
  (apply set/union (set (filter char? trees)) (map charset-seq (filter seq? trees))))

(defn seq->generator [tree]
  (case (first tree)
    :any gen/char-ascii
    :times (if-let [m (nth tree 3)]
             ;; nil m means exactly N
             (gen/vector (tree->generator (second tree)) (nth tree 2) m)
             (gen/vector (tree->generator (second tree)) (nth tree 2)))
    :* (let [limit *or-more-limit*]
         (gen/sized (fn [n] (gen/vector (tree->generator (second tree)) 0 (min n limit)))))
    :+ (let [limit *or-more-limit*]
         (gen/sized (fn [n] (gen/vector (tree->generator (second tree))
                                        1 (max 1 (min n limit))))))
    :? (gen/one-of [(gen/return "") (tree->generator (second tree))])
    :alt (gen/one-of (map tree->generator (rest tree)))
    :set (gen/elements (charset (rest tree)))
    :inverted (gen/elements (inverted (rest tree)))
    :btw (gen/elements (apply between (rest tree)))
    :digit (gen/elements digits)
    :not-digit (gen/elements not-digits)
    :word (gen/elements word)
    :not-word (gen/elements not-word)
    :space  (gen/elements space)
    :not-space (gen/elements not-space)
    :tab (gen/return \t)
    :newline (gen/return \n)
    :return (gen/return \r)))


;; This is a bit hairy.  It's trying to group runs of single chars together to make a string
;; when possible.  Also, handles special case of single item as a single generator rather
;; than a tuple as used in the general case.

(defn vec->generator [trees]
  (loop [xs trees cs [] gens []]
    (cond (empty? xs) (let [gens (if (empty? cs) gens (conj gens (gen/return (apply str cs))))]
                        (cond (empty? gens) (gen/return "")
                              (= (count gens) 1) (first gens)
                              :else (apply gen/tuple gens)))
          (char? (first xs)) (recur (rest xs) (conj cs (first xs)) gens)
          :else (recur (rest xs) [] (if (empty? cs)
                                      (conj gens (tree->generator (first xs)))
                                      (conj gens (gen/return (apply str cs))
                                            (tree->generator (first xs))))))))

(defn tree->generator [tree]
  (cond (vector? tree) (vec->generator tree)
        (seq? tree) (seq->generator tree)
        (char? tree) (gen/return tree)
        (string? tree) (gen/return tree)
        :else (throw (ex-info (str "Unimplemented generator for " (pr-str tree))
                              {:unimplemented tree}))))

;; this is the only function needed for the public side
(defn string-generator
  "Returns a test.check generator that generates strings matching the given regular
  expression `regex`.  (Fancy flags and POXIX extensions are not suppored; see the doc for
  more information about the supported regular expression syntax.)  The optional
  `or-more-limit` controls the maximum numbers of elements that are generated when matching
  a potentially unbounded regex (such as #\"x*\" or #\"y+\").  The default is 9."
  
  ([regex] 
   (gen/fmap #(if (coll? %) (apply str (flatten %)) (str %))
             (tree->generator (parse regex))))

  ([regex or-more-limit]
   (binding [*or-more-limit* or-more-limit]
     (string-generator regex))))
