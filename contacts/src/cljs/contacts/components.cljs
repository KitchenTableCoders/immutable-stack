(ns contacts.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))


(defn handle-change [e owner]
  (om/set-state! owner :edit-text (.. e -target -value)))


(defn end-edit [data edit-key owner cb]
  (om/set-state! owner :editing false)
  (om/update! data edit-key (om/get-state owner :edit-text))
  (cb {:value @data :edit-key edit-key}))


(defn editable [data owner {:keys [edit-key on-edit] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false
       :edit-text ""})
    om/IRenderState
    (render-state [_ {:keys [edit-text editing]}]
      (let [text (get data edit-key)]
        (dom/li nil
          (dom/span #js {:style (display (not editing))} text)
          (dom/input
            #js {:style (display editing)
                 :value edit-text
                 :onChange #(handle-change % owner)
                 :onKeyPress #(when (and (om/get-state owner :editing)
                                         (== (.-keyCode %) 13))
                                (end-edit data edit-key owner on-edit))
                 :onBlur (fn [e]
                           (when (om/get-state owner :editing)
                             (end-edit data edit-key owner on-edit)))})
          (dom/button
            #js {:style (display (not editing))
                 :onClick (fn [e]
                            (om/set-state! owner :edit-text text)
                            (om/set-state! owner :editing true))}
            "Edit"))))))
