(ns contacts.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as s :include-macros true :refer [defroute]]
            [goog.dom :as gdom]
            [contacts.util :as util]))

(enable-console-print!)

(def app-state (atom {}))

(defn contacts-view [contacts owner]
  (reify
    om/IRender
    (render [_]
      (println contacts)
      (apply dom/ul nil
        (map #(dom/li nil (str (:person/last-name %) ", " (:person/first-name %)))
             contacts)))))

(defn app-view [app owner]
  (reify
    om/IRender
    (render [_]
      (om/build contacts-view (:contacts app)))))

(util/edn-xhr
 {:method :get
  :url "/contacts"
  :on-complete
  (fn [res]
    (swap! app-state assoc :contacts res)
    (om/root app-view app-state
       {:target (gdom/getElement "contacts")}))})
