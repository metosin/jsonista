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

      (json/from-json +data+ (json/object-mapper {:decode-key-fn true}))
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
  (:import
    (jsonista.jackson
      DateSerializer
      FunctionalKeyDeserializer
      FunctionalSerializer
      KeywordSerializer
      KeywordKeyDeserializer
      PersistentHashMapDeserializer
      PersistentVectorDeserializer
      SymbolSerializer
      RatioSerializer FunctionalKeywordSerializer)
    (com.fasterxml.jackson.core JsonGenerator$Feature)
    (com.fasterxml.jackson.databind
      JsonSerializer
      ObjectMapper
      module.SimpleModule
      SerializationFeature DeserializationFeature)
    (com.fasterxml.jackson.databind.module SimpleModule)
    (java.io InputStream Writer File OutputStream DataOutput Reader)
    (java.net URL)
    (com.fasterxml.jackson.datatype.jsr310 JavaTimeModule)))

(defn- clojure-module
  "Create a Jackson Databind module to support Clojure datastructures.

  See [[object-mapper]] docstring for the documentation of the options."
  [{:keys [encode-key-fn decode-key-fn encoders date-format]
    :or {encode-key-fn true, decode-key-fn false}}]
  (doto (SimpleModule. "Clojure")
    (.addDeserializer java.util.List (PersistentVectorDeserializer.))
    (.addDeserializer java.util.Map (PersistentHashMapDeserializer.))
    (.addSerializer clojure.lang.Keyword (KeywordSerializer. false))
    (.addSerializer clojure.lang.Ratio (RatioSerializer.))
    (.addSerializer clojure.lang.Symbol (SymbolSerializer.))
    (.addSerializer java.util.Date (if date-format
                                     (DateSerializer. date-format)
                                     (DateSerializer.)))
    (as-> module
          (doseq [[type encoder] encoders]
            (cond
              (instance? JsonSerializer encoder) (.addSerializer module type encoder)
              (fn? encoder) (.addSerializer module type (FunctionalSerializer. encoder))
              :else (throw (ex-info
                             (str "Can't register encoder " encoder " for type " type)
                             {:type type, :encoder encoder})))))
    (cond->
      (true? decode-key-fn) (.addKeyDeserializer Object (KeywordKeyDeserializer.))
      (fn? decode-key-fn) (.addKeyDeserializer Object (FunctionalKeyDeserializer. decode-key-fn))
      (true? encode-key-fn) (.addKeySerializer clojure.lang.Keyword (KeywordSerializer. true))
      (fn? encode-key-fn) (.addKeySerializer clojure.lang.Keyword (FunctionalKeywordSerializer. encode-key-fn)))))

(defn ^ObjectMapper object-mapper
  "Create an ObjectMapper with Clojure support.

  The optional first parameter is a map of options. The following options are
  available:

  | Mapper options      |                                      |
  | ------------------- | -------------------------------------|
  | `:modules`          | vector of extra ObjectMapper modules |

  | Encoding options    |                                                   |
  | ------------------- | ------------------------------------------------- |
  | `:pretty`           | set to true use Jacksons pretty-printing defaults |
  | `:escape-non-ascii` | set to true to escape non ascii characters        |
  | `:date-format`      | string for custom date formatting. default: `yyyy-MM-dd'T'HH:mm:ss'Z'`  |
  | `:encode-key-fn`    | true to coerce keyword keys to strings, false to leave them as keywords, or a function to provide custom coercion (default: true) |
  | `:encoders`         | a map of custom encoders where keys should be types and values should be encoder functions |

  Encoder functions take two parameters: the value to be encoded and a
  JsonGenerator object. The function should call JsonGenerator methods to emit
  the desired JSON.

  | Decoding options    |                                                                |
  | ------------------- | -------------------------------------------------------------- |
  | `:decode-key-fn`    |  true to coerce keys to keywords, false to leave them as strings, or a function to provide custom coercion (default: false) |
  | `:bigdecimals`      |  true to decode doubles as BigDecimals (default: false) |"
  ([] (object-mapper {}))
  ([options]
   (doto (ObjectMapper.)
     (.registerModule (JavaTimeModule.))
     (.registerModule (clojure-module options))
     (cond-> (:pretty options) (.enable SerializationFeature/INDENT_OUTPUT)
             (:bigdecimals options) (.enable DeserializationFeature/USE_BIG_DECIMAL_FOR_FLOATS)
             (:escape-non-ascii options) (doto (-> .getFactory (.enable JsonGenerator$Feature/ESCAPE_NON_ASCII))))
     (as-> mapper
           (doseq [module (:modules options)]
             (.registerModule mapper module)))
     (.disable SerializationFeature/WRITE_DATES_AS_TIMESTAMPS))))

(def ^:deprecated ^ObjectMapper +default-mapper+
  "DEPRECATED: The default ObjectMapper instance."
  (object-mapper {}))

(def ^ObjectMapper default-object-mapper
  "The default ObjectMapper instance."
  (object-mapper {}))

(def ^ObjectMapper keyword-keys-object-mapper
  "ObjectMapper instance that uses keyword keys for maps"
  (object-mapper {:encode-key-fn true, :decode-key-fn true}))

;;
;; Protocols
;;

(defprotocol ReadValue
  (-read-value [this mapper]))

(extend-protocol ReadValue

  (Class/forName "[B")
  (-read-value [this ^ObjectMapper mapper]
    (.readValue mapper ^bytes this ^Class Object))

  nil
  (-read-value [_ _])

  File
  (-read-value [this ^ObjectMapper mapper]
    (.readValue mapper this ^Class Object))

  URL
  (-read-value [this ^ObjectMapper mapper]
    (.readValue mapper this ^Class Object))

  String
  (-read-value [this ^ObjectMapper mapper]
    (.readValue mapper this ^Class Object))

  Reader
  (-read-value [this ^ObjectMapper mapper]
    (.readValue mapper this ^Class Object))

  InputStream
  (-read-value [this ^ObjectMapper mapper]
    (.readValue mapper this ^Class Object)))

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
  "Decodes a value from a JSON from anything that
  satisfies [[ReadValue]] protocol. By default,
  File, URL, String, Reader and InputStream are supported.

  To configure, pass in an ObjectMapper created with [[object-mapper]],
  see [[object-mapper]] docstring for the available options."
  ([object]
   (-read-value object default-object-mapper))
  ([object ^ObjectMapper mapper]
   (-read-value object mapper)))

(defn ^String write-value-as-string
  "Encode a value as a JSON string.

  To configure, pass in an ObjectMapper created with [[object-mapper]]."
  ([object]
   (.writeValueAsString default-object-mapper object))
  ([object ^ObjectMapper mapper]
   (.writeValueAsString mapper object)))

(defn write-value-as-bytes
  "Encode a value as a JSON byte-array.

  To configure, pass in an ObjectMapper created with [[object-mapper]]."
  {:tag 'bytes}
  ([object]
   (.writeValueAsBytes default-object-mapper object))
  ([object ^ObjectMapper mapper]
   (.writeValueAsBytes mapper object)))

(defn write-value
  "Encode a value as JSON and write using the provided [[WriteValue]] instance.
  By default, File, OutputStream, DataOutput and Writer are supported.

  To configure, pass in an ObjectMapper created with [[object-mapper]],
  see [[object-mapper]] docstring for the available options."
  ([to object]
   (-write-value to object default-object-mapper))
  ([to object ^ObjectMapper mapper]
   (-write-value to object mapper)))
