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
