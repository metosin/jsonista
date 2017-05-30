# jsonista [![Continuous Integration status](https://secure.travis-ci.org/metosin/jsonista.png)](http://travis-ci.org/metosin/jsonista)

> *jsonissa / jsonista / jsoniin, jsonilla / jsonilta / jsonille*

Clojure library for fast JSON encoding and decoding. Aiming for better performance than [Cheshire](https://github.com/dakrone/cheshire) while still having all the necessary features for web development. Designed for use with [Muuntaja](https://github.com/metosin/muuntaja). Based on [Jackson](https://github.com/FasterXML/jackson).


## Latest version

Status: **alpha**.

[![Clojars Project](http://clojars.org/metosin/jsonista/latest-version.svg)](http://clojars.org/metosin/jsonista)

## Quickstart

```clojure
(require '[jsonista.core :as json])
(json/to-json {:hello 1})
;; => "{\"hello\":1}"

(def +data+ (json/to-json {:foo \"bar\"}))
(json/from-json +data+)
;; => {"foo" "bar"}
```


## License

Copyright &copy; 2016-2017 [Metosin Oy](http://www.metosin.fi).

Distributed under the Eclipse Public License, the same as Clojure.
