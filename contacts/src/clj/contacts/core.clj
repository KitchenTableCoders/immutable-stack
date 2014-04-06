(ns contacts.core
  (:require [contacts.util :as util]
            [ring.util.response :refer [file-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [bidi.bidi :refer [make-handler] :as bidi]))

(def routes
  ["/" {"" :index
        "index.html" :index}])

(defn index [req]
  (file-response "public/html/index.html" {:root "resources"}))

(defn handler [req]
  (let [match (bidi/match-route routes (:uri req))]
    (case (:handler match)
      :index (index req)
      req)))

(def app
  (-> handler
      (wrap-resource "public")
      wrap-edn-params))

(defonce server
  (run-jetty #'app {:port 8080 :join? false}))
