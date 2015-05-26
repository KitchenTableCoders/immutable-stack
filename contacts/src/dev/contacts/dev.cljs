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
  (-query [this]
    '[:person/first-name :person/last-name
      {:person/telephone [:telephone/number]}])
  Object
  (render [this]
    (let [{:keys [:person/first-name :person/last-name] :as props}
          (first (om/props this))]
      (dom/div nil
        (dom/div nil
          (dom/label nil "Full Name:")
          (dom/span nil (str last-name ", " first-name)))
        (dom/div nil
          (dom/label nil "Number:")
          (dom/span nil
            (:telephone/number (first (:person/telephone props)))))))))

(def contact (om/create-factory Contact))

(defui ContactList
  static om/IQueryParams
  (-params [this]
    {:contact (om/query Contact)})
  static om/IQuery
  (-query [this]
    '[{:app/contacts ?contact}])
  Object
  (render [this]
    (let [{:keys [:app/contacts]} (om/props this)]
      (apply dom/ul nil
        (map #(dom/li nil (contact %)) contacts)))))

(def contact-list (om/create-factory ContactList))

(defn main []
  (let [c (http/post "http://localhost:8081/query"
            {:transit-params (om/query ContactList)})]
    (go
      (let [contacts (:body (<! c))]
        (js/React.render
          (contact-list contacts)
          (gdom/getElement "contacts"))))))

(comment
  (let [c (http/post "http://localhost:8081/query"
            {:transit-params (om/query ContactList)})]
    (go (println (:body (<! c)))))

  (main)

  ;; works
  (om/bind-query
    (om/-query ContactList)
    (om/-params ContactList))
  )