(ns echelon.parser-test
  (:require [clojure.test :refer :all]
            [echelon.text :refer :all]
            [echelon.util :refer [transpose]]))


(defmacro same-parsed-names [& body]
  `(are [x# y#] (= (parse-name x#) y#)  ~@body))

(defmacro same-parsed-and-extracted-names [& body]
  (let [[strs parsed extracted] (transpose  (partition 3 body))]
    `(do (are [x# y#] (= (parse-name x#) y#)    ~@(interleave strs parsed))
         (are [x# z#] (= (extract-names x#) z#) ~@(interleave strs extracted)))))

(deftest north-america-extraction
  (testing "Extraction of north america."
    (same-parsed-names
     "EADS N.A."
     [[["eads" :north-america]]]


     "EADS North America"
     [[["eads" :north-america]]])))


(deftest saint-extraction
  (testing "Extraction of saint."
    (same-parsed-names
     "ST. XAVIER UNIVERSITY"
     [[[:saint "xavier" "university"]]]


     "SAINT XAVIER UNIVERSITY"
     [[[:saint "xavier" "university"]]])))

(deftest murica-extraction
  (testing "Extract of things related to the United States of America ."
    (same-parsed-names
     "U.S. SECURITIES MARKETS COALITION"
     [[[:usa "securities" "markets" "coalition"]]]

     "U.S.A SECURITIES MARKETS COALITION"
     [[[:usa "securities" "markets" "coalition"]]]

     "U.S.A. SECURITIES MARKETS COALITION"
     [[[:usa "securities" "markets" "coalition"]]]

     "U.S.A.A."
     [[["u.s.a.a."]]
      [["u.s.a.a"]]])))

(deftest number-extraction-test
  (testing "Extraction of numbers"
    (same-parsed-and-extracted-names
     "Independent School District No. 1 of Tulsa County, Oklahoma a/k/a Tulsa Public Schoo" ;;sic
     [[["independent" "school" "district" [:number "1"] "of" "tulsa" "county" "oklahoma"]
        :aka
        ["tulsa" "public" "schoo"]]]
     [["independent" "school" "district" [:number "1"] "of" "tulsa" "county" "oklahoma"]
      ["tulsa" "public" "schoo"]]


     "Wal-Mart 701 8th Street, NW #200, Washington, DC 20001"
     [[["wal" "mart"
         [:number "701"] "8th" "street" "nw"
         [:number "200"] "washington" "dc" [:number "20001"]]]]
     [["wal" "mart"
       [:number "701"] "8th" "street" "nw"
       [:number "200"] "washington" "dc" [:number "20001"]]]

     "Dairy 51.8 LLC"
     [[["dairy" [:number "51.8"] :llc]]]
     [["dairy" [:number "51.8"] :llc]]
     )))
    ;; "The Livingston Group (for Huntington Ingalls Incorporated)"
         ;; [["the"
         ;;   "livingston"
         ;;   "group"
         ;;   "for"
         ;;   "huntington"
         ;;   "ingalls"
         ;;   :incorporated]]

(deftest extract-corporates-test
  (testing "Extract corporates."
    (same-parsed-names
     "Terminix, International Co. Lp."
     [[["terminix" :international :company :lp]]]

     "ASSOCIATION OF NATIONAL ESTUARY PROGRAMS"
     [[[:association "of" "national" "estuary" "programs"]]]

     "Eagle 1 Resources, LCC"        ;take care of common misspellings
     [[["eagle" [:number "1"] "resources" :llc]]]

     "Abengoa Bioenergy Corp"
     [[["abengoa" "bioenergy" :corporation]]])))


(deftest extract-middle-men
  (testing "Extraction and disregarding of middle men lobbying groups."
    (same-parsed-and-extracted-names
     "THE LIVINGSTON GROUP (ON BEHALF OF GOODYEAR TIRE & RUBBER CO)"
     [[["the" "livingston" "group"]
       :obo
       ["goodyear" "tire" :and "rubber" :company]]]
     [["goodyear" "tire" :and "rubber" :company]])))

(deftest extract-initials-test
  (testing "Extract initials."
    (same-parsed-names
     "JOHN T. O'ROURKE LAW OFFICES"
     [[["john" "t"  "o'rourke" "law" "offices"]]
      [["john" "t." "o'rourke" "law" "offices"]]]

     "Lobbyists Offices of G.R.W , Inc."
     [[["lobbyists" "offices" "of" "g.r.w" :corporation]]])))

(deftest extract-ands-test
  (testing "Extract '&' and 'and' correctly ."
    (same-parsed-names
     "Rape, Abuse & Incest National Network"
     [[["rape" "abuse" :and "incest" "national" "network"]]]
     "Rape, Abuse And Incest National Network"
     [[["rape" "abuse" :and "incest" "national" "network"]]])))

(deftest domain-extraction-test
  (testing "Extract domains from strings."
    (same-parsed-names
     ;;Unsure if this is a good structure for domains
     "PARENTALRIGHTS.ORG"
     [[[
        ;;note that domains use a vector to encode structure
        [:domain "parentalrights" "org"]
        ]]]
     "www.industry.org.il"
     [[[[:domain "industry" "org" "il"]]]])))

(deftest formerly-extraction-test
  (testing "Extract 'formerly known as' relationships correctly."
    (same-parsed-and-extracted-names
     "Altria Client Services Inc. (formerly Altria Corporate Services, Inc.)"
     [[["altria" "client" "services" :corporation]
       :fka
       ["altria" "corporate" "services" :corporation]]]
     [["altria" "client" "services" :corporation]
      ["altria" "corporate" "services" :corporation]]

     "Altria Client Services Inc. (\"formarly Altria Corporate Services, Inc\")"
     [[["altria" "client" "services" :corporation]
       :fka
       ["altria" "corporate" "services" :corporation]]]
     [["altria" "client" "services" :corporation]
      ["altria" "corporate" "services" :corporation]]

     "American Coatings Association (f.k.a. National Paint and Coatings Association)"
     [[["american" "coatings" :association]
       :fka
       ["national" "paint" :and "coatings" :association]]]
     [["american" "coatings" :association]
      ["national" "paint" :and "coatings" :association]]

     "Real Estate Investment Securities Assn. (Formelry TENANT-IN-COMMON ASSOCIATION)"
     [[["real" "estate" "investment" "securities" :association]
       :fka
       ["tenant" "in" "common" :association]]]
     [["real" "estate" "investment" "securities" :association]
      ["tenant" "in" "common" :association]]

     ;;Unsure about whether North American should be included in the north america tag
     "SAWRUK MANAGEMENT-WYANDOTTE NATION (formerly North American Sports Management)"
     [[["sawruk" "management" "wyandotte" "nation"]
       :fka
       [:north-america "sports" "management"]]]
     [["sawruk" "management" "wyandotte" "nation"]
      [:north-america "sports" "management"]]

     ;;Unsure if / as whitespace is the right choice
     "GenCorp Inc./Aerojet Rocketdyne Inc. (fka Aerojet General Corporation)"
     [[["gencorp" :corporation "aerojet" "rocketdyne" :corporation]
       :fka
       ["aerojet" "general" :corporation]]]
     [["gencorp" :corporation "aerojet" "rocketdyne" :corporation]
      ["aerojet" "general" :corporation]]

     "4G Americas (f/k/a 3G Americas, LLC)"
     [[["4g" "americas"]
       :fka
       ["3g" "americas" :llc]]]
     [["4g" "americas"]
      ["3g" "americas" :llc]] )))


(run-tests)
