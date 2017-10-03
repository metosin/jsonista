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

  ;; 1005ns
  (title "encode: cheshire")
  (let [encode (fn [] (cheshire/generate-string {"hello" "world"}))]
    (assert (= +json+ (encode)))
    (cc/quick-bench (encode)))

  ;; 249ns
  ;; 201ns
  (title "encode: jsonista")
  (let [encode (fn [] (json/write-value-as-string {"hello" "world"}))]
    (assert (= +json+ (encode)))
    (cc/quick-bench (encode)))

  ;; 82ns
  (title "encode: str")
  (let [encode (fn [] (str "{\"hello\":\"" "world" "\"}"))]
    (assert (= +json+ (encode)))
    (cc/quick-bench (encode))))

(defn decode-perf []

  ;; 896ns
  (title "decode: cheshire")
  (let [decode (fn [] (cheshire/parse-string-strict +json+))]
    (assert (= +data+ (decode)))
    (cc/quick-bench (decode)))

  ;; 416ns
  ;; 378ns
  (title "decode: jsonista")
  (let [decode (fn [] (json/read-value +json+))]
    (assert (= +data+ (decode)))
    (cc/quick-bench (decode)))

  ;; 246ns
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
    (let [encode-cheshire (fn [] (cheshire/generate-string data))
          encode-jsonista (fn [] (json/write-value-as-string data))]

      (title file)
      (println data)

      ;  1.1µs (10b)
      ;  2.6µs (100b)
      ;  8.7µs (1k)
      ;   92µs (10k)
      ;  915µs (100k)
      (title "encode: cheshire")
      (assert (= json (encode-cheshire)))
      (cc/quick-bench (encode-cheshire))

      ;  0.2µs (10b)  - +450%
      ;  0.6µs (100b) - +330%
      ;  3.2µs (1k)   - +170%
      ;   36µs (10k)  - +150%
      ;  360µs (100k) - +150%
      (title "encode: jsonista")
      (assert (= json (encode-jsonista)))
      (cc/quick-bench (encode-jsonista)))))

(comment
  (encode-perf-different-sizes))

(defn decode-perf-different-sizes []
  (doseq [file ["dev-resources/json10b.json"
                "dev-resources/json100b.json"
                "dev-resources/json1k.json"
                "dev-resources/json10k.json"
                "dev-resources/json100k.json"]
          :let [data (cheshire/parse-string (slurp file))
                json (cheshire/generate-string data)]]
    (let [decode-cheshire (fn [] (cheshire/parse-string json))
          decode-jsonista (fn [] (json/read-value json))]

      (title file)

      ;  1.0µs (10b)
      ;  2.0µs (100b)
      ;   10µs (1k)
      ;  110µs (10k)
      ; 1000µs (100k)
      (title "decode: cheshire")
      (assert (= data (decode-cheshire)))
      (cc/quick-bench (decode-cheshire))

      ;  0.4µs (10b)  - +150%
      ;  1.5µs (100b) -  +30%
      ;  7.6µs (1k)   -  +30%
      ;   84µs (10k)  -  +30%
      ;  770µs (100k) -  +30%
      (title "decode: jsonista")
      (assert (= data (decode-jsonista)))
      (cc/quick-bench (decode-jsonista)))))


(comment
  (encode-perf)
  (decode-perf)
  (encode-perf-different-sizes)
  (decode-perf-different-sizes))
