# jsonista

[![Continuous Integration status](https://github.com/metosin/jsonista/actions/workflows/clojure.yml/badge.svg)](https://github.com/metosin/jsonista/actions/workflows/clojure.yml)
[![cljdoc badge](https://cljdoc.org/badge/metosin/jsonista)](https://cljdoc.org/d/metosin/jsonista/CURRENT)

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

> Hi! We are [Metosin](https://metosin.fi), a consulting company. These libraries have evolved out of the work we do for our clients.
> We maintain & develop this project, for you, for free. Issues and pull requests welcome!
> However, if you want more help using the libraries, or want us to build something as cool for you, consider our [commercial support](https://www.metosin.fi/en/open-source-support).

## Latest version

[![Clojars Project](http://clojars.org/metosin/jsonista/latest-version.svg)](http://clojars.org/metosin/jsonista)

Requires Java 17+

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
;; [tools.jackson.datatype/jackson-datatype-joda "3.2.1"]
(import '[tools.jackson.datatype.joda JodaModule])
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

## Streaming

See [docs/streaming.md](docs/streaming.md).

## Performance

* All standard encoders and decoders are written in Java
* Protocol dispatch with `read-value` & `write-value`
* Jackson `ObjectMapper` is used directly
* Small functions to support JVM Inlining

Measured using [lein-jmh](https://github.com/jgpc42/lein-jmh),
see [perf-tests](/test/jsonista/jmh.clj) for details.

The Jackson 2 -> Jackson 3 port (`2.0.0`) measured as performance-neutral at
payload sizes of 100 bytes and up.

### Throughput, relative

![encode](/docs/json-encode.png)

![decode](/docs/json-decode.png)

### Throughput, absolute

![encode](/docs/json-encode-t.png)

![decode](/docs/json-decode-t.png)

The four graphs above are from the Jackson 2 era and have not been
regenerated for Jackson 3.

### Throughput, data

The table that used to live here was captured on Jackson 2 and is stale for
this version. Regenerating it (`lein jmh '{:file "benchmarks.edn", :type
:quick, :format :table}'`, ~67 minutes) is tracked as a follow-up.

## Origin story

As [Miikka Koskinen, @miikka](https://github.com/miikka/), tells it on [his blog](https://quanttype.net/p/speed-up-code-with-pi-autoresearch/#fn:1):

> At that time, I was working at Metosin, a Clojure agency. Tommi Reiman ([@ikitommi](https://github.com/ikitommi)), one of the founders and a prolific open source developer, had an idea for how to make a fast JSON library. I thought Tommi’s idea was stupid and to show him that it won’t work, I started to implement a toy version.
>
> The idea wasn’t easy to implement and I figured I’d make something simpler first to have a baseline. The baseline was faster than Cheshire, the number one JSON library for Clojure, and we decided to ship it. Kalle Lehikoinen ([@kalekale](https://github.com/kalekale)) packaged it into jsonista. Tommi’s original idea never got implemented and I don’t even remember what it was anymore!

## Making a release

- Update `CHANGELOG.md` and increment the version number in `project.clj`
- Commit and push to Github
- Create a Github release [here](https://github.com/metosin/jsonista/releases)
  - Use the version number of the release for the tag name
- The [Github Actions release workflow](.github/workflows/release.yml) should fire and deploy a release to clojars

## License

Copyright &copy; 2016-2026 [Metosin Oy](http://www.metosin.fi).

Distributed under the Eclipse Public License 2.0.
