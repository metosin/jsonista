(ns jsonista.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [jsonista.core :as j]
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

(defn stays-same? [x] (= x (-> x j/write-value-as-string j/read-value)))

(defn make-canonical [x] (-> x j/read-value j/write-value-as-string))
(defn canonical= [x y] (= (make-canonical x) (make-canonical y)))

(def +kw-mapper+ (j/object-mapper {:decode-key-fn true}))
(def +upper-mapper+ (j/object-mapper {:decode-key-fn str/upper-case}))
(def +string-mapper+ (j/object-mapper {:decode-key-fn false}))

(deftest simple-roundrobin-test
  (is (stays-same? {"hello" "world"}))
  (is (stays-same? [1 2 3]))
  (is (= "0.75" (j/write-value-as-string 3/4))))

(deftest test-nil
  (is (nil? (j/read-value nil)))
  (is (= "null" (j/write-value-as-string nil))))

(deftest options-tests
  (let [data {:hello "world"}]
    (testing ":decode-key-fn"
      (is (= {"hello" "world"} (-> data j/write-value-as-string j/read-value)))
      (is (= {:hello "world"} (-> data (j/write-value-as-string) (j/read-value +kw-mapper+))))
      (is (= {:hello "world"} (-> data (j/write-value-as-string) (j/read-value j/keyword-keys-object-mapper))))
      (is (= {"hello" "world"} (-> data (j/write-value-as-string) (j/read-value +string-mapper+))))
      (is (= {"HELLO" "world"} (-> data (j/write-value-as-string) (j/read-value +upper-mapper+)))))
    (testing ":encode-key-fn"
      (let [data {:hello "world"}]
        (is (= "{\"hello\":\"world\"}" (j/write-value-as-string data (j/object-mapper {:encode-key-fn true}))))
        (is (= "{\"hello\":\"world\"}" (j/write-value-as-string data j/keyword-keys-object-mapper)))
        (is (= "{\":hello\":\"world\"}" (j/write-value-as-string data (j/object-mapper {:encode-key-fn false}))))
        (is (= "{\"HELLO\":\"world\"}" (j/write-value-as-string data (j/object-mapper {:encode-key-fn (comp str/upper-case name)}))))))
    (testing ":pretty"
      (is (= "{\n  \"hello\" : \"world\"\n}" (j/write-value-as-string data (j/object-mapper {:pretty true})))))
    (testing ":escape-non-ascii"
      (is (= "{\"imperial-money\":\"\\u00A3\"}" (j/write-value-as-string {:imperial-money "£"} (j/object-mapper {:escape-non-ascii true})))))
    (testing ":date-format"
      (is (= "{\"mmddyyyy\":\"00-01-70\"}" (j/write-value-as-string {:mmddyyyy (Date. 0)} (j/object-mapper {:date-format "mm-dd-yy"})))))))

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
          (is (canonical= (cheshire/generate-string data) (j/write-value-as-string data)))))
      (is (= expected (j/read-value (j/write-value-as-string data) j/keyword-keys-object-mapper))))))

(deftest write-vaue-as-bytes-test
  (is (= (j/write-value-as-string "kikka")
         (String. (j/write-value-as-bytes "kikka")))))

(deftest modules-test
  (let [mapper (j/object-mapper {:modules [(JodaModule.)]})
        data {:date (LocalDate. 0 DateTimeZone/UTC)}]
    (testing "with defaults"
      (is (str/includes? (j/write-value-as-string data) "\"yearOfEra\":1970")))
    (testing "with installed module"
      (is (= "{\"date\":\"1970-01-01\"}" (j/write-value-as-string data mapper))))))

(deftest bigdecimals-test
  (let [get-class #(-> "{\"value\": 0.2}" (j/read-value %) (get "value") class)]
    (testing "by default, doubles are used"
      (is (= Double (get-class j/default-object-mapper))))
    (testing ":bigdecimals"
      (is (= BigDecimal (get-class (j/object-mapper {:bigdecimals true})))))))

(defrecord StringLike [value])

(defn serialize-stringlike
  [x ^JsonGenerator jg]
  (.writeString jg (str (:value x))))

(generate/add-encoder StringLike serialize-stringlike)

(deftest custom-encoders
  (let [data {:like (StringLike. "boss")}
        expected {:like "boss"}
        mapper (j/object-mapper {:decode-key-fn true
                                 :encoders {StringLike serialize-stringlike}})]

    (testing "cheshire"
      (is (= expected (cheshire/parse-string
                        (cheshire/generate-string data)
                        true))))

    (testing "jsonista"
      (is (canonical= (cheshire/generate-string data) (j/write-value-as-string data mapper)))
      (is (= expected (-> data (j/write-value-as-string mapper) (j/read-value mapper)))))

    (testing "using JsonSerializer instances"
      (let [mapper (j/object-mapper {:decode-key-fn true
                                     :encoders {StringLike (FunctionalSerializer. serialize-stringlike)}})]
        (is (canonical= (cheshire/generate-string data) (j/write-value-as-string data mapper)))
        (is (= expected (-> data (j/write-value-as-string mapper) (j/read-value mapper)))))))

  (testing "invalid encoder can't be registered"
    (is (thrown-with-msg?
          ExceptionInfo
          #"Can't register encoder 123 for type class clojure.lang.Keyword"
          (j/object-mapper {:encoders {Keyword 123}})))))

(defn- str->input-stream [^String x] (ByteArrayInputStream. (.getBytes x "UTF-8")))

(defn tmp-file ^File [] (File/createTempFile "temp" ".json"))

(deftest read-value-types
  (let [original {"ok" 1}
        input-string (j/write-value-as-string original)
        file (tmp-file)]
    (spit file input-string)

    (testing "nil"
      (is (= nil (j/read-value nil))))

    (testing "byte-array"
      (is (= original (j/read-value (j/write-value-as-bytes original)))))

    (testing "File"
      (is (= original (j/read-value file))))

    (testing "URL"
      (is (= original (j/read-value (.toURL file)))))

    (testing "String"
      (is (= original (j/read-value input-string))))

    (testing "InputStream"
      (is (= original (j/read-value (str->input-stream input-string)))))

    (testing "Reader"
      (is (= original (j/read-value (InputStreamReader. (str->input-stream input-string))))))))

(deftest write-value-types
  (let [original {"ok" 1}
        expected (j/write-value-as-string original)
        file (tmp-file)]

    (testing "File"
      (j/write-value file original)
      (is (= expected (slurp file)))
      (.delete file))

    (testing "OutputStream"
      (j/write-value (FileOutputStream. file) original)
      (is (= expected (slurp file)))
      (.delete file))

    (testing "DataOutput"
      (j/write-value (RandomAccessFile. file "rw") original)
      (is (= expected (slurp file)))
      (.delete file))

    (testing "Writer"
      (j/write-value (FileWriter. file) original)
      (is (= expected (slurp file)))
      (.delete file))))

