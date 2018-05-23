(defproject metosin/jsonista "0.2.1-SNAPSHOT"
  :description "Clojure library for fast JSON encoding and decoding."
  :url "https://github.com/metosin/jsonista"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :source-paths ["src/clj"]
  :javac-options ["-Xlint:unchecked" "-target" "1.7" "-source" "1.7"]
  :java-source-paths ["src/java"]
  :plugins [[lein-codox "0.10.3"]
            [lein-virgil "0.1.6"]]
  :codox {:source-uri "http://github.com/metosin/jsonista/blob/master/{filepath}#L{line}"
          :output-path "doc"
          :metadata {:doc/format :markdown}}
  :dependencies [[com.fasterxml.jackson.core/jackson-databind "2.9.5"]
                 [com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.9.5"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [com.fasterxml.jackson.datatype/jackson-datatype-joda "2.9.5"]
                                  [cheshire "5.8.0"]
                                  [criterium "0.4.4"]]
                   :global-vars {*warn-on-reflection* true}}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :perf {:jvm-opts ^:replace ["-server"
                                         "-Xmx4096m"
                                         "-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"all" ["with-profile" "default:dev:default:dev,1.8"]
            "perf" ["with-profile" "default,dev,perf"]})
