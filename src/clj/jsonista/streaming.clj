(ns jsonista.streaming
  (:require [jsonista.core :as j])
  (:import [java.net URL]
           [java.io File Reader InputStream]
           [java.util Iterator]
           [com.fasterxml.jackson.core JsonToken JsonParser JsonFactory]
           [com.fasterxml.jackson.databind ObjectMapper]))

(defprotocol CreateParser
  (-create-parser [this om]))

(extend-protocol CreateParser
  (Class/forName "[B")
  (-create-parser [this ^JsonFactory factory]
    (.createParser factory ^bytes this))

  File
  (-create-parser [this ^JsonFactory factory]
    (.createParser factory this))

  URL
  (-create-parser [this ^JsonFactory factory]
    (.createParser factory this))

  String
  (-create-parser [this ^JsonFactory factory]
    (.createParser factory this))

  Reader
  (-create-parser [this ^JsonFactory factory]
    (.createParser factory this))

  InputStream
  (-create-parser [this ^JsonFactory factory]
    (.createParser factory this)))

(defn create-parser ^JsonParser
  ([this]
   (-create-parser this (.getFactory ^ObjectMapper j/default-object-mapper)))
  ([this ^ObjectMapper om]
   (-create-parser this (.getFactory om))))

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
  [^JsonParser parser]
  ;; Current token is empty (e.g. start of document) or just the token before array start
  (assert (= JsonToken/START_ARRAY (.nextToken parser)))
  ;; Current token is START_ARRAY
  (.nextToken parser)
  ;; Current token is the START_OBJECT for first object
  ;; Should stop at the END_ARRAY
  (wrap-values (.readValuesAs parser ^Class Object)))
