(ns contacts.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as gdom]
            [goog.events :as events]
            [secretary.core :as secretary :include-macros true :refer [defroute]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! >! <!]]
            [contacts.util :as util]
            [contacts.components :as c])
  (:import [goog History]
           [goog.history EventType]))

;; =============================================================================
;; Setup

(enable-console-print!)

(def history (History.))

(events/listen history EventType.NAVIGATE
  #(secretary/dispatch! (.-token %)))

(def app-state
  (atom {:route [:list-contacts]
         :contacts []
         :current-contact :none}))

;; =============================================================================
;; Components

(defn contact-numbers [numbers]
  (dom/div #js {:className "section"}
    (dom/label nil "Phone Numbers")
    (apply dom/ul #js {:id "phone-numbers" :className "list"}
      (map #(dom/li nil (:telephone/number %))
           numbers))))


(defn contact-address [address]
  (dom/li nil
      (dom/div nil (:address/street address))
      (dom/div nil (str (:address/city address) ", "
                        (:address/state address) " "
                        (:address/zipcode address)))))


(defn contact-addresses [addresses]
  (dom/div #js {:className "section"}
    (dom/label nil "Addresses")
    (apply dom/ul #js {:id "addresses" :className "list"}
      (map contact-address addresses))))


(defn contact-view [contact owner {:keys [current-contact sync]}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "contact-view"}
        (dom/button
          #js {:onClick (fn [e] (put! current-contact :none))
               :className "button"}
          "Back")
        (dom/div #js {:id "contact-info"}
          (dom/div #js {:className "contact-name editable"}
            (om/build c/editable contact
              {:opts
               {:edit-key :person/first-name
                :on-edit
                (fn [{:keys [value edit-key]}]
                  (if (:db/id value)
                    (put! sync
                      {:op :update
                       :data (select-keys value [:db/id edit-key])})
                    (put! sync
                       {:op :create :data value})))}})
            (om/build c/editable contact
              {:opts
               {:edit-key :person/last-name
                :on-edit
                (fn [{:keys [value edit-key]}]
                  (if (:db/id value)
                    (put! sync
                      {:op :update
                       :data (select-keys value [:db/id edit-key])})
                    (put! sync
                       {:op :create :data value})))}}))
          (contact-numbers (:person/telephone contact))
          (contact-addresses (:person/address contact)))))))


(defn contacts-view [contacts owner {:keys [current-contact sync]}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "contacts-view"}
        (dom/button
          #js {:id "add-contact"
               :className "button"
               :onClick (fn [e] (put! current-contact :new))}
          "Add contact")
        (apply dom/ul #js {:id "contacts-list"}
          (map (fn [p]
                 (let [id (:db/id p)]
                   (dom/li
                     #js {:onClick (fn [e] (put! current-contact id))}
                     (str (:person/last-name p) ", " (:person/first-name p)))))
               contacts))))))


(defn app-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:current-contact (chan)
       :sync (chan)})

    om/IWillMount
    (will-mount [_]

      ;; routes
      (defroute "/" []
        (om/update! app :route [:list-contacts]))

      (defroute "/:id" {id :id}
        (om/update! app :route [:view-contact id])
        ;; if page loads on a contact
        (when (and (= (:current-contact (om/value (om/get-props owner))) :none)
                   (not= id "new"))
          (put! (om/get-state owner :current-contact) id)))

      (.setEnabled history true)

      ;; routing loop
      (go (loop []
            (let [id (<! (om/get-state owner :current-contact))]
              (cond
               (= id :none)
               (do
                 (.setToken history "/")
                 (om/update! app :contacts
                   (vec (<! (util/edn-chan {:url "/contacts"})))))

               (= id :new)
               (do
                 (.setToken history "/new")
                 (om/update! app :current-contact
                   {:person/first-name "New Contact"
                    :person/last-name ""}))

               :else
               (let [contact (<! (util/edn-chan {:url (str "/contacts/" id)}))]
                 (.setToken history (str "/" id))
                 (om/update! app :current-contact contact))))
            (recur)))

       ;; sync loop
       (go (loop []
             (let [{:keys [op data]} (<! (om/get-state owner :sync))]
               (condp = op
                 :create
                 (let [data (<! (util/edn-chan
                                 {:method :post :url "/contacts"
                                  :data data}))]
                   (.setToken history (str "/" (:db/id data)))
                   (om/transact! app :current-contact #(merge % data)))

                 :update
                 (<! (util/edn-chan
                      {:method :put :url (str "/contacts/" (:db/id data))
                       :data (dissoc data :db/id)})))

               (recur)))))

    om/IRenderState
    (render-state [_ {:keys [current-contact sync]}]
      (let [route (:route app)
            opts  {:opts {:current-contact current-contact
                          :sync sync}}]
        (dom/div nil
          (case (first (:route app))
            :list-contacts (om/build contacts-view (:contacts app) opts)
            :view-contact  (om/build contact-view (:current-contact app) opts)))))))


(util/edn-xhr
  {:method :get
   :url "/contacts"
   :on-complete
   (fn [res]
     (swap! app-state assoc :contacts res)
     (om/root app-view app-state
       {:target (gdom/getElement "contacts")}))})
