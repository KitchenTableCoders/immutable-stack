(ns contacts.util
  (:require [datomic.api :as d]))

(def uri "datomic:mem://localhost:4334/contacts")

(defn read-all [f]
  (Util/readAll (io/reader f)))

(defn transact-all [conn f]
  (doseq [txd (read-all f)]
    (d/transact conn txd))
  :done)

(defn create-db []
  (d/create-database uri))

(defn get-conn []
  (d/connect uri))

(defn load-schema []
  (transact-all (get-conn) (io/resource "data/schema.edn")))

(defn load-data []
  (transact-all (get-conn) (io/resource "data/initial.edn")))

(defn init-db []
  (create-db)
  (load-schema)
  (load-data))
