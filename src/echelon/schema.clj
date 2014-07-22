(ns echelon.schema
  (:require [datomic.api :refer [tempid]]))

;; Lobbying codes taken from http://1.usa.gov/1lm74is
(def issue-codes
  {"ACC" "Accounting"
   "ADV" "Advertising"
   "AER" "Aerospace"
   "AGR" "Agriculture"
   "ALC" "Alcohol & Drug Abuse"
   "ANI" "Animals"
   "APP" "Apparel/Clothing Industry/Textiles"
   "ART" "Arts/Entertainment"
   "AUT" "Automotive Industry"
   "AVI" "Aviation/Aircraft/Airlines"
   "BAN" "Banking"
   "BEV" "Beverage Industry"
   "BNK" "Bankruptcy"
   "BUD" "Budget/Appropriations"
   "CAW" "Clean Air & Water (Quality)"
   "CDT" "Commodities (Big Ticket)"
   "CHM" "Chemicals/Chemical Industry"
   "CIV" "Civil Rights/Civil Liberties"
   "COM" "Communications/Broadcasting/Radio/TV"
   "CON" "Constitution"
   "CPI" "Computer Industry"
   "CPT" "Copyright/Patent/Trademark"
   "CSP" "Consumer Issues/Safety/Protection"
   "DEF" "Defense"
   "DIS" "Disaster Planning/Emergencies"
   "DOC" "District of Columbia"
   "ECN" "Economics/Economic Development"
   "EDU" "Education"
   "ENG" "Energy/Nuclear"
   "ENV" "Environmental/Superfund"
   "FAM" "Family Issues/Abortion/Adoption"
   "FIN" "Financial Institutions/Investments/Securities"
   "FIR" "Firearms/Guns/Ammunition"
   "FOO" "Food Industry (Safety, Labeling, etc.)"
   "FOR" "Foreign Relations"
   "FUE" "Fuel/Gas/Oil"
   "GAM" "Gaming/Gambling/Casino"
   "GOV" "Government Issues"
   "HCR" "Health Issues"
   "HOM" "Homeland Security"
   "HOU" "Housing"
   "IMM" "Immigration"
   "IND" "Indian/Native American Affairs"
   "INS" "Insurance"
   "INT" "Intelligence and Surveillance"
   "LAW" "Law Enforcement/Crime/Criminal Justice"
   "LBR" "Labor Issues/Antitrust/Workplace"
   "MAN" "Manufacturing"
   "MAR" "Marine/Maritime/Boating/Fisheries"
   "MED" "Medical/Disease Research/Clinical Labs"
   "MIA" "Media (Information/Publishing)"
   "MMM" "Medicare/Medicaid"
   "MON" "Minting/Money/Gold Standard"
   "NAT" "Natural Resources"
   "PHA" "Pharmacy"
   "POS" "Postal"
   "REL" "Religion"
   "RES" "Real Estate/Land Use/Conservation"
   "RET" "Retirement"
   "ROD" "Roads/Highway"
   "RRR" "Railroads"
   "SCI" "Science/Technology"
   "SMB" "Small Business"
   "SPO" "Sports/Athletics"
   "TAR" "Miscellaneous Tariff Bills"
   "TAX" "Taxation/Internal Revenue Code"
   "TEC" "Telecommunications"
   "TOB" "Tobacco"
   "TOR" "Torts"
   "TOU" "Travel/Tourism"
   "TRA" "Transportation"
   "TRD" "Trade (Domestic & Foreign)"
   "TRU" "Trucking/Shipping"
   "UNM" "Unemployment"
   "URB" "Urban Development/Municipalities"
   "UTI" "Utilities"
   "VET" "Veterans"
   "WAS" "Waste (hazardous/solid/interstate/nuclear)"
   "WEL" "Welfare"})

(defn string->issue-code [code]
  (keyword (str "lobbying.issue-code/" code)))

(def issue-code-attributes
  (for [[code description] issue-codes]
    {:db/id (tempid :db.part/user)
     :db/ident (keyword (str "lobbying.issue-code/" code))
     :db/doc (str "Code for activities relating to \"" description "\".")}))

;;Helper functions for datomic. We're not doing too many fancy things
;;here with datomic and the main struggle has just been understanding
;;the layout of the data, so we've abstracted away the creation of the
;;attribute maps for datomic.

(defn- enum [key] {:db/id (tempid :db.part/user) :db/ident key})

(defn- proto-prop [prop doc]
  {:db/id (tempid :db.part/db)
   :db/ident prop
   :db/doc doc
   :db.install/_attribute :db.part/db})

