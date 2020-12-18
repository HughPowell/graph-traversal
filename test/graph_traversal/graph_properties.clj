(ns graph-traversal.graph-properties
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as spec]
            [clojure.test :refer :all]
            [clojure.test.check.generators :as test-check-gen]
            [com.gfredericks.test.chuck.clojure-test :refer [checking]]
            [graph-traversal.graph :as sut]
            [clojure.data.generators :as data-gen])
  (:import (clojure.lang PersistentQueue)
           (java.util Random)))

(defn breadth-first-traverse [g s]
  ((fn rec-bfs [explored frontier]
     (lazy-seq
       (if (empty? frontier)
         nil
         (let [v (peek frontier)
               neighbors (map first (g v))]
           (cons v (rec-bfs
                     (into explored neighbors)
                     (into (pop frontier) (remove explored neighbors))))))))
   #{s} (conj (PersistentQueue/EMPTY) s)))

(defn connected? [graph]
  (let [vertices (set (keys graph))
        root-vertices (set/difference vertices (set (mapcat #(map first %) (vals graph))))
        traverses (map #(set (breadth-first-traverse graph %)) (or (seq root-vertices) (take 1 vertices)))]
    (boolean
      (and (= vertices (apply set/union traverses))
           (seq (apply set/intersection traverses))))))

(defn weighted-graph? [graph]
  (every? (fn [[vertex weight]] (and (keyword? vertex)
                                     (int? weight)))
          (apply concat (vals graph))))

(def graph-attributes
  (test-check-gen/let [vertices (->> test-check-gen/nat
                                     (test-check-gen/fmap inc)
                                     (test-check-gen/such-that #(spec/valid? ::sut/vertices %)))
                       edges (test-check-gen/such-that #(spec/valid? ::sut/random-graph [vertices %])
                                                       (test-check-gen/fmap #(+ (dec vertices) %) test-check-gen/nat)
                                                       100000)]
                      [vertices edges]))

(deftest a-randomly-generated-graph
  (checking "has the given number of vertices" 100
            [[vertices edges] graph-attributes
             seed test-check-gen/nat]
            (binding [data-gen/*rnd* (Random. seed)]
              (is (= vertices
                     (count (sut/random-graph vertices edges))))))

  (checking "has the given number of edges" 100
            [[vertices edges] graph-attributes
             seed test-check-gen/nat]
            (binding [data-gen/*rnd* (Random. seed)]
              (is (= edges
                     (apply + (map count (vals (sut/random-graph vertices edges))))))))

  (checking "is connected" 100
            [[vertices edges] graph-attributes
             seed test-check-gen/nat]
            (binding [data-gen/*rnd* (Random. seed)]
              (is (connected? (sut/random-graph vertices edges)))))

  (checking "has weighted edges" 100
            [[vertices edges] graph-attributes
             seed test-check-gen/nat]
            (binding [data-gen/*rnd* (Random. seed)]
              (is (weighted-graph? (sut/random-graph vertices edges))))))