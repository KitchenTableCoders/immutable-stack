(ns contacts.server
  (:require [contacts.util :as util]
            [ring.util.response :refer [file-response resource-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [contacts.middleware
             :refer [wrap-transit-body wrap-transit-response
                     wrap-transit-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [bidi.bidi :refer [make-handler] :as bidi]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [contacts.datomic]))

;; =============================================================================
;; Routes

(def routes
  ["" {"/" :demo1
       "/demo/1" :demo1
       "/demo/2" :demo2
       "/query"
        {:post {[""] :query}}}])

;; =============================================================================
;; Handlers

(defn demo [n req]
  (assoc (resource-response "html/demo" n ".html" {:root "public"})
    :headers {"Content-Type" "text/html"}))

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/transit+json"}
   :body    data})

;; CONTACT HANDLERS

(defn contacts [conn selector]
  (contacts.datomic/contacts (d/db conn) selector))

(defn contact-get [conn id]
  (contacts.datomic/get-contact (d/db conn) id))

(defmulti -fetch (fn [_ k _] k))

(defmethod -fetch :app/contacts
  [conn _ selector]
  (contacts conn selector))

(defn fetch
  ([conn k] (fetch conn k '[*]))
  ([conn k selector]
    (-fetch conn k selector)))

(defn populate [conn query]
  (letfn [(step [ret k]
            (cond
              (map? k)
              (let [[k v] (first k)]
                (assoc ret k (fetch conn k v)))

              (keyword? k)
              (assoc ret k (fetch conn k))

              :else
              (throw
                (ex-info (str "Invalid query key " k)
                  {:type :error/invalid-query-value}))))]
    (reduce step {} query)))

(defn query [req]
  (generate-response
    (populate (:datomic-connection req) (:transit-params req))))

;;;; PRIMARY HANDLER

(defn handler [req]
  (let [match (bidi/match-route routes (:uri req)
                :request-method (:request-method req))]
    ;(println match)
    (case (:handler match)
      :demo1 (demo 1 req)
      :demo2 (demo 2 req)
      :query (query req)
      :contact-get (contact-get req (:id (:params match)))
      req)))

(defn wrap-connection [handler conn]
  (fn [req] (handler (assoc req :datomic-connection conn))))

(defn contacts-handler [conn]
  (wrap-resource
    (wrap-transit-response
      (wrap-transit-params (wrap-connection handler conn)))
    "public"))

(defn contacts-handler-dev [conn]
  (fn [req]
    ((contacts-handler conn) req)))

;; =============================================================================
;; WebServer

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

;; =============================================================================
;; Route Testing

(comment
  (require '[contacts.core :as cc])
  (cc/dev-start)

  ;; get contact
  (handler {:uri "/query"
            :request-method :post
            :transit-params [{:app/contacts [:person/first-name :person/last-name
                                             {:person/telephone '[*]}]}]
            :datomic-connection (:connection (:db @cc/servlet-system))})

  (.basisT (d/db (:connection (:db @cc/servlet-system))))

  ;; create contact
  (handler {:uri "/contacts"
            :request-method :post
            :transit-params {:person/first-name "Bib" :person/last-name "Bibooo"}
            :datomic-connection (:connection (:db @cc/servlet-system))})

  )

