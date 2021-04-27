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

:benchmark                     :name    :mode        :samples  :score              :score-error  :params
-----------------------------  -------  -----------  --------  ------------------  ------------  --------------
jsonista.jmh/encode-data-json  :encode  :throughput  5         2005040.000  ops/s  18641.040     {:size "10b"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         332469.712   ops/s  58885.657     {:size "100b"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         60609.008    ops/s  11476.851     {:size "1k"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         4970.188     ops/s  638.168       {:size "10k"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         581.102      ops/s  5.168         {:size "100k"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         1205106.095  ops/s  42282.531     {:size "10b"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         450330.840   ops/s  23666.810     {:size "100b"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         105423.654   ops/s  8408.732      {:size "1k"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         9734.970     ops/s  118.249       {:size "10k"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         978.945      ops/s  8.672         {:size "100k"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         6761392.263  ops/s  113396.242    {:size "10b"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         1976136.012  ops/s  50656.525     {:size "100b"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         393627.163   ops/s  4972.565      {:size "1k"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         31114.706    ops/s  557.984       {:size "10k"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         2887.899     ops/s  59.618        {:size "100k"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         6108654.930  ops/s  2090135.205   {:size "10b"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         1944127.861  ops/s  431031.615    {:size "100b"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         356943.193   ops/s  3977.765      {:size "1k"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         31697.766    ops/s  1753.636      {:size "10k"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         2562.607     ops/s  177.252       {:size "100k"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         2208464.243  ops/s  187701.406    {:size "10b"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         524637.916   ops/s  28974.507     {:size "100b"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         80714.396    ops/s  4794.186      {:size "1k"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         7152.868     ops/s  882.978       {:size "10k"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         756.348      ops/s  77.543        {:size "100k"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         1380506.771  ops/s  61626.508     {:size "10b"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         526259.733   ops/s  8367.880      {:size "100b"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         101846.083   ops/s  1052.037      {:size "1k"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         9493.634     ops/s  177.983       {:size "10k"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         996.374      ops/s  12.745        {:size "100k"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         2933372.525  ops/s  40995.514     {:size "10b"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         686867.379   ops/s  3734.473      {:size "100b"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         146331.608   ops/s  1187.801      {:size "1k"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         13509.398    ops/s  248.500       {:size "10k"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         1363.565     ops/s  10.496        {:size "100k"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         5773791.401  ops/s  31457.861     {:size "10b"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         1379791.334  ops/s  370113.189    {:size "100b"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         245659.626   ops/s  12711.129     {:size "1k"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         23398.618    ops/s  2886.782      {:size "10k"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         2342.444     ops/s  155.288       {:size "100k"}
```

## License

Copyright &copy; 2016-2020 [Metosin Oy](http://www.metosin.fi).

Distributed under the Eclipse Public License 2.0.
