(ns echelon.core
  (:require [datomic.api :as d :refer [db q]]
            [clojure.pprint :refer [pprint]]
            [echelon.load :refer [load-database!]]
            [echelon.text :refer [extract-names clean]]
            [echelon.util :refer [group-by-features disjoint-lists]]
            [taoensso.timbre :as timbre]))
(timbre/refer-timbre)

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

(def rules
  '[[(name-of ?record ?name) [?record :lobbying.client/name ?name]]
    [(name-of ?record ?name) [?record :lobbying.registrant/organization-name ?name]]
    [(either-form ?form) [?form :record/type :lobbying.record/registration]]
    [(either-form ?form) [?form :record/type :lobbying.record/report]]
    [(represents ?record ?being) [?record :record/represents ?being]]
    ])

(defn beings-and-names [dbc]
  (d/q '[:find ?being ?name :in $ %
         :where
         [?being  :record/type :being.record/being]
         [?record :record/represents ?being]
         (name-of ?record ?name)]
       dbc
       rules))

(defn grouped-matches->datoms [dbc lst]
  (->> lst
       seq
       (map second)
       (filter #(< 1 (count %)))
       (map (partial map first))
       disjoint-lists
       (mapcat (partial merges-for-beings dbc))))


(defn same-client-registrant-merge-datoms [dbc]
  (info "Find datoms where forms indicate the client and registrant are the same")
  (->> (d/q '[:find ?client-being ?registrant-being
              :in $ %
              :where
              [?form       :lobbying.form/client-registrant-same true]
              [?form       :lobbying.form/client ?client]
              [?form       :lobbying.form/registrant ?registrant]
              (represents ?client ?client-being)
              (represents ?registrant ?registrant-being)]
            dbc
            rules)
       disjoint-lists
       (mapcat (partial merges-for-beings dbc))))

(defn merges-based-on-exact-name! [dbc]
  (info "Merging based on exact names")
  (let [dbc
        (->> (beings-and-names dbc)
             (group-by (comp clean second))
             (partial grouped-matches->datoms dbc))]
    (info "Trying to print how-many?")
    (info (how-many? dbc))
    dbc))

(defn merges-based-on-extracted-name [dbc]
  (info "Merging based on extracted names")
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
    (info (how-many? dbc))
    dbc))

(defn print-status []
  (let [c (d/connect uri)]
    (info (how-many? (db c)))))

(defn load-data []
  (d/delete-database uri)
  (d/create-database uri)
  (let [c (d/connect uri)]
    (info "Loading Database...")
    (load-database! c)
    (info "Loaded!")
    (print-status)))

(defn db-prn
  [stage dbc]
  (info stage)
  (info (how-many? dbc)))

(defn match-data []
  (info "Starting matching process")
  (let [conn (d/connect uri)]
    (->> (db conn)
         (db-prn "Starting merge process")
         same-client-registrant-merge-datoms
         (partition-all 1000)
         (map (partial d/transact conn))
         doall)
    (->> (db conn)
         (db-prn "Merged based on same client and registrant")
         (spy :info "Saving output")
         (do #(d/q '[:find ?being ?name
                     :in $ %
                     :with ?record
                     :where
                     [?being  :record/type :being.record/being]
                     [?record :record/represents ?being]
                     (name-of ?record ?name)]
                   %
                   rules))
         distinct
         (group-by first)
         seq
         (map (fn [[b bs]] [b (map second bs)]))
         (sort-by (comp first second))
         pprint
         with-out-str
         (spit "output/names-output.clj"))))

(defn -main [arg]
  (info (str "Running " arg " command"))
  (condp = arg
    "load"   (load-data)
    "match"  (match-data)
    "status" (print-status))
    (java.lang.System/exit 0))

(comment

  (let [dbc (db (d/connect uri))]
    (d/q '[:find ?client-being ?registrant-being
              :in $ %
              :where
              [?form       :lobbying.form/client-registrant-same true]
              [?form       :lobbying.form/client ?client]
              [?form       :lobbying.form/registrant ?registrant]
              (represents ?client ?client-being)
              (represents ?registrant ?registrant-being)]
            dbc
            rules)
    )

  )
