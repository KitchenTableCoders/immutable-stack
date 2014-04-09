(ns contacts.core
  (:require [goog.dom :as gdom]
            [goog.events :as events]
            [secretary.core :as secretary :include-macros true :refer [defroute]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contacts.util :as util])
  (:import goog.History
           goog.history.EventType))

(enable-console-print!)

(def app-state
  (atom {:route [:list-contacts]}))

(defn contact-view [contact owner]
  )

(defn contacts-view [contacts owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/button
          #js {:id "add-contact"
               :onClick (fn [e])}
          "Add contact")
        (apply dom/ul #js {:id "contacts-list"}
          (map #(dom/li nil
                  (str (:person/last-name %) ", " (:person/first-name %)))
               contacts))))))

(defn app-view [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      #_(defroute "/" []
        (om/update! app :route [:list-contacts]))
      #_(defroute "/:id" [id]
        (om/update! app :route [:view-contact id]))
      #_(let [h (History.)]
        (events/listen h EventType.NAVIGATE
          #(secretary/dispatch! (.-token %)))
        (.setEnabled h true)))
    om/IRender
    (render [_]
      (let [route (:route app)]
        (dom/div nil
          (case (first (:route app))
            :list-contacts (om/build contacts-view (:contacts app))
            :view-contact  (om/build contact-view (get-in app [:contacts (second route)]))))))))

(util/edn-xhr
  {:method :get
   :url "/contacts"
   :on-complete
   (fn [res]
     (swap! app-state assoc :contacts res)
     (om/root app-view app-state
       {:target (gdom/getElement "contacts")}))})
