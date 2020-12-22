(ns dev.repl
  (:require [kaocha.repl :refer :all]
            [graph-traversal.graph :refer :all]
            [graph-traversal.algorithms :refer :all]
            [graph-traversal.graph-properties :refer [a-randomly-generated-graph]]
            [graph-traversal.algorithms-properties :refer [djikstras-algorithm-finds-a-path-between-two-vertices
                                                           the-eccentricity-of-a-vertex
                                                           the-radius-of-a-graph
                                                           the-diameter-of-a-graph]]))
