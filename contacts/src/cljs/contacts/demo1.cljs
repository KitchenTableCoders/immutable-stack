(ns contacts.demo1
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as gdom]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [cljsjs.codemirror.mode.clojure]))

(defn main []
  (let [ed (js/CodeMirror.fromTextArea (gdom/getElement "input")
             #js {:lineNumbers true
                  :matchBrackets true
                  :mode #js {:name "clojure"}})]
    (.log js/console ed)))

(when (gdom/getElement "demo1")
  (main))

(comment
  ()
  )