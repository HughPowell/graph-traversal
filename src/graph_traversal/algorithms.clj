(ns graph-traversal.algorithms)

(defn- path-length [path]
  (apply + (map second path)))

(defn- next-vertex [unvisited tentative-paths]
  (->> tentative-paths
       (remove (fn [[_vertex path]] (= :infinity path)))
       (sort-by (fn [[_vertex path]] (path-length path)))
       (drop-while (fn [[vertex]] (not (contains? unvisited vertex))))
       ffirst))

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

  If there is no path between start and finish then :infinity is returned.

  E.g.
  (djikstras {:1 [[:2 1] [:3 2]]
              :2 [[:4 4]]
              :3 [[:4 2]]
              :4 []
              :4
              :1})
  => :infinity
  "
  [graph start finish]
  (loop [unvisited (set (keys graph))
         tentative-paths (assoc (into {} (map (fn [k] [k :infinity]) (keys graph))) start [[start 0]])]
    (let [current (next-vertex unvisited tentative-paths)]
      (cond
        (= finish current) (map first (get tentative-paths current))
        (nil? current) :infinity
        :else (let [path-to-current (get tentative-paths current)]
                (->> current
                     (get graph)
                     (filter (fn [[k]] (contains? unvisited k)))
                     (map (fn [[vertex _distance :as edge]]
                            [vertex
                             (let [tentative-path-to-vertex (get tentative-paths vertex)
                                   potential-path-to-vertex (conj path-to-current edge)]
                               (cond
                                 (= :infinity tentative-path-to-vertex)
                                 potential-path-to-vertex

                                 (< (path-length potential-path-to-vertex)
                                    (path-length tentative-path-to-vertex))
                                 potential-path-to-vertex

                                 :else
                                 tentative-path-to-vertex))]))
                     (into tentative-paths)
                     (recur (disj unvisited current))))))))

(comment
  (djikstras {:1 [[:2 1] [:3 2]],
              :2 [[:4 4]],
              :3 [[:4 2]],
              :4 []}
             :1
             :4))