(ns jsonista.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [jsonista.core :as j]
            [cheshire.core :as cheshire]
            [cheshire.generate :as generate]
            [clojure.string :as str])
  (:import (java.util UUID Date)
           (java.sql Timestamp)
           (tools.jackson.core JsonGenerator)
           (java.io ByteArrayInputStream InputStreamReader File FileOutputStream RandomAccessFile FileWriter)
           (jsonista.jackson FunctionalSerializer)
           (clojure.lang Keyword ExceptionInfo)
           (java.time Instant LocalTime LocalDateTime ZoneOffset)
           (tools.jackson.datatype.joda JodaModule)
           (org.joda.time LocalDate DateTimeZone)
           (tools.jackson.dataformat.cbor CBORMapper)))

(set! *warn-on-reflection* true)

(defn stays-same? [x] (= x (-> x j/write-value-as-string j/read-value)))

(defn- uuid->bytes
  "The 16-byte big-endian binary representation of a UUID (most significant
  bits followed by least significant bits), matching how CBOR encodes it."
  [^java.util.UUID uuid]
  (let [bb (java.nio.ByteBuffer/allocate 16)]
    (.putLong bb (.getMostSignificantBits uuid))
    (.putLong bb (.getLeastSignificantBits uuid))
    (.array bb)))

(defn make-canonical [x] (-> x j/read-value j/write-value-as-string))
(defn canonical= [x y] (= (make-canonical x) (make-canonical y)))

