(defproject metosin/jsonista "0.3.7"
  :description "Clojure library for fast JSON encoding and decoding."
  :url "https://github.com/metosin/jsonista"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v20.html"}
  :source-paths ["src/clj"]
  :resource-paths ["resources"]
  :javac-options ["-Xlint:unchecked" "-target" "1.8" "-source" "1.8"]
  :java-source-paths ["src/java"]
  :plugins [[lein-codox "0.10.8"]
            [lein-jmh "0.3.0"]]
  :deploy-repositories [["releases" {:url "https://repo.clojars.org/"
                                     :sign-releases false
                                     :username :env/CLOJARS_USER
                                     :password :env/CLOJARS_DEPLOY_TOKEN}]]
  :codox {:source-uri "http://github.com/metosin/jsonista/blob/master/{filepath}#L{line}"
          :output-path "doc"
          :metadata {:doc/format :markdown}}
  :dependencies [[com.fasterxml.jackson.core/jackson-core "2.14.1"]
                 [com.fasterxml.jackson.core/jackson-databind "2.14.1"]
                 [com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.14.1"]]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.10.1"]]}
             :dev {:dependencies [[org.clojure/clojure "1.10.1"]
                                  [jmh-clojure/jmh-clojure "0.4.1"]
                                  [com.fasterxml.jackson.datatype/jackson-datatype-joda "2.14.1"]
                                  [cheshire "5.11.0"]
                                  [com.taoensso/nippy "3.2.0"]
                                  [org.clojure/data.json "2.4.0"]
                                  [com.cognitect/transit-clj "1.0.329"]
                                  [org.msgpack/msgpack-core "0.9.3"]
                                  [org.msgpack/jackson-dataformat-msgpack "0.9.3"
                                   :exclusions [com.fasterxml.jackson.core/jackson-databind]]
                                  [com.clojure-goes-fast/clj-async-profiler "1.0.3"]
                                  [criterium "0.4.6"]]
                   :global-vars {*warn-on-reflection* true}}
             :virgil {:plugins [[lein-virgil "0.1.9"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :jmh {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :perf {:jvm-opts ^:replace ["-server"
                                         "-Xmx4096m"
                                         "-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"all" ["with-profile" "default:dev:default:dev,1.8:dev,1.9"]
            "perf" ["with-profile" "default,dev,perf"]
            "repl" ["with-profile" "default,dev,virgil" "repl"]})
