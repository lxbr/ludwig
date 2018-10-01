(ns com.github.lxbr.ludwig.engine
  (:require [com.github.lxbr.jsc :as jsc]
            [clojure.java.io :as io])
  (:import java.io.File))

(defn load-libjffi!
  []
  (let [tmp (File/createTempFile "libjffi-1.2" ".jnilib")]
    (.deleteOnExit tmp)
    (with-open [is (io/input-stream (io/resource "jni/Darwin/libjffi-1.2.jnilib"))]
      (io/copy is tmp))
    (System/load (.getAbsolutePath tmp))))

(defn create-context
  []
  (jsc/JSGlobalContextCreate 0))

(defonce null (long-array 0))

(defn value-to-string
  [ctx value]
  (let [string (jsc/JSValueToStringCopy ctx value null)
        bytes (byte-array
               (jsc/JSStringGetMaximumUTF8CStringSize string))
        len (jsc/JSStringGetUTF8CString string bytes (alength bytes))]
    (String. bytes 0 (dec len) "UTF-8")))

(defn object-get-prop
  [ctx object ^String prop-name]
  (let [prop (jsc/JSStringCreateWithUTF8CString
              (.getBytes prop-name "UTF-8"))
        value (jsc/JSObjectGetProperty ctx object prop null)]
    (jsc/JSStringRelease prop)
    value))

(defn get-trace
  [ctx error]
  (let [message (object-get-prop ctx error "message")
        stack (object-get-prop ctx error "stack")]
    (str
     (value-to-string ctx message)
     "\n"
     (value-to-string ctx stack))))

(defn eval-script
  [ctx ^String script]
  (let [script-ref (jsc/JSStringCreateWithUTF8CString
                    (.getBytes script "UTF-8"))
        err (long-array 1)
        ret (jsc/JSEvaluateScript
             ctx script-ref 0 0 0 err)]
    (jsc/JSStringRelease script-ref)
    (when-not (zero? (first err))
      (println (get-trace ctx (first err))))
    ret))
