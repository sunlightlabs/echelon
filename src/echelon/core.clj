(ns echelon.core
  (:require [datomic.api :as d :refer [db q]]
            [clojure.pprint :refer [pprint]]
            [echelon.load :refer [load-database!]]
            [echelon.text :refer [extract-names clean]]
            [echelon.util :refer [group-by-features disjoint-lists]]))

(def uri "datomic:free://localhost:4334/echelon")

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


(defn merges-for-beings [dbc [b1 & b2s]]
  (let [records (map
                 #(d/q '[:find ?record
                         :in $ ?being
                         :where [?record :record/represents ?being]]
                       dbc
                       %)
                 b2s)
        adds    (mapv #(vector :db/add (ffirst %) :record/represents b1)
                      records)
        retracts (map #(vector :db.fn/retractEntity %) b2s)
        data (vec (concat adds retracts))]
    data))

(def rules '[[(name-of ?record ?name) [?record :lobbying.client/name ?name]]
             [(name-of ?record ?name) [?record :lobbying.registrant/name ?name]]])

(defn beings-and-names [dbc]
  (d/q '[:find ?being ?name :in $ %
         :where
         [?being  :record/type :being.record/being]
         [?record :record/represents ?being]
         (name-of ?record ?name)]
       dbc
       rules))

(defn merges-based-on-exact-name [dbc]
  (println "Merging based on exact names")
  (let [dbc
        (->> (beings-and-names dbc)
             (group-by (comp clean second))
             seq
             (map second)
             (filter #(< 1 (count %)))
             (map (partial map first))
             disjoint-lists
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
             disjoint-lists
             (mapcat (partial merges-for-beings dbc))
             (d/with dbc)
             :db-after)]
    (println (how-many? dbc))
    dbc))

(defn load-data []
  (d/delete-database uri)
  (d/create-database uri)
  (let [c (d/connect uri)]
    (println "Loading Database...")
    (load-database! c)
    (println "Loaded!")
    (println (how-many? (db c)))))

(defn print-status []
  (let [c (d/connect uri)]
    (println (how-many? (db c)))))

(defn match-data []
  (println "Starting merge process")
  (println (how-many? (db (d/connect uri))))
    (as-> (db (d/connect uri)) hypothetical
            (merges-based-on-exact-name hypothetical)
            (merges-based-on-extracted-name hypothetical)
            (->> (d/q '[:find ?being ?name
                        :in $ %
                        :with ?record
                        :where
                        [?being  :record/type :being.record/being]
                        [?record :record/represents ?being]
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
                 (spit "output/names-output.clj"))))

(defn -main [arg]
  (condp = arg
    "load"   (load-data)
    "match"  (match-data)
    "status" (print-status))
    (java.lang.System/exit 0))
