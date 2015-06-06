(defproject calfpath "0.2.0-SNAPSHOT"
  :description "A la carte ring request matching"
  :url "https://github.com/kumarshantanu/calfpath"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :global-vars {*warn-on-reflection* true
                *assert* true}
  :java-source-paths ["java-src"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :profiles {:dev {:dependencies [[org.clojure/tools.nrepl "0.2.10"]]}
             :c16 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :c17 {:dependencies [[org.clojure/clojure "1.7.0-RC1"]]
                   :global-vars {*unchecked-math* :warn-on-boxed}}
             :perf {:dependencies [[compojure "1.3.4" :exclusions [[org.clojure/clojure] [clojure-complete]]]
                                   [citius    "0.1.1"]]
                    :test-paths ["perf"]
                    :jvm-opts ^:replace ["-server" "-Xms2048m" "-Xmx2048m"]}})
