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

      (json/read-value +data+ (json/object-mapper {:decode-key-fn true}))
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
    DelaySerializer
    FunctionalKeyDeserializer
    FunctionalSerializer
    KeywordSerializer
    KeywordKeyDeserializer
    HashMapDeserializer
    ArrayListDeserializer
    PersistentHashMapDeserializer
    PersistentVectorDeserializer
    SymbolSerializer
    RatioSerializer FunctionalKeywordSerializer)
   (com.fasterxml.jackson.core JsonGenerator$Feature JsonFactory)
   (com.fasterxml.jackson.annotation JsonInclude$Include)
   (com.fasterxml.jackson.databind
    JsonSerializer ObjectMapper SequenceWriter
    SerializationFeature DeserializationFeature Module)
   (com.fasterxml.jackson.databind.module SimpleModule)
   (java.io InputStream Writer File OutputStream DataOutput Reader)
   (java.net URL)
   (com.fasterxml.jackson.datatype.jsr310 JavaTimeModule)
   (java.util Iterator List Map Date)
   (clojure.lang Delay Keyword Ratio Symbol)))

(defn- ^Module clojure-module
  "Create a Jackson Databind module to support Clojure datastructures.

  See [[object-mapper]] docstring for the documentation of the options."
  [{:keys [encode-key-fn decode-key-fn encoders date-format]
    :or {encode-key-fn true, decode-key-fn false}}]
  (doto (SimpleModule. "Clojure")
    (.addDeserializer List (PersistentVectorDeserializer.))
    (.addDeserializer Map (PersistentHashMapDeserializer.))
    (.addSerializer Keyword (KeywordSerializer. false))
    (.addSerializer Ratio (RatioSerializer.))
    (.addSerializer Symbol (SymbolSerializer.))
    (.addSerializer Date (if date-format
                           (DateSerializer. date-format)
                           (DateSerializer.)))
    (.addSerializer Delay (DelaySerializer.))
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
     (true? encode-key-fn) (.addKeySerializer Keyword (KeywordSerializer. true))
     (fn? encode-key-fn) (.addKeySerializer Keyword (FunctionalKeywordSerializer. encode-key-fn)))))

(defn ^:no-doc ^Module java-collection-module
  "Create a Jackson Databind module to support Java HashMap and ArrayList."
  []
  (doto (SimpleModule. "JavaCollectionModule")
    (.addDeserializer List (ArrayListDeserializer.))
    (.addDeserializer Map (HashMapDeserializer.))))