(def +kw-mapper+ (j/object-mapper {:decode-key-fn true}))
(def +upper-mapper+ (j/object-mapper {:decode-key-fn str/upper-case}))
(def +string-mapper+ (j/object-mapper {:decode-key-fn false}))
(def +cbor-mapper+ (j/object-mapper {:mapper (CBORMapper.)
                                     :encode-key-fn true
                                     :decode-key-fn true}))

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
    (testing ":strip-nils"
      (let [data-with-nils {:hello "world" :goodbye nil}]
        (is (= "{\"hello\":\"world\"}" (j/write-value-as-string data-with-nils (j/object-mapper {:strip-nils true}))))))
    (testing ":strip-nils doesn't strip other empties"
      (let [data-with-nils {:hello "world" :goodbye nil :empty-string "" :empty-map {}}]
        (is (= "{\"hello\":\"world\",\"empty-string\":\"\",\"empty-map\":{}}" (j/write-value-as-string data-with-nils (j/object-mapper {:strip-nils true}))))))
    (testing ":strip-empties"
      (let [data-with-nils {:hello "world" :goodbye nil :empty-string "" :empty-map {}}]
        (is (= "{\"hello\":\"world\"}" (j/write-value-as-string data-with-nils (j/object-mapper {:strip-empties true}))))))
    (testing ":escape-non-ascii"
      (is (= "{\"imperial-money\":\"\\u00A3\"}" (j/write-value-as-string {:imperial-money "£"} (j/object-mapper {:escape-non-ascii true}))))
      (testing "is a no-op (not a crash) when combined with a non-JSON :mapper"
        ;; JsonWriteFeature/ESCAPE_NON_ASCII is JSON-specific in Jackson 3 and
        ;; isn't declared on CBORMapper$Builder's supertype - object-mapper
        ;; construction must not throw, and the resulting mapper must still work.
        (let [cbor-escape-mapper (j/object-mapper {:mapper (CBORMapper.) :escape-non-ascii true})]
          (is (some? cbor-escape-mapper))
          (is (= {"imperial-money" "£"}
                 (j/read-value (j/write-value-as-bytes {:imperial-money "£"} cbor-escape-mapper)
                                cbor-escape-mapper))))))
    (testing ":date-format"
      (is (= "{\"mmddyyyy\":\"00-01-70\"}" (j/write-value-as-string {:mmddyyyy (Date. 0)} (j/object-mapper {:date-format "mm-dd-yy"})))))
    (testing "java.util.Date subclasses"
      ;; java.sql.Date overrides toInstant() to throw - DateSerializer must not
      ;; call it on the value.
      (is (= "{\"d\":\"1970-01-01T00:00:00Z\"}" (j/write-value-as-string {:d (java.sql.Date. 0)})))
      (is (= "{\"d\":\"00-01-70\"}" (j/write-value-as-string {:d (java.sql.Date. 0)} (j/object-mapper {:date-format "mm-dd-yy"})))))
    (testing "empty beans (Jackson 3 default; :do-not-fail-on-empty-beans is a no-op)"
      ;; Jackson 2 threw InvalidDefinitionException for an object with no bean
      ;; accessors unless FAIL_ON_EMPTY_BEANS was disabled. Jackson 3's own
      ;; default already has this feature disabled, so jsonista's *unconfigured*
      ;; default silently changed from "throw" to "serialize as {}". This pins
      ;; the new default so it can't drift again unnoticed, and confirms
      ;; :do-not-fail-on-empty-beans (now a no-op) doesn't change the outcome.
      (is (= "{}" (j/write-value-as-string (Object.))))
      (is (= "{}" (j/write-value-as-string (Object.) (j/object-mapper {:do-not-fail-on-empty-beans true})))))))

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
        without-java-time #(update % :dates dissoc :instant :local-time :local-date-time)
        expected-with-byte-arrays (assoc expected :bytes (:bytes data))]

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
      (is (= expected (j/read-value (j/write-value-as-string data) j/keyword-keys-object-mapper))))

    (testing "cbor"
      (testing "works like standard json reading/writing"
        ;; CBOR and MessagePack are different binary encodings, so byte-level
        ;; expectations from the old msgpack test don't carry over. Instead we
        ;; round-trip the same rich data set through the CBOR mapper and check
        ;; it comes back equal, accounting for a couple of encoding-specific
        ;; quirks that are inherent to CBOR (not a jsonista behavior change):
        (let [result (j/read-value (j/write-value-as-bytes data +cbor-mapper+) +cbor-mapper+)]
          ;; Confirm our byte arrays are correct via string equality
          (is (= (String. ^bytes (:bytes expected-with-byte-arrays))
                 (String. ^bytes (:bytes result))))
          ;; Unlike msgpack, CBOR has native support for binary byte-strings, and
          ;; java.util.UUID's default Jackson (de)serializer uses that native
          ;; support instead of the textual representation used for JSON. So the
          ;; round-tripped value is the UUID's raw 16 bytes, not its string form.
          (is (= (seq (uuid->bytes (:uuid data)))
                 (seq (:uuid result))))
          ;; clojure.lang.BigInt has no custom jsonista serializer, so it falls
          ;; through to Jackson's generic Number handling, which serializes
          ;; unrecognized Number subtypes via JsonGenerator/writeNumber(String).
          ;; The CBOR generator (unlike msgpack's) encodes that as a text value
          ;; rather than parsing it back into a number, so it round-trips as a
          ;; string instead of a number.
          (is (= (str (get-in expected-with-byte-arrays [:numbers :big-int]))
                 (get-in result [:numbers :big-int])))
          ;; JSON round-tripping loses the fact that :big-decimal started out as
          ;; a BigDecimal - it's written as plain text and, without :bigdecimals,
          ;; re-read as a Long. CBOR is binary and preserves the original numeric
          ;; type across the round-trip, so it comes back as an exact BigDecimal
          ;; (4M) rather than a Long (4). `==` checks the numeric value matches;
          ;; `=` would report a false mismatch since Long and BigDecimal are
          ;; different numeric categories in Clojure.
          (is (== (get-in expected-with-byte-arrays [:numbers :big-decimal])
                  (get-in result [:numbers :big-decimal])))
          (is (instance? BigDecimal (get-in result [:numbers :big-decimal])))
          ;; Same root cause as msgpack's float tolerance: (float 3.14) widened
          ;; to double isn't exactly 3.14, so it can't be `=` to the double
          ;; literal 3.14 in `expected`. JSON sidesteps this because writing then
          ;; re-reading as text re-parses "3.14" into a double that matches the
          ;; literal; CBOR is binary and preserves the original Float bit pattern
          ;; instead, so we compare with the same rounding tolerance as before.
          (is (= (/ (Math/round (float (* (get-in expected-with-byte-arrays [:numbers :float]) 100)))
                    100.0)
                 (/ (Math/round (float (* (get-in result [:numbers :float]) 100)))
                    100.0)))
          ;; Confirm everything else matches exactly.
          (is (= (-> expected-with-byte-arrays
                     (dissoc :bytes :uuid)
                     (update-in [:numbers] dissoc :big-int :big-decimal :float))
                 (-> result
                     (dissoc :bytes :uuid)
                     (update-in [:numbers] dissoc :big-int :big-decimal :float)))))))))

