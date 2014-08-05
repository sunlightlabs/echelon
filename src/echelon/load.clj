(ns echelon.load
  (:require [datomic.api :as d :refer [db q]]
            [clojure.data.json :as json]
            [echelon.text :refer [clean]]
            [echelon.schema :refer [schema string->issue-code]]
            [echelon.util :refer [contains-nil? transpose db-prn]]
            [me.raynes.fs :as fs]
            [clojure.pprint :refer [pprint]]
            [clj-time.format :as f]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre]))
(timbre/refer-timbre)

(def datadir (or (env :data-location)
                 (System/getenv "DATA_LOCATION")))

(defn registration-jsons []
  (map (juxt (memfn getPath)
             (comp #(json/read-str % :key-fn keyword) slurp))
       (mapcat #(fs/glob (str datadir "/" % "/REG/*"))
               (range 2008 2015))))

(defn report-jsons []
  (map (juxt (memfn getPath)
             (comp #(json/read-str % :key-fn keyword) slurp))
       (apply concat (for [y (range 2008 2015) q (range 1 5)]
                       (fs/glob (str datadir "/" y "/Q" q "/*"))))))

(defn new-being [id]
  {:db/id id
   :record/type :being.record/being
   :being/id (str (d/squuid))})

(defn parse-time [s]
  (condp = s
    "03/031/2008"   (java.util.Date. "03/31/2008")
    "March 31, 2011"   (java.util.Date. "03/31/2011")
    "May 31, 2011"   (java.util.Date. "05/31/2011")
    (some->> s
             (f/parse (f/formatters :date-hour-minute-second))
             (.toDate))))

(defn temp-user []
  (d/tempid :db.part/user))

(defn parse-dec [s]
  (if (= "" s)
    (java.math.BigDecimal. "0")
    (java.math.BigDecimal. s)))

(defn structure-lobbyist
  "Provide structure to a lobbyist within the db, pulling from the
  given map."
  [i m lid]
  (let [basic  {:record/type :lobbying.record/lobbyist
                :record/represents lid
                :data/position i
                :lobbying.lobbyist/first-name
                (:lobbyist_first_name m)
                :lobbying.lobbyist/last-name
                (:lobbyist_last_name m)
                :lobbying.lobbyist/suffix
                (:lobbyist_suffix m)
                :lobbying.lobbyist/covered-official-position
                (or (:lobbyist_covered_position m)
                    (:lobbyist_covered_official_position m))}]
    (if (nil? (:lobbyist_is_new m))
      basic
      (assoc basic :lobbying.lobbyist/is-new (:lobbyist_is_new m)))))

(defn structure-affiliated-organzation
  "Provide structure to an affiliated organization within the db,
  pulling from the given map."
  [i m aid]
  {:record/type :lobbying.record/affiliated-organization
   :record/represents aid
   :data/position i
   :lobbying.affiliated-organization/name
   (:affiliated_organization_name m)

   :lobbying.affiliated-organization/main-address
   {:address/first-line
    (or (:affiliated_organization_address m) "")
    :address/city
    (or (:affiliated_organization_city m) "")
    :address/state
    (or (:affiliated_organization_state m) "")
    :address/country
    (or (:affiliated_organization_country m) "")}

   :lobbying.affiliated-organization/principal-place-of-business
   {:address/country
    (or (:affiliated_organization_ppb_country m) "")
    :address/state
    (or (:affiliated_organization_ppb_state m) "")}})

(defn structure-foreign-entity
  "Provide structure to a foreign entity within the db,
  pulling from the given map."
  [i m fid]
  {:record/type :lobbying.record/foreign-entity
   :record/represents fid
   :data/position i
   :lobbying.foreign-entity/name
   (:foreign_entity_name m)

   ;;TODO: What to do with nils for amount/incomes?
   :lobbying.foreign-entity/amount
   (or (some-> m :foreign_entity_amount parse-dec)
       (parse-dec "0"))

   :lobbying.foreign-entity/ownership-percentage
   (or (some-> m :foreign_entity_ownership_percentage parse-dec)
       (parse-dec "0"))

   :lobbying.foreign-entity/main-address
   {:address/first-line
    (or (m :foreign_entity_address) "")
    :address/city
    (or (m :foreign_entity_city) "")
    :address/state
    (or (m :foreign_entity_state) "")
    :address/country
    (or (m :foreign_entity_country) "")}

   :lobbying.foreign-entity/principal-place-of-business
   {:address/country
    (or (m :foreign_entity_ppb_country) "")
    :address/state
    (or (m :foreign_entity_ppb_state) "")}})

(defn structure-basic-form [f m]
  (let [contact-being-id (temp-user)
        client-being-id  (temp-user)
        registrant-being-id (temp-user)
        basic-form
        {:db/id (temp-user)
         :lobbying.form/source :lobbying.form/sopr-html
         :lobbying.form/document-id (:document_id m)
         :lobbying.form/filepath f
         :lobbying.form/client-registrant-same
         (-> m :client :client_self)

         :lobbying.form/contact
         (let [r (:registrant m)]
           {:record/type :lobbying.record/contact
            :record/represents  contact-being-id
            :lobbying.contact/name  (or (:registrant_contact r)
                                        (:registrant_contact_name r))
            :lobbying.contact/phone (or (:registrant_phone r )
                                        (:registrant_contact_phone r ))
            :lobbying.contact/email (or (:registrant_email r)
                                        (:registrant_contact_email r))})

         :lobbying.form/registrant
         (let [r (:registrant m)
               basic-registrant
               {:record/type :lobbying.record/registrant
                :record/represents
                registrant-being-id

                :lobbying.registrant/main-address
                {:address/first-line  (:registrant_address_one r)
                 :address/second-line (:registrant_address_two r)
                 :address/city        (:registrant_city r)
                 :address/state       (:registrant_state r)
                 :address/zipcode     (:registrant_zip r)
                 :address/country     (:registrant_country r)}

                :lobbying.registrant/principal-place-of-business
                {:address/city       (:registrant_ppb_city r)
                 :address/state      (:registrant_ppb_state r)
                 :address/zipcode    (:registrant_ppb_zip r)
                 :address/country    (:registrant_ppb_country r)


                 :lobbying.registrant/self-employed-individual
                 (r :self_employed_individual)}}
               registrant
               (if (r :registrant_name)
                 (assoc basic-registrant
                   :lobbying.registrant/name (:registrant_name r))
                 (if (r :self_employed_individual)
                   (merge basic-registrant
                          {:lobbying.registrant/prefix
                           (:registrant_individual_prefix r)

                           :lobbying.registrant/first-name
                           (:registrant_individual_firstname r)

                           :lobbying.registrant/last-name
                           (:registrant_individual_lastname r)})
                   (assoc basic-registrant
                     :lobbying.registrant/organization-name
                     (:registrant_org_name r))))]
           (if-let [d (:registrant_general_description r)]
             (assoc registrant :lobbying.registrant/description  d)
             registrant))

         ;;covers both reports and registrations
         :lobbying.form/client
         (let [c (:client m)
               u (:registration_update m)
               client
               {:record/type :lobbying.record/client
                :record/represents  client-being-id
                :lobbying.client/name (:client_name c)

                :lobbying.client/description
                (or (:client_general_description c)
                    (:client_new_general_description u))

                :lobbying.client/main-address
                {:address/first-line
                 (or (:client_address c)
                     (:client_new_address u))
                 :address/zipcode
                 (or (:client_zip c)
                     (:client_new_zip u))
                 :address/city
                 (or (:client_city c)
                     (:client_new_city u))
                 :address/state
                 (or (:client_state c)
                     (:client_new_state u))
                 :address/country
                 (or (:client_country c)
                     (:client_new_country u))}

                :lobbying.client/principal-place-of-business
                {:address/zipcode
                 (or (:client_ppb_zip c)
                     (:client_new_ppb_zip u))
                 :address/city
                 (or (:client_ppb_city c)
                     (:client_new_ppb_city u))
                 :address/state
                 (or (:client_ppb_state c)
                     (:client_new_ppb_state u))
                 :address/country
                 (or (:client_ppb_country c)
                     (:client_new_ppb_country u))}}]
           (if-let [s (:client_state_or_local_government c)]
             (assoc client :lobbying.client/state-or-local-government s)
             client))}]
    (if-let [t (-> m :datetimes :signature_date parse-time)]
      (assoc basic-form :lobbying.form/signature-date t)
      basic-form)))

(defn registration-datoms [f m]
  (let [basic (structure-basic-form f m)

        activity-being-id
        (temp-user)

        lobbyists
        (:lobbyists m)

        lobbyists-ids
        (repeatedly (count lobbyists) temp-user)


        affiliated-organizations
        (:affiliated_organizations m)

        affiliated-organizations-ids
        (repeatedly (count affiliated-organizations) temp-user)


        foreign-entities
        (:foreign_entities m)

        foreign-entities-ids
        (repeatedly (count foreign-entities) temp-user)


        being-ids
        (concat [(-> basic :lobbying.form/client :record/represents)
                 (-> basic :lobbying.form/contact :record/represents)
                 (-> basic :lobbying.form/registrant :record/represents)
                 activity-being-id]
                lobbyists-ids
                affiliated-organizations-ids
                foreign-entities-ids)

        beings
        (map new-being (distinct being-ids))

        registration
        {:record/type :lobbying.record/registration
         :lobbying.registration/house-id
         (get-in m [:registrant :registrant_house_id])
         :lobbying.registration/senate-id
         (get-in m [:registrant :registrant_senate_id])

         :lobbying.form/amendment
         (-> m :registration_type :amendment)

         :lobbying.registration/new-registrant
         (-> m :registration_type :new_registrant)
         :lobbying.registration/new-client-for-existing-registrant
         (-> m :registration_type :new_client_for_existing_registrant)


         :lobbying.registration/effective-date
         (-> m :datetimes :effective_date parse-time)

         :lobbying.registration/activity
         {:record/type :lobbying.record/activity
          :record/represents activity-being-id
          :lobbying.activity/general-details
          (:lobbying_issues_detail m)
          :lobbying.activity/issue-codes
          (map (comp string->issue-code :issue_code)
               (:lobbying_issue m))
          :lobbying.activity/lobbyists
          (map structure-lobbyist
               (range (count lobbyists))
               lobbyists
               lobbyists-ids)}

         :lobbying.registration/foreign-entities
         (map structure-foreign-entity
              (range (count foreign-entities))
              foreign-entities
              foreign-entities-ids)

         :lobbying.registration/affiliated-organizations
         (map structure-affiliated-organzation
              (range (count affiliated-organizations))
              affiliated-organizations
              affiliated-organizations-ids)}]
    (conj (vec beings)
          (merge basic registration))))

(defn structure-report-activity
  [i {:keys [general_issue_area specific_issues houses_and_agencies
             foreign_entity_interest foreign_entity_interest_none
             lobbyists]}
   aid]
  (let [lobbyists-ids (repeatedly (count lobbyists) temp-user )]
    [lobbyists-ids
     {:record/type :lobbying.record/activity
      :data/position i
      :record/represents aid
      :lobbying.activity/specific-issues specific_issues
      :lobbying.activity/houses-and-agencies houses_and_agencies
      :lobbying.activity/no-foreign-entity-interest foreign_entity_interest_none
      :lobbying.activity/foreign-entity-interest foreign_entity_interest
      :lobbying.activity/issue-code (string->issue-code  general_issue_area)
      :lobbying.activity/lobbyists
      (map structure-lobbyist
           (range (count lobbyists))
           lobbyists
           lobbyists-ids)}]))

(defn report-datoms [f m]
  (let [basic (structure-basic-form f m)

        activities (:lobbying_activities m)
        activities-ids
        (repeatedly (count activities) temp-user)

        [lobbyists-ids,structured-activities]
        (if (empty? activities)
          [[] []]
          (->> (map structure-report-activity
                    (range (count activities))
                    activities
                    activities-ids)
               transpose))
        lobbyists-ids (apply concat lobbyists-ids)


        removed-affiliated-organizations
        (-> m :registration_update :removed_affiliated_organizations)
        removed-affiliated-organizations-ids
        (repeatedly (count removed-affiliated-organizations) temp-user)

        added-affiliated-organizations
        (-> m :registration_update :added_affiliated_organizations)
        added-affiliated-organizations-ids
        (repeatedly (count added-affiliated-organizations) temp-user)

        removed-foreign-entities
        (-> m :registration_update :removed_foreign_entities)
        removed-foreign-entities-ids
        (repeatedly (count removed-foreign-entities) temp-user)

        added-foreign-entities
        (-> m :registration_update :added_foreign_entities)
        added-foreign-entities-ids
        (repeatedly (count added-foreign-entities) temp-user)

        removed-lobbyists
        (-> m :registration_update :removed-lobbyists)
        removed-lobbyist-ids
        (repeatedly (count removed-lobbyists) temp-user)

        added-lobbyists
        (-> m :registration_update :added-lobbyists)
        added-lobbyist-ids
        (repeatedly (count added-lobbyists) temp-user)

        being-ids
        (concat [(-> basic :lobbying.form/client :record/represents)
                 (-> basic :lobbying.form/contact :record/represents)
                 (-> basic :lobbying.form/registrant :record/represents)]
                activities-ids
                lobbyists-ids
                removed-affiliated-organizations-ids
                added-affiliated-organizations-ids
                removed-foreign-entities-ids
                added-foreign-entities-ids)

        beings
        (map new-being (distinct being-ids))

        report  {:record/type :lobbying.record/report

                 :lobbying.report/year
                 (java.lang.Long. (:report_year m))
                 :lobbying.report/quarter
                 (java.lang.Long. (str (nth (:report_quarter m) 1)))

                 :lobbying.form/amendment
                 (-> m :report_is_amendment)

                 :lobbying.report/no-activity (:report_no_activity m)

                 :lobbying.report/house-id
                 (:client_registrant_house_id m)
                 :lobbying.report/senate-id
                 (:client_registrant_senate_id m)

                 :lobbying.report/activities
                 structured-activities

                 :lobbying.report/removed-lobbying-issues
                 (map (comp string->issue-code :issue_code)
                      (:lobbying_issuse m))

                 :lobbying.report/added-affiliated-organizations
                 (map structure-affiliated-organzation
                      (range (count added-affiliated-organizations))
                      added-affiliated-organizations
                      added-affiliated-organizations-ids)

                 :lobbying.report/removed-affiliated-organizations
                 (map structure-affiliated-organzation
                      (range (count added-affiliated-organizations))
                      removed-affiliated-organizations
                      removed-affiliated-organizations-ids)

                 :lobbying.report/added-foreign-entities
                 (map structure-foreign-entity
                      (range (count added-foreign-entities))
                      added-foreign-entities
                      added-foreign-entities-ids)

                 :lobbying.report/removed-foreign-entities
                 (map structure-foreign-entity
                      (range (count removed-foreign-entities))
                      removed-foreign-entities
                      removed-foreign-entities-ids)}

        termination-fields
        (if (m :report_is_termination)
          {:lobbying.report/terminated true
           :lobbying.report/termination-date
           (-> m :datetimes :termination_date parse-time)}
          {:lobbying.report/terminated false})

        ;; TODO: add in  amounts for expenses and incomes
        income-fields
        (if (m :income_less_than_five_thousand)
          {:lobbying.report/income-less-than-five-thousand true}
          {:lobbying.report/income-less-than-five-thousand false
           :lobbying.report/income
           (parse-dec (if-let [i (:income_amount m)] i "0"))})

        expense-fields
        (if (m :expense_less_than_five_thousand)
          {:lobbying.report/expense-less-than-five-thousand true}
          {:lobbying.report/expense-less-than-five-thousand false
           :lobbying.report/expense
           (parse-dec (if-let [i (:expense_amount m)] i "0"))})

        reporting-method
        (if-let [mt (m :expense_reporting_method)]
          {:lobbying.report/reporting-method (keyword (str "lobbying.reporting-method/" mt))}
          {})]
    (conj (vec beings)
          (merge basic report termination-fields income-fields expense-fields
                 reporting-method))))

(defn load-data! [conn]
  (info "Loading registrations")
  (doseq [result (->> (registration-jsons)
                      (map (partial apply registration-datoms))
                      (filter (complement contains-nil?))
                      (pmap (partial d/transact-async conn)))]
    (try @result
         (catch Exception e
           (pprint result)
           (throw e))))
  (info "Loading reports")
  (doseq [result (->> (report-jsons)
                      (filter #(and (-> % second :client :client_name nil? not)
                                    (-> % second :report_quarter nil? not)))
                      (map (partial apply report-datoms))
                      (filter (complement contains-nil?))
                      (pmap (partial d/transact-async conn)))]
    (try @result
         (catch Exception e
           (pprint result)
           (pprint e)))))

(defn load-schema! [conn]
  @(d/transact conn schema))

(defn load-database! [conn]
  (info "Schema loading...")
  (load-schema! conn)
  (info "Data loading...")
  (load-data! conn))
