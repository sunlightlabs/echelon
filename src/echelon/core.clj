(ns echelon.core
  (:require [datomic.api :as d :refer [db q]]
            [clojure.pprint :refer [pprint]]
            [echelon.load :refer [load-database!]]
            [echelon.text :refer [extract-names]]
            [echelon.util :refer [group-by-features]]))

(def uri "datomic:free://localhost:4334/echelon")

(defn how-many?
  "How many beings are there?"
  [dbc]
  (let [f #(-> (d/q '[:find (count ?being)
                      :in $ ?type
                      :where
                      [?r :being/type ?type]
                      [?r :being/represents ?being]]
                    dbc
                    %)
               ffirst)]
    {:clients (f :being.type/:client)
     :firms   (f :being.type/:firm)}))


(defn merges-for-beings [dbc [b1 & b2s]]
  (let [records (map
                 #(d/q '[:find ?record
                         :in $ ?being
                         :where [?record :being/represents ?being]]
                       dbc
                       %)
                 b2s)
        adds    (mapv #(vector :db/add (ffirst %) :being/represents b1)
                      records)
        retracts (map #(vector :db.fn/retractEntity %) b2s)
        data (vec (concat adds retracts))]
    data))

(def rules '[[(name-of ?record ?name) [?record :client/name ?name]]
             [(name-of ?record ?name) [?record :firm/name ?name]]])

(defn beings-and-names [dbc]
  (d/q '[:find ?being ?name :in $ %
         :where
         [?being  :being/type :being.type/:being]
         [?record :being/represents ?being]
         (name-of ?record ?name)]
       dbc
       rules))

(defn merges-based-on-exact-name [dbc]
  (println "Merging based on exact names")
  (let [dbc
        (->> (beings-and-names dbc)
             (group-by second)
             seq
             (map second)
             (filter #(< 1 (count %)))
             (map (partial map first))
             (mapcat (partial merges-for-beings dbc))
             (d/with dbc)
             :db-after)]
    (println (how-many? dbc))
    dbc))

(defn merges-based-on-extracted-name [dbc]
  (println "Merging based on extracted names")
  (let [dbc
        (->> (beings-and-names dbc)
             (group-by-features (comp extract-names second))
             seq
             (map second)
             (filter #(< 1 (count %)))
             (map (partial map first))
             (mapcat (partial merges-for-beings dbc))
             (d/with dbc)
             :db-after)]
    (println (how-many? dbc))
    dbc))

(defn -main [arg]
  (condp = arg
    "load"
    (do
      (d/delete-database uri)
      (d/create-database uri)
      (def c (d/connect uri))
      (println "Loading Database...")
      (load-database! c)
      (println "Loaded!")
      (println (how-many? (db c)))
      (java.lang.System/exit 0))
    "match"
    (do
      (as-> (db (d/connect uri)) hypothetical
            (merges-based-on-exact-name hypothetical)
            (merges-based-on-extracted-name hypothetical)
            (->> (d/q '[:find ?being ?name
                        :in $ %
                        :with ?record
                        :where
                        [?being  :being/type :being.type/:being]
                        [?record :being/represents ?being]
                        (name-of ?record ?name)]
                      hypothetical
                      rules)
                 distinct
                 (group-by first)
                 seq
                 (map (fn [[b bs]] [b (map second bs)]))
                 (sort-by (comp first second))
                 pprint
                 with-out-str
                 (spit "names-output.clj")))
      (java.lang.System/exit 0))))
