(ns jsonista.json-perf-test
  (:require [criterium.core :as cc]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [jsonista.test-utils :refer :all]
            [jsonista.core :as j]
            [cheshire.core :as cheshire]
            [cognitect.transit :as transit]
            [taoensso.nippy :as nippy]
            [jsonista.tagged :as jt]
            [clojure.edn :as edn])
  (:import (com.fasterxml.jackson.databind ObjectMapper)
           (java.util Map Date)
           (java.io ByteArrayOutputStream ByteArrayInputStream Reader)
           (java.net URL)
           (java.util.zip GZIPInputStream)
           (clojure.lang Keyword PersistentHashSet)
           (com.fasterxml.jackson.core JsonGenerator JsonToken JsonParser)
           (com.fasterxml.jackson.databind JsonNode)))

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
  (assert (= +json+ (j/write-value-as-string {"hello" "world"})))
  (cc/quick-bench (j/write-value-as-string {"hello" "world"}))

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
  (assert (= +data+ (j/read-value +json+)))
  (cc/quick-bench (j/read-value +json+))

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
    (assert (= json (j/write-value-as-string data)))
    (cc/quick-bench (j/write-value-as-string data))))

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
    (assert (= data (j/read-value json)))
    (cc/quick-bench (j/read-value json))))

(comment
  (encode-perf)
  (decode-perf)
  (encode-perf-different-sizes)
  (decode-perf-different-sizes))

;;
;; transit in-process round-robin
;;

(defn ->transit [data]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer data)
    (.toByteArray out)))

(defn <-transit [bytes]
  (let [in (ByteArrayInputStream. bytes)
        reader (transit/reader in :json)]
    (transit/read reader)))

(defn ->json [data]
  (j/write-value-as-bytes data))

(defn <-json [bytes]
  (j/read-value bytes))

(defn encode-decode-transit-jsonista-different-sizes []
  (doseq [file ["dev-resources/json10b.json"
                "dev-resources/json100b.json"
                "dev-resources/json1k.json"
                "dev-resources/json10k.json"
                "dev-resources/json100k.json"]
          :let [data (cheshire/parse-string (slurp file))]]

    (title file)

    (println data)
    (println (-> data ->transit <-transit))
    (println (-> data ->json <-json))

    (assert
      (= data
         (-> data ->transit <-transit)
         (-> data ->json <-json)))

    ;  6.8µs (10b)
    ; 14.8µs (100b)
    ; 25.6µs (1k)
    ;  252µs (10k)
    ; 1760µs (100k)
    (title "encode-decode: transit")
    (cc/quick-bench
      (-> data ->transit <-transit))

    ; 0.52µs (10b)
    ; 2.50µs (100b)
    ; 10.0µs (1k)
    ;  100µs (10k)
    ; 1300µs (100k)
    (title "encode-decode: jsonista")
    (cc/quick-bench
      (-> data ->json <-json))))

(defn tagged-json-edn-transit []

  (title "tagged json vs edn vs transit")

  (let [mapper (j/object-mapper
                 {:decode-key-fn true
                  :modules [(jt/module
                              {:handlers {Keyword {:tag "~k"
                                                   :encode jt/encode-keyword
                                                   :decode keyword}
                                          PersistentHashSet {:tag "~s"
                                                             :encode jt/encode-collection
                                                             :decode set}
                                          Date {:tag "~d"
                                                :encode (fn [^Date d, ^JsonGenerator gen]
                                                          (.writeNumber gen (.getTime d)))
                                                :decode (fn [n] (Date. ^int n))}}})]})
        data (-> (cheshire/parse-string (slurp "dev-resources/json1k.json") true)
                 (assoc-in [:results 0 :tags] #{::kikka ::kukka})
                 (assoc-in [:results 0 :created] (new Date)))]

    ;; 26µs
    (title "jsonista")
    (cc/quick-bench
      (j/read-value (j/write-value-as-bytes data)))

    ;; 28µs
    (title "jsonista-tagged")
    (cc/quick-bench
      (j/read-value (j/write-value-as-bytes data mapper) mapper))

    ;; 190µs
    (title "edn")
    (cc/quick-bench
      (edn/read-string (pr-str data)))

    ;; 82µs
    (title "transit")
    (cc/quick-bench
      (<-transit (->transit data)))

    ;; 53µs
    (title "nippy")
    (cc/quick-bench
      (nippy/thaw (nippy/freeze data)))))

(comment
  (encode-decode-transit-jsonista-different-sizes)
  (tagged-json-edn-transit))

(comment
  (require '[clj-async-profiler.core :as prof])
  (prof/serve-files 8111)
  (prof/list-event-types)

  (defn read-geojson-features
    [^Reader f]
    (let [^JsonNode tree (.readTree j/default-object-mapper f)
          ^JsonNode node (.get tree "features")]
      (->> (map (fn [node]
                  (.treeToValue j/default-object-mapper node ^Class Object))
                node))))

  ;; Get test file
  (let [url (URL. "https://meri.digitraffic.fi/api/v1/locations/latest")
        con (.openConnection url)]
    (.setRequestProperty con "Accept-Encoding" "gzip")
    (with-open [is (.getInputStream con)
                is (GZIPInputStream. is)
                rdr (io/reader is)]
      (io/copy rdr (io/file "temp/fintraffic-ship-locations.geojson"))))

  ;; Create test file with just top level array
  (spit "temp/fintraffic-ship-locations-features.geojson"
        (j/write-value-as-string (get (j/read-value (slurp "temp/fintraffic-ship-locations.geojson")) "features")
                                 (j/object-mapper {:pretty true})))

  (prof/start)
  (prof/stop)
  (prof/profile
    {:event :alloc
     :title "Jsonista normal"}
    (dotimes [i 100]
      (with-open [rdr (io/reader "temp/fintraffic-ship-locations.geojson")]
        (j/read-value rdr ))))

  (prof/profile
    {:event :alloc
     :title "Jsonista readTree"}
    (dotimes [i 100]
      (with-open [rdr (io/reader "temp/fintraffic-ship-locations.geojson")]
        (doall (read-geojson-features rdr)))))

  (prof/profile
    {:event :alloc
     :title "Jsonista readValues"}
    (dotimes [i 100]
      (with-open [rdr (io/reader "temp/fintraffic-ship-locations-features.geojson")]
        (doall (iterator-seq (.readValues (.readerFor j/default-object-mapper ^Class Object) rdr))))))

  ;; Seq keeps cache, so the full value is stored in memory...?
  (defn read-array-values [^JsonParser parser]
    (when (not= JsonToken/END_ARRAY (.nextToken parser))
      (let [v (.readValueAs parser ^Class Object) ]
        (lazy-seq (cons v (read-array-values parser))))))

  (prof/profile
    {:event :alloc
     :title "Jsonista readValues"}
    (dotimes [i 100]
      (with-open [rdr (io/reader "temp/fintraffic-ship-locations.geojson")]
        (let [parser (.createParser (.getFactory j/default-object-mapper) rdr)]
          (.nextToken parser) ;; START_OBJECT
          (.nextToken parser) ;; "type"
          (.nextToken parser) ;; "FeatureCollection"
          (.nextToken parser) ;; "features"
          (.nextToken parser) ;; START_ARRAY
          (last (read-array-values parser))
          ;; TODO: Could also assert that file ends validly?
          )))))
