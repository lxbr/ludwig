(ns com.github.lxbr.ludwig.main
  (:require [clojure.main]
            [com.github.lxbr.ludwig.engine :as jsc]
            [clojure.java.io :as io]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.reader-types :as reader-types]
            [cljs.tagged-literals])
  (:import clojure.lang.LineNumberingPushbackReader
           java.util.zip.GZIPInputStream)
  (:gen-class
   :name com.github.lxbr.ludwig.Main))

(set! *warn-on-reflection* true)

(defn repl-init
  []
  (println "Enter :cljs/quit to exit the REPL"))

(defn repl-eval
  [ctx string]
  (let [escaped-string (pr-str string)]
    (->> (str "com.github.lxbr.ludwig.eval_str(" escaped-string ")")
         (jsc/eval-script ctx)
         (jsc/value-to-string ctx))))

(defn repl-read
  [request-prompt request-exit]
  (if-some [result ({:line-start request-prompt :stream-end request-exit}
                    (clojure.main/skip-whitespace *in*))]
    result
    (let [[form string]
          (binding [reader/*read-eval* false
                    reader/*data-readers* cljs.tagged-literals/*cljs-data-readers*]
            (reader/read+string
             (reader-types/source-logging-push-back-reader *in*)
             {:features #{:cljs}
              :read-cond :allow}))]
      (if (= :cljs/quit form)
        request-exit
        string))))

(defn repl-caught
  [^Throwable t]
  (prn t))

(defn repl-print
  [value]
  (println value))

(defn repl-prompt
  []
  (print "cljs.user=> "))

;; copied from clojure.main
;; native image generation struggles with dynamic class loading
;; we need to remove all references to code that can trigger
;; dynamic class loading: eval (obviously), read (via *read-eval* in LispReader/tools.reader)
(defn repl
  [options]
  (let [{:keys [init need-prompt prompt flush read eval print caught]
         :or {need-prompt (if (instance? LineNumberingPushbackReader *in*)
                            #(.atLineStart ^LineNumberingPushbackReader *in*)
                            #(identity true))
              flush       flush}}
        options
        request-prompt (Object.)
        request-exit (Object.)
        read-eval-print
        (fn []
          (try
            (let [input (clojure.main/with-read-known (read request-prompt request-exit))]
              (or (#{request-prompt request-exit} input)
                  (let [value (eval input)]
                    (print value)
                    (set! *3 *2)
                    (set! *2 *1)
                    (set! *1 value))))
            (catch Throwable e
              (caught e)
              (set! *e e))))]
    (clojure.main/with-bindings
      (try
        (init)
        (catch Throwable e
          (caught e)
          (set! *e e)))
      (prompt)
      (flush)
      (loop []
        (when-not 
       	    (try (identical? (read-eval-print) request-exit)
	         (catch Throwable e
	           (caught e)
	           (set! *e e)
	           nil))
          (when (need-prompt)
            (prompt)
            (flush))
          (recur))))))

(defn -main
  [& args]
  (jsc/load-libjffi!)
  (let [init-script (with-open [is (-> (io/resource "out/ludwig.js.gz")
                                       (io/input-stream))
                                gzis (GZIPInputStream. is)]
                      (slurp gzis))
        ctx (jsc/create-context)]
    (jsc/eval-script ctx init-script)
    (repl
     {:init repl-init
      :prompt repl-prompt
      :eval (fn [value] (repl-eval ctx value))
      :read repl-read
      :caught repl-caught
      :print repl-print})))
