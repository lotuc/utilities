(ns build
  (:require
   [clojure.java.io :as io]
   [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def native-app-basis (b/create-basis {:project "deps.edn"}))
(def native-app-jar "target/native-app.jar")
(def native-app-src-dirs ["src" "resources"])

(defn clean [_]
  (println "\ncleanup ...")
  (b/delete {:path "target"}))

(defn jar [_]
  (clean _)

  (println "\npreparing ...")
  (b/copy-dir {:src-dirs native-app-src-dirs :target-dir class-dir})

  (println "\ncompiling Clojure ...")
  (b/compile-clj {:basis native-app-basis :src-dirs native-app-src-dirs :class-dir class-dir})

  (println "\nmaking uberjar ...")
  (b/uber {:class-dir class-dir :uber-file native-app-jar :basis native-app-basis :main "lotuc.ring-web.native-app"}))

(defn exe [_]
  (let [f (io/file native-app-jar)
        dir (.getParent (io/file native-app-jar))
        cmd-args ["native-image" "-jar" (.getName f)
                  "--initialize-at-build-time=ch.qos.logback"
                  "--initialize-at-build-time=com.zaxxer.hikari.HikariConfig"
                  "--initialize-at-build-time=com.zaxxer.hikari.HikariDataSource"
                  "-H:IncludeResources=native_app.edn"
                  "-H:IncludeResources=ring-web-common.edn"
                  "--no-fallback"
                  "--trace-class-initialization=com.zaxxer.hikari.HikariConfig"]]
    (when-not (.exists f) (jar _))
    (println)
    (println cmd-args "...")
    (b/process {:dir dir :command-args cmd-args})))
