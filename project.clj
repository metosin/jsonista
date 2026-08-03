(defproject metosin/jsonista "2.0.0-SNAPSHOT"
  :description "Clojure library for fast JSON encoding and decoding."
  :url "https://github.com/metosin/jsonista"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v20.html"}
  :source-paths ["src/clj"]
  :resource-paths ["resources"]
  :javac-options ["-Xlint:unchecked" "--release" "17"]
  :java-source-paths ["src/java"]
  :plugins [[lein-ancient "1.0.0-RC3"]
            [lein-codox "0.10.8"]
            [lein-jmh "0.3.0"]]
  :deploy-repositories [["releases" {:url "https://repo.clojars.org/"
                                     :sign-releases false
                                     :username :env/CLOJARS_USER
                                     :password :env/CLOJARS_DEPLOY_TOKEN}]]
  :codox {:source-uri "http://github.com/metosin/jsonista/blob/master/{filepath}#L{line}"
          :output-path "doc"
          :metadata {:doc/format :markdown}}
  :dependencies [[tools.jackson.core/jackson-core "3.2.1"]
                 [tools.jackson.core/jackson-databind "3.2.1"]]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.12.5"]]}
             :dev {:dependencies [[org.clojure/clojure "1.12.5"]
                                  [jmh-clojure/jmh-clojure "0.4.1"]
                                  [tools.jackson.datatype/jackson-datatype-joda "3.2.1"]
                                  [tools.jackson.dataformat/jackson-dataformat-cbor "3.2.1"]
                                  [cheshire "6.2.0"]
                                  [com.taoensso/nippy "3.8.1"]
                                  [org.clojure/data.json "2.5.2"]
                                  [com.cognitect/transit-clj "1.1.363"]
                                  [com.clojure-goes-fast/clj-async-profiler "1.8.0"]
                                  [criterium "0.4.6"]]
                   :global-vars {*warn-on-reflection* true}}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.3"]]}
             :jmh {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :perf {:jvm-opts ^:replace ["-server"
                                         "-Xmx4096m"
                                         "-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"all" ["with-profile" "default:dev:default:dev,1.11"]
            "perf" ["with-profile" "default,dev,perf"]
            "repl" ["with-profile" "default,dev" "repl"]})