(defn- prop-fn [m]
  (fn [prop doc]
    (-> (proto-prop prop doc)
        (merge m))))

(def bool-prop
  (prop-fn {:db/valueType :db.type/boolean
            :db/cardinality :db.cardinality/one}))

(def long-prop
  (prop-fn {:db/valueType :db.type/long
            :db/cardinality :db.cardinality/one}))

(def double-prop
  (prop-fn {:db/valueType :db.type/double
            :db/cardinality :db.cardinality/one}))

(def float-prop
  (prop-fn {:db/valueType :db.type/float
            :db/cardinality :db.cardinality/one}))

(def dec-prop
  (prop-fn {:db/valueType :db.type/bigdec
            :db/cardinality :db.cardinality/one}))

(def string-prop
  (prop-fn {:db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}))

(def fulltext-prop
  (prop-fn {:db/valueType :db.type/string
            :db/cardinality :db.cardinality/one
            :db/fulltext true}))

(def indexed-string-prop
  (prop-fn {:db/valueType :db.type/string
            :db/cardinality :db.cardinality/one
            :db/index true}))

(def unique-string-prop
  (prop-fn {:db/valueType :db.type/string
            :db/cardinality :db.cardinality/one
            :db/index true
            :db/unique :db.unique/identity}))

(def indexed-prop
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

(def data-attributes
  [(long-prop       :data/position
              "Remembering the order in which we received the given data.")])

(def being-framework-attributes
  [(enum            :being.record/being)
   (indexed-string-prop     :being/id "A uuid for the being")
   (ref-prop        :record/type "A record's type.")
   (ref-prop        :record/represents
                    "Indicates that the record entity with this
                    attribute represents a being. This should be the
                    only property that will ever be overwritten.")])

(def address-attributes
  [(string-prop     :address/first-line  "First line for an address")
   (string-prop     :address/second-line "Second line for an address")
   (string-prop     :address/zipcode     "Zipcode for an address")
   (string-prop     :address/city        "City for an address")
   (string-prop     :address/state       "State for an address")
   (string-prop     :address/country     "Country for an address")])

(def client-attributes
  [(enum            :lobbying.record/client)
   (fulltext-prop   :lobbying.client/name         "Client name.")
   (fulltext-prop   :lobbying.client/description  "Client description.")
   (component-prop  :lobbying.client/main-address "Main address for the client.")
   (component-prop  :lobbying.client/principal-place-of-business
                    "Primary location where a taxpayers's business is
                   performed (bit.ly/1s3ZbG7)")])

(def registrant-attributes
  [(enum            :lobbying.record/registrant)
   (fulltext-prop   :lobbying.registrant/name         "Registrant name.")
   (fulltext-prop   :lobbying.registrant/description  "Registrant description.")
   (bool-prop       :lobbying.registrant/self-employed-individual
                    "Whether an individual is self employed.")
   (bool-prop       :lobbying.registrant/organization-or-lobbying
                    "Whether a registrant is an organization or lobbying.")
   (component-prop  :lobbying.registrant/main-address
                    "Main address for reaching the registrant.")
   (component-prop  :lobbying.registrant/principal-place-of-business
                    "Primary location where a taxpayers's business is
                    performed (bit.ly/1s3ZbG7)")])

(def contact-attributes
  [(enum            :lobbying.record/contact)
   (string-prop     :lobbying.contact/name-prefix  "Contact name prefix.")
   (fulltext-prop   :lobbying.contact/name  "Contact name.")
   (string-prop     :lobbying.contact/phone "Contact phone.")
   (string-prop     :lobbying.contact/email "Contact email.")])

(def lobbyist-attributes
  [(enum            :lobbying.record/lobbyist)
   (fulltext-prop   :lobbying.lobbyist/first-name "First name of lobbyist.")
   (fulltext-prop   :lobbying.lobbyist/last-name  "Last name of lobbyist.")
   (string-prop     :lobbying.lobbyist/suffix     "Suffix of lobbyist.")
   (fulltext-prop     :lobbying.lobbyist/covered-official-position
                    "No idea!")])

(def activity-attributes
  [(enum            :lobbying.record/activity)
   (fulltext-prop   :lobbying.activity/general-details
                    "Details about the lobbying generally done by the
                    registrant for the client on various issues.")
   (fulltext-prop   :lobbying.activity/specific-details
                    "Details about the lobbying specifically done by the
                    registrant for the client.")
   ;;foreign interest?
   ;; (string-prop  :lobbying.activity/houses-and-agencies
   ;;                  "Details about the lobbying specifically done by the
   ;;                  registrant for the client.")
   (component-props :lobbying.activity/issue-codes
                    "The issue codes generally associated with the activity.")
   (component-props :lobbying.activity/lobbyists
                    "The foreign entities for the activity.")])


(def foreign-entity-attributes
  [(enum            :lobbying.record/foreign-entity)
   (fulltext-prop   :lobbying.foreign-entity/name
                    "Name of foreign entity.")
   (dec-prop        :lobbying.foreign-entity/amount
                    "Amount contributed to lobbying efforts.")
   (dec-prop        :lobbying.foreign-entity/ownership-percentage
                    "Ownership percentage in client.")
   (component-prop  :lobbying.foreign-entity/main-address
                    "Main address for foreign entity")
   (component-prop  :lobbying.foreign-entity/principal-place-of-business
                    "Principal place of business for foreign entity.")])

(def affiliated-organization-attributes
  [(enum            :lobbying.record/affiliated-organization)
   (fulltext-prop   :lobbying.affiliated-organization/name
                    "Name of foreign entity.")
   (component-prop  :lobbying.affiliated-organization/main-address
                    "Main address for affiliated organization.")
   (component-prop  :lobbying.affiliated-organization/principal-place-of-business
                    "Principal place of business for affiliated organization.")])

(def common-form-attributes
  [ ;;Common parts of each form
   (bool-prop       :lobbying.form/amendment
                    "Whether the form is an amendment.")
   (indexed-string-prop    :lobbying.form/house-id
                           "Id given out the clerk of the house of
                     representatives identiying a client and registrant.")
   (indexed-string-prop    :lobbying.form/senate-id
                           "Id given out the senate identiying a client and
                     registrant.")
   (instant-prop    :lobbying.form/signature-date
                    "We have no idea what this means.")
   (component-prop  :lobbying.form/client
                    "The client for the form.")
   (component-prop  :lobbying.form/registrant
                    "The registrant for the form.")
   (bool-prop       :lobbying.form/client-registrant-same
                    "Whether the client and registrant are the same entity")
   (component-prop  :lobbying.form/contact
                    "The contact for the form.")
   (component-prop  :lobbying.form/individual
                    "Potentially used, if the registrant is an individual for the form.")
   (ref-prop        :lobbying.form/source "Where the data came from.")
   (enum            :lobbying.form/sopr-html)
   (unique-string-prop     :lobbying.form/document-id
                           "Id of a document (provided by sopr).")
   (string-prop     :lobbying.form/filepath
                    "File which data for entity and subcomponents were pulled from.")  ])

(def registration-form-attributes
  [(enum            :lobbying.record/registration)
   (bool-prop       :lobbying.form/new-registrant
                    "Whether the form is for an new registrant.")
   (bool-prop       :lobbying.form/new-client-for-existing-registrant
                    "Whether the form is for an new client for an existing
              registrant.")
   (instant-prop    :lobbying.registration/effective-date
                    "No idea what this one actually means.")
   (component-props :lobbying.registration/affiliated-organizations
                    "The affiliated organizations for the engagement.")
   (component-props :lobbying.registration/foreign-entities
                    "The foreign entities for the engagement.")
   (component-prop  :lobbying.registration/activity
                    "Initial description of lobbying activity")])

(def report-form-attributes
  [(enum            :lobbying.record/report)
   (component-props :lobbying.report/removed-foreign-entities
                    "Removed foreign entities.")
   (component-props :lobbying.report/added-foreign-entities
                    "Added foreign entities.")
   (component-props :lobbying.report/removed-affiliated-organizations
                    "Removed affiliated organizations.")
   (component-props :lobbying.report/added-affiliated-organizations
                    "Added affiliated organizations.")
   (component-props :lobbying.report/removed-lobbyists
                    "Removed lobbyists.")
   (component-props :lobbying.report/added-lobbyists
                    "Added lobbyists.")
   (component-props :lobbying.registration/activities
                    "Initial description of lobbying activity")
   (ref-props       :lobbying.report/removed-lobbying-issues
                    "Removed lobbying issues.")
   (ref-props       :lobbying.report/added-lobbying-issues
                    "Added lobbying issues.")])

(def schema
  (vec (concat data-attributes
               issue-code-attributes
               address-attributes
               being-framework-attributes
               client-attributes
               registrant-attributes
               contact-attributes
               lobbyist-attributes
               affiliated-organization-attributes
               foreign-entity-attributes
               activity-attributes
               common-form-attributes
               registration-form-attributes
               report-form-attributes)))
