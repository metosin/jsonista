(defproject metosin/jsonista "0.1.0-SNAPSHOT"
  :description "Clojure library for fast JSON encoding and decoding."
  :url "https://github.com/metosin/jsonista"
  :license {:name         "Eclipse Public License"
            :url          "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments     "same as Clojure"}
  :source-paths ["src/clj"]
  :javac-options ["-Xlint:unchecked" "-target" "1.7" "-source" "1.7"]
  :java-source-paths ["src/java"]
  :plugins [[lein-codox "0.10.3"]
            [lein-virgil "0.1.6"]]
  :codox {:src-uri     "http://github.com/metosin/jsonista/blob/master/{filepath}#L{line}"
          :output-path "doc"
          :metadata    {:doc/format :markdown}}
  :dependencies [[com.fasterxml.jackson.core/jackson-databind "2.8.7"]
                 [com.fasterxml.jackson.core/jackson-core "2.8.7"]]
  :profiles {:dev  {:dependencies [[org.clojure/clojure "1.8.0"]
                                   [criterium "0.4.4"]
                                   [cheshire "5.7.1"]]}
             :1.7  {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.9  {:dependencies [[org.clojure/clojure "1.9.0-alpha15"]]}
             :perf {:jvm-opts ^:replace ["-server"
                                         "-Xmx4096m"
                                         "-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"all"  ["with-profile" "dev:dev,1.7:dev,1.9"]
            "perf" ["with-profile" "default,dev,perf"]})
