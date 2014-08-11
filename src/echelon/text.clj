(ns echelon.text
  (:require [instaparse.core :as insta]
            [clojure.string :as s]
            [taoensso.timbre :as timbre]))
(timbre/refer-timbre)

(def single-parse
  (insta/parser (slurp "src/echelon/parser.bnf")))

(def all-parses
  (partial insta/parses single-parse))

(def transform-mapping
  (merge
   {:corporates first
    :splitters  first
    :other  vector
    :beings vector
    :name   vector
    :initial str
    :initials str}
   (->> [:usa :saint :north-america :numero :and]
        (map (juxt identity constantly ))
        (into {}))))

(def transform (partial insta/transform transform-mapping))

(defn clean [x] (-> x s/lower-case
                    (s/replace "  " " ")
                    s/trim))

(defn extract-names [x]
  (let [val (deref (future (all-parses (clean x))) 100 :timeout)]
    (condp = val
      :timeout
      (do
        (println (str "Cannot extract name quickly: \"" x"\""))
        [])
      []
      (do
        (println (str "Cannot extract any name: \"" x"\""))
        [])
      (do
        (comment
          (when (not= 1 (count val))
            (println (str "Cannot unambiguously parse: \"" x "\""))))
        (-> val last transform vec)))))

(def s (clean "U.S.A.A."))
(single-parse s)
(all-parses s :unhide :all)

(extract-names s)
