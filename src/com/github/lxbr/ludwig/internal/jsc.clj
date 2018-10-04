(ns com.github.lxbr.ludwig.internal.jsc
  (:require [com.github.lxbr.jsc.util :as util]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn generate-bindings
  []
  (->> (io/resource "deps.edn")
       (slurp)
       (edn/read-string)
       (:ludwig/headers-path)
       (util/generate-bindings-for-lib-and-headers nil)))

(defmacro create-bindings!
  []
  (generate-bindings))

(create-bindings!)
