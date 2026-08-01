## 2.0.0

jsonista now uses [Jackson 3](https://github.com/FasterXML/jackson-3) (`3.2.1`).

**Breaking changes:**

* **Requires Java 17+** (was Java 8+). Jackson 3 has a hard Java 17 baseline.
* **Jackson coordinates changed** from `com.fasterxml.jackson.*` to
  `tools.jackson.*`. If you pin Jackson versions in your own project, update
  them.
* **Trailing content after a JSON value is now an error.** Jackson 3 enables
  `FAIL_ON_TRAILING_TOKENS` by default, so `(read-value "{} garbage")` throws
  a `tools.jackson.core.exc.StreamReadException` instead of returning `{}`.
  Measured directly, this check has no detectable performance cost.
* **Jackson exceptions are now unchecked.** `IOException` and
  `JsonProcessingException` are replaced by `JacksonException` and its
  subtypes, all of which extend `RuntimeException`. Existing
  `catch IOException` handlers around jsonista calls will no longer fire.
* **`:factory` is JSON-only.** Jackson 3 requires format-specific mappers for
  other formats; pass e.g. a `CBORMapper` via `:mapper` instead.
* **Objects with no bean accessors now serialize as `{}` instead of
  throwing.** Jackson 2 enabled `FAIL_ON_EMPTY_BEANS` by default, so
  serializing a value jsonista has no serializer for and that exposes no
  getters (an unsupported class, a `deftype` with no fields, etc.) raised
  `InvalidDefinitionException`. Jackson 3's own default has this feature
  disabled, and jsonista now takes that default as-is rather than
  overriding it, so the same value silently serializes to `"{}"`.
  `:do-not-fail-on-empty-beans` is unaffected but is now a no-op unless
  something else (a custom `:mapper` or `:modules`) re-enabled the feature.
* Enums now serialize using `toString` by default.
* **`java.net.URL` is no longer accepted by `read-value`/`read-values`.**
  Jackson 3 removed the `URL` overloads from `ObjectMapper.readValue` and
  `ObjectReader.readValues`, and jsonista now follows suit instead of
  routing around it. Passing a `URL` throws `IllegalArgumentException: No
  implementation of method: :-read-value of protocol: #'jsonista.core/ReadValue
  found for class: java.net.URL` (or the equivalent for `ReadValues`) — not a
  Jackson exception, since dispatch fails in the Clojure protocol layer
  before ever reaching Jackson.

  `ReadValue`/`ReadValues` are public protocols — jsonista's documented
  extension mechanism — so URL support is trivially restorable. Simplest,
  since `InputStream` is already supported and this needs no Jackson
  imports:

  ```clojure
  (with-open [in (.openStream url)] (j/read-value in))
  (with-open [in (.openStream url)] (into [] (j/read-values in)))
  ```

  Or restore `URL` as a first-class type in your own code:

  ```clojure
  (extend-protocol j/ReadValue
    java.net.URL
    (-read-value [this mapper]
      (with-open [in (.openStream ^java.net.URL this)]
        (j/read-value in mapper))))
  ```

  **Caution:** `read-values` is lazy. Wrapping it in `with-open` without
  realizing the sequence *inside* the `with-open` closes the underlying
  stream before it's consumed — the stream is closed once control leaves
  the block, regardless of whether anything has read from it yet. This
  fails silently in some contexts (an empty sequence, no error) and throws
  `Stream closed` in others, depending on how much buffering happened
  before the close. That's why the example above wraps `read-values` in
  `into []` inside the `with-open`, forcing full realization before the
  stream closes — don't return the lazy iterator/seq out of the block.

**Performance:**

* **Decoding is 15-23% faster with string keys and 28-48% faster with keyword
  keys than 1.0.0** (measured across 100 B - 100 KB payloads with
  cheshire/data.json as cross-run controls, see
  `bench/2026-08-01-full-comparison.md`): repeated object keys hit a bounded
  keyword cache instead of `Keyword.intern` (same clear-when-full bounding
  strategy jackson-core uses internally for property-name interning in
  `tools.jackson.core.util.InternCache`), JSON objects of 8 entries or
  fewer build `PersistentArrayMap`s directly, and untyped decoding walks the
  token stream in a single pass (`ClojureUntypedDeserializer`) instead of
  double-dispatching through databind's `UntypedObjectDeserializer`.
* **JSON objects with 8 or fewer entries now decode to `PersistentArrayMap`**
  (insertion-ordered, matching small Clojure map literals and cheshire)
  instead of `PersistentHashMap`. Equality semantics are unchanged; only code
  inspecting the concrete map class or relying on `PersistentHashMap` seq
  order would notice.
* Encoding throughput is unchanged at payloads of 1 KB and up. Payloads under
  ~100 bytes encode slower than under Jackson 2; the regression is measurable
  in the raw Jackson 3 engine with no jsonista code in the loop.
* **`java.util.Date` serialization no longer takes a lock.** `DateSerializer`
  (behind the `:date-format` option) now formats through an immutable
  `java.time.format.DateTimeFormatter` instead of a shared `SimpleDateFormat`
  guarded by `synchronized`, removing the contention point when many threads
  serialize dates through one mapper (3.3× throughput at 8 threads in a
  contention microbenchmark). `:date-format` patterns are now interpreted by
  `DateTimeFormatter.ofPattern`: common patterns — including the default
  `yyyy-MM-dd'T'HH:mm:ss'Z'` — produce identical output, but a few pattern
  letters (e.g. week-based `Y`) have subtly different semantics.

**Other changes:**

* `jackson-datatype-jsr310` is no longer a dependency — `java.time` support is
  built into Jackson 3 databind.
* **`jsonista.tagged/encode-collection` now writes elements through the live
  generator.** Jackson 3 removed `JsonGenerator.getCodec()`, which the old
  implementation used to serialize each collection element to a `String` and
  emit it with `writeRawValue`. Elements now go through `writePOJO` directly,
  which also avoids the intermediate `String`.

  Output is byte-identical under Jackson's default pretty-printer (verified
  against the pre-port implementation). A **custom `PrettyPrinter`** that
  breaks arrays across lines may see a difference, and it is a fix rather
  than a regression: the old path serialized nested elements in a separate
  pass starting from indentation depth zero and then embedded that text
  verbatim, so nested content could come out mis-indented relative to its
  surroundings. Elements are now written at their true nesting depth.
