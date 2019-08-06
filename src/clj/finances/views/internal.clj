(ns finances.views.internal
  (:require [finances.html :as html]
            [hiccup.page :refer [html5 include-css include-js]]))

(defmulti render
  (fn [x & _] x))

(defn layout [config options body]
  (html5
   [:head [:title (str "Finances - " (:title options))]]
   [:body.mui-container
    (html/navbar)
    body
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))
