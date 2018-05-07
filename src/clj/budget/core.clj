(ns budget.core
  (:require
   [ring.middleware
    [defaults :refer [site-defaults wrap-defaults]]]
   [compojure.core :as c :refer [defroutes GET POST ANY]]
   [compojure.route :as r]
   [hiccup.page :refer [html5]]

   ,,,))

(defroutes all-routes

  (GET "/" []
       (html5
        [:head
         [:title "test"]]
        [:body "Hello from clojure"]))

  (r/resources "/")

  (r/not-found
   (html5 "not found")))
