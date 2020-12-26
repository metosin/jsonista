(ns jsonista.tagged-value
  (:import [jsonista.jackson FunctionalSerializer TaggedValueOrPersistentVectorDeserializer]
           [com.fasterxml.jackson.core JsonGenerator]
           [com.fasterxml.jackson.databind.module SimpleModule]))

(defn- encode-keyword [^String tag ^clojure.lang.Keyword kw ^JsonGenerator gen]
  (.writeStartArray gen)
  (.writeString gen tag)
  (.writeString gen (.substring (.toString kw) 1))
  (.writeEndArray gen))

(defn module
  "Create a Jackson Databind module to support losslessly encoded tagged values.

   This provides both encoders and decoders and is a means of using jsonista to replace something
   like transit, while still maintaining support for more EDN types. Types are encoded using a JSON
   list and a customizable tag. For example, `:foo/bar` would serialize to `[\"!kw\", \"foo/bar\"]`
   by default, where the first string in the JSON list is a tag for what follows.

   For now, supported additional types are:

   * Keyword"
  ^SimpleModule
  [{:keys [keyword-tag]
    :or {keyword-tag "!kw"}}]
  (doto (SimpleModule. "TaggedValue")
    (.addDeserializer java.util.List (TaggedValueOrPersistentVectorDeserializer. ^String keyword-tag))
    (.addSerializer clojure.lang.Keyword (FunctionalSerializer. #(encode-keyword keyword-tag %1 %2)))))
