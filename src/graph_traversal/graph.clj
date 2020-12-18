(ns graph-traversal.graph
  (:require [clojure.spec.alpha :as spec]
            [clojure.data.generators :as data-gen]
            [expound.alpha :as expound]
            [clojure.set :as set]))

(spec/def ::edges nat-int?)
(spec/def ::vertices (spec/and pos-int? #(< % Integer/MAX_VALUE)))

(spec/def ::random-graph
  (spec/and (spec/tuple ::vertices ::edges)
            (fn [[vertices edges]]
              (<= (dec vertices) edges (* (/ vertices 2) (dec vertices))))))

(defn- minimally-connected-graph [vertices]
  (first
    (reduce
      (fn [[graph detached-vertices] vertex]
        (if (seq detached-vertices)
          (let [vertices-to-attach (set (take (inc (mod (data-gen/int) (count detached-vertices)))
                                              (disj detached-vertices vertex)))]
            [(assoc graph vertex vertices-to-attach)
             (set/difference detached-vertices vertices-to-attach)])
          [(assoc graph vertex #{})]))
      [{} (set (rest vertices))]
      vertices)))

(defn- fully-connected-graph [vertices]
  (->> vertices
       (map (fn [vertex] [vertex (disj vertices vertex)]))
       (into {})))

(defn- move-edge [[from to]]
  (let [vertex (first (drop-while (fn [v] (empty? (get from v)))
                                  (data-gen/shuffle (keys from))))
        edge (data-gen/rand-nth (seq (get from vertex)))]
    [(update from vertex disj edge)
     (update to vertex conj edge)]))

(defn- add-weights [graph]
  (->> graph
       (map (fn [[vertex edges]] [vertex (set (map (fn [edge] [edge (Math/abs (data-gen/byte))]) edges))]))
       (into {})))

(defn random-graph
  "Create a random, directed, weighted, connected graph with the given number
  of vertices and edges.

  The vertices of the resulting graph are represented as keyword keys to a map.
  Each key maps to a set of keyword-integer tuples (represented as a vector)
  describing each of the vertices connected to and the weight of each edge.

  E.g.
  (random-graph 4 4) might produce

  => {:A #{[:B 5] [:C 1]}
      :B #{[:D 2]}
      :C #{[:D 6]}
      :D #{}}"
  [vertices edges]
  (when-not (spec/valid? ::random-graph [vertices edges])
    (throw (ex-info (expound/expound-str ::random-graph [vertices edges])
                    (spec/explain-data ::random-graph [vertices edges]))))
  (let [vertices' (->> (iterate #(conj % (data-gen/keyword)) #{})
                       (drop-while #(> vertices (count %)))
                       first)
        fully-connected-graph (fully-connected-graph vertices')
        minimally-connected-graph (minimally-connected-graph vertices')]
    (->> [fully-connected-graph minimally-connected-graph]
         (iterate move-edge)
         (map second)
         (drop-while (fn [graph] (< (apply + (map count (vals graph))) edges)))
         first
         add-weights)))