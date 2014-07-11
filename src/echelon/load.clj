(ns echelon.load
  (:require [datomic.api :as d :refer [db q]]
            [clojure.data.json :as json]
            [echelon.text :refer [clean]]
            [me.raynes.fs :as fs]
            [clojure.pprint :refer [pprint]]))

(def datadir "/home/zmaril/data/original/sopr_html/")

(defn list-ld1-forms []
  (mapcat #(fs/glob (str datadir "/" % "/REG/*"))
          (range 2008 2015)))

(defn list-ld2-forms []
  (for [y (range 2004 2015) q (range 1 5)]
    (fs/glob (str datadir "LD2/" y "/Q" q "/*"))))


(def counter (atom 0))

(defn ld1-datoms [m]
  (let [client-name  (some-> m :client :client_name clean)
        firm-name    (some-> m :registrant :registrant_name clean)
        document-id  (some-> m :document_id)
        blank?       (some nil? [client-name firm-name document-id])]
    (when blank?
      (swap! counter inc))
    (if blank?
      []
      [{:db/id #db/id[:db.part/user -1]
        :being/type :being.type/:being}
       {:db/id #db/id[:db.part/user -2]
        :being/type :being.type/:client
        :client/name client-name
        :being/represents #db/id[:db.part/user -1]}
       {:db/id #db/id[:db.part/user -3]
        :being/type :being.type/:being}
       {:db/id #db/id[:db.part/user -4]
        :being/type :being.type/:firm
        :firm/name  firm-name
        :being/represents #db/id[:db.part/user -3]}
       {:db/id #db/id[:db.part/tx -1]
        :data/document-id document-id
        :data/source :data.source/sopr-html}])))


(defn load-data! [conn]
  (->> (list-ld1-forms)
       (map (comp
             (partial d/transact conn)
             ld1-datoms
             #(json/read-str % :key-fn keyword)
             slurp))
       doall)
  (comment
    (->> (list-ld2-forms)
         (filter (complement nil?))
         (apply concat)
         (pmap (comp (partial add-ld2-form! conn) json/read-str slurp))
         doall)))

(defn load-schema! [conn]
  (d/transact conn (read-string (slurp "src/echelon/schema.edn"))))

(defn load-database! [conn]
  (println "Schema loading...")
  (load-schema! conn)
  (println "Data loading...")
  (load-data! conn)
  (println (str "There are " @counter " blank forms")))
