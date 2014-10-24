(ns echelon.annotate
  (:require [datomic.api :as d :refer [db q]]
            [clojure.pprint :refer [pprint]]
            [echelon.load :refer [load-database! report-jsons registration-jsons registration-datoms]]
            [taoensso.timbre :as timbre]
            [echelon.util :refer [uri finder ident-finder]]
            [clojure.data.json :as json]
            [clojure.string :as s]))

(defn annotate-report [dbc {:keys [document_id] :as m}]
  (let [[client-id registrant-id]
        (first
         (d/q '[:find ?client-id ?registrant-id
                :in $ ?document-id
                :where
                [?document         :lobbying.form/document-id ?document-id]

                [?document         :lobbying.form/client ?client]
                [?client           :record/represents ?client-being]
                [?client-being     :being/id ?client-id]

                [?document         :lobbying.form/registrant ?registrant]
                [?registrant       :record/represents ?registrant-being]
                [?registrant-being :being/id ?registrant-id]]
              dbc
              document_id))]
    (-> m
        (assoc-in [:client :being_id] client-id)
        (assoc-in [:registrant :being_id] registrant-id))))

(defn annotate-registration [dbc {:keys [document_id] :as m}]
  (let [[client-id registrant-id]
        (first
         (d/q '[:find ?client-id ?registrant-id
                :in $ ?document-id
                :where
                [?document         :lobbying.form/document-id ?document-id]

                [?document         :lobbying.form/client ?client]
                [?client           :record/represents ?client-being]
                [?client-being     :being/id ?client-id]

                [?document         :lobbying.form/registrant ?registrant]
                [?registrant       :record/represents ?registrant-being]
                [?registrant-being :being/id ?registrant-id]]
              dbc
              document_id))
        affiliated-organizations
        (->>
         (d/q '[:find ?org-id ?pos
                :in $ ?document-id
                :where
                [?document  :lobbying.form/document-id ?document-id]
                [?document
                 :lobbying.registration/affiliated-organizations ?org]
                [?org       :data/position ?pos]
                [?org       :record/represents ?org-being]
                [?org-being :being/id ?org-id]]
              dbc
              document_id)
         (sort-by second)
         (map first))]

    (-> m
        (assoc-in [:client :being_id] client-id)
        (assoc-in [:registrant :being_id] registrant-id)
        (update-in
         [:affiliated_organizations]
         (fn [orgs]
           (map #(assoc %1 :being_id %2) orgs affiliated-organizations))))))

(defn write-report  [dbc [f m]]
  (spit
   (s/replace f "sopr_html" "annotated")
   (json/write-str (annotate-report dbc m))))

(defn annotate []
  (->> (report-jsons)
       (map (partial write-report (db (d/connect uri)) ))
       doall
       count))

;; (let [dbc (db (d/connect uri))]
;;   (->> (registration-jsons)
;;        (filter (comp not empty? :affiliated_organizations second))
;;        (map (partial apply registration-datoms))
;;        (take 1)))

;; (let [dbc (db (d/connect uri))]
;;   (->> (registration-jsons)
;;        ;(filter (comp not empty? :affiliated_organizations second))
;;        (map second)
;;        (map (partial annotate-registration dbc))

;;        (take 10)
;;        ))


;; (let [dbc (db (d/connect uri))]
;;   (->>
;;    (d/q '[:find ?client ?client-being ?client-id
;;           ?registrant
;;           :in $ ?document-id
;;           :where
;;           [?document         :lobbying.form/document-id ?document-id]

;;           [?document         :lobbying.form/client ?client]
;;           [?client           :record/represents ?client-being]
;;           [?client-being     :being/id ?client-id]

;;           [?document         :lobbying.form/registrant ?registrant]
;;         ;  [?registrant       :record/represents ?registrant-being]
;;           ;; [?registrant-being :being/id ?registrant-id]
;;           ]
;;         dbc
;;         "015c22da-368e-4790-aa2e-ebf553fc4804")))

;; (let [dbc (db (d/connect uri))]
;;   (d/touch (d/entity dbc 17592186045731)))

;; (let [dbc (db (d/connect uri))]
;;   (annotate-registration dbc "000a4697-9a2d-4fe2-940a-97b7e2e987f3"))

;; (let [dbc (db (d/connect uri))]
;;   (->>
;;    (d/q
;;     '[:find ?v ?tx ?added
;;       :in $ ?e
;;       :where
;;       [?e :record/represents ?v ?tx ?added]]
;;     (d/history dbc)
;;     17592186045731)
;;    seq
;;    (sort-by second)

;;    ))

;; (let [dbc (db (d/connect uri))]
;;   (->>
;;    (d/q '[:find ?e ?a ?v ?added
;;           :in $ ?tx
;;           :where
;;           [?e ?a ?v ?tx ?added]
;;           ]
;;         dbc
;;         13194155463162)))

;; (let [dbc (db (d/connect uri))]
;;   (->> (d/q '[:find ?e ?a ?v ?op
;;              :in ?log ?t1
;;              :where
;;              [(tx-ids ?log ?t1 (inc ?t1)) [?tx]]
;;              [(tx-data ?log ?tx) [[?e ?a ?v _ ?op]]]
;;              ]
;;            (d/log (d/connect uri))
;;            13194155463162)
;;       seqpas
;;       (map #(update-in % [1] (partial ident-finder dbc)))))
