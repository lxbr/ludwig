(ns build-native
  (:require [com.github.lxbr.ludwig.native-image :as ni]))

(ni/build (System/getenv "GRAALVM_HOME"))

(System/exit 0)


