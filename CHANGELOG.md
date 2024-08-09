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
