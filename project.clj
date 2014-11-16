(defproject echelon "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.datomic/datomic-pro "0.9.4956"]

                 ;;System admin
                 [environ "0.5.0"]
                 [com.taoensso/timbre "3.2.1"]

                 ;;Data munging
                 [org.clojure/data.json "0.2.5"]
                 [me.raynes/fs "1.4.4"]
                 [org.jordanlewis/data.union-find "0.1.0"]
                 [clj-time "0.7.0"]
                 [instaparse "1.3.2"]

                 ;;Analysis
                 [incanter "1.5.5"]

                 ;;Testing
                 [instagenerate "0.1.0-SNAPSHOT"]
                 [org.clojure/core.logic "0.8.7"]
                 [org.clojure/test.check "0.5.9"]
                 [org.clojure/test.generative "0.5.0"]]
  :plugins [[lein-environ "0.5.0"]]
  :profiles {:dev
             [:user {:jvm-opts ["-Xmx4g" "-Xms1g"]}]
             :prod
             [:user {:jvm-opts ["-Xmx15g" "-Xms5g" "-server"
;                                "-agentpath:/home/zmaril/software/yjp-2013-build-13088/bin/linux-x86-64/libyjpagent.so"
                                ]}]}

  :main echelon.core)