(defn ^ObjectMapper object-mapper
  "Create an ObjectMapper with Clojure support.

  The optional first parameter is a map of options. The following options are
  available:

  | Mapper options      |                                                            |
  | ------------------- | ---------------------------------------------------------- |
  | `:modules`          | vector of extra ObjectMapper modules                       |
  | `:factory`          | A Jackson JsonFactory for this given mapper                |
  | `:mapper`           | The base ObjectMapper to start with - overrides `:factory` |

  | Encoding options              |                                                                                                                                   |
  |-------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
  | `:order-by-keys`              | set to true to order map keys alphabetically                                                                                      |
  | `:pretty`                     | set to true use Jacksons pretty-printing defaults                                                                                 |
  | `:escape-non-ascii`           | set to true to escape non ascii characters                                                                                        |
  | `:strip-nils`                 | remove any keys that have nil values                                                                                              |
  | `:strip-empties`              | remove any keys that have nil or empty (\"\", {}, [], etc) values                                                                 |
  | `:do-not-fail-on-empty-beans` | serialize objects with no accessors as empty objects instead of throwing an exception                                             |
  | `:date-format`                | string for custom date formatting. default: `yyyy-MM-dd'T'HH:mm:ss'Z'`                                                            |
  | `:encode-key-fn`              | true to coerce keyword keys to strings, false to leave them as keywords, or a function to provide custom coercion (default: true) |
  | `:encoders`                   | a map of custom encoders where keys should be types and values should be encoder functions                                        |
  | `:close`                      | close OutputStreams & other closeable targets after write-value (default: true) |

  Encoder functions take two parameters: the value to be encoded and a
  JsonGenerator object. The function should call JsonGenerator methods to emit
  the desired JSON.

  | Decoding options    |                                                                |
  | ------------------- | -------------------------------------------------------------- |
  | `:decode-key-fn`    | true to coerce keys to keywords, false to leave them as strings, or a function to provide custom coercion (default: false) |
  | `:bigdecimals`      | true to decode doubles as BigDecimals (default: false) |"
  ([] (object-mapper {}))
  ([options]
   (let [factory (:factory options)
         maybe-mapper (:mapper options)
         base-mapper (cond
                       maybe-mapper maybe-mapper
                       factory (ObjectMapper. ^JsonFactory factory)
                       :else (ObjectMapper.))
         mapper (doto ^ObjectMapper base-mapper
                  (.registerModule (JavaTimeModule.))
                  (.registerModule (clojure-module options))
                  (cond->
                   (:order-by-keys options) (.configure SerializationFeature/ORDER_MAP_ENTRIES_BY_KEYS true)
                   (:pretty options) (.enable SerializationFeature/INDENT_OUTPUT)
                   (:bigdecimals options) (.enable DeserializationFeature/USE_BIG_DECIMAL_FOR_FLOATS)
                   (:strip-nils options) (.setSerializationInclusion JsonInclude$Include/NON_NULL)
                   (:strip-empties options) (.setSerializationInclusion JsonInclude$Include/NON_EMPTY)
                   (:do-not-fail-on-empty-beans options) (.disable SerializationFeature/FAIL_ON_EMPTY_BEANS)
                   (:escape-non-ascii options) (doto (-> .getFactory (.enable JsonGenerator$Feature/ESCAPE_NON_ASCII)))
                   (contains? options :close) (.configure JsonGenerator$Feature/AUTO_CLOSE_TARGET (boolean (:close options)))))]
     (doseq [module (:modules options)]
       (.registerModule mapper module))
     (.disable mapper SerializationFeature/WRITE_DATES_AS_TIMESTAMPS)
     mapper)))

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

(defprotocol ReadValues
  (-read-values [this mapper]))

(extend-protocol ReadValues

  (Class/forName "[B")
  (-read-values [this ^ObjectMapper mapper]
    (.readValues (.readerFor mapper ^Class Object) ^bytes this))

  nil
  (-read-values [_ _])

  File
  (-read-values [this ^ObjectMapper mapper]
    (.readValues (.readerFor mapper ^Class Object) this))

  URL
  (-read-values [this ^ObjectMapper mapper]
    (.readValues (.readerFor mapper ^Class Object) this))

  String
  (-read-values [this ^ObjectMapper mapper]
    (.readValues (.readerFor mapper ^Class Object) this))

  Reader
  (-read-values [this ^ObjectMapper mapper]
    (.readValues (.readerFor mapper ^Class Object) this))

  InputStream
  (-read-values [this ^ObjectMapper mapper]
    (.readValues (.readerFor mapper ^Class Object) this)))

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

(defprotocol WriteAll
  (-write-all [this ^SequenceWriter writer]))

(extend-protocol WriteAll

  (Class/forName "[Ljava.lang.Object;")
  (-write-all [this ^SequenceWriter w]
    (.writeAll w ^"[Ljava.lang.Object;" this))

  Iterable
  (-write-all [this ^SequenceWriter w]
    (.writeAll w this)))

(defprotocol WriteValues
  (-write-values [this values mapper])
  (-write-values-as-array [this values mapper]))

(defmacro ^:private -write-values*
  [method this value mapper]
  `(doto ^SequenceWriter
       (-write-all
        ~value
        (-> ~mapper
            (.writerFor Object)
            (.withRootValueSeparator "\n")
            (.without SerializationFeature/FLUSH_AFTER_WRITE_VALUE)
            (. ~method ~this)))
     (.close)))


(extend-protocol WriteValues
  File
  (-write-values [this value ^ObjectMapper mapper]
    (-write-values* writeValues this value mapper))
  (-write-values-as-array [this value ^ObjectMapper mapper]
    (-write-values* writeValuesAsArray this value mapper))

  OutputStream
  (-write-values [this value ^ObjectMapper mapper]
    (-write-values* writeValues this value mapper))
  (-write-values-as-array [this value ^ObjectMapper mapper]
    (-write-values* writeValuesAsArray this value mapper))

  DataOutput
  (-write-values [this value ^ObjectMapper mapper]
    (-write-values* writeValues this value mapper))
  (-write-values-as-array [this value ^ObjectMapper mapper]
    (-write-values* writeValuesAsArray this value mapper))

  Writer
  (-write-values [this value ^ObjectMapper mapper]
    (-write-values* writeValues this value mapper))
  (-write-values-as-array [this value ^ObjectMapper mapper]
    (-write-values* writeValuesAsArray this value mapper)))

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

(defn- wrap-values
  [^Iterator iterator]
  (when iterator
    (reify
      Iterable
      (iterator [this] iterator)
      Iterator
      (hasNext [this] (.hasNext iterator))
      (next [this] (.next iterator))
      (remove [this] (.remove iterator))
      clojure.lang.IReduceInit
      (reduce [_ f val]
        (loop [ret val]
          (if (.hasNext iterator)
            (let [ret (f ret (.next iterator))]
              (if (reduced? ret)
                @ret
                (recur ret)))
            ret)))
      clojure.lang.Sequential)))

(defn read-values
  "Decodes a sequence of values from a JSON as an iterator
  from anything that satisfies [[ReadValue]] protocol.
  By default, File, URL, String, Reader and InputStream are supported.

  The returned object is an Iterable, Iterator and IReduceInit.
  It can be reduced on via [[reduce]] and turned into a lazy sequence
  via [[iterator-seq]].

  To configure, pass in an ObjectMapper created with [[object-mapper]],
  see [[object-mapper]] docstring for the available options."
  ([object]
   (wrap-values (-read-values object default-object-mapper)))
  ([object ^ObjectMapper mapper]
   (wrap-values (-read-values object mapper))))

(defn write-values
  "Encodes a sequence of values as JSON, separating values with a line return.
  By default, `to` can be a File, OutputStream, DataOutput or Writer.

  By default, `values` can be an array or an Iterable.

  To configure, pass in an ObjectMapper created with [[object-mapper]],
  see [[object-mapper]] docstring for the available options."
  ([to values]
   (-write-values to values default-object-mapper))
  ([to values ^ObjectMapper mapper]
   (-write-values to values mapper)))

(defn write-values-as-array
  "Encodes a sequence of values as a JSON array.
  By default, `to` can be a File, OutputStream, DataOutput or Writer.

  By default, `values` can be an array or an Iterable.

  To configure, pass in an ObjectMapper created with [[object-mapper]],
  see [[object-mapper]] docstring for the available options."
  ([to values]
   (-write-values-as-array to values default-object-mapper))
  ([to values ^ObjectMapper mapper]
   (-write-values-as-array to values mapper)))
