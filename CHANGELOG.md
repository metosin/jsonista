## Unreleased

* `read-value` supports now `byte-array`.

* Updated deps:

```clj
[com.fasterxml.jackson.core/jackson-databind "2.9.8"] is available but we use "2.9.7"
[com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.9.8"] is available but we use "2.9.7"
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
