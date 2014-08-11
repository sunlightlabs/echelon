(ns echelon.core-test
  (:require [clojure.test :refer :all]
            [echelon.text :refer :all]))

(deftest extract-names-test
  (testing "Extract names testing."
    (are [x y ] (= (extract-names x) y)
         "Altria Client Services Inc. (formerly Altria Corporate Services, Inc.)"
         [["altria" "client" "services" :incorporated]
          :formerly
          ["altria" "corporate" "services" :incorporated]]

         "ST. XAVIER UNIVERSITY"
         [[:saint "xavier" "university"]]

         "U.S. SECURITIES MARKETS COALITION"
         [[:usa "securities" "markets" "coalition"]]

         "American Coatings Association (f.k.a. National Paint and Coatings Association)"
         [["american" "coatings" :association]
          :formerly
          ["national" "paint" "and" "coatings" :association]]

         ;; "The Livingston Group (for Huntington Ingalls Incorporated)"
         ;; [["the"
         ;;   "livingston"
         ;;   "group"
         ;;   "for"
         ;;   "huntington"
         ;;   "ingalls"
         ;;   :incorporated]]
         "EADS N.A."
         [["eads" :north-america]]

         "EADS North America"
         [["eads" :north-america]]

         "Terminix, International Co. Lp."
         [["terminix" :international :company :lp]]

         "Real Estate Investment Securities Assn. (Formelry TENANT-IN-COMMON ASSOCIATION)"
         [["real" "estate" "investment" "securities" :association]
          :formerly
          ["tenant" "in" "common" :association]]

         "ASSOCIATION OF NATIONAL ESTUARY PROGRAMS"
         [[:association "of" "national" "estuary" "programs"]]
         "JOHN T. O'ROURKE LAW OFFICES"
         [["john" "t." "o'rourke" "law" "offices"]]

         "Independent School District No. 1 of Tulsa County, Oklahoma a/k/a Tulsa Public Schoo" ;;sic
         [["independent" "school" "district" :numero "1" "of" "tulsa" "county" "oklahoma"]
          :aka
          ["tulsa" "public" "schoo"]]

         "Eagle 1 Resources, LCC"     ;take care of common mispellings
         [["eagle" "1" "resources" :llc]]

         "Altria Client Services Inc. (\"formarly Altria Corporate Services, Inc\")"
         [["altria" "client" "services" :incorporated]
          :formerly
          ["altria" "corporate" "services" :incorporated]]

         ;Unsure about whether North American should be included in the north america tag
         "SAWRUK MANAGEMENT-WYANDOTTE NATION (formerly North American Sports Management)"
         [["sawruk" "management" "wyandotte" "nation"]
          :formerly
          [:north-america "sports" "management"]]

         ;;Unsure if this is a good structure for domains
         "PARENTALRIGHTS.ORG"
         [[[:domain "parentalrights" "org"]]]
         )))

(run-tests)
