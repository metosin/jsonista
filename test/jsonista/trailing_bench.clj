(ns jsonista.trailing-bench
  "Isolates the decode cost of Jackson 3's FAIL_ON_TRAILING_TOKENS default.

  Both mappers are identical except for that single feature, and both run in
  one JMH invocation, so the comparison is within-run and carries no
  cross-run drift. Driven by benchmarks-trailing.edn; the main
  benchmarks.edn is deliberately left untouched."
  (:require [jsonista.core :as j]
            [jsonista.tagged :as jt])
  (:import (tools.jackson.databind DeserializationFeature ObjectMapper)
           (tools.jackson.databind.cfg MapperBuilder)
           (clojure.lang Keyword)))

(set! *warn-on-reflection* true)

(defn json-data [size] (slurp (str "dev-resources/json" size ".json")))

(def ^ObjectMapper strict-mapper
  "Stock Jackson 3 defaults: FAIL_ON_TRAILING_TOKENS enabled.
  Mirrors the mapper jsonista.jmh uses for decode-jsonista."
  (j/object-mapper
   {:modules [(jt/module {:handlers {Keyword {:tag "!k"
                                             :encode jt/encode-keyword
                                             :decode keyword}}})]}))

(def ^ObjectMapper lenient-mapper
  "Identical to strict-mapper with FAIL_ON_TRAILING_TOKENS disabled — i.e.
  Jackson 2's decode behavior."
  (let [^MapperBuilder b (.rebuild strict-mapper)]
    (.build ^MapperBuilder
            (.disable b
                      ^"[Ltools.jackson.databind.DeserializationFeature;"
                      (into-array DeserializationFeature
                                  [DeserializationFeature/FAIL_ON_TRAILING_TOKENS])))))

(defn decode-strict [x] (j/read-value x strict-mapper))
(defn decode-lenient [x] (j/read-value x lenient-mapper))
