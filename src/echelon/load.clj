(ns echelon.load
  (:require [datomic.api :as d :refer [db q]]
            [clojure.data.json :as json]
            [echelon.text :refer [clean]]
            [echelon.schema :refer [schema string->issue-code]]
            [echelon.util :refer [contains-nil?]]
            [me.raynes.fs :as fs]
            [clojure.pprint :refer [pprint]]
            [clj-time.format :as f]))

(def datadir "/home/zmaril/data/sopr_html/")

(defn list-registration-forms []
  (mapcat #(fs/glob (str datadir "/" % "/REG/*"))
          (range 2008 2015)))

(defn list-ld2-forms []
  (for [y (range 2004 2015) q (range 1 5)]
    (fs/glob (str datadir "LD2/" y "/Q" q "/*"))))

(defn new-being [id]
  {:db/id id
   :record/type :being.record/being
   :being/id (str (d/squuid))})

(defn parse-time [s]
  (if (= s "03/031/2008")
    (java.util.Date. "03/31/2008")
    (some->> s
             (f/parse (f/formatters :date-hour-minute-second))
             (.toDate))))

(defn temp-user []
  (d/tempid :db.part/user))

(defn parse-dec [s]
  (if (= "" s)
    (java.math.BigDecimal. "0")
    (java.math.BigDecimal. s)))

