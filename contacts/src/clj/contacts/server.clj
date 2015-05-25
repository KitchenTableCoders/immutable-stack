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
  ["" {"/" :index
       "/index.html" :index
       "/query"
        {:post {[""] :query}}}])

;; =============================================================================
;; Handlers

(defn index [req]
  (assoc (resource-response "html/index.html" {:root "public"})
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

(defn fetch
  ([conn k] (fetch conn k '[*]))
  ([conn k selector] (fetch conn k selector nil))
  ([conn k selector context]
    (case k
      :contacts (contacts conn selector)
      (throw
        (ex-info (str "No data route for " k)
          {:type :error/invalid-data-route})))))

(defn populate
  ([conn query-map] (populate conn query-map nil))
  ([conn query-map context]
   (letfn [(step [ret k v]
             (cond
               (map? v)
               (if (contains? v :self)
                 (assoc ret
                   k (->> (fetch conn k (:self v))
                       (map #(merge {:self %}
                              (populate conn (dissoc v :self) %)))
                       vec))
                 (assoc ret k (populate conn v)))

               (vector? v)
               (fetch conn k v context)

               :else
               (throw
                 (ex-info (str "Invalid query-map value " v)
                   {:type :error/invalid-query-map-value}))))]
     (reduce-kv step {} query-map))))

(defn query [req]
  (generate-response
    (populate (:datomic-connection req) (:transit-params req))))

;;;; PRIMARY HANDLER

(defn handler [req]
  (let [match (bidi/match-route routes (:uri req)
                :request-method (:request-method req))]
    ;(println match)
    (case (:handler match)
      :index (index req)
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
            :transit-params {:contacts {:self [:person/first-name :person/last-name]}}
            :datomic-connection (:connection (:db @cc/servlet-system))})

  ;; create contact
  (handler {:uri "/contacts"
            :request-method :post
            :transit-params {:person/first-name "Bib" :person/last-name "Bibooo"}
            :datomic-connection (:connection (:db @cc/servlet-system))})

  )

