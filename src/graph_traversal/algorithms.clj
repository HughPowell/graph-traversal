(ns graph-traversal.algorithms)

(defn- path-length [path]
  (apply + (map second path)))

(defn- next-vertex [unvisited tentative-paths]
  (->> tentative-paths
       (sort-by (fn [[_vertex path]] (path-length path)))
       (drop-while (fn [[vertex]] (not (contains? unvisited vertex))))
       (drop-while (fn [[_vertex path]] (= ##Inf (path-length path))))
       ffirst))

(defn- djikstras* [graph start finish]
  (loop [unvisited (set (keys graph))
         tentative-paths (assoc (into {} (map (fn [k] [k [[start ##Inf]]]) (keys graph))) start [[start 0]])]
    (let [current (next-vertex unvisited tentative-paths)]
      (cond
        (= finish current) (get tentative-paths current)
        (nil? current) [[start 0] [finish ##Inf]]
        :else (let [path-to-current (get tentative-paths current)]
                (->> current
                     (get graph)
                     (filter (fn [[k]] (contains? unvisited k)))
                     (map (fn [[vertex _distance :as edge]]
                            [vertex
                             (let [tentative-path-to-vertex (get tentative-paths vertex)
                                   potential-path-to-vertex (conj path-to-current edge)]
                               (if (< (path-length potential-path-to-vertex)
                                      (path-length tentative-path-to-vertex))
                                 potential-path-to-vertex
                                 tentative-path-to-vertex))]))
                     (into tentative-paths)
                     (recur (disj unvisited current))))))))

(defn djikstras
  "Determines the shortest path between start and finish in graph using
  [Djikstra's algorithm](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm).

  If there is a path between start and finish the output is the sequence of
  vertices representing the shortest path.

  E.g.
  (djikstras {:1 [[:2 1] [:3 2]]
              :2 [[:4 4]]
              :3 [[:4 2]]
              :4 []
              :1
              :4})
  => (:1 :3 :4)

  If there is no path between start and finish then an empty vector is returned.

  E.g.
  (djikstras {:1 [[:2 1] [:3 2]]
              :2 [[:4 4]]
              :3 [[:4 2]]
              :4 []
              :4
              :1})
  => []
  "
  [graph start finish]
  (let [path (djikstras* graph start finish)
        path-length' (path-length path)]
    (if (= path-length' ##Inf) [] (map first path))))

(defn eccentricity
  "Calculate the [eccentricity](https://en.wikipedia.org/wiki/Distance_(graph_theory)#Related_concepts)
  of the vertex in the graph.

  E.g.
  (eccentricity {:1 [[:2 1] [:3 2]],
                 :2 [[:4 4]],
                 :3 [[:4 2]],
                 :4 []}
                :1)
  => 4
  "
  [graph vertex]
  (let [paths (map #(djikstras* graph vertex %) (keys graph))]
    (->> paths
         (map #(path-length %))
         sort
         last)))

(comment
  (djikstras {:1 [[:2 1] [:3 2]],
              :2 [[:4 4]],
              :3 [[:4 2]],
              :4 []}
             :1
             :4)

  (eccentricity {:1 [[:2 1] [:3 2]],
                 :2 [[:4 4]],
                 :3 [[:4 2]],
                 :4 []}
                :1))
