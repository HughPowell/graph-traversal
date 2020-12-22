(ns graph-traversal.algorithms-properties
  (:require [clojure.data.generators :as data-gen]
            [clojure.set :as set]
            [clojure.test :refer :all]
            [clojure.test.check.generators :as test-check-gen]
            [com.gfredericks.test.chuck.clojure-test :refer [checking]]
            [graph-traversal.algorithms :as sut]
            [graph-traversal.graph :as graph]
            [graph-traversal.test-concerns :as test-concerns])
  (:import (java.util Random)))

(defn path-length
  ([path]
   (apply + (map second path)))
  ([graph path]
   (->> path
        (partition 2 1)
        (map (fn [[from to]] (->> from
                                  (get graph)
                                  (filter (fn [[vertex]] (= vertex to)))
                                  first)))
        path-length)))

(defn connected? [seed graph start finish]
  (contains? (set (test-concerns/random-breadth-first-traversal seed graph start)) finish))

(defn path-contains? [path start finish]
  (let [vertices (set (map first path))]
    (and (contains? vertices start)
         (contains? vertices finish))))

(defn shorter-path? [graph start finish prospective-path]
  (let [prospective-path-length (path-length graph prospective-path)]
    (loop [paths (map (fn [edge] [[start 0] edge]) (get graph start))]
      (cond
        (empty? paths)
        false

        (->> paths
             (filter #(path-contains? % start finish))
             (some #(< (path-length %) prospective-path-length)))
        true

        :else
        (recur
          (mapcat (fn [path]
                    (->> path
                         last
                         first
                         (get graph)
                         (map #(conj path %))
                         (filter (fn [path] (< (path-length path) prospective-path-length)))
                         (filter (fn [path] (= (count path) (count (set path)))))
                         seq))
                  paths))))))

(def graph-gen
  (test-check-gen/let [[vertices edges] test-concerns/graph-attributes
                       seed test-check-gen/nat]
                      (binding [data-gen/*rnd* (Random. seed)]
                        (graph/random-graph vertices edges))))

(deftest djikstras-algorithm-finds-a-path-between-two-vertices

  (checking "that only contains vertices from the graph" 100
            [graph graph-gen
             start (test-check-gen/elements (keys graph))
             finish (test-check-gen/elements (keys graph))]
            (is (set/subset?
                  (set (sut/djikstras graph start finish))
                  (set (keys graph)))))

  (checking "that starts at the start and finishes at the finish when start and finished are connected" 100
            [graph graph-gen
             start (test-check-gen/elements (keys graph))
             finish (test-check-gen/elements (keys graph))
             seed test-check-gen/nat]
            (let [path (sut/djikstras graph start finish)]
              (if (connected? seed graph start finish)
                (is (and (= start (first path))
                         (= finish (last path))))
                (is (empty? path)))))

  (checking "using a legal path through the graph" 100
            [graph graph-gen
             start (test-check-gen/elements (keys graph))
             finish (test-check-gen/elements (keys graph))]
            (is (every? (fn [[from to]]
                          (contains? (set (map first (get graph from))) to))
                        (partition 2 1 (sut/djikstras graph start finish)))))

  (checking "where there is no path shorter between the two" 100
            [graph graph-gen
             start (test-check-gen/elements (keys graph))
             finish (test-check-gen/elements (keys graph))]
            (is (not (shorter-path? graph start finish (sut/djikstras graph start finish))))))

(defn traversal-paths [graph start]
  (loop [frontier [start]
         explored #{}
         paths {start [[start 0]]}]
    paths
    (let [current (peek frontier)
          unexplored-vertices (remove (fn [[vertex]] (contains? explored vertex)) (get graph current))]
      (if (empty? frontier)
        (map (fn [[vertex]] (get paths vertex [[vertex ##Inf]])) graph)
        (recur
          (into (pop frontier) (map first unexplored-vertices))
          (conj explored current)
          (->> unexplored-vertices
               (map (fn [[vertex :as edge]] [vertex (conj (get paths current) edge)]))
               (into paths)))))))

(deftest the-eccentricity-of-a-vertex

  ;; NOTE: These properties won't catch all wrong results, but should catch most programmatic errors
  ;; over a number of runs.
  ;; Are there properties that will?
  ;; TODO: This test is a little on the slow side. What optimisations are possible?

  (checking "is zero if there is only one vertex and positive otherwise" 100
            [graph graph-gen
             vertex (test-check-gen/elements (keys graph))]
            (let [eccentricity (sut/eccentricity graph vertex)]
              (if (= 1 (count graph))
                (is (zero? eccentricity))
                (is (pos? eccentricity)))))

  (checking "is infinite if the graph is disconnected from the vertex and finite if it isn't" 100
            [graph graph-gen
             vertex (test-check-gen/elements (keys graph))
             seed test-check-gen/nat]
            (let [eccentricity (sut/eccentricity graph vertex)]
              (if (every? #(connected? seed graph vertex %) (keys graph))
                (is (not= ##Inf eccentricity))
                (is (= ##Inf eccentricity)))))

  (checking "is at most as long as the longest path with the fewest edges" 100
            [graph graph-gen
             vertex (test-check-gen/elements (keys graph))]
            (let [eccentricity (sut/eccentricity graph vertex)
                  traversal-paths (traversal-paths graph vertex)]
              (is (>= (last (sort (map path-length traversal-paths)))
                      eccentricity)))))

(deftest the-radius-of-a-graph

  ;; NOTE: These properties won't catch all wrong results, but should catch most programmatic errors
  ;; over a number of runs.
  ;; Are there properties that will?
  ;; TODO: This test is too slow, so the number of runs has been decreased. What optimisations are possible?

  (let [test-runs 30]

    (checking "is zero if there is only one vertex and positive otherwise" test-runs
              [graph graph-gen]
              (let [radius (sut/radius graph)]
                (if (= 1 (count graph))
                  (is (zero? radius))
                  (is (pos? radius)))))

    (checking "is never infinite" test-runs
              [graph graph-gen]
              (is (not= ##Inf (sut/radius graph))))

    (checking "is at most the length of the shortest path where the path is the shortest distance between any two vertices" test-runs
              [graph graph-gen]
              (let [radius (sut/radius graph)
                    traversal-paths-per-vertex (map #(traversal-paths graph %) (keys graph))
                    longest-path-from-vertex (map #(last (sort (map path-length %))) traversal-paths-per-vertex)]
                (is (>= (apply min longest-path-from-vertex)
                        radius))))))

(defn all-vertices-connected? [seed graph]
  ;; This is likely to run until the heat death of the universe for medium, highly connected graphs
  ;; I think the odds of that happening are slim given the random nature of the generated graphs
  (->> graph
       keys
       (mapcat (fn [start]
                 (map (fn [finish]
                        (connected? seed graph start finish))
                      (keys graph))))
       (drop-while true?)
       empty?))

(deftest the-diameter-of-a-graph

  ;; NOTE: These properties won't catch all wrong results, but should catch most programmatic errors
  ;; over a number of runs.
  ;; Are there properties that will?
  ;; TODO: This test is too slow, so the number of runs has been decreased. What optimisations are possible?

  (let [test-runs 35]
    (checking "is zero if there is only one vertex and positive otherwise" test-runs
              [graph graph-gen]
              (let [diameter (sut/diameter graph)]
                (if (= 1 (count graph))
                  (is (zero? diameter))
                  (is (pos? diameter)))))

    (checking "is infinite if any vertex cannot reach all other vertices" test-runs
              [graph graph-gen
               seed test-check-gen/nat]
              (let [diameter (sut/diameter graph)]
                (if (all-vertices-connected? seed graph)
                  (is (not= ##Inf diameter))
                  (is (= ##Inf diameter)))))

    (checking "is at most the length of the longest path where the path is the shortest distance between any two vertices" test-runs
              [graph graph-gen]
              (let [diameter (sut/diameter graph)
                    traversal-paths-per-vertex (map #(traversal-paths graph %) (keys graph))
                    longest-path-from-vertex (map #(last (sort (map path-length %))) traversal-paths-per-vertex)]
                (is (>= (apply max longest-path-from-vertex)
                        diameter))))))

(comment
  (djikstras-algorithm-finds-a-path-between-two-vertices)
  (the-eccentricity-of-a-vertex)
  (the-radius-of-a-graph)
  (the-diameter-of-a-graph))
