(ns build
  (:require [clojure.java.io :as io]
            [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as deps-deploy]))

(def lib 'org.lotuc/quartz-util)
(def version "0.1.0-SNAPSHOT")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))

(def pom-file "target/classes/META-INF/maven/org.lotuc/quartz-util/pom.xml")
(def jar-file "target/lotuc-quartz-util.jar")

(defn clean [_]
  (println "\nclean target/")
  (b/delete {:path "target"}))

(defn javac [_]
  (b/javac {:src-dirs ["src-java"] :class-dir class-dir
            :javac-opts ["-proc:full"]}))

(defn jar [_]
  (println "\nwriting pom ...")
  (b/write-pom
   {:src-pom "pom0.xml"
    :lib lib
    :version version
    :target "target"
    :src-dirs ["src"]
    :resource-dirs ["resources"]
    :class-dir class-dir
    :basis (b/create-basis nil)
    :scm {:url "https://github.com/lotuc/utilities/tree/main/quartz-util"
          :connection "scm:git:git://github.com/lotuc/utilities.git"
          :developerConnection "scm:git:ssh://git@github.com/lotuc/utilities.git"
          :tag (b/git-process {:git-args "rev-parse HEAD"})}})

  (io/copy (io/file pom-file) (io/file "pom.xml"))

  (println "\ncopy source & resources ...")
  (b/copy-dir {:src-dirs ["src"] :target-dir class-dir})

  (println "\nmaking jar ...")
  (b/jar {:class-dir class-dir :jar-file jar-file :basis basis}))

(defn deploy [{:keys [installer]
               :or {installer :local}
               :as _}]
  (clean _)

  (javac _)

  (jar _)

  (-> {:installer (or (:installer _) :local)
       :sign-releases? false
       :artifact jar-file}
      deps-deploy/deploy))
