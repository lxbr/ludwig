(ns com.github.lxbr.ludwig.native-image
  (:require [clojure.main]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.tools.reader]
            [clojure.tools.deps.alpha :as deps]
            [com.github.lxbr.jsc]
            [com.github.lxbr.ludwig.engine]
            [com.github.lxbr.ludwig.main])
  (:import java.io.File))

(defn build
  [graal-home-path]
  (println "Graal Home at" graal-home-path)
  (.mkdirs (io/file *compile-path*))
  (let [prefixes #{"com.github.lxbr.jsc"
                   "com.github.lxbr.effing"
                   "com.github.lxbr.ludwig.main"
                   "com.github.lxbr.ludwig.engine"
                   "clojure.tools.reader"}]
    (binding [*compiler-options* {:direct-linking true}]
      (print "AOT compiling Clojure sources...")
      (->> (all-ns)
           (map (comp name ns-name))
           (filter #(some (fn [^String prefix] (.startsWith % prefix)) prefixes))
           (run! (comp compile symbol)))
      (println "DONE")))
  (println "Starting native build...")
  (let [classpath (-> '{:deps {org.clojure/clojure       {:mvn/version "1.9.0"}
                               org.clojure/clojurescript {:mvn/version "1.10.339"}
                               com.github.jnr/jffi       {:mvn/version "1.2.17"
                                                          :classifier  ["" "native"]}}}
                      (deps/resolve-deps nil)
                      (deps/make-classpath [*compile-path* "."] nil))
        {:keys [out err exit]}
        (sh/sh (str graal-home-path File/separator "bin/native-image")
               "--no-server"
               "--verbose"
               #_"--report-unsupported-elements-at-runtime"
               "-H:Name=ludwig"
               "-H:+JNI"
               "-H:ReflectionConfigurationFiles=graal/reflect.json"
               "-H:IncludeResources=(.*/out/ludwig.js.gz)|(jni/Darwin/libjffi-1.2.jnilib)"
               "-cp" classpath
               "com.github.lxbr.ludwig.Main")]
    (if (zero? exit)
      (println out)
      (println err))))
