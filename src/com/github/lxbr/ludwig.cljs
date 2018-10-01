(ns com.github.lxbr.ludwig
  (:require [cljs.js]))

(def compiler-state
  (cljs.js/empty-state))

(defn ^:export eval-str
  [string]
  (let [result (atom nil)]
    (cljs.js/eval-str
     compiler-state
     string
     "<REPL>"
     {:eval    cljs.js/js-eval
      ;; :source-map true
      :context :expr}
     #(let [{:keys [value error]} %]
        (reset! result (or error value))))
    (pr-str @result)))
