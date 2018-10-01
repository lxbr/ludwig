(ns build-js
  (:require [clojure.java.io :as io]
            [com.github.lxbr.ludwig.build :as b])
  (:import java.util.zip.GZIPOutputStream))

(b/build-js)

(with-open [os (io/output-stream "out/ludwig.js.gz")
            gzos (GZIPOutputStream. os)]
  (io/copy (io/file "out/ludwig.js") gzos))

(System/exit 0)
