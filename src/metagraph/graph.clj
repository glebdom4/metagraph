(ns metagraph.graph
  (:require [clojure.set :as s]
            [metagraph.protocols :refer [Attribute Component contains-vertex?]]))

(defrecord Vertex [name attrs]
  Object
  (toString [_] (str "Vertex '" name \')))

(defrecord Edge [start-vertex end-vertex attrs oriented]
  Object
  (toString [_] (str "Edge: " start-vertex  " - " end-vertex)))

(defrecord MetagraphFragment [vertices meta-vertices edges has-oriented-edges?])

(defrecord MetaVertex [name attrs meta-fragment]
  Object
  (toString [_] (str "MetaVertex '" name \')))

(defn- add-attr* [cmpt name val]
  (update cmpt :attrs assoc name val))

(defn- remove-attr* [cmpt name]
  (update cmpt :attrs dissoc name))

;; (defn- in-vertices? [vertices edge]
;;   (let [{v1 :start-vertex v2 :end-vertex} edge]
;;     (and (contains? vertices v1) (contains? vertices v2))))

(defn- contains-vertex?* [vs mvs v]
  (or (contains? vs v) (contains? mvs v) (some #(contains-vertex? % v) mvs)))

(defn- check-added-edge-vertices [vs mvs edge]
  (let [{v1 :start-vertex v2 :end-vertex} edge
        vertices (s/union vs mvs)]
    (if-not (and (contains-vertex?* vs mvs v1)
                 (contains-vertex?* vs mvs v2))
      (throw
       (ex-info (str "The vertices of the edge "
                     "are not contained in the metagraph fragment.")
                {:cause {:edge edge}})))))

(defn- in-edge? [edge vertex]
  (let [{v1 :start-vertex v2 :end-vertex} edge]
    (or (= v1 vertex) (= v2 vertex))))

(defn- check-removed-vertex-edges [frag vertex]
  (let [edges (:edges frag)]
    (if-let [edge (some #(in-edge? % vertex) edges)]
      (throw
       (ex-info (str "This vertex is contained "
                     "in the edges of the metagraph.")
                {:cause {:edge edge}})))))

(defn- update-has-oriented [frag edge]
  (if-let [has-oriented (or (:oriented edge) (:has-oriented-edges? frag))]
    (assoc frag :has-oriented-edges? has-oriented)
    frag))

(defn- check-nested-vertices [meta-vertices v]
  (if-let [mv (some #(contains-vertex? % v) meta-vertices)]
    (throw
     (ex-info (str "The deleted vertex is contained in the nested meta-vertex.")
              {:cause {:meta-vertex mv}}))))

(extend-type Vertex
  Attribute
  (add-attr [this attr-name attr-value]
    (add-attr* this attr-name attr-value))
  (remove-attr [this attr-name]
    (remove-attr* this attr-name))
  Component
  (contains-vertex? [this v]
    (= this v))
  (add-to-fragment [this frag]
    (update frag :vertices conj this))
  (remove-from-fragment [this frag]
    (if (contains? (:vertices frag) this)
      (do
        (check-removed-vertex-edges frag this)
        (check-nested-vertices (:meta-vertices frag) this)
        (update frag :vertices disj this))
      frag)))

(extend-type Edge
  Attribute
  (add-attr [this attr-name attr-value]
    (add-attr* this attr-name attr-value))
  (remove-attr [this attr-name]
    (remove-attr* this attr-name))
  Component
  (contains-vertex? [this v]
    (let [{v1 :start-vertex v2 :end-vertex} this]
      (or (contains-vertex? v1 v) (contains-vertex? v2 v))))
  (add-to-fragment [this frag]
    (do
      (check-added-edge-vertices (:vertices frag) (:meta-vertices frag) this)
      (-> (update frag :edges conj this)
          (update-has-oriented this))))
  (remove-from-fragment [this frag]
    (update frag :edges disj this)))

(extend-type MetagraphFragment
  Component
  (contains-vertex? [this v]
    (let [{vs :vertices mvs :meta-vertices} this]
      (contains-vertex?* vs mvs v)))
  (add-to-fragment [this frag]
    (let [v (s/union (:vertices this) (:vertices frag))
          e (s/union (:edges this) (:edges frag))
          mv (s/union (:meta-vertices this) (:meta-vertices frag))
          has-oriented (or (:has-oriented-edges? this) (:has-oriented-edges? frag))]
      (MetagraphFragment. v mv e has-oriented)))
  (remove-from-fragment [this frag]
    (do
      (comment "Will be implemented in the future releases")
      this)))

(extend-type MetaVertex
  Attribute
  (add-attr [this attr-name attr-value]
    (add-attr* this attr-name attr-value))
  (remove-attr [this attr-name]
    (remove-attr* this attr-name))
  Component
  (contains-vertex? [this v]
    (contains-vertex? (:meta-fragment this) v))
  (add-to-fragment [this frag]
    (update frag :meta-vertices conj this))
  (remove-from-fragment [this frag]
    (if (contains? (:meta-vertices frag) this)
      (do
        (check-removed-vertex-edges frag this)
        (check-nested-vertices (:meta-vertices frag) this)
        (update frag :meta-vertices disj this))
      frag)))

(defn make-vertex [name & {:keys [atrs] :or {atrs {}}}]
  (Vertex. name atrs))

(defn make-edge
  [start-vertex end-vertex & {:keys [atrs oriented] :or {atrs {} oriented false}}]
  (Edge. start-vertex end-vertex atrs oriented))

(defn make-fragment [vertices meta-vertices edges]
  (do
    (doall (map (partial check-added-edge-vertices vertices meta-vertices) edges))
    (let [has-oriented (boolean (some #(= (:oriented %) true) edges))]
      (MetagraphFragment. vertices meta-vertices edges has-oriented))))

(defn make-meta-vertex [name meta-fragment & {:keys [atrs] :or {atrs {}}}]
  (if-not (:has-oriented-edges? meta-fragment)
    (MetaVertex. name atrs meta-fragment)
    (throw
     (IllegalArgumentException.
      (str "A meta-vertex cannot contain a metagraph fragment "
           "with oriented edges.")))))
