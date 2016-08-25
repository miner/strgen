# strgen

A Clojure library with a test.check generator that generates strings from regular
expressions.  Use `miner.strgen/string-generator` with [test.check][tc] and [clojure.spec][cs].  

The clojure.spec library is new in Clojure 1.9, currently available as a pre-release version
1.9.0-alpha11.

[tc]: https://github.com/clojure/test.check "test.check"
[cs]: http://clojure.org/guides/spec

## Usage

```clojure
(require '[miner.strgen :as sg])
(require '[clojure.spec :as s])
(require '[clojure.test.check.generators :as gen])

(gen/sample (sg/string-generator #"[A-Z]{2,4}"))
;;=> ("ZOHX" "ZOXZ" "INX" "JO" "MRMZ" "TO" "PEHM" "YNK" "FJ" "JWH")


(let [re #"fo+(bar)?"]
	(s/exercise (s/with-gen (s/spec (s/and string? #(re-matches re %)))
                         #(sg/string-generator re))))

;;=> (["fo" "fo"] ["fobar" "fobar"] ["fo" "fo"] ["foo" "foo"] ["foooo" "foooo"] ["fooo" "fooo"] ["fo" "fo"] ["foobar" "foobar"] ["fooooobar" "fooooobar"] ["fooooobar" "fooooobar"])

```
 
## Limitations

Not all Java [regular expressions][re] are supported.  The basics work, including: *?+. [abc]
[a-z] [^a] \n (a|b) \w \W \d \D \s \S x{N} x{N,} x{N,M}.  ^x$ basically ignores the leading
^ and trailing $ as they generate no characters.  (Capture groups) and \1 style back
references are not supported.  Character groups like [:alnum:] are not supported.  All the
other fancy flags, quantifiers, character classes, etc. are unsupported.  In summary, if I
couldn't use the regex feature without looking it up, I didn't support it.  If people
ask for something, I might work on it.

[re]: http://en.wikipedia.org/wiki/Regular_expression

When generating X-or-more items, the generator limits the number of items to a reasonably
small limt (less than 10).  I should allow the user more control over this.


## Related Projects

If you need better support for Java regular expressions when generating Strings, you should
consider using the [test.chuck][chuck] library which provides the `string-from-regex`
generator.

[chuck]: https://github.com/gfredericks/test.chuck "test.chuck"


## License

Copyright © 2016 Stephen E. Miner
Distributed under the Eclipse Public License, same as Clojure.