(defn registration-datoms [[f m]]
  (let [lobbyists                (:lobbyists m)
        foreign-entities         (:foreign_entities m)
        affiliated-organizations (:affiliated_organizations m)

        contact-being-id         (temp-user)
        client-being-id          (temp-user)
        registrant-being-id      (temp-user)
        activity-being-id        (temp-user)

        lobbyist-being-ids
        (repeatedly (count lobbyists) temp-user)
        affiliated-organization-beings-ids
        (repeatedly (count affiliated-organizations) temp-user)
        foreign-entity-beings-ids
        (repeatedly (count foreign-entities) temp-user)

        beings
        (map new-being (concat [contact-being-id client-being-id registrant-being-id
                                activity-being-id]
                               lobbyist-being-ids
                               affiliated-organization-beings-ids
                               foreign-entity-beings-ids))
        registration  {:db/id (d/tempid :db.part/user)
                       :record/type :lobbying.record/registration

                       :lobbying.form/source :lobbying.form/sopr-html
                       :lobbying.form/document-id (:document_id m)

                       :lobbying.form/house-id
                       (get-in m [:registrant :registrant_house_id])
                       :lobbying.form/senate-id
                       (get-in m [:registrant :registrant_senate_id])

                       :lobbying.form/signature-date
                       (-> m :datetimes :signature_date parse-time)
                       :lobbying.registration/effective-date
                       (-> m :datetimes :effective_date parse-time)

                       :lobbying.form/client
                       (let [c (:client m)]
                         {:record/type :lobbying.record/client
                          :record/represents client-being-id
                          :lobbying.client/name        (:client_name c)
                          :lobbying.client/description (:client_general_description c)

                          :lobbying.client/main-address
                          {:address/first-line (:client_address c)
                           :address/zipcode    (:client_zip c)
                           :address/city       (:client_city c)
                           :address/state      (:client_state c)
                           :address/country    (:client_country c)}

                          :lobbying.client/principal-place-of-business
                          {:address/zipcode    (:client_ppb_zip c)
                           :address/city       (:client_ppb_city c)
                           :address/state      (:client_ppb_state c)
                           :address/country    (:client_ppb_country c)}})

                       :lobbying.form/registrant
                       (let [r (:registrant m)]
                         {:record/type :lobbying.record/registrant
                          :record/represents registrant-being-id
                          :lobbying.registrant/name        (:registrant_name r)
                          :lobbying.registrant/description (:registrant_general_description r)

                          :lobbying.registrant/main-address
                          {:address/first-line  (:registrant_address_one r)
                           :address/second-line (:registrant_address_two r)
                           :address/zipcode     (:registrant_zip r)
                           :address/city        (:registrant_city r)
                           :address/state       (:registrant_state r)
                           :address/country     (:registrant_country r)}

                          :lobbying.registrant/principal-place-of-business
                          {:address/zipcode    (:registrant_ppb_zip r)
                           :address/city       (:registrant_ppb_city r)
                           :address/state      (:registrant_ppb_state r)
                           :address/country    (:registrant_ppb_country r)}})

                       :lobbying.form/contact
                       (let [r (:registrant m)]
                         {:record/type :lobbying.record/contact
                          :record/represents contact-being-id
                          :lobbying.contact/name  (:registrant_contact r)
                          :lobbying.contact/phone (:registrant_phone r )
                          :lobbying.contact/email (:registrant_email r)})

                       :lobbying.registration/activity
                       {:record/type :lobbying.record/activity
                        :record/represents activity-being-id
                        :lobbying.activity/general-details
                        (:lobbying_issues_detail  m)
                        :lobbying.activity/issue-codes
                        (->> m :lobbying_issues
                             (map (comp string->issue-code :issue_code)))
                        :lobbying.activity/lobbyists
                        (map-indexed
                         #(do {:record/type :lobbying.record/lobbyist
                               :record/represents (nth lobbyist-being-ids %1)
                               :data/position %1
                               :lobbying.lobbyist/first-name
                               (:lobbyist_first_name %2)
                               :lobbying.lobbyist/last-name
                               (:lobbyist_last_name %2)
                               :lobbying.lobbyist/suffix
                               (:lobbyist_suffix %2)
                               :lobbying.lobbyist/covered-official-position
                               (:lobbyist_covered_official_position %2)})
                         lobbyists)}

                       :lobbying.registration/foreign-entities
                       (map-indexed
                        #(do
                           {:record/type :lobbying.record/foreign-entity
                            :record/represents
                            (nth foreign-entity-beings-ids %1)
                            :data/position %1
                            :lobbying.foreign-entity/name
                            (:foreign_entity_name %2)

                            :lobbying.foreign-entity/amount
                            (-> %2
                                :foreign_entity_amount
                                parse-dec)

                            :lobbying.foreign-entity/ownership-percentage
                            (-> %2
                                :foreign_entity_ownership_percentage
                                parse-dec)

                            :lobbying.foreign-entity/main-address
                            {:address/first-line
                             (:foreign_entity_address %2)
                             :address/city
                             (:foreign_entity_city %2)
                             :address/state
                             (:foreign_entity_state %2)
                             :address/country
                             (:foreign_entity_country %2)}

                            :lobbying.foreign-entity/principal-place-of-business
                            {:address/country
                             (:foreign_entity_ppb_country %2)
                             :address/state
                             (:foreign_entity_ppb_state %2)}})
                        foreign-entities)

                       :lobbying.registration/affiliated-organizations
                       (map-indexed
                        #(do {:record/type :lobbying.record/affiliated-organization
                              :record/represents
                              (nth affiliated-organization-beings-ids %1)
                              :data/position %1
                              :lobbying.affiliated-organization/name
                              (:affiliated_organization_name %2)

                              :lobbying.affiliated-organization/main-address
                              {:address/first-line
                               (:affiliated_organization_address %2)
                               :address/city
                               (:affiliated_organization_city %2)
                               :address/state
                               (:affiliated_organization_state %2)
                               :address/country
                               (:affiliated_organization_country %2)}

                              :lobbying.affiliated-organization/principal-place-of-business
                              {:address/country
                               (:affiliated_organization_ppb_country %2)
                               :address/state
                               (:affiliated_organization_ppb_state %2)}})
                        affiliated-organizations)
                       }]
    (conj (vec beings)
          registration)))

(defn load-data! [conn]
  (doseq [datoms
          (map (comp
                registration-datoms
                (juxt identity (comp #(json/read-str % :key-fn keyword) slurp)))
               (list-registration-forms))
          :when (not (contains-nil? datoms))]
    @(d/transact conn datoms)))

(defn load-schema! [conn]
  @(d/transact conn schema))

(defn load-database! [conn]
  (println "Schema loading...")
  (load-schema! conn)
  (println "Data loading...")
  (load-data! conn))

(comment
  (->> (list-registration-forms)
       (map (comp #(json/read-str % :key-fn keyword)
                  slurp))
       (filter (comp not empty? :affiliated_organizations) )
       first
       (vector "")
       registration-datoms
       ))

(comment
  (doseq [form (list-registration-forms)
          :when (-> form
                    slurp
                    (json/read-str :key-fn keyword)
                    (#(registration-datoms ["" %] ))
                    contains-nil?)]
    (println form)))
