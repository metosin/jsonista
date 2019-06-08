(defproject metosin/jsonista "0.2.4-SNAPSHOT"
  :description "Clojure library for fast JSON encoding and decoding."
  :url "https://github.com/metosin/jsonista"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v20.html"}
  :source-paths ["src/clj"]
  :javac-options ["-Xlint:unchecked" "-target" "1.7" "-source" "1.7"]
  :java-source-paths ["src/java"]
  :plugins [[lein-codox "0.10.6"]]
  :deploy-repositories [["releases" :clojars]]
  :codox {:source-uri "http://github.com/metosin/jsonista/blob/master/{filepath}#L{line}"
          :output-path "doc"
          :metadata {:doc/format :markdown}}
  :dependencies [[com.fasterxml.jackson.core/jackson-databind "2.9.9"]
                 [com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.9.9"]]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.10.1"]]}
             :dev      {:dependencies [[com.fasterxml.jackson.datatype/jackson-datatype-joda "2.9.9"]
                                       [cheshire "5.8.1"]
                                       [com.cognitect/transit-clj "0.8.313"]
                                       [criterium "0.4.5"]]
                        :global-vars {*warn-on-reflection* true}}
             :virgil {:plugins [[lein-virgil "0.1.9"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.0"]]}
             :perf {:jvm-opts ^:replace ["-server"
                                         "-Xmx4096m"
                                         "-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"all" ["with-profile" "default:dev:default:dev,1.8:dev,1.10"]
            "perf" ["with-profile" "default,dev,perf"]
            "repl" ["with-profile" "default,dev,virgil" "repl"]})
