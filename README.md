# jsonista [![Continuous Integration status](https://github.com/metosin/jsonista/workflows/Run%20tests/badge.svg?event=push)](https://github.com/metosin/jsonista/actions) [![cljdoc badge](https://cljdoc.xyz/badge/metosin/jsonista)](https://cljdoc.xyz/d/metosin/jsonista/CURRENT)

> *jsonissa / jsonista / jsoniin, jsonilla / jsonilta / jsonille*

Clojure library for fast JSON encoding and decoding.

* Explicit configuration
* Uses [jackson-databind](https://github.com/FasterXML/jackson-databind)
* Mostly written in Java for speed
* [API docs](https://cljdoc.org/d/metosin/jsonista/CURRENT/api/jsonista)
* [FAQ](https://cljdoc.org/d/metosin/jsonista/CURRENT/doc/frequently-asked-questions)

Faster than [data.json](https://github.com/clojure/data.json) or [Cheshire](https://github.com/dakrone/cheshire) while still having the necessary features for web development. Designed for use with [Muuntaja](https://github.com/metosin/muuntaja).

Blogged:
* [Faster JSON processing with jsonista](http://www.metosin.fi/blog/faster-json-processing-with-jsonista/)

## Latest version

[![Clojars Project](http://clojars.org/metosin/jsonista/latest-version.svg)](http://clojars.org/metosin/jsonista)

Requires Java1.8+

## Quickstart

```clojure
(require '[jsonista.core :as j])

(j/write-value-as-string {"hello" 1})
;; => "{\"hello\":1}"

(j/read-value *1)
;; => {"hello" 1}
```

## Examples

Using explicit ObjectMapper:

```clj
(-> {:dog {:name "Teppo"}}
    (j/write-value-as-bytes j/default-object-mapper)
    (j/read-value j/default-object-mapper))
;; => {"dog" {"name" "Teppo"}}
```

Using keyword keys:

```clj
(-> {:dog {:name "Teppo"}}
    (j/write-value-as-bytes j/keyword-keys-object-mapper)
    (j/read-value j/keyword-keys-object-mapper))
;; => {:dog {:name "Teppo"}}
```

Changing how map keys are encoded & decoded:

```clojure
(defn reverse-string [s] (apply str (reverse s)))

(def mapper
  (j/object-mapper
    {:encode-key-fn (comp reverse-string name)
     :decode-key-fn (comp keyword reverse-string)}))

(-> {:kikka "kukka"}
    (doto prn)
    (j/write-value-as-string mapper)
    (doto prn)
    (j/read-value mapper)
    (prn))
; {:kikka "kukka"}
; "{\"akkik\":\"kukka\"}"
; {:kikka "kukka"}
```

Reading & writing directly into a file:

```clojure
(def file (java.io.File. "hello.json"))

(j/write-value file {"hello" "world"})

(slurp file)
;; => "{\"hello\":\"world\"}"

(j/read-value file)
;; => {"hello" "world"}
```

Adding support for [joda-time](http://www.joda.org/joda-time) Classes, used by [clj-time](https://github.com/clj-time/clj-time).

```clj
;; [com.fasterxml.jackson.datatype/jackson-datatype-joda "2.9.5"]
(import '[com.fasterxml.jackson.datatype.joda JodaModule])
(import '[org.joda.time LocalDate])

(def mapper
  (j/object-mapper
    {:modules [(JodaModule.)]}))

(j/write-value-as-string (LocalDate. 0) mapper)
; "\"1970-01-01\""
```

### Tagged JSON

Adding support for lossless encoding data using tagged values. This
includes both reading and writing support.

```clj
(def mapper
  (j/object-mapper
    {:encode-key-fn true
     :decode-key-fn true
     :modules [(jt/module
                 {:handlers {Keyword {:tag "!kw"
                                      :encode jt/encode-keyword
                                      :decode keyword}
                             PersistentHashSet {:tag "!set"
                                                :encode jt/encode-collection
                                                :decode set}}})]}))

(-> {:system/status #{:status/good}}
    (j/write-value-as-string mapper)
    (doto prn)
    (j/read-value mapper))
; prints "{\"system/status\":[\"!set\",[[\"!kw\",\"status/good\"]]]}"
; => {:system/status #{:status/good}}
```

In simple [perf tests](https://github.com/metosin/jsonista/blob/master/test/jsonista/json_perf_test.clj), tagged JSON is much faster than EDN or Transit.

## Performance

* All standard encoders and decoders are written in Java
* Protocol dispatch with `read-value` & `write-value`
* Jackson `ObjectMapper` is used directly
* Small functions to support JVM Inlining

Measured using [lein-jmh](https://github.com/jgpc42/lein-jmh),
see [perf-tests](/test/jsonista/jmh.clj) for details.

### Throughput, relative

![encode](/docs/json-encode.png)

![decode](/docs/json-decode.png)

### Throughput, absolute

![encode](/docs/json-encode-t.png)

![decode](/docs/json-decode-t.png)

### Throughput, data

```bash
:benchmark                     :name    :mode        :samples  :score              :score-error  :params
-----------------------------  -------  -----------  --------  ------------------  ------------  --------------
jsonista.jmh/encode-data-json  :encode  :throughput  5         1534830.890  ops/s  155359.246    {:size "10b"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         341613.782   ops/s  26261.051     {:size "100b"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         69673.326    ops/s  1647.625      {:size "1k"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         5658.247     ops/s  999.701       {:size "10k"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         581.924      ops/s  39.758        {:size "100k"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         1073164.879  ops/s  270854.390    {:size "10b"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         424662.238   ops/s  146705.515    {:size "100b"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         101765.336   ops/s  4868.846      {:size "1k"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         9427.517     ops/s  711.480       {:size "10k"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         939.962      ops/s  124.443       {:size "100k"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         6718559.441  ops/s  564494.417    {:size "10b"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         2021530.135  ops/s  227934.280    {:size "100b"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         358639.582   ops/s  33561.700     {:size "1k"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         32536.978    ops/s  8135.004      {:size "10k"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         2687.242     ops/s  185.516       {:size "100k"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         6883276.103  ops/s  695669.799    {:size "10b"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         1969207.512  ops/s  262952.863    {:size "100b"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         365593.510   ops/s  20251.435     {:size "1k"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         30955.299    ops/s  497.706       {:size "10k"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         2727.378     ops/s  268.712       {:size "100k"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         1576813.105  ops/s  312986.468    {:size "10b"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         395571.705   ops/s  12482.641     {:size "100b"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         63238.895    ops/s  18113.050     {:size "1k"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         5620.609     ops/s  1125.897      {:size "10k"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         601.479      ops/s  43.190        {:size "100k"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         1066258.082  ops/s  133519.722    {:size "10b"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         552961.483   ops/s  48478.413     {:size "100b"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         102688.960   ops/s  2318.402      {:size "1k"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         9668.507     ops/s  1742.569      {:size "10k"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         1022.688     ops/s  82.562        {:size "100k"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         2909222.815  ops/s  162043.029    {:size "10b"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         681307.555   ops/s  13851.758     {:size "100b"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         141054.989   ops/s  16569.794     {:size "1k"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         13235.087    ops/s  1263.748      {:size "10k"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         1308.170     ops/s  173.721       {:size "100k"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         5783012.047  ops/s  650655.756    {:size "10b"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         1394300.173  ops/s  14904.386     {:size "100b"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         272135.776   ops/s  2335.753      {:size "1k"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         26725.329    ops/s  3507.918      {:size "10k"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         2398.088     ops/s  337.678       {:size "100k"}
```

## License

Copyright &copy; 2016-2020 [Metosin Oy](http://www.metosin.fi).

Distributed under the Eclipse Public License 2.0.
