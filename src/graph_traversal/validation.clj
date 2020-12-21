(ns graph-traversal.validation
  (:require [clojure.spec.alpha :as spec]
            [expound.alpha :as expound]))

(defn validate [spec x]
  (when-not (spec/valid? spec x)
    (throw (ex-info (expound/expound-str spec x)
                    (spec/explain-data spec x)))))
