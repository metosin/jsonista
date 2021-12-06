(ns jsonista.jmh
  (:require [jsonista.core :as j]
            [cheshire.core :as cheshire]
            [clojure.data.json :as json]
            [jsonista.tagged :as jt])
  (:import (com.fasterxml.jackson.databind ObjectMapper)
           (clojure.lang Keyword)))

(set! *warn-on-reflection* true)

;;
;; run with lein jmh '{:file "benchmarks.edn", :type :quick, :format :table}'
;; perf measured with the following setup:
;;
;; Model Name:            MacBook Pro
;; Model Identifier:      MacBookPro11,3
;; Processor Name:        Intel Core i7
;; Processor Speed:       2,5 GHz
;; Number of Processors:  1
;; Total Number of Cores: 4
;; L2 Cache (per Core):   256 KB
;; L3 Cache:              6 MB
;; Memory:                16 GB
;;

(defn json-data [size] (slurp (str "dev-resources/json" size ".json")))
(defn edn-data [size] (cheshire/parse-string (json-data size)))

(defn encode-data-json [x] (json/write-str x))
(defn decode-data-json [x] (json/read-str x))

(defn encode-cheshire [x] (cheshire/generate-string x))
(defn decode-cheshire [x] (cheshire/parse-string x))

(defn encode-jsonista [x] (j/write-value-as-string x))
(defn decode-jsonista [x] (j/read-value x))

(let [mapper (j/object-mapper {:modules [(j/java-collection-module)]})]
  (defn encode-jsonista-fast [x] (.writeValueAsString mapper x))
  (defn decode-jsonista-fast [x] (.readValue mapper ^String x ^Class Object)))

(let [mapper (ObjectMapper.)]
  (defn encode-jackson [x] (.writeValueAsString mapper x))
  (defn decode-jackson [x] (.readValue mapper ^String x ^Class Object)))

(let [mapper (j/object-mapper
               {:modules [(jt/module
                            {:handlers {Keyword {:tag "!k"
                                                 :encode jt/encode-keyword
                                                 :decode keyword}}})]})]
  (defn encode-jsonista [x] (j/write-value-as-string x mapper))
  (defn decode-jsonista [x] (j/read-value x mapper)))
