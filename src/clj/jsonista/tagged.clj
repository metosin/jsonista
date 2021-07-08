(ns jsonista.tagged
  (:import (jsonista.jackson FunctionalSerializer TaggedValueOrPersistentVectorDeserializer)
           (com.fasterxml.jackson.core JsonGenerator)
           (com.fasterxml.jackson.databind.module SimpleModule)
           (clojure.lang Keyword Symbol)
           (java.util List)
           (com.fasterxml.jackson.databind ObjectMapper)))

(defn ^FunctionalSerializer serializer [^String tag encoder]
  (FunctionalSerializer.
    (fn [value ^JsonGenerator gen]
      (.writeStartArray gen)
      (.writeString gen tag)
      (encoder value gen)
      (.writeEndArray gen))))

(defn encode-keyword [^Keyword x ^JsonGenerator gen]
  (let [^Symbol s (.sym x)]
    (.writeStartArray gen)
    (when-let [namespace (.getNamespace s)]
      (.writeString gen namespace))
    (.writeString gen (.getName s))
    (.writeEndArray gen)))

(defn decode-keyword [[^String arg1 & [^String arg2 :as args]]]
  (if (seq args)
    (Keyword/intern arg1 arg2)
    (Keyword/intern arg1)))

(defn encode-collection [es ^JsonGenerator gen]
  (let [mapper ^ObjectMapper (.getCodec gen)]
    (.writeStartArray gen)
    (doseq [e es] (.writeRawValue gen (.writeValueAsString mapper e)))
    (.writeEndArray gen)))

(defn encode-str [^Object o ^JsonGenerator gen]
  (.writeString gen (.toString o)))

(defn module
  "Create a Jackson Databind module to support losslessly encoded tagged values.

   This provides both encoders and decoders and is a means of using jsonista to replace something
   like transit, while still maintaining support for more EDN types. Types are encoded using a JSON
   list and a customizable tag. For example, `:foo/bar` would serialize to `[\"!kw\", [\"foo\", \"bar\"]]`
   by default, where the first string in the JSON list is a tag for what follows.

       (def mapper (j/object-mapper
                     {:decode-key-fn true
                      :modules [(jt/module
                                  {:handlers {Keyword {:tag \"!kw\"
                                                       :encode jt/encode-keyword
                                                       :decode jt/decode-keyword}
                                              PersistentHashSet {:tag \"!set\"
                                                                 :encode jt/encode-collection
                                                                 :decode set}}})]}))

      (-> {:kikka #{:kukka :kakka}}
          (j/write-value-as-string mapper)
          (doto prn)
          (j/read-value mapper))
      ; prints \"{\\\"kikka\\\":[\\\"!set\\\",[[\\\"!kw\\\",[\\\"kukka\\\"]],[\\\"!kw\\\",[\\\"kakka\\\"]]]]}\"
      ; => {:kikka #{:kukka :kakka}}"
  ^SimpleModule
  [{:keys [handlers]}]
  (let [tags (->> handlers (vals) (map :tag))]
    (assert (apply distinct? tags) (str "non-distinct tags found: " tags)))
  (let [decoders (->> (for [[_ {:keys [tag decode]}] handlers] [tag decode]) (into {}))]
    (reduce-kv
      (fn [^SimpleModule module t {:keys [tag encode] :or {encode encode-str}}]
        (.addSerializer module t (serializer tag encode)))
      (doto (SimpleModule. "TaggedValue")
        (.addDeserializer List (TaggedValueOrPersistentVectorDeserializer. decoders)))
      handlers)))
