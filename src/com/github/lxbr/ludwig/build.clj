(ns com.github.lxbr.ludwig.build
  (:require [cljs.build.api :as cljs]))

(defn build-js
  []
  (cljs/build
   "src"
   {:output-to          "out/ludwig.js"
    :output-dir         "out"
    :optimizations      :simple
    :verbose            true
    :static-fns         true
    :optimize-constants false
    :parallel-build     true
    :dump-core          true
    :source-map         false
    :npm-deps           false
    :process-shim       false
    :fn-invoke-direct   true
    :cache-analysis     true}))
