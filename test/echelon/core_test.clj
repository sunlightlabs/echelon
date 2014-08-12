(ns echelon.core-test
  (:require [clojure.test :refer :all]
            [echelon.text :refer :all]))


(deftest north-america-extraction
  (testing "Extraction of north america."
    (are [x y ] (= (extract-names x) y)
         "EADS N.A."
         [["eads" :north-america]]

         "EADS North America"
         [["eads" :north-america]])))

(deftest saint-extraction
  (testing "Extraction of saint."
    (are [x y ] (= (extract-names x) y)

         "ST. XAVIER UNIVERSITY"
         [[:saint "xavier" "university"]]

         "SAINT XAVIER UNIVERSITY"
         [[:saint "xavier" "university"]])))

(deftest murica-extraction
  (testing "Extract of things related to the United States of America ."
    (are [x y ] (= (extract-names x) y)
         "U.S. SECURITIES MARKETS COALITION"
         [[:usa "securities" "markets" "coalition"]]

         "U.S.A SECURITIES MARKETS COALITION"
         [[:usa "securities" "markets" "coalition"]]

         "U.S.A. SECURITIES MARKETS COALITION"
         [[:usa "securities" "markets" "coalition"]]

         "U.S.A.A."
         [["u.s.a.a."]])))

(deftest number-extraction-test
  (testing "Extraction of numbers"
    (are [x y] (= (extract-names x) y)
         "Independent School District No. 1 of Tulsa County, Oklahoma a/k/a Tulsa Public Schoo" ;;sic
         [["independent" "school" "district" [:number "1"] "of" "tulsa" "county" "oklahoma"]
          :aka
          ["tulsa" "public" "schoo"]]

         "Wal-Mart 701 8th Street, NW #200, Washington, DC 20001"
         [["wal" "mart"
            [:number "701"] "8th" "street" "nw"
            [:number "200"] "washington" "dc" [:number "20001"]]])))
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
    (are [x y ] (= (extract-names x) y)

         "Terminix, International Co. Lp."
         [["terminix" :international :company :lp]]

         "ASSOCIATION OF NATIONAL ESTUARY PROGRAMS"
         [[:association "of" "national" "estuary" "programs"]]

         "Eagle 1 Resources, LCC"    ;take care of common misspellings
         [["eagle" [:number "1"] "resources" :llc]]

         "Abengoa Bioenergy Corp"
         [["abengoa" "bioenergy" :corporation]])))


(deftest extract-initials-test
  (testing "Extract initials."
    (are [x y ] (= (extract-names x) y)
         "JOHN T. O'ROURKE LAW OFFICES"
         [["john" "t." "o'rourke" "law" "offices"]])))

(deftest extract-ands-test
  (testing "Extract '&' and 'and' correctly ."
    (are [x y ] (= (extract-names x) y)
         "Rape, Abuse & Incest National Network"
         [["rape" "abuse" :and "incest" "national" "network"]]
         "Rape, Abuse And Incest National Network"
         [["rape" "abuse" :and "incest" "national" "network"]])))

(deftest domain-extraction-test
  (testing "Extract domains from strings."
    (are [x y ] (= (extract-names x) y)
         ;;Unsure if this is a good structure for domains
         "PARENTALRIGHTS.ORG"
         [[[:domain "parentalrights" "org"]]])))

(deftest formerly-extraction-test
  (testing "Extract 'formerly known as' relationships correctly."
    (are [x y ] (= (extract-names x) y)
         "Altria Client Services Inc. (formerly Altria Corporate Services, Inc.)"
         [["altria" "client" "services" :incorporated]
          :fka
          ["altria" "corporate" "services" :incorporated]]

         "Altria Client Services Inc. (\"formarly Altria Corporate Services, Inc\")"
         [["altria" "client" "services" :incorporated]
          :fka
          ["altria" "corporate" "services" :incorporated]]

         "American Coatings Association (f.k.a. National Paint and Coatings Association)"
         [["american" "coatings" :association]
          :fka
          ["national" "paint" :and "coatings" :association]]

         "Real Estate Investment Securities Assn. (Formelry TENANT-IN-COMMON ASSOCIATION)"
         [["real" "estate" "investment" "securities" :association]
          :fka
          ["tenant" "in" "common" :association]]

         ;;Unsure about whether North American should be included in the north america tag
         "SAWRUK MANAGEMENT-WYANDOTTE NATION (formerly North American Sports Management)"
         [["sawruk" "management" "wyandotte" "nation"]
          :fka
          [:north-america "sports" "management"]]

         ;;Unsure if / as whitespace is the right choice
         "GenCorp Inc./Aerojet Rocketdyne Inc. (fka Aerojet General Corporation)"
         [["gencorp" :incorporated "aerojet" "rocketdyne" :incorporated]
          :fka
          ["aerojet" "general" :corporation]])))

(run-tests)
