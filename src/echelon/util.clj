(ns echelon.util
  (:require [jordanlewis.data.union-find :refer
             [union-find union get-canonical]]))

(defn group-by-features
  "Like group-by, but inserts values multiple times based on f
  returning a vector of keys."
  [f coll]
  (persistent!
   (reduce
    (fn [ret x]
      (reduce
       (fn [rett k]
         (assoc! rett k (conj (get rett k []) x)))
       ret
       (f x)))
    (transient {})
    coll)))

                                        ;
;(disjoint-lists [[1 2 3] [4 5 6] [10 12 13] [1 4 2] [2 3 4 5 6]] )
(defn disjoint-lists [lsts]
  "Given a list of lists, returns the disjoint sets formed by the
  equivalence classes described by the arguments. "
  (let [els (distinct (apply concat lsts))
        uf  (apply union-find els)
        uf  (reduce
             (fn [uf [fst & rst]] (reduce #(union %1 fst %2) uf rst))
             uf lsts)]
    (->> (.elt-map uf)
         keys
         (group-by uf)
         vals)))

(defn contains-nil? [arg]
  (if (coll? arg)
    (reduce #(or %1 %2) false (map contains-nil? arg))
    (nil? arg)))
