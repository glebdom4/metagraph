(ns metagraph.protocols)

(defprotocol Attribute
  "A protocol for operations with component attributes"
  (add-attr [this attr-name attr-val] "Adds an attribute to the component")
  (remove-attr [this attr-name] "Removes an attribute from the component"))

(defprotocol Component
  "A protocol for operations with metagraph components"
  (contains-vertex? [this vertex] "Checks if the component contains a vertex")
  (add-to-fragment [this fragment mv-name] "Adds the component to a fragment")
  (remove-from-fragment [this fragment] "Removes the component to a fragment")
  (t-remove-from-fragment [this fragment] "Transitively removes the component to a fragment"))
