(ns contacts.middleware
  (:require [ring.util.response :refer :all]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayOutputStream]))

(defn- write [x t opts]
  (let [baos (ByteArrayOutputStream.)
        w    (transit/writer baos t opts)
        _    (transit/write w x)
        ret  (.toString baos)]
    (.reset baos)
    ret))

(defn- transit-request? [request]
  (if-let [type (:content-type request)]
    (let [mtch (re-find #"^application/transit\+(json|msgpack)" type)]
      [(not (empty? mtch)) (keyword (second mtch))])))

(defn- read-transit [request {:keys [opts]}]
  (let [[res t] (transit-request? request)]
    (if res
      (if-let [body (:body request)]
        (let [rdr (transit/reader body t opts)]
          (try
            [true (transit/read rdr)]
            (catch Exception ex
              [false nil])))))))

(def ^{:doc "The default response to return when a Transit request is malformed."}
default-malformed-response
  {:status  400
   :headers {"Content-Type" "text/plain"}
   :body    "Malformed Transit in request body."})

(defn wrap-transit-body
  "Middleware that parses the body of Transit request maps, and replaces the :body
  key with the parsed data structure. Requests without a Transit content type are
  unaffected.
  Accepts the following options:
  :keywords?          - true if the keys of maps should be turned into keywords
  :opts               - a map of options to be passed to the transit reader
  :malformed-response - a response map to return when the JSON is malformed"
  {:arglists '([handler] [handler options])}
  [handler & [{:keys [malformed-response]
               :or {malformed-response default-malformed-response}
               :as options}]]
  (fn [request]
    (if-let [[valid? transit] (read-transit request options)]
      (if valid?
        (handler (assoc request :body transit))
        malformed-response)
      (handler request))))

(defn- assoc-transit-params [request transit]
  (let [request (assoc request :transit-params transit)]
    (if (map? transit)
      (update-in request [:params] merge transit)
      request)))

(defn wrap-transit-params
  "Middleware that parses the body of Transit requests into a map of parameters,
  which are added to the request map on the :transit-params and :params keys.
  Accepts the following options:
  :malformed-response - a response map to return when the JSON is malformed
  :opts               - a map of options to be passed to the transit reader
  Use the standard Ring middleware, ring.middleware.keyword-params, to
  convert the parameters into keywords."
  {:arglists '([handler] [handler options])}
  [handler & [{:keys [malformed-response]
               :or {malformed-response default-malformed-response}
               :as options}]]
  (fn [request]
    (if-let [[valid? transit] (read-transit request options)]
      (if valid?
        (handler (assoc-transit-params request transit))
        malformed-response)
      (handler request))))

(defn wrap-transit-response
  "Middleware that converts responses with a map or a vector for a body into a
  Transit response.
  Accepts the following options:
  :encoding - one of #{:json :json-verbose :msgpack}
  :opts     - a map of options to be passed to the transit writer"
  {:arglists '([handler] [handler options])}
  [handler & [{:as options}]]
  (let [{:keys [encoding opts] :or {encoding :json}} options]
    (assert (#{:json :json-verbose :msgpack} encoding) "The encoding must be one of #{:json :json-verbose :msgpack}.")
    (fn [request]
      (let [response (handler request)]
        (if (coll? (:body response))
          (let [transit-response (update-in response [:body] write encoding opts)]
            (if (contains? (:headers response) "Content-Type")
              transit-response
              (content-type transit-response (format "application/transit+%s; charset=utf-8" (name encoding)))))
          response)))))

;(ns contacts.middleware
;  (:require [cognitect.transit :as transit])
;  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
;           [java.nio.charset StandardCharsets]))
;
;(defn str->is [str]
;  (ByteArrayInputStream. (.getBytes str StandardCharsets/UTF_8)))
;
;(defn transit-request?
;  [req]
;  (if-let [^String type (:content-type req)]
;    (not (empty? (re-find #"^application/transit+json" type)))))
;
;(defprotocol TransitRead
;  (transit-read [this]))
;
;(extend-type String
;  TransitRead
;  (transit-edn [s]
;    (transit/read (transit/reader (str->is s) :json))))
;
;(extend-type java.io.InputStream
;  TransitRead
;  (transit-read [is]
;    (transit/read (transit/reader is :json))))
;
;(defn wrap-transit-params
;  [handler]
;  (fn [req]
;    (if-let [body (and (transit-request? req) (:body req))]
;      (let [transit-params (transit-read body)
;            req* (assoc req
;                   :transit-params transit-params
;                   :params (merge (:params req) transit-params))]
;        (handler req*))
;      (handler req))))
