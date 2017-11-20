(ns jsonista.json-perf-test
  (:require [criterium.core :as cc]
            [clojure.test :refer :all]
            [jsonista.test-utils :refer :all]
            [jsonista.core :as json]
            [cheshire.core :as cheshire])
  (:import (com.fasterxml.jackson.databind ObjectMapper)
           (java.util Map)))

(set! *warn-on-reflection* true)

;;
;; start repl with `lein perf repl`
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

(def ^String +json+ (cheshire.core/generate-string {"hello" "world"}))

(def +data+ (cheshire.core/parse-string +json+))

(defn encode-perf []

  ;; 1220ns
  (title "encode: cheshire")
  (assert (= +json+ (cheshire/generate-string {"hello" "world"})))
  (cc/quick-bench (cheshire/generate-string {"hello" "world"}))

  ;; 200ns
  (title "encode: jsonista")
  (assert (= +json+ (json/write-value-as-string {"hello" "world"})))
  (cc/quick-bench (json/write-value-as-string {"hello" "world"}))

  ;; 110ns
  (title "encode: str")
  (let [encode (fn [key value] (str "{\"" key "\":\"" value "\"}"))]
    (assert (= +json+ (encode "hello" "world")))
    (cc/quick-bench (encode "hello" "world"))))

(defn decode-perf []

  ;; 920ns
  (title "decode: cheshire")
  (assert (= +data+ (cheshire/parse-string-strict +json+)))
  (cc/quick-bench (cheshire/parse-string-strict +json+))

  ;; 412ns
  (title "decode: jsonista")
  (assert (= +data+ (json/read-value +json+)))
  (cc/quick-bench (json/read-value +json+))

  ;; 260ns
  (title "decode: jackson")
  (let [mapper (ObjectMapper.)
        decode (fn [] (.readValue mapper +json+ Map))]
    (assert (= +data+ (decode)))
    (cc/quick-bench (decode))))

(defn encode-perf-different-sizes []
  (doseq [file ["dev-resources/json10b.json"
                "dev-resources/json100b.json"
                "dev-resources/json1k.json"
                "dev-resources/json10k.json"
                "dev-resources/json100k.json"]
          :let [data (cheshire/parse-string (slurp file))
                json (cheshire/generate-string data)]]

    (title file)

    ;  1.2µs (10b)
    ;  3.0µs (100b)
    ; 12.6µs (1k)
    ;  134µs (10k)
    ; 1290µs (100k)
    (title "encode: cheshire")
    (assert (= json (cheshire/generate-string data)))
    (cc/quick-bench (cheshire/generate-string data))

    ; 0.23µs (10b)
    ; 0.58µs (100b)
    ;  3.3µs (1k)
    ;   36µs (10k)
    ;  380µs (100k)
    (title "encode: jsonista")
    (assert (= json (json/write-value-as-string data)))
    (cc/quick-bench (json/write-value-as-string data))))

(defn decode-perf-different-sizes []
  (doseq [file ["dev-resources/json10b.json"
                "dev-resources/json100b.json"
                "dev-resources/json1k.json"
                "dev-resources/json10k.json"
                "dev-resources/json100k.json"]
          :let [data (cheshire/parse-string (slurp file))
                json (cheshire/generate-string data)]]

    (title file)

    ;  1.0µs (10b)
    ;  2.2µs (100b)
    ;  9.3µs (1k)
    ;  106µs (10k)
    ; 1010µs (100k)
    (title "decode: cheshire")
    (assert (= data (cheshire/parse-string json)))
    (cc/quick-bench (cheshire/parse-string json))

    ; 0.40µs (10b)
    ;  1.6µs (100b)
    ;  7.8µs (1k)
    ;   84µs (10k)
    ;  806µs (100k)
    (title "decode: jsonista")
    (assert (= data (json/read-value json)))
    (cc/quick-bench (json/read-value json))))

(comment
  (encode-perf)
  (decode-perf)
  (encode-perf-different-sizes)
  (decode-perf-different-sizes))
