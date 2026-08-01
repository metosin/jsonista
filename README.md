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
* Untyped decoding builds Clojure data structures directly, in a single pass
* Protocol dispatch with `read-value` & `write-value`
* Jackson `ObjectMapper` is used directly
* Small functions to support JVM Inlining

Measured using [lein-jmh](https://github.com/jgpc42/lein-jmh),
see [perf-tests](/test/jsonista/jmh.clj) for details.

Compared to jsonista `1.0.0` (Jackson 2), `2.0.0` (Jackson 3 plus decoder
optimizations) decodes 15-23% faster with string keys and 28-48% faster with
keyword keys at payloads of 100 bytes and up; encoding is unchanged at 1 KB
and up. Payloads under ~100 bytes encode slower than under Jackson 2 - the
regression is present in the raw Jackson 3 engine itself and is not jsonista
overhead. Method, controls and full data:
[bench/2026-08-01-full-comparison.md](/bench/2026-08-01-full-comparison.md).

### Throughput, relative

![encode](/docs/json-encode.png)

![decode](/docs/json-decode.png)

### Throughput, absolute

![encode](/docs/json-encode-t.png)

![decode](/docs/json-decode-t.png)

The graphs are generated from the Jackson 3 benchmark run
(`bench/2026-08-01-full-jackson321.txt`).

### Throughput, data

Captured 2026-08-01 on Jackson 3.2.1, Apple Silicon MacBook Pro, OpenJDK
25.0.2 (Corretto): 1 fork, 3×3 s warmup + 5×3 s measurement per point (error
bars ≤3% for most cells). All numbers are ops/s, higher is better.

**encode** (string-keyed maps):

|               | 10b | 100b | 1k | 10k | 100k |
|---------------|----:|-----:|---:|----:|-----:|
| data.json     | 5,917,892 | 1,421,856 | 210,328 | 16,047 | 1,647 |
| cheshire      | 2,519,476 | 1,541,245 | 471,888 | 44,068 | 4,253 |
| jsonista      | 9,287,998 | 3,481,862 | 716,087 | 57,715 | 5,528 |
| Jackson (raw) | 9,376,698 | 3,523,568 | 655,584 | 55,877 | 5,534 |

**decode** (string keys):

|               | 10b | 100b | 1k | 10k | 100k |
|---------------|----:|-----:|---:|----:|-----:|
| data.json     | 8,481,497 | 2,108,504 | 346,346 | 27,968 | 2,602 |
| cheshire      | 2,189,307 | 1,260,362 | 307,258 | 27,377 | 3,006 |
| jsonista      | 9,031,389 | 2,411,301 | 420,738 | 37,290 | 3,759 |
| Jackson (raw) | 8,448,628 | 2,406,312 | 441,294 | 40,911 | 4,254 |

**encode, keyword keys** (`keyword-keys-object-mapper`; data.json writes
keyword keys natively, cheshire likewise):

|           | 10b | 100b | 1k | 10k | 100k |
|-----------|----:|-----:|---:|----:|-----:|
| data.json | 7,047,371 | 1,394,450 | 216,929 | 16,633 | 1,619 |
| cheshire  | 2,156,053 | 1,489,408 | 415,399 | 39,578 | 4,270 |
| jsonista  | 8,852,407 | 3,388,647 | 666,115 | 56,608 | 5,357 |

**decode, keyword keys** (data.json with `:key-fn keyword`, cheshire with
`(parse-string s true)`):

|           | 10b | 100b | 1k | 10k | 100k |
|-----------|----:|-----:|---:|----:|-----:|
| data.json | 7,449,560 | 1,757,660 | 325,437 | 27,528 | 2,688 |
| cheshire  | 2,106,946 | 1,102,503 | 268,505 | 24,393 | 2,570 |
| jsonista  | 8,814,925 | 2,520,516 | 443,022 | 39,829 | 3,964 |

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
