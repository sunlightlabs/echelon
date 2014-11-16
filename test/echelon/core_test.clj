(ns echelon.core-test
  (:require [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [instaparse.core :as ip]
            [instagenerate.core :as ig]
            [clojure.core.logic :as cl]
            [echelon.text :refer [parser]]))



(parser "google inc.")

(cl/run* [input]
  (ig/instaparseo parser input
                  [:beings [:name "google" [:corporates [:corporation "inc."]]]]))

(cl/run 1 [input]
  (ig/instaparseo parser input
                  [:beings [:name "google"]]
                  ))
