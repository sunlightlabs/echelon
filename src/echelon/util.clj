(ns echelon.util
  (:require [jordanlewis.data.union-find :refer
             [union-find union get-canonical]]
            [datomic.api :as d :refer [db q]]
            [taoensso.timbre :as timbre]))
(timbre/refer-timbre)

(def uri "datomic:free://127.0.0.1:4334/echelon")

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

(defn transpose [m]
  (apply mapv vector m))

(defn ident-finder [dbc k]
  (:db/ident (d/touch (d/entity dbc k))))

(defn finder [k]
  (d/touch (d/entity (db (d/connect uri)) k)))


(defn how-many-complicated?
  "How many beings are there?"
  [dbc]
  (->> (d/q '[:find ?being ?type
              :where
              [?being :record/type :being.record/being]
              [?record :record/represents ?being]
              [?record :record/type ?type]]
            dbc)
       (group-by first)
       vals
       (map #(map second %))
       (map (comp sort distinct))
       frequencies
       (map (fn [[k v]] [(map (partial ident-finder dbc) k) v]))
       (into {})))

(defn how-many?
  "How many beings are there?"
  [dbc]
  (->> (how-many-complicated? dbc)
       (group-by-features first)
       (map (fn [[k v]]
              [k (reduce + (map second v))]))
       (into {})))

(defn db-prn
  [stage dbc]
  (info stage)
  (info (how-many? dbc))
  dbc)


(defn breakout
  ([vs] (breakout -1 vs))
  ([n vs]
     (->> (group-by first vs)
          (map (fn [[k v]]
                 [k  (let [rst (distinct (map rest v))]
                       (if (or (= 1 (count (first rst)))
                               (zero? n))
                         (apply concat rst)
                         (breakout (dec n) rst)))]))
          (into {}))))

(defn prune [m]
  (->> m
       (map
        (fn [[k vs]]
          (if (seq? vs)
            (if (= 1 (count vs))
              [k (first vs)]
              [k vs])
            [k (prune vs)])))
       (into {})))

(defn modify [d f m]
  (->> m
       (map (fn [[k v]]
              (if (= 0 d)
                (f k v)
                [k (modify (dec d) f v)])))
       (into {})))

(->> [[:a :b 1]
      [:a :b 2]
      [:a :c 3]
      [:a :c 4]]
     breakout
     (modify 0 (fn [k v] [(name k) v]))
     (modify 1 (fn [k v] [(first (name k) ) v]))
     )
