(ns jsonista.tagged-test
  (:require [clojure.test :refer [deftest is are testing]]
            [jsonista.core :as j]
            [jsonista.tagged :as jt])
  (:import (clojure.lang Keyword PersistentHashSet)))

(deftest tagged-value-test
  (let [mapper (j/object-mapper {:modules [(jt/module
                                            {:handlers {Keyword {:tag "!kw"
                                                                 :encode jt/encode-keyword
                                                                 :decode jt/decode-keyword}
                                                        PersistentHashSet {:tag "!set"
                                                                           :encode jt/encode-collection
                                                                           :decode set}}})]})
        data {:system/status :status/good
              :system/statuses #{:status/good :status/bad}}]
    (testing "encoding"
      (testing "with defaults"
        (is (= "{\"system/status\":\"status/good\",\"system/statuses\":[\"status/good\",\"status/bad\"]}"
               (j/write-value-as-string data))))
      (testing "with installed module"
        (is (= "{\"system/status\":[\"!kw\",[\"status\",\"good\"]],\"system/statuses\":[\"!set\",[[\"!kw\",[\"status\",\"good\"]],[\"!kw\",[\"status\",\"bad\"]]]]}"
               (j/write-value-as-string data mapper)))))

    (testing "formaly valid but not readable keywords"
      (are [kw] (= kw (-> kw (j/write-value-as-string mapper) (j/read-value mapper)))
        (keyword "a/b" "c")
        (keyword "" "a")))

    (testing "decoding"
      (testing "with defaults"
        (is (= {"system/status" "status/good"
                "system/statuses" ["status/good" "status/bad"]}
               (j/read-value (j/write-value-as-string data)))))
      (testing "with installed module"
        (is (= {"system/status" :status/good
                "system/statuses" #{:status/good :status/bad}}
               (j/read-value (j/write-value-as-string data mapper) mapper)))))

    (testing "empty list"
      (is (= {"foo" []} (j/read-value (j/write-value-as-string {:foo []} mapper) mapper))))))
