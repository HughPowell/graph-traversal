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

(defn connected? [graph start finish]
  (contains? (set (test-concerns/breadth-first-traverse graph start)) finish))

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
            (let [path (sut/djikstras graph start finish)]
              (if (connected? graph start finish)
                (is (set/subset?
                      (set path)
                      (set (keys graph))))
                (is (= :infinity path)))))

  (checking "that starts at the start and finishes at the finish" 100
            [graph graph-gen
             start (test-check-gen/elements (keys graph))
             finish (test-check-gen/elements (keys graph))]
            (let [path (sut/djikstras graph start finish)]
              (if (connected? graph start finish)
                (is (and (= start (first path))
                         (= finish (last path))))
                (is (= :infinity path)))))

  (checking "or :infinity if one does not exist" 100
            [graph graph-gen
             start (test-check-gen/elements (keys graph))
             finish (test-check-gen/elements (keys graph))]
            (let [path (sut/djikstras graph start finish)]
              (if (connected? graph start finish)
                (is (every? (fn [[from to]]
                              (contains? (set (map first (get graph from))) to))
                            (partition 2 1 path)))
                (is (= :infinity path)))))

  (checking "where there is no path shorter between the two" 100
            [graph graph-gen
             start (test-check-gen/elements (keys graph))
             finish (test-check-gen/elements (keys graph))]
            (let [path (sut/djikstras graph start finish)]
              (if (connected? graph start finish)
                (is (not (shorter-path? graph start finish path)))
                (is (= :infinity path))))))

(defn traversal [graph start]
  (loop [frontier [start]
         explored #{}
         paths {start [[start 0]]}]
    (let [current (peek frontier)
          unexplored-vertices (remove (fn [[vertex]] (contains? explored vertex)) (get graph current))]
      (if (empty? frontier)
        (vals paths)
        (recur
          (into (pop frontier) (map first unexplored-vertices))
          (conj explored current)
          (->> unexplored-vertices
               (map (fn [[vertex :as edge]] [vertex (conj (get paths current) edge)]))
               (into paths)))))))

(deftest the-eccentricity-of-a-vertex

  ;; TODO: I _think_ these don't provide complete coverage. Are there properties that would?
  ;; TODO: This test is a little on the slow side. What optimisations are possible?

  (checking "is infinite if the graph is disconnected and not if it isn't" 100
            [graph graph-gen
             vertex (test-check-gen/elements (keys graph))]
            (let [eccentricity (sut/eccentricity graph vertex)]
              (if (every? #(connected? graph vertex %) (keys graph))
                (is (not (= :infinity eccentricity)))
                (is (= :infinity eccentricity)))))

  (checking "is at most as long as the longest path with the fewest edges" 100
            [graph graph-gen
             vertex (test-check-gen/elements (keys graph))]
            (let [eccentricity (sut/eccentricity graph vertex)
                  traversal-paths (traversal graph vertex)]
              (if (= :infinity eccentricity)
                (is (> (count (keys graph)) (count (set (mapcat #(map first %) traversal-paths)))))
                (is (>= (last (sort (map path-length traversal-paths))) eccentricity))))))

(comment
  (djikstras-algorithm-finds-a-path-between-two-vertices)
  (the-eccentricity-of-a-vertex))
