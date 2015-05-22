(ns contacts.server
  (:require [contacts.util :as util]
            [ring.util.response :refer [file-response resource-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [contacts.middleware
             :refer [wrap-transit-body wrap-transit-response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [bidi.bidi :refer [make-handler] :as bidi]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [contacts.datomic]))

;; =============================================================================
;; Routing

(def routes
  ["" {"/" :index
       "/index.html" :index
       "/contacts"
        {:get
          {[""] :contacts
           ["/" :id] :contact-get}
         :post  {[""] :contact-create}
         :put   {["/" :id] :contact-update}
         :delete {["/" :id] :contact-delete}}
       "/phone"
        {:post   {[""] :phone-create}
         :put    {["/" :id] :phone-update}
         :delete {["/" :id] :phone-delete}}}])

;; =============================================================================
;; Handlers

(defn index [req]
  (assoc (resource-response "html/index.html" {:root "public"})
    :headers {"Content-Type" "text/html"}))


(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/transit+json"}
   :body (pr-str data)})


;; CONTACT HANDLERS

(defn contacts [req]
  (generate-response
    (vec
      (contacts.datomic/contacts
        (d/db (:datomic-connection req))))))


(defn contact-get [req id]
  (generate-response
    (contacts.datomic/get-contact
      (d/db (:datomic-connection req)) id)))


(defn contact-create [req]
  (generate-response
    (contacts.datomic/create-contact
      (:datomic-connection req)
      ;; must have form {:person/first-name "x" :person/last-name "y}
      (:edn-params req))))


(defn contact-update [req id]
  (generate-response
    (contacts.datomic/update-contact
      (:datomic-connection req)
      (assoc (:edn-params req) :db/id id))))


(defn contact-delete [req id]
  (generate-response
    (contacts.datomic/delete-contact
      (:datomic-connection req)
      id)))


;;;; PHONE HANLDERS

(defn phone-create [req]
  (generate-response
    (contacts.datomic/create-phone
      (:datomic-connection req)
      ;; must have form {:person/_telephone person-id}
      (:edn-params req))))

(defn phone-update [req id]
  (generate-response
    (contacts.datomic/update-phone
      (:datomic-connection req)
      (assoc (:edn-params req) :db/id id))))

(defn phone-delete [req id]
  (generate-response
    (contacts.datomic/delete-phone
      (:datomic-connection req)
      id)))

;;;; PRIMARY HANDLER

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
      :contact-create (contact-create req)
      :contact-update (contact-update req (:id (:params match)))
      :contact-delete (contact-delete req (:id (:params match)))

      :phone-create (phone-create req)
      :phone-update (phone-update req (:id (:params match)))
      :phone-delete (phone-delete req (:id (:params match)))

      req)))


(defn wrap-connection [handler conn]
  (fn [req] (handler (assoc req :datomic-connection conn))))


(defn contacts-handler [conn]
  (wrap-resource
    (wrap-transit-response (wrap-transit-body (wrap-connection handler conn)))
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

;; =============================================================================
;; Route Testing

(comment

  ;; get contact
  (handler {:uri "/contacts/17592186045438"
            :request-method :get
            :datomic-connection (:connection (:db @contacts.core/servlet-system))})

  ;; create contact
  (handler {:uri "/contacts"
            :request-method :post
            :edn-params {:person/first-name "Bib" :person/last-name "Bibooo"}
            :datomic-connection (:connection (:db @contacts.core/servlet-system))})

  ;; update contact
  (handler {:uri "/contacts/17592186045434"
            :request-method :put
            :edn-params {:person/first-name "k" :person/last-name "b"}
            :datomic-connection (:connection (:db @contacts.core/servlet-system))})

  ;; delete contact
  (handler {:uri "/contacts/17592186045434"
            :request-method :delete
            :datomic-connection (:connection (:db @contacts.core/servlet-system))})

  ;; create phone
  (handler {:uri "/phone"
            :request-method :post
            :edn-params {:telephone/number "000-111-2222"
                         :person/_telephone "17592186045438"}
            :datomic-connection (:connection (:db @contacts.core/servlet-system))})

  ;; update phone
  (handler {:uri "/phone/17592186045444"
            :request-method :put
            :edn-params {:telephone/number "999-888-7777"}
            :datomic-connection (:connection (:db @contacts.core/servlet-system))})


  ;; delete phone
  (handler {:uri "/phone/17592186045444"
            :request-method :delete
            :datomic-connection (:connection (:db @contacts.core/servlet-system))})


  )

