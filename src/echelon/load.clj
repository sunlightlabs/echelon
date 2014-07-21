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

(defn registration-datoms [[f m]]
  (let [lobbyists           (:lobbyists m)
        contact-being-id    (d/tempid :db.part/user)
        client-being-id     (d/tempid :db.part/user)
        registrant-being-id (d/tempid :db.part/user)
        activity-being-id   (d/tempid :db.part/user)
        lobbyist-being-ids
        (repeatedly (count lobbyists) #(d/tempid :db.part/user))
        affliated-organization-beings-ids []
        foreign-entities-beings-ids []
        beings
        (map new-being (concat [contact-being-id client-being-id registrant-being-id
                                activity-being-id]
                               lobbyist-being-ids
                               affliated-organization-beings-ids
                               foreign-entities-beings-ids))
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
                         lobbyists)}}]
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
  (d/transact conn schema))

(defn load-database! [conn]
  (println "Schema loading...")
  (load-schema! conn)
  (println "Data loading...")
  (load-data! conn))
