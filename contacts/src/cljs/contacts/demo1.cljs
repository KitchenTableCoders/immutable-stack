(ns contacts.demo1
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as gdom]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [cljsjs.codemirror.mode.clojure]
            [cljsjs.codemirror.addons.matchbrackets]
            [cljsjs.codemirror.addons.closebrackets]))

(defn main []
  (let [ed (js/CodeMirror.fromTextArea (gdom/getElement "input")
             #js {:lineNumbers true
                  :matchBrackets true
                  :closeBrackets true
                  :mode #js {:name "clojure"}})]
    (.log js/console ed)))

(when (gdom/getElement "demo1")
  (main))

(comment
  ()
  )