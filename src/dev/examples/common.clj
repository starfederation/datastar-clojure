(ns examples.common
  (:require
    [dev.onionpancakes.chassis.core :as h]
    [dev.onionpancakes.chassis.compiler :as hc]
    [starfederation.datastar.clojure.api :as d*]))


(defn page-scaffold [body]
  (hc/compile
    [[h/doctype-html5]
     [:html
      [:head
       [:meta {:charset "UTF-8"}]
       [:script {:type "module"
                 :src d*/CDN-url}]]
      [:body body]]]))

