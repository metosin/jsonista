# jsonista [![Continuous Integration status](https://secure.travis-ci.org/metosin/jsonista.png)](http://travis-ci.org/metosin/jsonista)

> *jsonissa / jsonista / jsoniin, jsonilla / jsonilta / jsonille*

Clojure library for fast JSON encoding and decoding.

* Explicit configuration
* Embrace Java for speed
* Uses [Jackson](https://github.com/FasterXML/jackson) directly
* [API docs](https://metosin.github.io/jsonista/)

Aiming to be faster than [Cheshire](https://github.com/dakrone/cheshire) while still having all the necessary features for web development. Designed for use with [Muuntaja](https://github.com/metosin/muuntaja).


## Latest version

[![Clojars Project](http://clojars.org/metosin/jsonista/latest-version.svg)](http://clojars.org/metosin/jsonista)

## Quickstart

```clojure
(require '[jsonista.core :as jsonista])

(jsonista/write-value-as-string {:hello 1})
;; => "{\"hello\":1}"

(def +data+ (jsonista/write-value-as-string {:foo "bar"}))

(jsonista/read-value +data+)
;; => {"foo" "bar"}
```

## Performance

* All standard encoders and decoders are written in Java
* Protocol dispatch with `read-value` & `write-value`
* Jackson `ObjectMapper` is used directly
* Small functions to support JVM Inlining

```clj
(require '[cheshire.core :as cheshire])
(require '[jsonista.core :as jsonista])
(require '[criterium.core :as cc])

;; encode

(def data {:kikka "kukka"})

(cc/quick-bench
  (cheshire/generate-string data))
; 1248ns

(cc/quick-bench
  (jsonista/write-value-as-string data))
; 227ns

;; decode

(def json "{\"kikka\": \"kukka\"}")

(cc/quick-bench
  (cheshire/parse-string json))
; 978ns

(cc/quick-bench
  (jsonista/read-value json))
; 380ns
```

## License

Copyright &copy; 2016-2017 [Metosin Oy](http://www.metosin.fi).

Distributed under the Eclipse Public License, the same as Clojure.
