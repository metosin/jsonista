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
jsonista.jmh/encode-data-json  :encode  :throughput  5         2011809.137  ops/s  12600.809     {:size "10b"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         382677.707   ops/s  2861.142      {:size "100b"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         66403.631    ops/s  597.436       {:size "1k"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         5480.185     ops/s  58.379        {:size "10k"}
jsonista.jmh/encode-data-json  :encode  :throughput  5         576.691      ops/s  15.682        {:size "100k"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         996875.314   ops/s  5688.227      {:size "10b"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         482130.613   ops/s  2685.181      {:size "100b"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         128936.005   ops/s  879.709       {:size "1k"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         12209.066    ops/s  94.285        {:size "10k"}
jsonista.jmh/encode-cheshire   :encode  :throughput  5         1258.157     ops/s  12.340        {:size "100k"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         6356105.348  ops/s  85360.100     {:size "10b"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         2010379.039  ops/s  67648.165     {:size "100b"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         409264.663   ops/s  3704.992      {:size "1k"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         34527.245    ops/s  251.065       {:size "10k"}
jsonista.jmh/encode-jsonista   :encode  :throughput  5         2934.595     ops/s  15.858        {:size "100k"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         6275467.563  ops/s  123578.482    {:size "10b"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         2092035.098  ops/s  11417.613     {:size "100b"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         408380.251   ops/s  10912.350     {:size "1k"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         31992.554    ops/s  230.781       {:size "10k"}
jsonista.jmh/encode-jackson    :encode  :throughput  5         2887.485     ops/s  12.491        {:size "100k"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         2257552.949  ops/s  23890.443     {:size "10b"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         498261.935   ops/s  2348.572      {:size "100b"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         85191.855    ops/s  321.961       {:size "1k"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         7763.264     ops/s  250.502       {:size "10k"}
jsonista.jmh/decode-data-json  :decode  :throughput  5         771.691      ops/s  6.559         {:size "100k"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         1099821.870  ops/s  14796.659     {:size "10b"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         544013.773   ops/s  4122.539      {:size "100b"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         109517.975   ops/s  911.623       {:size "1k"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         10017.553    ops/s  50.871        {:size "10k"}
jsonista.jmh/decode-cheshire   :decode  :throughput  5         1014.003     ops/s  18.609        {:size "100k"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         3476196.425  ops/s  21535.641     {:size "10b"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         792773.466   ops/s  8209.591      {:size "100b"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         160180.797   ops/s  554.940       {:size "1k"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         14151.302    ops/s  107.906       {:size "10k"}
jsonista.jmh/decode-jsonista   :decode  :throughput  5         1508.829     ops/s  5.855         {:size "100k"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         5145394.434  ops/s  84237.662     {:size "10b"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         1339393.911  ops/s  6660.176      {:size "100b"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         274465.912   ops/s  1589.614      {:size "1k"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         29607.044    ops/s  183.068       {:size "10k"}
jsonista.jmh/decode-jackson    :decode  :throughput  5         2539.491     ops/s  17.753        {:size "100k"}
```

## Making a release

- Update `CHANGELOG.md` and increment the version number in `project.clj`
- Commit and push to Github
- Create a Github release [here](https://github.com/metosin/jsonista/releases)
  - Use the version number of the release for the tag name
- The [Github Actions release workflow](.github/workflows/release.yml) should fire and deploy a release to clojars

## License

Copyright &copy; 2016-2021 [Metosin Oy](http://www.metosin.fi).

Distributed under the Eclipse Public License 2.0.
