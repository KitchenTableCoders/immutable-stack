(ns contacts.demo1
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as gdom]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]))

(defn main []
  )

(when (gdom/getElement "demo1")
  (main))