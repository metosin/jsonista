(ns jsonista.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [jsonista.core :as jsonista]
            [cheshire.core :as cheshire]
            [cheshire.generate :as generate]
            [clojure.string :as str])
  (:import (java.util UUID Date)
           (java.sql Timestamp)
           (com.fasterxml.jackson.core JsonGenerator)
           (java.io ByteArrayInputStream InputStreamReader File FileOutputStream RandomAccessFile FileWriter)
           (jsonista.jackson FunctionalSerializer)
           (clojure.lang Keyword ExceptionInfo)
           (java.time Instant LocalTime LocalDateTime ZoneOffset)
           (com.fasterxml.jackson.datatype.joda JodaModule)
           (org.joda.time LocalDate DateTimeZone)))

(set! *warn-on-reflection* true)

(defn stays-same? [x] (= x (-> x jsonista/write-value-as-string jsonista/read-value)))

(defn make-canonical [x] (-> x jsonista/read-value jsonista/write-value-as-string))
(defn canonical= [x y] (= (make-canonical x) (make-canonical y)))

(def +kw-mapper+ (jsonista/object-mapper {:decode-key-fn true}))
(def +upper-mapper+ (jsonista/object-mapper {:decode-key-fn str/upper-case}))
(def +string-mapper+ (jsonista/object-mapper {:decode-key-fn false}))

(deftest simple-roundrobin-test
  (is (stays-same? {"hello" "world"}))
  (is (stays-same? [1 2 3]))
  (is (= "0.75" (jsonista/write-value-as-string 3/4))))

(deftest test-nil
  (is (nil? (jsonista/read-value nil)))
  (is (= "null" (jsonista/write-value-as-string nil))))

(deftest options-tests
  (let [data {:hello "world"}]
    (testing ":decode-key-fn"
      (is (= {"hello" "world"} (-> data jsonista/write-value-as-string jsonista/read-value)))
      (is (= {:hello "world"} (-> data (jsonista/write-value-as-string) (jsonista/read-value +kw-mapper+))))
      (is (= {"hello" "world"} (-> data (jsonista/write-value-as-string) (jsonista/read-value +string-mapper+))))
      (is (= {"HELLO" "world"} (-> data (jsonista/write-value-as-string) (jsonista/read-value +upper-mapper+)))))
    (testing ":encode-key-fn"
      (let [data {:hello "world"}]
        (is (= "{\"hello\":\"world\"}" (jsonista/write-value-as-string data (jsonista/object-mapper {:encode-key-fn true}))))
        (is (= "{\":hello\":\"world\"}" (jsonista/write-value-as-string data (jsonista/object-mapper {:encode-key-fn false}))))
        (is (= "{\"HELLO\":\"world\"}" (jsonista/write-value-as-string data (jsonista/object-mapper {:encode-key-fn (comp str/upper-case name)}))))))
    (testing ":pretty"
      (is (= "{\n  \"hello\" : \"world\"\n}" (jsonista/write-value-as-string data (jsonista/object-mapper {:pretty true})))))
    (testing ":escape-non-ascii"
      (is (= "{\"imperial-money\":\"\\u00A3\"}" (jsonista/write-value-as-string {:imperial-money "£"} (jsonista/object-mapper {:escape-non-ascii true})))))
    (testing ":date-format"
      (is (= "{\"mmddyyyy\":\"00-01-70\"}" (jsonista/write-value-as-string {:mmddyyyy (Date. 0)} (jsonista/object-mapper {:date-format "mm-dd-yy"})))))))

