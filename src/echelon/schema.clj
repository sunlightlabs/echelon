(ns schema.clj)

(defn enum [key] {:db/id #db/id[:db.part/user] :db/ident key})

(defn string-prop [prop doc]
  {:db/id #db/id[:db.part/db]
   :db/ident prop
   :db/valueType :db.type/string
   :db/cardinality :db.cardinality/one
   :db/doc doc
   :db.install/_attribute :db.part/db})

(defn ref-prop [prop doc]
  {:db/id #db/id[:db.part/db]
   :db/ident prop
   :db/valueType :db.type/ref
   :db/cardinality :db.cardinality/one
   :db/doc doc
   :db.install/_attribute :db.part/db})

(defn component-prop [prop doc]
  {:db/id #db/id[:db.part/db]
   :db/ident prop
   :db/valueType :db.type/ref
   :db/cardinality :db.cardinality/one
   :db/doc doc
   :db.install/_attribute :db.part/db})

(def schema
  [(ref-prop :data/source "Where the data came from.")
   (enum :data.source/sopr-html)
   (string-prop :data/document-id "Id of a document.")

   (ref-prop :record/type "A record's type.")
   (enum :record.type/:being)
   (enum :lobbying.type/:activity)
   (enum :lobbying.type/:affiliated-organization)
   (enum :lobbying.type/:client)
   (enum :lobbying.type/:contact)
   (enum :lobbying.type/:foreign-entity)
   (enum :lobbying.type/:individual)
   (enum :lobbying.type/:lobbyist)
   (enum :lobbying.type/:registrant)

   (ref-prop :record/represents
             "Indicates that the record entity with this attribute
             represents a being. This should be the only property that
             will ever be overwritten.")

   (string-prop :lobbying.organization/name   "An organization client's name.")

   (component-prop :lobbying/main-address "Main address for reaching")
   (component-prop
    :/principal-place-of-business
    "Legal term for the primary location where a taxpayers's business
    is performed (wording taken from bit.ly/1s3ZbG7)")
   (string-prop :address/first-line  "First line ofbr an address")
   (string-prop :address/second-line "Second line for an address")
   (string-prop :address/zipcode     "Zipcode for an address") ;;TODO: ref prop
   (string-prop :address/state       "State for an address") ;;TODO: ref prop
   (string-prop :address/country     "Country for an address") ;;TODO: ref prop



   ])
