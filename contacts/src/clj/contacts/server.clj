(ns contacts.server
  (:require [contacts.util :as util]
            [ring.util.response :refer [file-response resource-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [bidi.bidi :refer [make-handler] :as bidi]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [contacts.datomic]
            ))

(def routes
  ["" {"/" :index
       "/index.html" :index
       "/contacts"
        {:get
          {[""] :contacts
           ["/" :id] :contact-get}
         :update {[:id] :contact-update}
         :create {[:id] :contact-create}
         :delete {[:id] :contact-delete}}}])

(defn index [req]
  (assoc (resource-response "html/index.html" {:root "public"})
    :headers {"Content-Type" "text/html"}))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn contacts [req]
  (generate-response
    (vec
      (contacts.datomic/display-contacts
        (d/db (:datomic-connection req))))))

(defn contact-get [req id]
  (generate-response
    (contacts.datomic/get-contact
      (d/db (:datomic-connection req)) id)))

(defn handler [req]
  (let [match (bidi/match-route
                routes
                (:uri req)
                :request-method (:request-method req))]
    ;(println match)
    (case (:handler match)
      :index (index req)
      :contacts (contacts req)
      :contact-get (contact-get req (:id (:params match)))

      req)))

(defn wrap-connection [handler conn]
  (fn [req] (handler (assoc req :datomic-connection conn))))

(defn contacts-handler [conn]
  (wrap-resource
    (wrap-edn-params (wrap-connection handler conn))
    "public"))

(defn contacts-handler-dev [conn]
  (fn [req]
    ((contacts-handler conn) req)))

(defrecord WebServer [port handler container datomic-connection]
  component/Lifecycle
  (start [component]
    ;; NOTE: fix datomic-connection
    (if container
      (let [req-handler (handler (:connection datomic-connection))
           container (run-jetty req-handler {:port port :join? false})]
       (assoc component :container container))
      ;; if no container
      (assoc component :handler (handler (:connection datomic-connection)))))
  (stop [component]
    (.stop container)))

(defn dev-server [web-port] (WebServer. web-port contacts-handler-dev true nil))

(defn prod-server [] (WebServer. nil contacts-handler false nil))
