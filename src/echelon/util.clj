(ns echelon.util)

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
