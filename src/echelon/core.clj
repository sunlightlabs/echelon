(ns echelon.core
  (:require [datomic.api :as d :refer [db q]]
            [clojure.pprint :refer [pprint]]
            [echelon.load :refer [load-database! report-jsons registration-jsons]]
            [echelon.annotate :refer [annotate]]
            [echelon.text :refer [extract-names clean]]
            [echelon.util :refer [group-by-features disjoint-lists db-prn uri]]
            [taoensso.timbre :as timbre]))
(timbre/refer-timbre)

(defn merges-for-beings [dbc [b1 & b2s :as beings]]
  (if (= 1 (count (distinct beings)))
    []
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
      data)))

(def rules
  '[[(name-of ?record ?name)
     [(ground [:lobbying.client/name
               :lobbying.foreign-entity/name
               :lobbying.registrant/name
               :lobbying.affiliated-organization/name])
      [?name-attr ...]]
     [?record ?name-attr ?name]]
    [(either-form ?form) [?form :record/type :lobbying.record/registration]]
    [(either-form ?form) [?form :record/type :lobbying.record/report]]
    [(represents ?record ?being) [?record :record/represents ?being]]])

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
       (mapcat (partial merges-for-beings dbc))
       vec))


(defn same-client-registrant-merge-datoms [dbc]
  (info "Find datoms where forms indicate the client and registrant are the same")
  (let [datoms
        (->>  (concat (registration-jsons) (report-jsons))
              (map second)
              (filter (comp :client_self :client))
              (map :document_id)
              (d/q '[:find ?client-being ?registrant-being
                     :in $ % [?document-id ...]
                     :where
                     [?form :lobbying.form/document-id ?document-id]
                     [?form :lobbying.form/client ?client]
                     [?form :lobbying.form/registrant ?registrant]
                     (represents ?client ?client-being)
                     (represents ?registrant ?registrant-being)]
                   dbc
                   rules)
              (mapcat (partial merges-for-beings dbc))
              vec)]
    (info "Datoms produced for sames merges")
    datoms))

(defn same-exact-name-merge-datoms [dbc]
  (info "Merging based on exact names")
  (let [datoms
        (->> (beings-and-names dbc)
             (group-by (comp clean second))
             (grouped-matches->datoms dbc))]
    (info "Datoms produced for exact name merges")
    datoms))

(defn same-extracted-name-merge-datoms [dbc]
  (info "Merging based on extracted names")
  (let [datoms
        (->> (beings-and-names dbc)
             (group-by-features (comp extract-names second))
             (grouped-matches->datoms dbc))]
    (info "Datoms produced for extracted names")
    datoms))

(defn print-status []
  (let [c (d/connect uri)]
    (db-prn "Printing db status" (db c))))

(defn load-data []
  (d/create-database uri)
  (let [c (d/connect uri)]
    (info "Loading Database...")
    (load-database! c)
    (info "Loaded!")
    (print-status)))


(defn execute-merge-fn [conn dbc f]
  (doseq [result (->> dbc f
                      (partition-all 1)
                      (pmap (partial d/transact conn)))]
    (try @result
         (catch Exception e
           (throw e)))))

(def match-functions
  {"same-on-form" same-client-registrant-merge-datoms
   "exact-name"   same-exact-name-merge-datoms
   "extracted-name" same-extracted-name-merge-datoms})

(defn match-data [args]
  (info "Starting matching process")
  (let [conn (d/connect uri)
        dbc (db conn)
        mfs (if (= (first args) "all")
          ["same-on-form" "exact-name" "extracted-name"]
          (map match-functions args))]

    (when (some nil? mfs)
      (throw (Throwable. (str "No match functions found for:" args))))

    (print-status)

    (doseq [mf mfs]
      (execute-merge-fn conn dbc mf)
      (print-status))

    (info "Saving output")
    (->> (d/q '[:find ?being ?name
                :in $ %
                :with ?record
                :where
                [?being  :record/type :being.record/being]
                [?record :record/represents ?being]
                (name-of ?record ?name)]
              (db conn)
              rules)
         distinct
         (group-by first)
         seq
         (map (fn [[b bs]] [b (map second bs)]))
         (sort-by (comp first second))
         pprint
         with-out-str
         (spit "output/names-output.clj"))))

(defn -main [& args]
  (info (str "Running " (first args) " command"))
  (condp = (first args)
    "annotate" (annotate)
    "load"   (load-data)
    "match"  (match-data (rest args))
    "status" (print-status))
  (java.lang.System/exit 0))
