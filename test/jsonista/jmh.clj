(ns jsonista.jmh
  (:require [jsonista.core :as j]
            [cheshire.core :as cheshire]
            [clojure.data.json :as json]
            [jsonista.tagged :as jt])
  (:import (tools.jackson.databind.json JsonMapper)
           (clojure.lang Keyword)))

(set! *warn-on-reflection* true)

;;
;; run with lein jmh '{:file "benchmarks.edn", :type :quick, :format :table}'
;; README numbers (2026-07-31) measured with:
;;
;; MacBook Pro (Apple Silicon)
;; OpenJDK 25.0.2 (Corretto)
;; 1 fork, 3x3s warmup + 5x3s measurement per point
;;

(defn json-data [size] (slurp (str "dev-resources/json" size ".json")))
(defn edn-data [size] (cheshire/parse-string (json-data size)))
(defn edn-data-kw [size] (cheshire/parse-string (json-data size) true))

(defn encode-data-json [x] (json/write-str x))
(defn decode-data-json [x] (json/read-str x))

(defn encode-cheshire [x] (cheshire/generate-string x))
(defn decode-cheshire [x] (cheshire/parse-string x))

(defn encode-jsonista [x] (j/write-value-as-string x))
(defn decode-jsonista [x] (j/read-value x))

(let [mapper (j/object-mapper {:modules [(j/java-collection-module)]})]
  (defn encode-jsonista-fast [x] (.writeValueAsString mapper x))
  (defn decode-jsonista-fast [x] (.readValue mapper ^String x ^Class Object)))

(let [mapper ^JsonMapper (.build (JsonMapper/builder))]
  (defn encode-jackson [x] (.writeValueAsString mapper x))
  (defn decode-jackson [x] (.readValue mapper ^String x ^Class Object)))

;; keyword-keys workload: encode/decode with keyword map keys.
;; cheshire is untouched by jsonista changes and serves as the cross-run
;; noise control for this group.
(defn encode-jsonista-kw [x] (j/write-value-as-string x j/keyword-keys-object-mapper))
(defn encode-jsonista-kw-bytes [x] (j/write-value-as-bytes x j/keyword-keys-object-mapper))
(defn decode-jsonista-kw [x] (j/read-value x j/keyword-keys-object-mapper))

(defn encode-cheshire-kw [x] (cheshire/generate-string x))
(defn decode-cheshire-kw [x] (cheshire/parse-string x true))

(defn encode-data-json-kw [x] (json/write-str x))
(defn decode-data-json-kw [x] (json/read-str x :key-fn keyword))

;; NOTE: these deliberately no longer shadow encode-jsonista/decode-jsonista —
;; the shadowing meant the :encode/:decode benchmarks measured a tagged-module
;; mapper instead of stock jsonista (see bench/2026-07-31-comparison.md, §5).
(let [mapper (j/object-mapper
               {:modules [(jt/module
                            {:handlers {Keyword {:tag "!k"
                                                 :encode jt/encode-keyword
                                                 :decode keyword}}})]})]
  (defn encode-jsonista-tagged [x] (j/write-value-as-string x mapper))
  (defn decode-jsonista-tagged [x] (j/read-value x mapper)))
