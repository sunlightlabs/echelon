(ns echelon.simple
  (:gen-class)
  (:require  [clojure.data.json :as json]
            [echelon.text :refer [clean extract-names]]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.cli :refer [parse-opts]]
            [jordanlewis.data.union-find :refer :all]
            [com.rpl.specter :refer :all]
            [taoensso.timbre :as timbre]) )


(defn read-jsons-file [file]
  (map #(json/read-str % :key-fn keyword)
       (line-seq (clojure.java.io/reader file))))

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

(defn invert-index [m k1 k2]
  (into {} (map (juxt k1 k2) m)))

(defn cluster-names [m]
  (let [id-date (invert-index m :id :created_at)
        id-name (invert-index m :id :name)

        clustered-names
        (->> m 
             (update [ALL :name] extract-names)
             (reduce (fn [group {id :id extracted-names :name}]
                       (reduce #(merge-with concat %1 {%2 [id]}) group
                               extracted-names)) {}) vals cluster)
        clustered-ids
        (map (juxt (fn [lst]
                 (apply min-key (comp date-str->int id-date) lst))
               identity)
             clustered-names)]
    (->> clustered-ids
         (filter #(< 1 (count (second %))))
         (map #(hash-map :main-id (first %1) :cluster-ids (second %1))))))

#_(->> clustered-ids 
         (update [ALL (keypath 1) ALL] id-name)
         (map (comp distinct second))
         (filter #(> (count %) 1)))
#_(cluster-names (read-jsons-file "/home/zmaril/Downloads/json_dump/organizations.json"))
(def cli-options
  [["-i" "--organizations-input ORGANIZATIONS_JSON_FILE" "Input file containing organizations in ocd/json"
    :id :org-input ]
   ["-o" "--organizations-output ORGANIZATIONS_JSON_FILE" "Output file containing"
    :id :org-output]])

(defn -main [& args]
  (let [{:keys [org-input org-output] :as m} (:options (parse-opts args cli-options))]
    (->> org-input
         read-jsons-file
         cluster-names
         json/write-str
         (spit org-output))))
#_(-main "-i" "/home/zmaril/Downloads/json_dump/organizations.json" "-o" "output.json")
