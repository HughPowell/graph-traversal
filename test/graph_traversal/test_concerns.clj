(ns graph-traversal.test-concerns
  (:require [clojure.test.check.generators :as test-check-gen]
            [clojure.spec.alpha :as spec]
            [graph-traversal.graph :as graph])
  (:import (clojure.lang PersistentQueue)))

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
   #{s}
   (conj (PersistentQueue/EMPTY) s)))

(def graph-attributes
  (test-check-gen/let [vertices (->> test-check-gen/nat
                                     (test-check-gen/fmap inc)
                                     (test-check-gen/such-that #(spec/valid? ::graph/vertices %)))
                       edges (test-check-gen/such-that #(spec/valid? ::graph/random-graph [vertices %])
                                                       (test-check-gen/fmap #(+ (dec vertices) %) test-check-gen/nat)
                                                       100000)]
                      [vertices edges]))

