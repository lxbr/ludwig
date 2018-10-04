(ns com.github.lxbr.ludwig.engine
  (:require [com.github.lxbr.ludwig.internal.jsc :as jsc]))

(defn create-context
  [lib]
  (jsc/JSGlobalContextCreate lib 0))

(defonce null (long-array 0))

(defn value-to-string
  [lib ctx value]
  (let [string (jsc/JSValueToStringCopy lib ctx value null)
        bytes (byte-array
               (jsc/JSStringGetMaximumUTF8CStringSize lib string))
        len (jsc/JSStringGetUTF8CString lib string bytes (alength bytes))]
    (String. bytes 0 (dec len) "UTF-8")))

(defn object-get-prop
  [lib ctx object ^String prop-name]
  (let [prop (jsc/JSStringCreateWithUTF8CString
              lib
              (.getBytes prop-name "UTF-8"))
        value (jsc/JSObjectGetProperty lib ctx object prop null)]
    (jsc/JSStringRelease lib prop)
    value))

(defn get-trace
  [lib ctx error]
  (let [message (object-get-prop lib ctx error "message")
        stack (object-get-prop lib ctx error "stack")]
    (str
     (value-to-string lib ctx message)
     "\n"
     (value-to-string lib ctx stack))))

(defn eval-script
  [lib ctx ^String script]
  (let [script-ref (jsc/JSStringCreateWithUTF8CString
                    lib
                    (.getBytes script "UTF-8"))
        err (long-array 1)
        ret (jsc/JSEvaluateScript lib ctx script-ref 0 0 0 err)]
    (jsc/JSStringRelease lib script-ref)
    (when-not (zero? (first err))
      (println (get-trace lib ctx (first err))))
    ret))
