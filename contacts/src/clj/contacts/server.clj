(ns contacts.server
  (:require [contacts.util :as util]
            [ring.util.response :refer [file-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [bidi.bidi :refer [make-handler] :as bidi]
            [com.stuartsierra.component :as component]))

(def routes
  ["/" {"" :index
        "index.html" :index}])

(defn index [req]
  (file-response "public/html/index.html" {:root "resources"}))

(defn handler [req]
  (println "in-handler req: " req)

  (let [match (bidi/match-route routes (:uri req))]
    (println "match: " match)

    (case (:handler match)
      :index (index req)
      req)))

(defn wrap-connection [handler conn]
  (fn [req] (handler (assoc req :datomic-connection conn))))

(defn contacts-handler [conn]
  (wrap-resource
    (wrap-edn-params (wrap-connection handler conn))
    "public"))

(defrecord WebServer [port container datomic-connection]
  component/Lifecycle
  (start [component]
    (let [ handler (contacts-handler datomic-connection)
           container (run-jetty handler {:port port :join? false})]
      (assoc component :container container)))
  (stop [component]
    (.stop container)))

(defn web-server [web-port] (WebServer. web-port nil nil))