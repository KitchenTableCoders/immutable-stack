(ns contacts.dev
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl :as repl]
            [cljs-http.client :as http]
            [cljs.pprint :as pprint :refer [pprint]]
            [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defonce conn
  (repl/connect "http://localhost:9000/repl"))

(defn log [x]
  (println) ;; flush past prompt
  (pprint x))

(defui Contact
  static om/IQuery
  (queries [this]
    '{:self [:person/first-name :person/last-name]})
  Object
  (render [this]
    (let [{:keys [:person/first-name :person/last-name]}
          (:self (om/props this))]
      (dom/div nil
        (str last-name ", " first-name)))))

(def contact (om/create-factory Contact))

(defui ContactList
  static om/IQueryParams
  (params [this]
    {:contacts {:contact (om/complete-query Contact)}})
  static om/IQuery
  (queries [this]
    '{:contacts ?contact})
  Object
  (render [this]
    (apply dom/ul nil
      (map #(dom/li nil (contact %)) (om/props this)))))

(def contact-list (om/create-factory ContactList))

(defn main []
  (let [c (http/post "http://localhost:8081/contacts"
            {:transit-params {:selector (om/complete-query ContactList)}})]
    (go
      (js/React.render
        (contact-list (:body (<! c)))
        (gdom/getElement "contacts")))))

(comment
  (let [c (http/post "http://localhost:8081/contacts"
            {:transit-params {:selector (om/complete-query Contact)}})]
    (go (println (:body (<! c)))))

  (main)

  (om/get-query ContactList :contacts)

  ;; works
  (om/bind-query
    (:contacts (om/queries ContactList))
    (:contacts (om/params ContactList)))
  )