(ns contacts.demo2
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as gdom]
            [goog.object :as object]
            [cljs.pprint :refer [pprint]]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]))

(defn log [x]
  (println) ;; flush past prompt
  (pprint x))

(defn fetch [q]
  (http/post "http://localhost:8081/query" {:transit-params q}))

(defn label+span [label-text span-text]
  (dom/div nil
    (dom/label nil label-text)
    (dom/span nil span-text)))

(defui AddressInfo
  static om/IQuery
  (query [this]
    '[:address/street :address/city :address/zipcode])
  Object
  (render [this]
    (let [{:keys [:address/street :address/city
                  :address/state :address/zipcode]}
          (om/props this)]
      (dom/div nil
        (dom/div nil street)
        (dom/div nil (str city ", " state " " zipcode))))))

(def address-info (om/create-factory AddressInfo))

(defui Contact
  static om/IQuery
  (query [this]
    '[:person/first-name :person/last-name
      {:person/telephone [:telephone/number]}])
  Object
  (render [this]
    (let [{:keys [:person/first-name :person/last-name] :as props}
          (om/props this)]
      (dom/div nil
        (label+span "Full Name:"
          (str last-name ", " first-name))
        (label+span "Number:"
          (:telephone/number (first (:person/telephone props))))))))

(def contact (om/create-factory Contact))

(defui ContactList
  static om/IQueryParams
  (params [this]
    {:contact (om/query Contact)})
  static om/IQuery
  (query [this]
    '[{:app/contacts ?contact}])
  Object
  (render [this]
    (let [{:keys [:app/contacts]} (om/props this)]
      (dom/div nil
        (dom/h3 nil "Contacts")
        (apply dom/ul nil
          (map #(dom/li nil (contact %)) contacts))))))

(def contact-list (om/create-factory ContactList))

(defn main []
  (let [c (fetch (om/get-query ContactList))]
    (go
      (let [contacts (:body (<! c))]
        (js/React.render
          (contact-list contacts)
          (gdom/getElement "demo2"))))))

(when (gdom/getElement "demo2")
  (main))

(comment
  (require '[cljs.pprint :as pprint])

  (let [c (fetch [{:app/contacts [:person/first-name]}])]
    (go (log (:body (<! c)))))

  (let [c (fetch [{:app/contacts
                   [:person/first-name
                    :person/last-name]}])]
    (go (log (:body (<! c)))))

  (let [c (fetch [{:app/contacts
                   [:person/first-name
                    :person/last-name
                    {:person/telephone [:telephone/number]}]}])]
    (go (log (:body (<! c)))))

  (main)

  ;; works
  (om/bind-query
    (om/-query ContactList)
    (om/-params ContactList))
  )