* **`:escape-non-ascii` is JSON-only.** `JsonWriteFeature/ESCAPE_NON_ASCII` is
  JSON-format-specific in Jackson 3, unlike Jackson 2's format-agnostic
  `JsonGenerator.Feature` equivalent. Combined with a non-JSON `:mapper`
  (e.g. `CBORMapper`), `:escape-non-ascii` is now a documented no-op instead
  of applying to the binary format as it did under Jackson 2.

## 1.0.0 (2026-03-06)

* Jsonista has been fairly stable for a couple of years now. Let's call this 1.0!
* Bonus content: check the _origin story_ at the bottom of README.md
* Updated deps:
```
[com.fasterxml.jackson.core/jackson-core "2.21.2"] is available but we use "2.21.1"
[com.fasterxml.jackson.core/jackson-databind "2.21.2"] is available but we use "2.21.1"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.21.2"] is available but we use "2.21.1"
```

## 0.3.14 (2026-03-06)

* Updated deps:
```
[com.fasterxml.jackson.core/jackson-core "2.21.1"]
[com.fasterxml.jackson.core/jackson-databind "2.21.1"]
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.21.1"]
```

## 0.3.13 (2024-12-19)

* Document thrown exceptions
* Updated deps:
```
[com.fasterxml.jackson.core/jackson-core "2.18.2"] is available but we use "2.17.2"
[com.fasterxml.jackson.core/jackson-databind "2.18.2"] is available but we use "2.17.2"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.18.2"] is available but we use "2.17.2"
```

## 0.3.12 (2024-11-01)

