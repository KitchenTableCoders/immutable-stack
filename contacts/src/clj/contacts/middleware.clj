(ns contacts.middleware
  (:require [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.nio.charset StandardCharsets]))

(defn str->is [str]
  (ByteArrayInputStream. (.getBytes str StandardCharsets/UTF_8)))

(defn transit-request?
  [req]
  (if-let [^String type (:content-type req)]
    (not (empty? (re-find #"^application/transit+json" type)))))

(defprotocol TransitRead
  (transit-read [this]))

(extend-type String
  TransitRead
  (transit-edn [s]
    (transit/read (transit/reader (str->is s) :json))))

(extend-type java.io.InputStream
  TransitRead
  (transit-read [is]
    (transit/read (transit/reader is :json))))

(defn wrap-transit-params
  [handler]
  (fn [req]
    (if-let [body (and (transit-request? req) (:body req))]
      (let [transit-params (transit-read body)
            req* (assoc req
                   :transit-params transit-params
                   :params (merge (:params req) transit-params))]
        (handler req*))
      (handler req))))
