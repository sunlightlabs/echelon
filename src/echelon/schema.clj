(ns echelon.schema
  (:require [echelon.ali :refer [alis-attributes]]))

;;Helper functions for datomic. We're not doing too many fancy things
;;here with datomic and the main struggle has just been understanding
;;the layout of the data, so we've abstracted away the creation of the
;;attribute maps for datomic.

(defn- enum [key] {:db/id #db/id[:db.part/user] :db/ident key})

(defn- proto-prop [prop doc]
  {:db/id #db/id [:db.part/db]
   :db/ident prop
   :db/doc doc
   :db.install/_attribute :db.part/db})

(defn- prop-fn [m]
  (fn [prop doc]
    (-> (proto-prop prop doc)
        (merge m))))

(def string-prop
  (prop-fn {:db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}))

(def instant-prop
  (prop-fn {:db/valueType :db.type/instant
            :db/cardinality :db.cardinality/one}))

(def ref-prop
  (prop-fn {:db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one}))

(def ref-props
  (prop-fn {:db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many}))

(def component-prop
  (prop-fn {:db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one
            :db/isComponent true}))

(def component-props
  (prop-fn {:db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many
            :db/isComponent true}))

;;Various grouping of attributes from the schema

(def being-framework-attributes
  [(enum            :being.records/being)
   (string-prop     :being/id "A uuid for the being")
   (ref-prop        :records/type "A record's type.")
   (ref-prop        :record/represents
                    "Indicates that the record entity with this
                    attribute represents a being. This should be the
                    only property that will ever be overwritten.")])

(def address-attributes
  [(string-prop    :address/first-line  "First line for an address")
   (string-prop    :address/second-line "Second line for an address")
   (string-prop    :address/zipcode     "Zipcode for an address")
   (string-prop    :address/state       "State for an address")
   (string-prop    :address/country     "Country for an address")])

(def client-attributes
  [(enum           :lobbying.records/client)
   (string-prop    :lobbying.client/name         "Client name.")
   (string-prop    :lobbying.client/description  "Client description.")
   (component-prop :lobbying.client/main-address "Main address for the client.")
   (component-prop :lobbying.client/principal-place-of-business
                   "Primary location where a taxpayers's business is
                   performed (bit.ly/1s3ZbG7)")])

(def registrant-attributes
  [(enum            :lobbying.records/registrant)
   (string-prop     :lobbying.registrant/name         "Registrant name.")
   (string-prop     :lobbying.registrant/description  "Registrant description.")
   (component-prop  :lobbying.registrant/main-address
                    "Main address for reaching the registrant.")
   (component-prop  :lobbying.registrant/principal-place-of-business
                    "Primary location where a taxpayers's business is
                    performed (bit.ly/1s3ZbG7)")])

(def contact-attributes
  [(enum            :lobbying.records/contact)
   (string-prop     :lobbying.contact/name-prefix  "Contact name prefix.")
   (string-prop     :lobbying.contact/name  "Contact name.")
   (string-prop     :lobbying.contact/phone "Contact phone.")
   (string-prop     :lobbying.contact/email "Contact email.")])

(def lobbyist-attributes
  [(enum            :lobbying.records/lobbyist)
   (string-prop     :lobbying.lobbyist/first-name "First name of lobbyist.")
   (string-prop     :lobbying.lobbyist/last-name  "Last name of lobbyist.")
   (string-prop     :lobbying.lobbyist/suffix     "Suffix of lobbyist.")
   (string-prop     :lobbying.lobbyist/covered-official-position
                    "No idea, this is often blank.")])

(def activity-attributes
  [(enum            :lobbying.records/activity)
   (string-prop     :lobbying.activity/issues-details
                    "Details about the lobbying generally done by the
                    registrant for the client on various issues.")
   (component-props :lobbying.activity/issue-codes
                    "The issue codes generally associated with the activity.")
   (component-props :lobbying.activity/lobbyists
                    "The foreign entities for the activity.")])

(def transaction-annotations-attributes
  )

;;(enum               :lobbying.records/affiliated-organization)
;;(enum            :lobbying.records/foreign-entity)
;;(enum            :lobbying.records/individual)

(def common-form-attributes
  [;;Common parts of each form
   (string-prop     :lobbying.form/house-id
                    "Id given out the clerk of the house of
                     representatives identiying a client and registrant.")
   (string-prop     :lobbying.form/senate-id
                    "Id given out the senate identiying a client and
                     registrant.")
   (instant-prop    :lobbying.form/signature-date
                    "We have no idea what this means.")
   (component-prop  :lobbying.form/client
                   "The client for the form.")
   (component-prop  :lobbying.form/registrant
                   "The registrant for the form.")
   (component-prop  :lobbying.form/contact
                   "The contact for the form.")
   (component-prop  :lobbying.form/individual
                    "Potentially used, if the registrant is an individual for the form.")
   (ref-prop        :lobbying.form/source "Where the data came from.")
   (enum            :lobbying.form/sopr-html)
   (string-prop     :lobbying.formd/document-id
                    "Id of a document (provided by sopr).")])

(def registration-form-attributes
  [(enum            :lobbying.records/registration)
   (instant-prop    :lobbying.engagement/effective-date
                    "No idea what this one actually means.")
   (component-props :lobbying.engagement/affiliated-organizations
                    "The affiliated organizations for the engagement.")
   (component-props :lobbying.engagement/foreign-entity
                    "The foreign entities for the engagement.")])

(def report-form-attributes
  [(enum            :lobbying.records/report)])

(def schema
  (concat alis-attributes
          address-attributes
          being-framework-attributes
          client-attributes
          registrant-attributes
          contact-attributes
          lobbyist-attributes
          activity-attributes
          transaction-annotations-attributes
          common-form-attributes
          registration-form-attributes
          report-form-attributes))
