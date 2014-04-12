(ns contacts.util
  (:require [goog.events :as events]
            [cljs.reader :as reader]
            [cljs.core.async :refer [chan put! close!]])
  (:import goog.net.EventType
           [goog.events EventType]
           [goog.net XhrIo]))

(def ^:private meths
  {:get "GET"
   :put "PUT"
   :post "POST"
   :delete "DELETE"})

(defn edn-xhr [{:keys [method url data on-complete]}]
  (let [xhr (XhrIo.)]
    (events/listen xhr goog.net.EventType.COMPLETE
      (fn [e]
        (on-complete (reader/read-string (.getResponseText xhr)))))
    (. xhr
      (send url (meths method) (when data (pr-str data))
        #js {"Content-Type" "application/edn"}))))

(defn edn-chan [opts]
  (let [opts (if-not (contains? opts :method)
               (assoc opts :method :get)
               opts)
        c    (chan)]
    (edn-xhr
      (assoc opts
        :on-complete (fn [edn] (put! c edn) (close! c))))
    c))