* Fix `delay` serialization on Clojure 1.12 [#84](https://github.com/metosin/jsonista/pull/84)

## 0.3.11 (2024-09-18)

* New streaming support, including `read-values` and `write-values`.
  See [docs/streaming.md](docs/streaming.md). Thanks to [@bsless](https://github.com/bsless)!
  [#82](https://github.com/metosin/jsonista/pull/82)

## 0.3.10 (2024-08-09)

* The `:strip-nils` option now doesn't strip empty values like `{}` or `""`.
  Use the new `:strip-empties` option if you want the old behaviour.
  Thanks to [@dominicfreeston](https://github.com/dominicfreeston)!
  [#78](https://github.com/metosin/jsonista/pull/78),
  [#79](https://github.com/metosin/jsonista/pull/79)
* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.17.2"] is available but we use "2.17.1"
[com.fasterxml.jackson.core/jackson-databind "2.17.2"] is available but we use "2.17.1"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.17.2"] is available but we use "2.17.1"
[com.fasterxml.jackson.datatype/jackson-datatype-joda "2.17.2"] is available but we use "2.17.1"
```

## 0.3.9 (2024-06-29)

* add `:do-not-fail-on-empty-beans` option [#75](https://github.com/metosin/jsonista/pull/75)
* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.17.1"] is available but we use "2.15.2"
[com.fasterxml.jackson.core/jackson-databind "2.17.1"] is available but we use "2.15.2"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.17.1"] is available but we use "2.15.2"
```

## 0.3.8 (2023-09-28)

* new options `:order-by-keys` to sort map keys alphabetically [#70](https://github.com/metosin/jsonista/pull/70)
* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.15.2"] is available but we use "2.14.1"
[com.fasterxml.jackson.core/jackson-databind "2.15.2"] is available but we use "2.41.1"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.15.2"] is available but we use "2.14.1"
```

## 0.3.7 (2022-12-02)

* new options `:strip-nils` to remove any keys that have nil values [#67](https://github.com/metosin/jsonista/pull/67)
* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.14.1"] is available but we use "2.13.2"
[com.fasterxml.jackson.core/jackson-databind "2.14.1"] is available but we use "2.13.2.2"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.14.1"] is available but we use "2.13.2"
```

## 0.3.6. (2022-04-05)

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.13.2"] is available but we use "2.13.0"
[com.fasterxml.jackson.core/jackson-databind "2.13.2.2"] is available but we use "2.13.0"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.13.2"] is available but we use "2.13.0"
```

## 0.3.5. (2021-12-07)

* implement `com.fasterxml.jackson.databind.deser.ContextualDeserializer` for 30% faster de-serialization of Maps and Vectors.
* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.13.0"] is available but we use "2.12.5"
[com.fasterxml.jackson.core/jackson-databind "2.13.0"] is available but we use "2.12.5"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.13.0"] is available but we use "2.12.5"
```

## 0.3.4 (2021-09-16)

* add `deps.edn` to the project
* run tests with Java17
* Provide GraalVM native-image --initialize-at-build-time args, [#58](https://github.com/metosin/jsonista/pull/58)

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.12.5"] is available but we use "2.12.3"
[com.fasterxml.jackson.core/jackson-databind "2.12.5"] is available but we use "2.12.3"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.12.5"] is available but we use "2.12.3"
```

## 0.3.3 (2021-05-02)

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.12.3"] is available but we use "2.12.2"
[com.fasterxml.jackson.core/jackson-databind "2.12.3"] is available but we use "2.12.2"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.12.3"] is available but we use "2.12.2"
```

## 0.3.2 (2021-04-23)

* Remove reflection on ObjectMapper
* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.12.2"] is available but we use "2.12.0"
[com.fasterxml.jackson.core/jackson-databind "2.12.2"] is available but we use "2.12.0"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.12.2"] is available but we use "2.12.0"
```

## 0.3.1 (2021-01-27)

* new options for `j/object-mapper`:

* `:factory` - A Jackson JsonFactory for this given mapper
* `:mapper` - The base ObjectMapper to start with - overrides `:factory`

```clj
(require '[jsonista.core :as j])

(import '(org.msgpack.jackson.dataformat MessagePackFactory))

(def mapper
  (j/object-mapper
    {:factory (MessagePackFactory.)
     :encode-key-fn true
     :decode-key-fn true}))

(-> {:kikka 6}
    (j/write-value-as-bytes mapper)
    (j/read-value mapper))
; => {:kikka 6}
```

## 0.3.0 (2020-12-27)

* new `jsonista.tagged` ns for EDN/Transit -style tagged wire formats:

```clj
(require '[jsonista.core :as j])
(require '[jsonista.tagged :as jt])

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

* **BREAKING**: latest version of Jackson fails on serializing Joda-times if the `JodaModule` is not present. This is good.

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.12.0"] is available but we use "2.11.2"
[com.fasterxml.jackson.core/jackson-databind "2.12.0"] is available but we use "2.11.2"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.12.0"] is available but we use "2.11.2"
```

## 0.2.7 (2020-08-25)

* Fix [#33](https://github.com/metosin/jsonista/issues/33): "Cannot set a custom java.time.LocalTime encoder"
* Deprecate `jsonista.core/+default-mapper`
  * `jsonista.core/default-object-mapper` for defaults
  * `jsonista.core/keyword-keys-object-mapper` for encoding & decoding keys into keywords

```clj
(-> {:dog {:name "Teppo"}}
    (j/write-value-as-bytes j/keyword-keys-object-mapper)
    (j/read-value j/keyword-keys-object-mapper))
;; => {:dog {:name "Teppo"}}
```

* Add empty `deps.edn`

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.11.2"] is available but we use "2.11.0"
[com.fasterxml.jackson.core/jackson-databind "2.11.2"] is available but we use "2.11.0"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.11.2"] is available but we use "2.11.0"
```

## 0.2.6 (2020-05-05)

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.11.0"]
[com.fasterxml.jackson.core/jackson-databind "2.11.0"] is available but we use "2.10.0"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.11.0"] is available but we use "2.10.0"
```

## 0.2.5 (2019-09-04)

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-databind "2.10.0"] is available but we use "2.9.9.1"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.10.0"] is available but we use "2.9.9"
```

## 0.2.4 (2019-08-05)

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-databind "2.9.9.1"] is available but we use "2.9.9"
```

## 0.2.3 (2019-06-08)

* `read-value` supports now `byte-array`.

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-databind "2.9.9"] is available but we use "2.9.7"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.9.9"] is available but we use "2.9.7"
```

## 0.2.2 (2018-09-22)

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-databind "2.9.7"] is available but we use "2.9.5"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.9.7"] is available but we use "2.9.5"
```

## 0.2.1 (2018-05-23)

* Add support for `:bigdecimals` option in `object-mapper` to parse floats into `BigDecimal` instead of `Double`

## 0.2.0 (2018-05-01)

* **BREAKING**: Requires Java1.8
* Added support to all `java.time` Classes via `com.fasterxml.jackson.datatype.jsr310/JavaTimeModule`.
* New `:modules` option for `object-mapper` to setup modules:

```clj
(require '[jsonista.core :as j])

;; [com.fasterxml.jackson.datatype/jackson-datatype-joda "2.9.5"]
(import '[com.fasterxml.jackson.datatype.joda JodaModule])
(import '[org.joda.time DateTime])

(j/write-value-as-string
  {:time (DateTime. 0)}
  (j/object-mapper
    {:modules [(JodaModule.)]}))
; "{\"time\":\"1970-01-01T00:00:00.000Z\"}"
```

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-databind "2.9.5"] is available but we use "2.9.3"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.9.5"]
```

## 0.1.1 (2018-01-09)

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-databind "2.9.3"] is available but we use "2.9.2"
```

* Removed deps:

```clj
[com.fasterxml.jackson.core/jackson-core "2.9.2"]
```

## 0.1.0 (2017-12-04)

* Initial release.
