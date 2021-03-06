(ns echelon.text
  (:require [instaparse.core :as insta]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(def single-parse
  (-> "parser.bnf" io/resource slurp insta/parser))

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
    :number (juxt (constantly :number) str)
    :initials str}
   (->> [:usa :saint :north-america :and]
        (map (juxt identity constantly ))
        (into {}))))

(def transform (partial insta/transform transform-mapping))

(defn clean [x] (-> x s/lower-case
                    (s/replace "  " " ")
                    s/trim))

(defn remove-cruft [arg]
  (if (coll? arg)
    (filter (partial not= ".") arg)
    arg))

(defn parse-name [x]
  (let [val (deref (future (all-parses (clean x))) 1000 :timeout)]
    (condp = val
      :timeout
      (do
        (println (str "Cannot extract name quickly: \"" x"\""))
        [[x]])
      []
      (do
        (println (str "Cannot extract any name: \"" x"\""))
        [[[x]]])
      (do
        (comment
          (when (not= 1 (count val))
            (println (str "Cannot unambiguously parse: \"" x "\""))))
        (->> val transform vec (map (partial map remove-cruft)) distinct)))))

(defn extract-name [[first-name & others]]
  (let [first-name (remove-cruft first-name)
        others (map remove-cruft others)]
    (if (some (partial = :obo) others)
      (->> others
           (drop-while (partial not= :obo))
           (drop 1 )
           extract-name)
      (->> others
           (partition 2)
           (filter (comp #{:fka :dba :aka} first))
           (map second)
           (concat [first-name])))))

(defn extract-names [x]
  (->> (parse-name x)
       (mapcat extract-name)
       distinct))

[(extract-names "working name")
 (extract-names "not.working")
 (extract-names "too slow                                                                     ")]
