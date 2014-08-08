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
   {:corporates first
    :splitters  first
    :usa        (constantly :usa)
    :saint      (constantly :saint)
    :other  vector
    :beings vector
    :name   vector}
   t))

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
        (when (not= 1 (count val))
          (println (str "Cannot unambiguously parse: \"" x "\"")))
        (-> val first transform vec)))))

(def s (clean "U.S. SECURITIES MARKETS COALITION"
))
(single-parse s)
(all-parses s)
(extract-names s)
