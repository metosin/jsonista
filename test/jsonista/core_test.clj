(ns jsonista.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [jsonista.core :as json]
            [cheshire.core :as cheshire]
            [cheshire.generate :as generate])
  (:import (java.util UUID Date)
           (java.sql Timestamp)
           (com.fasterxml.jackson.core JsonGenerator)
           (java.io ByteArrayInputStream InputStreamReader)))

(defn stays-same? [x] (= x (-> x json/write-value-as-string json/read-value)))

(defn make-canonical [x] (-> x json/read-value json/write-value-as-string))
(defn canonical= [x y] (= (make-canonical x) (make-canonical y)))

(def +kw-mapper+ (json/object-mapper {:keywordize? true}))

(deftest simple-roundrobin-test
  (is (stays-same? {"hello" "world"}))
  (is (stays-same? [1 2 3]))
  (is (= "0.75" (json/write-value-as-string 3/4))))

(deftest test-nil
  (is (nil? (json/read-value nil)))
  (is (= "null" (json/write-value-as-string nil))))

(let [data {:hello "world"}]
  (is (= {"hello" "world"} (-> data json/write-value-as-string json/read-value)))
  (is (= {:hello "world"} (-> data (json/write-value-as-string) (json/read-value +kw-mapper+))))
  #_(is (= "{\n  \"hello\" : \"world\"\n}" (json/write-value-as-string data (json/object-mapper {:pretty true}))))
  (is (= "{\"imperial-money\":\"\\u00A3\"}" (json/write-value-as-string {:imperial-money "£"} (json/object-mapper {:escape-non-ascii true}))))
  #_(is (= "{\"mmddyyyy\":\"00-01-70\"}" (json/write-value-as-string {:mmddyyyy (Date. 0)} {:date-format "mm-dd-yy"}))))

(json/write-value-as-string {:imperial-money "£"} (json/object-mapper {:escape-non-ascii true}))

(deftest options-tests
  (let [data {:hello "world"}]
    (is (= {"hello" "world"} (-> data json/write-value-as-string json/read-value)))
    (is (= {:hello "world"} (-> data (json/write-value-as-string) (json/read-value +kw-mapper+))))
    (is (= "{\n  \"hello\" : \"world\"\n}" (json/write-value-as-string data (json/object-mapper {:pretty true}))))
    (is (= "{\"imperial-money\":\"\\u00A3\"}" (json/write-value-as-string {:imperial-money "£"} (json/object-mapper {:escape-non-ascii true}))))
    (is (= "{\"mmddyyyy\":\"00-01-70\"}" (json/write-value-as-string {:mmddyyyy (Date. 0)} (json/object-mapper {:date-format "mm-dd-yy"}))))))

(deftest roundrobin-tests
  (let [data     {:numbers   {:integer     (int 1)
                              :long        (long 2)
                              :double      (double 1.2)
                              :float       (float 3.14)
                              :big-integer (biginteger 3)
                              :big-decimal (bigdec 4)
                              :ratio       3/4
                              :short       (short 5)
                              :byte        (byte 6)
                              :big-int     (bigint 7)}
                  :boolean   true
                  :string    "string"
                  :character \c
                  :keyword   :keyword
                  :q-keyword :qualified/:keyword
                  :set       #{1 2 3}
                  :queue     (conj (clojure.lang.PersistentQueue/EMPTY) 1 2 3)
                  :list      (list 1 2 3)
                  :bytes     (.getBytes "bytes")
                  :uuid      (UUID/fromString "fbe5a1e8-6c91-42f6-8147-6cde3188fd25")
                  :symbol    'symbol
                  :java-set  (doto (java.util.HashSet.) (.add 1) (.add 2) (.add 3))
                  :java-map  (doto (java.util.HashMap.) (.put :foo "bar"))
                  :java-list (doto (java.util.ArrayList.) (.add 1) (.add 2) (.add 3))
                  :date      (Date. 0)
                  :timestamp (Timestamp. 0)}
        expected {:numbers   {:integer     1
                              :long        2
                              :double      1.2
                              :float       3.14
                              :big-integer 3
                              :big-decimal 4
                              :ratio       0.75
                              :short       5
                              :byte        6
                              :big-int     7}
                  :boolean   true
                  :string    "string"
                  :character "c"
                  :keyword   "keyword"
                  :q-keyword "qualified/:keyword"
                  :set       [1 3 2]
                  :queue     [1 2 3]
                  :list      [1 2 3]
                  :bytes     "Ynl0ZXM="
                  :uuid      "fbe5a1e8-6c91-42f6-8147-6cde3188fd25"
                  :symbol    "symbol"
                  :java-set  [1 2 3]
                  :java-map  {:foo "bar"}
                  :java-list [1 2 3]
                  :date      "1970-01-01T00:00:00Z"
                  :timestamp "1970-01-01T00:00:00Z"}]

    (testing "cheshire"
      (is (= expected (cheshire/parse-string (cheshire/generate-string data) true))))

    (testing "jsonista"
      (is (canonical= (cheshire/generate-string data) (json/write-value-as-string data)))
      (is (= expected (json/read-value (json/write-value-as-string data) +kw-mapper+))))))

(defrecord StringLike [value])

(defn serialize-stringlike
  [x ^JsonGenerator jg]
  (.writeString jg (str (:value x))))

(generate/add-encoder StringLike serialize-stringlike)

(deftest custom-encoders
  (let [data {:like (StringLike. "boss")}
        expected {:like "boss"}
        mapper (json/object-mapper {:keywordize? true
                                  :encoders {StringLike serialize-stringlike}})]

    (testing "cheshire"
      (is (= expected (cheshire/parse-string
               (cheshire/generate-string data)
               true))))

    (testing "jsonista"
      (is (canonical= (cheshire/generate-string data) (json/write-value-as-string data mapper)))
      (is (= expected (-> data (json/write-value-as-string mapper) (json/read-value mapper)))))))

(defn- str->input-stream [x] (ByteArrayInputStream. (.getBytes x "UTF-8")))

(deftest read-value-input-types
  (let [original {"ok" 1}
        input-string (json/write-value-as-string original)]
    (is (= original (json/read-value input-string)))
    (is (= original (json/read-value (str->input-stream input-string))))
    (is (= original (json/read-value (InputStreamReader. (str->input-stream input-string)))))))
