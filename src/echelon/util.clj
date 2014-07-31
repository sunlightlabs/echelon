(ns echelon.util
  (:require [jordanlewis.data.union-find :refer
             [union-find union get-canonical]]
            [datomic.api :as d :refer [db q]]
            [taoensso.timbre :as timbre]))
(timbre/refer-timbre)

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

(defn how-many?
  "How many beings are there?"
  [dbc]
  (let [f #(-> (d/q '[:find (count ?being)
                      :in $ ?type
                      :where
                      [?r :record/type ?type]
                      [?r :record/represents ?being]]
                    dbc
                    %)
               ffirst)]
    {:clients    (f :lobbying.record/client)
     :registrant (f :lobbying.record/registrant)
     :lobbyist   (f :lobbying.record/lobbyist)
     :activity   (f :lobbying.record/activity)
     :contact    (f :lobbying.record/contact)}))

(defn db-prn
  [stage dbc]
  (info stage)
  (info (how-many? dbc))
  dbc)
