(ns contacts.server
  (:require [contacts.util :as util]
            [ring.util.response :refer [file-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [bidi.bidi :refer [make-handler] :as bidi]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [contacts.datomic]
            ))

(def routes
  ["/" {"" :index
        "index.html" :index
        "contacts" :contacts}
   ])

(defn index [req]
  (file-response "public/html/index.html" {:root "resources"}))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)}
  )

(defn contacts [req]
  (generate-response (contacts.datomic/list-contacts (d/db (:datomic-connection req)))))

(defn handler [req]


  (let [match (bidi/match-route routes (:uri req))]
    (println "match: " match)

    (case (:handler match)
      :index (index req)
      :contacts (let [ r (contacts req)] (println r) r)
      req)))

(defn wrap-connection [handler conn]
  (fn [req] (handler (assoc req :datomic-connection conn))))

(defn contacts-handler* [conn]
  (wrap-resource
    (wrap-edn-params (wrap-connection handler conn))
    "public"))

(def contacts-hander [conn]
  (fn [req]
    ((contact-handler* conn) req)))

;

(defrecord WebServer [port handler container datomic-connection]
  component/Lifecycle
  (start [component]
    (let [ handler1   (handler datomic-connection)
           container (run-jetty handler1 {:port port :join? false})]
      (assoc component :container container)))
  (stop [component]
    (.stop container)))



(defn web-server [web-port] (WebServer. web-portd contacts-handler* nil nil))