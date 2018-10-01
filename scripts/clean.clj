(ns clean
  (:require [clojure.java.io :as io]))

(defn clean-dir
  [path]
  (let [file (io/file path)]
    (when (.exists file)
      (->> (rseq (vec (file-seq file)))
           (run! io/delete-file)))))

(clean-dir "classes")
(clean-dir "out")

(System/exit 0)
