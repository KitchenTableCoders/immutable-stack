(ns contacts.servlet
  (:gen-class
    :extends javax.servlet.http.HttpServlet)
  (:require contacts.servlet-context-listener)
  (:require [ring.util.servlet :refer [defservice]]))


(defn -init-void [this])

(defn wrapper-service [req]
  ((:handler @contacts.servlet-context-listener/servlet-system) req))

(defservice wrapper-service)





