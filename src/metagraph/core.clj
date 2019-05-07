(ns ^{:doc "Graph operators"
      :author "Gleb Yeliseev"}
    metagraph.core
  (:refer-clojure :exclude [+ -])
  (:require
   [clojure.string :as str]
   [zprint.core :as zp]
   [metagraph.protocols :as pr]
   [metagraph.help-functions :as hf]
   [metagraph.graph :refer [make-vertex make-edge
                            make-fragment make-meta-vertex]]))

(set! *warn-on-reflection* true)

(defn- check-attributes-possibility  [cmnt]
  (if-not (satisfies? pr/Attribute cmnt)
    (throw
     (IllegalArgumentException.
      (str "Attributes cannot be added to a component of the "
           (hf/get-class-name cmnt) " type.")))))

(defn add-attribute
  "Adds an attribute or attributes (name-value) to the component"
  ([component attr-name attr-value]
   (do
     (check-attributes-possibility component)
     (pr/add-attr component attr-name attr-value)))
  ([component attr-name attr-value & {:as more}]
   (let [component (add-attribute component attr-name attr-value)]
     (reduce-kv add-attribute component more))))

(defn remove-attribute
  "Removes an attribute or attributes (by name) from the component"
  ([component attr-name]
   (do
    (check-attributes-possibility component)
    (pr/remove-attr component attr-name)))
  ([component attr-name & more]
   (let [component (remove-attribute component attr-name)]
     (reduce remove-attribute component more))))

(defn add-components
  "Adds a component to the metagraph (the output is a new meta-vertex)"
  [mv-name & components]
  (let [frag (make-fragment #{} #{} #{})]
    (->> (reduce #(pr/add-to-fragment %2 %1 mv-name) frag components)
      (make-meta-vertex mv-name))))

(defn remove-components
  "Removes a component from the metagraph (the output is a new meta-vertex)"
  [mv transitively & components]
  (let [{mv-name :name frag :meta-fragment} mv
        rm-func (if transitively pr/t-remove-from-fragment pr/remove-from-fragment)]
    (make-meta-vertex mv-name (reduce #(rm-func %2 %1) frag components))))

(defn serialize
  "Saves a metagraph component to an EDN file"
  [component fout & {:keys [human-readable]}]
  (let [file-name (str fout "-" (hf/now) "-SNAPSHOT.edn")]
    (if human-readable
      (spit file-name (zp/zprint-str component {:record {:hang? nil}}))
      (spit file-name (prn-str component)))))

(defn deserialize
  "Reads a metagraph component from an EDN file"
  [fin]
  (read-string (slurp fin)))

(def ++ make-edge)
(def + add-components)
(def - (partial remove-components false))
(def -* (partial remove-components true))

(defmacro def-v [func name & others]
  (let [str-name (str name)]
    `(def ~name (~func ~str-name ~@others))))
