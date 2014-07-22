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
