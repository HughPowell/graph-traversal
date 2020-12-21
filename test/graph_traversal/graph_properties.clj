(ns graph-traversal.graph-properties
  (:require [clojure.data.generators :as data-gen]
            [clojure.set :as set]
            [clojure.test :refer :all]
            [clojure.test.check.generators :as test-check-gen]
            [com.gfredericks.test.chuck.clojure-test :refer [checking]]
            [graph-traversal.graph :as sut]
            [graph-traversal.test-concerns :as test-concerns])
  (:import (java.util Random)))

(defn connected? [graph]
  (let [vertices (set (keys graph))
        root-vertices (set/difference vertices (set (mapcat #(map first %) (vals graph))))
        traverses (map #(set (test-concerns/breadth-first-traverse graph %))
                       (or (seq root-vertices) (take 1 vertices)))]
    (boolean
      (and (= vertices (apply set/union traverses))
           (seq (apply set/intersection traverses))))))

(defn weighted-graph? [graph]
  (every? (fn [[vertex weight]] (and (keyword? vertex)
                                     (int? weight)))
          (apply concat (vals graph))))

(deftest a-randomly-generated-graph
  (checking "has the given number of vertices" 100
            [[vertices edges] test-concerns/graph-attributes
             seed test-check-gen/nat]
            (binding [data-gen/*rnd* (Random. seed)]
              (is (= vertices
                     (count (sut/random-graph vertices edges))))))

  (checking "has the given number of edges" 100
            [[vertices edges] test-concerns/graph-attributes
             seed test-check-gen/nat]
            (binding [data-gen/*rnd* (Random. seed)]
              (is (= edges
                     (apply + (map count (vals (sut/random-graph vertices edges))))))))

  (checking "is connected" 100
            [[vertices edges] test-concerns/graph-attributes
             seed test-check-gen/nat]
            (binding [data-gen/*rnd* (Random. seed)]
              (is (connected? (sut/random-graph vertices edges)))))

  (checking "has weighted edges" 100
            [[vertices edges] test-concerns/graph-attributes
             seed test-check-gen/nat]
            (binding [data-gen/*rnd* (Random. seed)]
              (is (weighted-graph? (sut/random-graph vertices edges))))))

(comment
  (a-randomly-generated-graph))