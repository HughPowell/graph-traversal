(ns graph-traversal.validation
  (:require [clojure.spec.alpha :as spec]
            [expound.alpha :as expound]))

(spec/def ::edge (spec/tuple some? pos?))
(spec/def ::edges (spec/coll-of ::edge))
(spec/def ::graph (spec/map-of some? ::edges :min-count 1))

(defn validate [spec x]
  (when-not (spec/valid? spec x)
    (throw (ex-info (expound/expound-str spec x)
                    (spec/explain-data spec x)))))
