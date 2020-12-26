(ns jsonista.tagged-value-test
  (:require [clojure.test :refer [deftest is testing]]
            [jsonista.core :as j]
            [jsonista.tagged-value :as tagged-value]))

(deftest tagged-value-test
  (let [mapper (j/object-mapper {:modules [(tagged-value/module {:keyword-tag "!kw"})]})
        data {:system/status :status/good
              :system/statuses [:status/good :status/bad]}]
    (testing "encoding"
      (testing "with defaults"
        (is (= "{\"system/status\":\"status/good\",\"system/statuses\":[\"status/good\",\"status/bad\"]}"
               (j/write-value-as-string data))))
      (testing "with installed module"
        (is (= "{\"system/status\":[\"!kw\",\"status/good\"],\"system/statuses\":[[\"!kw\",\"status/good\"],[\"!kw\",\"status/bad\"]]}"
               (j/write-value-as-string data mapper)))))

    (testing "decoding"
      (testing "with defaults"
        (is (= {"system/status" "status/good"
                "system/statuses" ["status/good" "status/bad"]}
               (j/read-value (j/write-value-as-string data)))))
      (testing "with installed module"
        (is (= {"system/status" :status/good
                "system/statuses" [:status/good :status/bad]}
               (j/read-value (j/write-value-as-string data mapper) mapper)))))

    (testing "empty list"
      (let [data {:foo []}]
        (is (= {"foo" []} (j/read-value (j/write-value-as-string {:foo []} mapper) mapper)))))))
