# strgen

A Clojure library with a test.check generator that generates strings from regular
expressions.  Use `miner.strgen/string-generator` with [test.check][tc] and [clojure.spec][cs].  

The clojure.spec library is new in Clojure 1.9.

[tc]: https://github.com/clojure/test.check "test.check"
[cs]: http://clojure.org/guides/spec

![strgen](img/sturgeon.png)


## Version

\[com.velisco/strgen "0.1.7"]

[![strgen on Clojars][shield]][st]

[latest]: https://clojars.org/com.velisco/strgen/latest-version.svg "strgen on Clojars"
[shield]: https://img.shields.io/clojars/v/com.velisco/strgen.svg "strgen on Clojars"
[st]: https://clojars.org/com.velisco/strgen "strgen on Clojars"


## Usage

```clojure
(require '[miner.strgen :as sg])
(require '[clojure.spec :as s])
(require '[clojure.test.check.generators :as gen])

(gen/sample (sg/string-generator #"[A-Z]{2,4}"))

;;=> ("ZOHX" "ZOXZ" "INX" "JO" "MRMZ" "TO" "PEHM" "YNK" "FJ" "JWH")

(s/def ::foobar (let [re #"fo+(bar)?"] 
                   (s/spec (s/and string? #(re-matches re %))
                           :gen  #(sg/string-generator re))))
						   
(s/exercise ::foobar)

;;=> (["fo" "fo"] ["fobar" "fobar"] ["fo" "fo"] ["foo" "foo"] ["foooo" "foooo"] 
;;    ["fooo" "fooo"] ["fo" "fo"] ["foobar" "foobar"] ["fooooobar" "fooooobar"] 
;;    ["fooooobar" "fooooobar"])

```
 
## Limitations

Not all Java [regular expressions][re] are supported.  The basics work, including:
x* y? z+. [abc] [a-z] [^a] \n (a|b) \w \W \d \D \s \S x{N} x{N,} x{M,N}.  ^x$ ignores the leading
^ and trailing $ as they generate no characters.  (Capture groups) and \1 style back
references are not supported.  Character groups like [:alnum:] are not supported.  All the
other fancy flags, quantifiers, character classes, etc. are unsupported.  In summary, if I
couldn't use the regex feature without looking it up, I didn't support it.  If people ask
for something, I might work on it.

[re]: http://en.wikipedia.org/wiki/Regular_expression

When generating X-or-more items for regular expressions such as #"x*" or #"y+", the
generator limits the number of items to a reasonably small count.  You can control this with
an optional second arg `or-more-limit` (an integer, default 9) when calling
`string-generator`.


## Related Projects

If you need better support for Java regular expressions when generating Strings, you should
consider using the [test.chuck][chuck] library which provides the `string-from-regex`
generator.

[chuck]: https://github.com/gfredericks/test.chuck "test.chuck"


## License

Copyright © 2016-2017 Stephen E. Miner

Distributed under the Eclipse Public License, same as Clojure.

