(ns echelon.simple
  (:gen-class)
  (:require  [clojure.data.json :as json]
            [echelon.text :refer [clean extract-names]]
            [clojure.string :as st]
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

(defn cluster-helper [id-date m]
  ;(spit (str "debug-" (quot (System/currentTimeMillis) 1000)) m)
  (->> m vals cluster
       (map (juxt #(apply min-key (comp date-str->int id-date) %) identity))
       (filter #(< 1 (count (second %))))
       (map #(hash-map :main-id (first %1) :cluster-ids (second %1))) ))

(defn cluster-orgs [m]
  (let [;m (take 100 m)
        id-date (invert-index m :id :created_at)]
   (->> m 
        (update [ALL :name] extract-names)
        (reduce (fn [group {id :id extracted-names :name}]
                  (reduce #(merge-with concat %1 {%2 [id]}) group
                          extracted-names)) {})
        (cluster-helper id-date))))

(defn extract-persons-name [s]
  (as-> s $
       (st/lower-case $)
       (filter #(or (Character/isLetter %) (Character/isSpace %)) $)
       (st/join $)
       (st/replace $ #"  " " ")
       (st/trim $)))

(defn cluster-people [org-ids m]
  (let [;m (take 100 m)
        id-date (invert-index m :id :created_at)
        merger (fn [group {:keys [id name] :as m}]
                   (merge-with concat group {[name (-> m :memberships first :organization :id org-ids)] [id]}))]
    (->> m
         (update [ALL :name] extract-persons-name)
         (reduce merger {})
         (cluster-helper id-date))))

(def cli-options
  [["-i" "--organizations-input ORGANIZATIONS_JSON_FILE" "Input file containing organizations in ocd/json"
    :id :org-input ]
   ["-p" "--people-input PEOPLE_JSON_FILE" "Input file containing people in ocd/json"
    :id :people-input]
   ["-o" "--organizations-output ORGANIZATIONS_JSON_FILE" "Output file containing"
    :id :org-output]])

(defn -main [& args]
  (let [{:keys [org-input org-output people-input] :as m}
        (:options (parse-opts args cli-options))

        clustered-orgs
        (->> org-input
             read-jsons-file
             cluster-orgs)

        old-id->new-id
        (reduce (fn [m {:keys [main-id cluster-ids]}]
                  (apply assoc m (interleave cluster-ids (repeat main-id))))
                {}
                clustered-orgs)
        
        clustered-people
        (->> people-input
             read-jsons-file
             (cluster-people old-id->new-id))
        
        merged-ids (concat clustered-people clustered-orgs)]
    (spit org-output (json/write-str merged-ids))))

#_(-main "-i" "/home/zmaril/Downloads/json_dump/organizations.json"
       "-p" "/home/zmaril/Downloads/json_dump/people.json"
       "-o" "output.json")

#_(->> (read-jsons-file "/home/zmaril/Downloads/json_dump/people.json")
     (map :memberships)
     (map count)
     frequencies)
