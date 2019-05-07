(ns metagraph.help-functions
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]
            [java-time :as jt]))

(defn get-class-name [obj]
  (second (str/split (str (type obj)) #" ")))

(defn now []
  (str (jt/local-date) "T" (jt/format "hh:mm:ss" (jt/local-time))))

(defn pprint-str [obj]
  (with-out-str (pp/pprint obj)))
