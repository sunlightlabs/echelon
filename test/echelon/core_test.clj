(ns echelon.core-test
  (:require [clojure.test :refer :all]
            [echelon.text :refer :all]))

(deftest extract-names-test
  (testing "FIXME, I fail."
    (are [x y ] (= (extract-names x) y)
         "Altria Client Services Inc. (formerly Altria Corporate Services, Inc.)"
         [["altria" "client" "services" :incorporated]
          :formerly
          ["altria" "corporate" "services" :incorporated]]

         "ST. XAVIER UNIVERSITY"
         [[:saint "xavier" "university"]]

         "U.S. SECURITIES MARKETS COALITION"
         [[:usa "securities" "markets" "coalition"]])))
