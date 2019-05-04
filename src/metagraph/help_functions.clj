(ns metagraph.help-functions
  (:require [clojure.string :as str]))

(defn get-class-name [obj]
  (second (str/split (str (type obj)) #" ")))
