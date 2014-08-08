(ns echelon.text
  (:require [instaparse.core :as insta]
            [clojure.string :as s]
            [taoensso.timbre :as timbre]))
(timbre/refer-timbre)

(def single-parse
  (insta/parser (slurp "src/echelon/parser.bnf")))

(def all-parses
  (partial insta/parses single-parse))

(defn transform [t]
  (insta/transform
   {:name (fn [& ts] (s/join " " ts))
    :names vector}
   t))

(defn clean [x] (-> x s/lower-case
                    (s/replace "." "")
                    (s/replace "  " " ")
                    s/trim))

(defn extract-names [x]
  (let [val (deref (future (all-parses (clean x))) 1000 :timeout)]
    (condp = val
      :timeout
      (do
        (println (str "Cannot extract name quickly: \"" x"\""))
        [])
      []
      (do
        (println (str "Cannot extract any name: \"" x"\""))
        [])
      (-> val transform vec))))
