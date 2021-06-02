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
➜  jsonista git:(master) ✗ lein jmh '{:file "benchmarks.edn", :type :quick, :format :table}'
{:% 100.0 :eta "00:00:00"}

:benchmark                         :name    :mode        :samples  :score              :score-error  :params
-----------------------------      -------  -----------  --------  ------------------  ------------  --------------
jsonista.jmh/encode-data-json      :encode  :throughput  5         1905463.089  ops/s  63122.305     {:size "10b"}
jsonista.jmh/encode-data-json      :encode  :throughput  5         345349.400   ops/s  175312.845    {:size "100b"}
jsonista.jmh/encode-data-json      :encode  :throughput  5         55137.457    ops/s  20339.204     {:size "1k"}
jsonista.jmh/encode-data-json      :encode  :throughput  5         5194.837     ops/s  395.984       {:size "10k"}
jsonista.jmh/encode-data-json      :encode  :throughput  5         539.111      ops/s  219.494       {:size "100k"}
jsonista.jmh/encode-cheshire       :encode  :throughput  5         965841.282   ops/s  384153.551    {:size "10b"}
jsonista.jmh/encode-cheshire       :encode  :throughput  5         414779.218   ops/s  77713.579     {:size "100b"}
jsonista.jmh/encode-cheshire       :encode  :throughput  5         95781.504    ops/s  16569.179     {:size "1k"}
jsonista.jmh/encode-cheshire       :encode  :throughput  5         9114.238     ops/s  824.365       {:size "10k"}
jsonista.jmh/encode-cheshire       :encode  :throughput  5         888.121      ops/s  190.972       {:size "100k"}
jsonista.jmh/encode-jsonista       :encode  :throughput  5         4354049.558  ops/s  1215677.321   {:size "10b"}
jsonista.jmh/encode-jsonista       :encode  :throughput  5         1698787.064  ops/s  940540.979    {:size "100b"}
jsonista.jmh/encode-jsonista       :encode  :throughput  5         376730.022   ops/s  154961.364    {:size "1k"}
jsonista.jmh/encode-jsonista       :encode  :throughput  5         29272.337    ops/s  5208.374      {:size "10k"}
jsonista.jmh/encode-jsonista       :encode  :throughput  5         2275.436     ops/s  1329.558      {:size "100k"}
jsonista.jmh/encode-jackson        :encode  :throughput  5         5471224.116  ops/s  2598678.138   {:size "10b"}
jsonista.jmh/encode-jackson        :encode  :throughput  5         1509416.980  ops/s  497578.108    {:size "100b"}
jsonista.jmh/encode-jackson        :encode  :throughput  5         386633.021   ops/s  1158.922      {:size "1k"}
jsonista.jmh/encode-jackson        :encode  :throughput  5         31780.542    ops/s  2320.829      {:size "10k"}
jsonista.jmh/encode-jackson        :encode  :throughput  5         2735.888     ops/s  406.603       {:size "100k"}
jsonista.jmh/decode-data-json      :decode  :throughput  5         2094889.246  ops/s  89077.322     {:size "10b"}
jsonista.jmh/decode-data-json      :decode  :throughput  5         485354.569   ops/s  23670.883     {:size "100b"}
jsonista.jmh/decode-data-json      :decode  :throughput  5         83811.653    ops/s  3133.162      {:size "1k"}
jsonista.jmh/decode-data-json      :decode  :throughput  5         7741.879     ops/s  155.608       {:size "10k"}
jsonista.jmh/decode-data-json      :decode  :throughput  5         797.233      ops/s  14.762        {:size "100k"}
jsonista.jmh/decode-cheshire       :decode  :throughput  5         1433093.847  ops/s  90976.064     {:size "10b"}
jsonista.jmh/decode-cheshire       :decode  :throughput  5         450070.726   ops/s  131950.612    {:size "100b"}
jsonista.jmh/decode-cheshire       :decode  :throughput  5         91872.057    ops/s  6432.237      {:size "1k"}
jsonista.jmh/decode-cheshire       :decode  :throughput  5         8509.100     ops/s  156.138       {:size "10k"}
jsonista.jmh/decode-cheshire       :decode  :throughput  5         887.882      ops/s  33.875        {:size "100k"}
jsonista.jmh/decode-jsonista       :decode  :throughput  5         2608856.640  ops/s  474804.764    {:size "10b"}
jsonista.jmh/decode-jsonista       :decode  :throughput  5         607787.943   ops/s  13728.880     {:size "100b"}
jsonista.jmh/decode-jsonista       :decode  :throughput  5         120542.299   ops/s  39147.980     {:size "1k"}
jsonista.jmh/decode-jsonista       :decode  :throughput  5         10827.731    ops/s  5616.145      {:size "10k"}
jsonista.jmh/decode-jsonista       :decode  :throughput  5         1128.572     ops/s  232.442       {:size "100k"}
jsonista.jmh/decode-jackson        :decode  :throughput  5         5526592.411  ops/s  1463758.000   {:size "10b"}
jsonista.jmh/decode-jackson        :decode  :throughput  5         1316257.448  ops/s  163658.831    {:size "100b"}
jsonista.jmh/decode-jackson        :decode  :throughput  5         252440.943   ops/s  14566.399     {:size "1k"}
jsonista.jmh/decode-jackson        :decode  :throughput  5         23871.633    ops/s  1625.629      {:size "10k"}
jsonista.jmh/decode-jackson        :decode  :throughput  5         2211.046     ops/s  91.282        {:size "100k"}
```

## License

Copyright &copy; 2016-2021 [Metosin Oy](http://www.metosin.fi).

Distributed under the Eclipse Public License 2.0.
