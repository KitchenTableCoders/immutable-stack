(ns contacts.servlet-context-listener
  (:gen-class
    :extends javax.servlet.ServletContextListener)
  (:import [javax.servlet ServletContextEvent])
  (:require
    [com.stuartsierra.component :as component]
    contacts.system))



(def servlet-system (atom nil))

(defn -contextDestroyed [this ^ServletContextEvent contextEvent])


(defn -contextInitialized [this ^ServletContextEvent contextEvent]
  (let [s (contacts.system/prod-system
            {:db-uri   "datomic:mem://localhost:4334/contacts"})]
    (let [started-system (component/start s)]
      (reset! servlet-system started-system))))

