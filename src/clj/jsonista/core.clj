(ns jsonista.core
  "JSON encoding and decoding based on Jackson Databind.

  Encoding example:

      (require '[jsonista.core :as json])
      (json/write-value-as-string {:hello 1})
      ;; => \"{\\\"hello\\\":1}\"

  Decoding example:

      (def +data+ (json/write-value-as-string {:foo \"bar\"}))
      (json/read-value +data+)
      ;; => {\"foo\" \"bar\"}

  ## Configuration

  You can configure encoding and decoding by creating a custom mapper object
  with jsonista.core/object-mapper. The options are passed in as a map.

  For example, to convert map keys into keywords while decoding:

      (json/from-json +data+ (json/object-mapper {:keywordize? true}))
      ;; => {:foo \"bar\"}

  See the docstring of [[object-mapper]] for all available options.

  ## Custom encoders

  Custom encoder is a function that take a value and a JsonGenerator object as
  the parameters. The function should call JsonGenerator methods to emit the
  desired JSON. This is the same as how custom encoders work in Cheshire.

  Custom encoders are configured by the object-mapper option :encoders, which is a
  map from types to encoder functions.

  For example, to encode java.awt.Color:

       (let [encoders {java.awt.Color (fn [color gen] (.writeString gen (str color)))}
             mapper (json/object-mapper {:encoders encoders})]
         (json/write-value-as-string (java.awt.Color. 1 2 3) mapper))
       ;; => \"\\\"java.awt.Color[r=1,g=2,b=3]\\\"\"

  ## Jsonista vs. Cheshire

  jsonista uses Jackson Databind while Cheshire uses Jackson Core. In our
  benchmarks, jsonista performs better than Cheshire (take look at
  json_perf_test.clj). On the other hand, Cheshire has a wider set of features
  and has been used in production much more."
  (:require [clojure.java.io :as io])
  (:import
    com.fasterxml.jackson.databind.ObjectMapper
    com.fasterxml.jackson.databind.module.SimpleModule
    com.fasterxml.jackson.databind.SerializationFeature
    com.fasterxml.jackson.core.JsonGenerator$Feature
    (jsonista.jackson
      DateSerializer
      FunctionalSerializer
      KeywordSerializer
      KeywordKeyDeserializer
      PersistentHashMapDeserializer
      PersistentVectorDeserializer
      SymbolSerializer
      RatioSerializer)
    (java.io InputStream InputStreamReader Writer File OutputStream DataOutput)))

(set! *warn-on-reflection* true)

(defn- make-clojure-module
  "Create a Jackson Databind module to support Clojure datastructures.

  See [[object-mapper]] docstring for the documentation of the options."
  [{:keys [keywordize? encoders date-format]}]
  (doto (SimpleModule. "Clojure")
    (.addDeserializer java.util.List (PersistentVectorDeserializer.))
    (.addDeserializer java.util.Map (PersistentHashMapDeserializer.))
    (.addSerializer clojure.lang.Keyword (KeywordSerializer. false))
    (.addSerializer clojure.lang.Ratio (RatioSerializer.))
    (.addSerializer clojure.lang.Symbol (SymbolSerializer.))
    (.addKeySerializer clojure.lang.Keyword (KeywordSerializer. true))
    (.addSerializer java.util.Date (if date-format
                                     (DateSerializer. date-format)
                                     (DateSerializer.)))
    (as-> module
          (doseq [[cls encoder-fn] encoders]
            (.addSerializer module cls (FunctionalSerializer. encoder-fn))))
    (cond->
      ;; This key deserializer decodes the map keys into Clojure keywords.
      keywordize? (.addKeyDeserializer Object (KeywordKeyDeserializer.)))))

(defn ^ObjectMapper object-mapper
  "Create an ObjectMapper with Clojure support.

  The optional first parameter is a map of options. The following options are
  available:

  | Encoding options                                                     ||
  | ------------------- | ------------------------------------------------- |
  | `:pretty`           | set to true use Jacksons pretty-printing defaults |
  | `:escape-non-ascii` | set to true to escape non ascii characters        |
  | `:date-format`      | string for custom date formatting. default: `yyyy-MM-dd'T'HH:mm:ss'Z'`  |
  | `:encoders`         | a map of custom encoders where keys should be types and values should be encoder functions |

  Encoder functions take two parameters: the value to be encoded and a
  JsonGenerator object. The function should call JsonGenerator methods to emit
  the desired JSON.

  | Decoding options |                                                                |
  | ---------------- | -------------------------------------------------------------- |
  | `:keywordize?`   | set to true to convert map keys into keywords (default: false) |"
  ([] (object-mapper {}))
  ([options]
   (doto (ObjectMapper.)
     (.registerModule (make-clojure-module options))
     (cond-> (:pretty options) (.enable SerializationFeature/INDENT_OUTPUT)
             (:escape-non-ascii options) (.enable ^"[Lcom.fasterxml.jackson.core.JsonGenerator$Feature;"
                                                  (into-array [JsonGenerator$Feature/ESCAPE_NON_ASCII]))))))

(def ^ObjectMapper +default-mapper+
  "The default ObjectMapper instance."
  (object-mapper {}))

;;
;; Protocols
;;

(defprotocol ReadValue
  (-read-value [this mapper]))

(extend-protocol ReadValue
  String
  (-read-value [this ^ObjectMapper mapper]
    (.readValue mapper this ^Class Object))

  InputStreamReader
  (-read-value [this ^ObjectMapper mapper]
    (.readValue mapper this ^Class Object))

  InputStream
  (-read-value [this ^ObjectMapper mapper]
    (.readValue mapper this ^Class Object))

  nil
  (-read-value [this mapper]))

(defprotocol WriteValue
  (-write-value [this value mapper]))

(extend-protocol WriteValue
  File
  (-write-value [this value ^ObjectMapper mapper]
    (.writeValue mapper this value))

  OutputStream
  (-write-value [this value ^ObjectMapper mapper]
    (.writeValue mapper this value))

  DataOutput
  (-write-value [this value ^ObjectMapper mapper]
    (.writeValue mapper this value))

  Writer
  (-write-value [this value ^ObjectMapper mapper]
    (.writeValue mapper this value)))

;;
;; public api
;;

(defn read-value
  "Decode a value from a JSON string, InputStream, InputStreamReader or
  anything that satisfies jsonista.core/ReadValue protocol.

  To configure, pass in an ObjectMapper created with [[object-mapper]]."
  ([object]
   (-read-value object +default-mapper+))
  ([object ^ObjectMapper mapper]
   (-read-value object mapper)))

(defn write-value-as-string
  "Encode a value as a JSON string.

  To configure, pass in an ObjectMapper created with [[object-mapper]]."
  ([object]
   (.writeValueAsString +default-mapper+ object))
  ([object ^ObjectMapper mapper]
   (.writeValueAsString mapper object)))

(defn write-value-as-bytes
  "Encode a value as a JSON byte-array.

  To configure, pass in an ObjectMapper created with [[object-mapper]]."
  ([object]
   (.writeValueAsBytes +default-mapper+ object))
  ([object ^ObjectMapper mapper]
   (.writeValueAsBytes mapper object)))

(defn write-value
  "Encode a value as JSON and write using the provided [[WriteValue]] instance.
  File, OutputStream, DataOutput and Writer are supported by default.

  To configure, pass in an ObjectMapper created with [[object-mapper]], or pass in a map with options.
  See [[object-mapper]] docstring for the available options."
  ([object writer]
   (-write-value +default-mapper+ object writer))
  ([object to ^ObjectMapper mapper]
   (-write-value to object mapper)))
