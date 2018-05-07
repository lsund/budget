(ns budget.core
  (:require
   [ring.middleware
    [defaults :refer [site-defaults wrap-defaults]]]
   [compojure.core :as c :refer [defroutes GET POST ANY]]
   [compojure.route :as r]
   [hiccup.page :refer [html5 include-css include-js]]

   ,,,))

(defroutes all-routes

  (GET "/" []
       (html5
        [:head
         [:title "test"]
         (apply include-js  ["/js/compiled/budget.js"])
         (apply include-css ["/css/style.css"])]
      [:body
         [:div#cljs-target]]))

  (r/resources "/")

  (r/not-found
   (html5 "not found")))
