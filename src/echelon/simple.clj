(ns echelon.simple
 (:require  [clojure.data.json :as json]
            [echelon.text :refer [clean extract-names]]
            [echelon.util :refer [contains-nil? transpose db-prn]]
            [clojure.pprint :refer [pprint]]
            [clj-time.format :as f]
            [jordanlewis.data.union-find :refer :all]
            [environ.core :refer [env]]
            [com.rpl.specter :refer :all]
            [taoensso.timbre :as timbre]) )


(def datadir "/home/zmaril/Downloads/json_dump")
(def organizations-json-file (str datadir "/organizations.json"))
(def people-jsons-file (str datadir "/people.json"))
(defn organizations-json [] (map #(json/read-str % :key-fn keyword)
                                 (line-seq (clojure.java.io/reader organizations-json-file))))

(defn union-lst
  [uf lst]
  (let [pairs (partition 2 (interleave lst (rest lst)))]
    (reduce (fn [uf [a b]] (union uf a b))
            uf pairs)))

(defn cluster [lst]
  (let [uf (reduce union-lst (->> lst
                         (apply concat)
                         (apply union-find)) lst)]
    (vals
     (group-by uf (keys (.elt-map uf))))))

(defn date-str->int [s]
  (.getTime (clojure.instant/read-instant-date s)))

(defn test-fn []
  (let [name-id-date (organizations-json)

        id-date
        (into {} (map (juxt :id :created_at) name-id-date))

        clustered-names
        (->> name-id-date
             (update [ALL :name] extract-names)
             (reduce (fn [group {id :id extracted-names :name}]
                       (reduce #(merge-with concat %1 {%2 [id]}) group
                               extracted-names)) {}) vals cluster)]
    (map (juxt (fn [lst]
                 (apply min-key (comp date-str->int id-date) lst))
               identity)
         clustered-names)))
