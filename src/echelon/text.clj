(ns echelon.text
  (:require [instaparse.core :as insta]
            [clojure.string :as s]))

(def single-parse
  (insta/parser (slurp "src/echelon/parser.bnf")))

(def all-parses
  (partial insta/parses single-parse))

(defn transform [t]
  (insta/transform
   {:name (fn [& ts] (s/join " " ts))
    :names vector}
   t))

(defn clean [x] (-> x s/lower-case (s/replace "." "") s/trim))

(defn extract-names [x] (-> x clean all-parses transform vec))

(comment
  (insta/parses
   single-parse
   (clean "AK Steel Holding Corporation (Formerly Known As AK Steel)")
   :partial true
   :unhide :all)
  (insta/parse
   single-parse
   (clean "AK Steel Holding Corporation (Formerly Known As AK Steel)"))
  (insta/parses
   single-parse
   (clean "Bolton-St. Johns, LLC (f/k/a Bolton-St. Johns, Inc.)")
   :partial true :unhide :all))