(deftest roundrobin-tests
  (let [data {:numbers {:integer (int 1)
                        :long (long 2)
                        :double (double 1.2)
                        :float (float 3.14)
                        :big-integer (biginteger 3)
                        :big-decimal (bigdec 4)
                        :ratio 3/4
                        :short (short 5)
                        :byte (byte 6)
                        :big-int (bigint 7)}
              :boolean true
              :string "string"
              :character \c
              :keyword :keyword
              :q-keyword :qualified/:keyword
              :set #{1 2 3}
              :queue (conj (clojure.lang.PersistentQueue/EMPTY) 1 2 3)
              :list (list 1 2 3)
              :bytes (.getBytes "bytes")
              :uuid (UUID/fromString "fbe5a1e8-6c91-42f6-8147-6cde3188fd25")
              :symbol 'symbol
              :java-set (doto (java.util.HashSet.) (.add 1) (.add 2) (.add 3))
              :java-map (doto (java.util.HashMap.) (.put :foo "bar"))
              :java-list (doto (java.util.ArrayList.) (.add 1) (.add 2) (.add 3))
              :dates {:date (Date. 0)
                      :timestamp (Timestamp. 0)
                      :instant (Instant/ofEpochMilli 0)
                      :local-time (LocalTime/ofNanoOfDay 0)
                      :local-date-time (LocalDateTime/ofEpochSecond 0 0 ZoneOffset/UTC)}}
        expected {:numbers {:integer 1
                            :long 2
                            :double 1.2
                            :float 3.14
                            :big-integer 3
                            :big-decimal 4
                            :ratio 0.75
                            :short 5
                            :byte 6
                            :big-int 7}
                  :boolean true
                  :string "string"
                  :character "c"
                  :keyword "keyword"
                  :q-keyword "qualified/:keyword"
                  :set [1 3 2]
                  :queue [1 2 3]
                  :list [1 2 3]
                  :bytes "Ynl0ZXM="
                  :uuid "fbe5a1e8-6c91-42f6-8147-6cde3188fd25"
                  :symbol "symbol"
                  :java-set [1 2 3]
                  :java-map {:foo "bar"}
                  :java-list [1 2 3]
                  :dates {:date "1970-01-01T00:00:00Z"
                          :timestamp "1970-01-01T00:00:00Z"
                          :instant "1970-01-01T00:00:00Z"
                          :local-time "00:00:00"
                          :local-date-time "1970-01-01T00:00:00"}}
        without-java-time #(update % :dates dissoc :instant :local-time :local-date-time)]

    (testing "cheshire"
      (testing "fails with java-time"
        (is (thrown? Exception (cheshire/generate-string data))))
      (testing "parses others nicely"
        (is (= (without-java-time expected)
               (cheshire/parse-string (cheshire/generate-string (without-java-time data)) true)))))

    (testing "jsonista"
      (testing "works like cheshire"
        (let [data (without-java-time data)]
          (is (canonical= (cheshire/generate-string data) (jsonista/write-value-as-string data)))))
      (is (= expected (jsonista/read-value (jsonista/write-value-as-string data) +kw-mapper+))))))

(deftest write-vaue-as-bytes-test
  (is (= (jsonista/write-value-as-string "kikka")
         (String. (jsonista/write-value-as-bytes "kikka")))))

(deftest modules-test
  (let [mapper (jsonista/object-mapper {:modules [(JodaModule.)]})
        data {:date (LocalDate. 0 DateTimeZone/UTC)}]
    (testing "with defaults"
      (is (str/includes? (jsonista/write-value-as-string data) "\"yearOfEra\":1970")))
    (testing "with installed module"
      (is (= "{\"date\":\"1970-01-01\"}" (jsonista/write-value-as-string data mapper))))))

(defrecord StringLike [value])

(defn serialize-stringlike
  [x ^JsonGenerator jg]
  (.writeString jg (str (:value x))))

(generate/add-encoder StringLike serialize-stringlike)

(deftest custom-encoders
  (let [data {:like (StringLike. "boss")}
        expected {:like "boss"}
        mapper (jsonista/object-mapper {:decode-key-fn true
                                        :encoders {StringLike serialize-stringlike}})]

    (testing "cheshire"
      (is (= expected (cheshire/parse-string
                        (cheshire/generate-string data)
                        true))))

    (testing "jsonista"
      (is (canonical= (cheshire/generate-string data) (jsonista/write-value-as-string data mapper)))
      (is (= expected (-> data (jsonista/write-value-as-string mapper) (jsonista/read-value mapper)))))

    (testing "using JsonSerializer instances"
      (let [mapper (jsonista/object-mapper {:decode-key-fn true
                                            :encoders {StringLike (FunctionalSerializer. serialize-stringlike)}})]
        (is (canonical= (cheshire/generate-string data) (jsonista/write-value-as-string data mapper)))
        (is (= expected (-> data (jsonista/write-value-as-string mapper) (jsonista/read-value mapper)))))))

  (testing "invalid encoder can't be registered"
    (is (thrown-with-msg?
          ExceptionInfo
          #"Can't register encoder 123 for type class clojure.lang.Keyword"
          (jsonista/object-mapper {:encoders {Keyword 123}})))))

(defn- str->input-stream [^String x] (ByteArrayInputStream. (.getBytes x "UTF-8")))

(defn tmp-file ^File [] (File/createTempFile "temp" ".json"))

(deftest read-value-types
  (let [original {"ok" 1}
        input-string (jsonista/write-value-as-string original)
        file (tmp-file)]
    (spit file input-string)

    (testing "nil"
      (is (= nil (jsonista/read-value nil))))

    (testing "File"
      (is (= original (jsonista/read-value file))))

    (testing "URL"
      (is (= original (jsonista/read-value (.toURL file)))))

    (testing "String"
      (is (= original (jsonista/read-value input-string))))

    (testing "InputStream"
      (is (= original (jsonista/read-value (str->input-stream input-string)))))

    (testing "Reader"
      (is (= original (jsonista/read-value (InputStreamReader. (str->input-stream input-string))))))))

(deftest write-value-types
  (let [original {"ok" 1}
        expected (jsonista/write-value-as-string original)
        file (tmp-file)]

    (testing "File"
      (jsonista/write-value file original)
      (is (= expected (slurp file)))
      (.delete file))

    (testing "OutputStream"
      (jsonista/write-value (FileOutputStream. file) original)
      (is (= expected (slurp file)))
      (.delete file))

    (testing "DataOutput"
      (jsonista/write-value (RandomAccessFile. file "rw") original)
      (is (= expected (slurp file)))
      (.delete file))

    (testing "Writer"
      (jsonista/write-value (FileWriter. file) original)
      (is (= expected (slurp file)))
      (.delete file))))
