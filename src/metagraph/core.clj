(ns ^{:doc "Graph operators"
      :author "Gleb Yeliseev"}
    metagraph.core
  (:require
   [metagraph.protocols :as pr]
   [metagraph.help-functions :refer [get-class-name]]
   [metagraph.graph :refer [make-vertex make-edge
                            make-fragment make-meta-vertex]]))

(set! *warn-on-reflection* true)

(defn- check-attributes-possibility  [cmnt]
  (if-not (satisfies? pr/Attribute cmnt)
    (throw
     (IllegalArgumentException.
      (str "Attributes cannot be added to a component of the "
           (get-class-name cmnt) " type.")))))

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
    (make-meta-vertex mv-name (reduce #(pr/add-to-fragment %2 %1) frag components))))

(defn remove-components
  "Removes a component from the metagraph (the output is a new meta-vertex)"
  [mv & components]
  (let [{mv-name :name frag :meta-fragment} mv ]
   (make-meta-vertex mv-name (reduce #(pr/remove-from-fragment %2 %1) frag components)))