(deftest write-vaue-as-bytes-test
  (is (= (j/write-value-as-string "kikka")
         (String. (j/write-value-as-bytes "kikka")))))

(deftest modules-test
  (let [mapper (j/object-mapper {:modules [(JodaModule.)]})
        data {:date (LocalDate. 0 DateTimeZone/UTC)}]
    (testing "fails with missing joda-module"
      (is (thrown-with-msg?
            Exception
            #"Joda date/time type `org.joda.time.LocalDate` not supported by default"
            (j/write-value-as-string data))))
    (testing "with installed module"
      (is (= "{\"date\":\"1970-01-01\"}" (j/write-value-as-string data mapper))))))

(deftest bigdecimals-test
  (let [get-class #(-> "{\"value\": 0.2}" (j/read-value %) (get "value") class)]
    (testing "by default, doubles are used"
      (is (= Double (get-class j/default-object-mapper))))
    (testing ":bigdecimals"
      (is (= BigDecimal (get-class (j/object-mapper {:bigdecimals true})))))))

(defrecord StringLike [value])

;; Cheshire is still on Jackson 2, so its custom-encoder callback receives a
;; com.fasterxml.jackson.core.JsonGenerator, while jsonista's FunctionalSerializer
;; passes a tools.jackson.core.JsonGenerator (Jackson 3). Before the port these
;; were the same class, so one function with one type hint served both callers;
;; now they're unrelated class hierarchies, so we need a hinted function per side
;; to avoid both a ClassCastException (wrong hint) and reflection (no hint).
(defn serialize-stringlike
  [x ^com.fasterxml.jackson.core.JsonGenerator jg]
  (.writeString jg (str (:value x))))

(defn serialize-stringlike-jsonista
  [x ^JsonGenerator jg]
  (.writeString jg (str (:value x))))

(generate/add-encoder StringLike serialize-stringlike)

(deftest custom-encoders
  (let [data {:like (StringLike. "boss")}
        expected {:like "boss"}
        mapper (j/object-mapper {:decode-key-fn true
                                 :encoders {StringLike serialize-stringlike-jsonista}})]

    (testing "cheshire"
      (is (= expected (cheshire/parse-string
                        (cheshire/generate-string data)
                        true))))

    (testing "jsonista"
      (is (canonical= (cheshire/generate-string data) (j/write-value-as-string data mapper)))
      (is (= expected (-> data (j/write-value-as-string mapper) (j/read-value mapper)))))

    (testing "using JsonSerializer instances"
      (let [mapper (j/object-mapper {:decode-key-fn true
                                     :encoders {StringLike (FunctionalSerializer. serialize-stringlike-jsonista)}})]
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

    (testing "String"
      (is (= original (j/read-value input-string))))

    (testing "InputStream"
      (is (= original (j/read-value (str->input-stream input-string)))))

    (testing "Reader"
      (is (= original (j/read-value (InputStreamReader. (str->input-stream input-string))))))))

(deftest read-values-types
  (let [original [{"ok" 1}]
        input-string (j/write-value-as-string original)
        file (tmp-file)]
    (spit file input-string)

    (testing "nil"
      (is (= nil (j/read-values nil))))

    (testing "byte-array"
      (is (= original (j/read-values (j/write-value-as-bytes original)))))

    (testing "File"
      (is (= original (j/read-values file))))

    (testing "String"
      (is (= original (j/read-values input-string))))

    (testing "InputStream"
      (is (= original (j/read-values (str->input-stream input-string)))))

    (testing "Reader"
      (is (= original (j/read-values (InputStreamReader. (str->input-stream input-string))))))))

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

(deftest write-values-types
  (let [original [{"ok" 1} {"ok" 2}]
        expected-array (j/write-value-as-string original)
        expected-lines (str/join "\n" (mapv j/write-value-as-string original))
        file (tmp-file)]

    (testing "File"
      (j/write-values file original)
      (is (= expected-lines (slurp file)))
      (.delete file)
      (j/write-values-as-array file original)
      (is (= expected-array (slurp file)))
      (.delete file))

    (testing "OutputStream"
      (j/write-values (FileOutputStream. file) original)
      (is (= expected-lines (slurp file)))
      (.delete file)
      (j/write-values-as-array (FileOutputStream. file) original)
      (is (= expected-array (slurp file)))
      (.delete file))

    (testing "DataOutput"
      (j/write-values (RandomAccessFile. file "rw") original)
      (is (= expected-lines (slurp file)))
      (.delete file)
      (j/write-values-as-array (RandomAccessFile. file "rw") original)
      (is (= expected-array (slurp file)))
      (.delete file))

    (testing "Writer"
      (j/write-values (FileWriter. file) original)
      (is (= expected-lines (slurp file)))
      (.delete file)
      (j/write-values-as-array (FileWriter. file) original)
      (is (= expected-array (slurp file)))
      (.delete file))))

(deftest read-values-iteration
  (let [original [{"ok" 1}]
        ^java.util.Iterator it (j/read-values (j/write-value-as-bytes original))]
    (is (instance? java.util.Iterator it))
    (is (.hasNext it))
    (is (= (first original) (.next it)))
    (is (false? (.hasNext it)))))

(deftest read-values-reduction
  (let [original [{"ok" 1}]
        ^java.util.Iterator it (j/read-values (j/write-value-as-bytes original))
        xf (map #(update % "ok" inc))]
    (is (= (into [] xf original) (into [] xf it)))))

(deftest write-values-iterable
  (let [original [{"ok" 1} {"ok" 2}]
        xf (map #(update % "ok" inc))
        expected "{\"ok\":2}\n{\"ok\":3}"
        file (tmp-file)
        eduction (->Eduction xf original)]

    (j/write-values file eduction)
    (is (= expected (slurp file)))
    (.delete file)))

(deftest write-values-as-array-iterable
  (let [original [{"ok" 1}]
        xf (map #(update % "ok" inc))
        expected (j/write-value-as-string (into [] xf original))
        file (tmp-file)
        eduction (->Eduction xf original)]

    (j/write-values-as-array file eduction)
    (is (= expected (slurp file)))
    (.delete file)))

;; clojure 1.12 seems to have changed delay so that jackson barfs on it
;; this test documents the old behaviour that we preserve with our custom DelaySerializer
(deftest test-delay
  (let [d (delay 1)
        d2 (delay (list :a 1))]
    (is (= "{\"realized\":false}" (j/write-value-as-string d)))
    (is (= "{\"realized\":false}" (j/write-value-as-string d2)))
    (force d)
    (force d2)
    (is (= "{\"realized\":true}" (j/write-value-as-string d)))
    (is (= "{\"realized\":true}" (j/write-value-as-string d2)))))

(deftest keyword-encoding-test
  (testing "repeated writes produce identical output (SerializedString cache)"
    (let [data {:foo 1 :bar/baz :some/value}]
      (is (= "{\"foo\":1,\"bar/baz\":\"some/value\"}" (j/write-value-as-string data)))
      (is (= "{\"foo\":1,\"bar/baz\":\"some/value\"}" (j/write-value-as-string data)))))
  (testing "keyword keys and values needing standard JSON escaping"
    (let [k (keyword "quote\"and\\slash")]
      ;; the value comes back as a plain string - keyword values don't
      ;; roundtrip through JSON - but both must be escaped correctly
      (is (= {k "quote\"and\\slash"}
             (-> {k k} j/write-value-as-string (j/read-value j/keyword-keys-object-mapper))))))
  (testing ":escape-non-ascii applies to keyword keys and values"
    ;; pins that the SerializedString fast path is bypassed when the
    ;; generator's escaping config isn't the JSON default
    (let [mapper (j/object-mapper {:escape-non-ascii true})]
      (is (= "{\"p\\u00E4\\u00E4\":\"\\u00F6\\u00F6\"}"
             (j/write-value-as-string {:pää :öö} mapper)))
      ;; same mapper family without the option keeps raw UTF-8
      (is (= "{\"pää\":\"öö\"}" (j/write-value-as-string {:pää :öö}))))))

(deftest map-decoding-test
  (testing "small objects decode to array maps, like Clojure map literals"
    (is (instance? clojure.lang.PersistentArrayMap (j/read-value "{}")))
    (is (instance? clojure.lang.PersistentArrayMap (j/read-value "{\"a\":1}")))
    (is (instance? clojure.lang.PersistentArrayMap
                   (j/read-value (j/write-value-as-string (zipmap (range 8) (range 8)))))))
  (testing "large objects decode to hash maps"
    (is (instance? clojure.lang.PersistentHashMap
                   (j/read-value (j/write-value-as-string (zipmap (range 9) (range 9)))))))
  (testing "roundtrip across the array-map/hash-map boundary"
    (doseq [n [0 1 7 8 9 16 17 100]]
      (let [data (into {} (map (fn [i] [(keyword (str "k" i)) i])) (range n))]
        (is (= data (-> data j/write-value-as-string (j/read-value j/keyword-keys-object-mapper)))
            (str n " keys")))))
  (testing "duplicate keys keep the last value (assoc semantics)"
    (is (= {:a 2} (j/read-value "{\"a\":1,\"a\":2}" j/keyword-keys-object-mapper)))
    ;; duplicate whose first occurrence is in the array-map buffer and whose
    ;; second lands after the spill into the transient hash map
    (let [json (str "{" (str/join "," (map #(str "\"k" % "\":" %) (range 9))) ",\"k0\":99}")]
      (is (= (-> (zipmap (map #(keyword (str "k" %)) (range 9)) (range 9))
                 (assoc :k0 99))
             (j/read-value json j/keyword-keys-object-mapper))))))

(deftest keyword-key-decoding-test
  (testing "repeated keys decode to the identical interned keyword"
    ;; holds via Keyword.intern alone, but pins the KeywordKeyDeserializer
    ;; cache to interning semantics
    (let [a (j/read-value "{\"foo\":1}" j/keyword-keys-object-mapper)
          b (j/read-value "{\"foo\":2}" j/keyword-keys-object-mapper)]
      (is (identical? (-> a keys first) (-> b keys first)))))
  (testing "slashes intern as namespaced keywords"
    (is (= {:a/b 1} (j/read-value "{\"a/b\":1}" j/keyword-keys-object-mapper))))
  (testing "many distinct keys (cache overflow) still decode correctly"
    (let [data (into {} (map (fn [i] [(keyword (str "key" i)) i])) (range 3000))]
      (is (= data (-> data j/write-value-as-string (j/read-value j/keyword-keys-object-mapper)))))))

;; Characterization test: pins a behavior change inherited from Jackson 3's
;; defaults (FAIL_ON_TRAILING_TOKENS is now enabled), rather than driving new
;; code. It is expected to pass immediately.
(deftest trailing-tokens-test
  (testing "Jackson 3 rejects trailing content after a complete value"
    (is (thrown? tools.jackson.core.exc.StreamReadException
                 (j/read-value "{\"a\":1} trailing")))))
