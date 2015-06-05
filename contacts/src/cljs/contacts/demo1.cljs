(ns contacts.demo1
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as gdom]
            [goog.events :as events]
            [cljs.pprint :as pprint :refer [pprint]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [cljs.reader :as reader]
            [cljsjs.codemirror.mode.clojure]
            [cljsjs.codemirror.addons.matchbrackets])
  (:import [goog.events EventType]))

(defn log [x]
  (println) ;; flush past prompt
  (pprint x))

(defn fetch [q]
  (http/post "http://localhost:8081/query" {:transit-params q}))

(defn main []
  (let [ed (js/CodeMirror.fromTextArea (gdom/getElement "input")
             #js {:lineNumbers true
                  :matchBrackets true
                  :mode #js {:name "clojure"}})]
    (events/listen (gdom/getElement "submit") EventType.CLICK
      (fn [e]
        (go
          (set! (.-innerHTML (gdom/getElement "output"))
            (with-out-str
              (binding [pprint/*print-right-margin* 40]
                (pprint
                 (:body
                   (<! (fetch (reader/read-string (.getValue ed))))))))))))))

(when (gdom/getElement "demo1")
  (main))

(comment
  (let [c (fetch [{:app/contacts [:person/first-name]}])]
    (go (log (:body (<! c)))))
  )