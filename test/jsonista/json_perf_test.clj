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

  ;; 1160ns
  (title "encode: cheshire")
  (assert (= +json+ (cheshire/generate-string {"hello" "world"})))
  (cc/quick-bench (cheshire/generate-string {"hello" "world"}))

  ;; 224ns
  (title "encode: jsonista")
  (assert (= +json+ (json/write-value-as-string {"hello" "world"})))
  (cc/quick-bench (json/write-value-as-string {"hello" "world"}))

  ;; 116ns
  (title "encode: str")
  (let [encode (fn [key value] (str "{\"" key "\":\"" value "\"}"))]
    (assert (= +json+ (encode "hello" "world")))
    (cc/quick-bench (encode "hello" "world"))))

(defn decode-perf []

  ;; 910ns
  (title "decode: cheshire")
  (assert (= +data+ (cheshire/parse-string-strict +json+)))
  (cc/quick-bench (cheshire/parse-string-strict +json+))

  ;; 390ns
  (title "decode: jsonista")
  (assert (= +data+ (json/read-value +json+)))
  (cc/quick-bench (json/read-value +json+))

  ;; 230ns
  (title "decode: jackson")
  (let [mapper (ObjectMapper.)
        decode (fn [] (.readValue mapper +json+ Map))]
    (assert (= +data+ (decode)))
    (cc/quick-bench (decode))))

(defn minify
  "drops extra keys from data to test PAM vs PHM differences"
  [data]
  (cond-> data
          (data "results") (update data "results" (partial map #(dissoc % "picture" "login" "cell" "dob" "email" "gender" "registered")))))

(defn encode-perf-different-sizes []
  (doseq [file ["dev-resources/json10b.json"
                "dev-resources/json100b.json"
                "dev-resources/json1k.json"
                "dev-resources/json10k.json"
                "dev-resources/json100k.json"]
          :let [data (cheshire/parse-string (slurp file))
                json (cheshire/generate-string data)]]

    (title file)

    ;  1.1µs (10b)
    ;  2.9µs (100b)
    ; 11.7µs (1k)
    ;  125µs (10k)
    ; 1230µs (100k)
    (title "encode: cheshire")
    (assert (= json (cheshire/generate-string data)))
    (cc/quick-bench (cheshire/generate-string data))

    ; 0.17µs (10b)
    ; 0.50µs (100b)
    ;  2.8µs (1k)
    ;   29µs (10k)
    ;  338µs (100k)
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
    ;  1.9µs (100b)
    ;  9.2µs (1k)
    ;  102µs (10k)
    ;  990µs (100k)
    (title "decode: cheshire")
    (assert (= data (cheshire/parse-string json)))
    (cc/quick-bench (cheshire/parse-string json))

    ; 0.40µs (10b)
    ;  1.5µs (100b)
    ;  7.1µs (1k)
    ;   77µs (10k)
    ;  760µs (100k)
    (title "decode: jsonista")
    (assert (= data (json/read-value json)))
    (cc/quick-bench (json/read-value json))))

(comment
  (encode-perf)
  (decode-perf)
  (encode-perf-different-sizes)
  (decode-perf-different-sizes))
