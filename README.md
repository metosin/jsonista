# jsonista [![Continuous Integration status](https://img.shields.io/travis/metosin/jsonista.svg)](http://travis-ci.org/metosin/jsonista) [![cljdoc badge](https://cljdoc.xyz/badge/metosin/jsonista)](https://cljdoc.xyz/d/metosin/jsonista/CURRENT)

> *jsonissa / jsonista / jsoniin, jsonilla / jsonilta / jsonille*

Clojure library for fast JSON encoding and decoding.

* Explicit configuration
* Uses [jackson-databind](https://github.com/FasterXML/jackson-databind)
* Mostly written in Java for speed
* [API docs](https://metosin.github.io/jsonista/)
* [FAQ](./docs/faq.md)

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
jsonista.jmh/encode-data-json  :encode  :throughput  5         406998.934   ops/s  152242.102    {:size "10b"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         146750.626   ops/s  13532.113     {:size "100b"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         28543.913    ops/s  5982.429      {:size "1k"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         1994.604     ops/s  193.798       {:size "10k"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         229.534      ops/s  3.574         {:size "100k"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         1202854.746  ops/s  63201.698     {:size "10b"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         468291.944   ops/s  37267.515     {:size "100b"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         108479.571   ops/s  2483.284      {:size "1k"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         10293.254    ops/s  92.099        {:size "10k"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         1060.651     ops/s  4.026         {:size "100k"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         6783801.957  ops/s  54798.388     {:size "10b"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         2261881.877  ops/s  41683.847     {:size "100b"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         394457.058   ops/s  4225.290      {:size "1k"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         35557.637    ops/s  532.431       {:size "10k"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         3060.934     ops/s  101.250       {:size "100k"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         6272986.538  ops/s  8067758.467   {:size "10b"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         1554381.542  ops/s  2291008.911   {:size "100b"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         275663.380   ops/s  258073.341    {:size "1k"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         30998.301    ops/s  7629.493      {:size "10k"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         2848.233     ops/s  248.972       {:size "100k"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         910109.735   ops/s  5590.181      {:size "10b"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         245111.831   ops/s  2604.368      {:size "100b"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         42535.710    ops/s  647.046       {:size "1k"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         3705.661     ops/s  21.401        {:size "10k"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         412.894      ops/s  4.897         {:size "100k"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         1440650.958  ops/s  63665.904     {:size "10b"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         538246.521   ops/s  5078.053      {:size "100b"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         110073.963   ops/s  4482.999      {:size "1k"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         9422.709     ops/s  763.989       {:size "10k"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         991.322      ops/s  65.215        {:size "100k"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         2463256.808  ops/s  233941.924    {:size "10b"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         606524.401   ops/s  24347.671     {:size "100b"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         143850.780   ops/s  26755.413     {:size "1k"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         13433.671    ops/s  1946.566      {:size "10k"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         1214.055     ops/s  204.500       {:size "100k"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         6098939.624  ops/s  768530.918    {:size "10b"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         1435602.777  ops/s  20860.504     {:size "100b"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         274066.827   ops/s  2922.479      {:size "1k"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         25650.308    ops/s  1388.505      {:size "10k"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         2569.702     ops/s  13.894        {:size "100k"}
```

## License

Copyright &copy; 2016-2020 [Metosin Oy](http://www.metosin.fi).

Distributed under the Eclipse Public License 2.0.
