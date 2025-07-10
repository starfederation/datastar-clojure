(ns bb-example.common
  (:require
    [cheshire.core                          :as json]
    [clojure.java.io                        :as io]
    [hiccup2.core                           :as h]
    [starfederation.datastar.clojure.api    :as d*]
    [starfederation.datastar.clojure.consts :as consts]))

(defn parse-signals [signals]
  (if (string? signals)
    (json/parse-string signals)
    (-> signals
        io/reader
        json/parse-stream)))


(defn get-signals [req]
  (some-> req d*/get-signals parse-signals))



(defn GET [path handler]
  {:path path
   :method :get
   :response handler})


(defn POST [path handler]
  {:path path
   :method :post
   :response handler})




(defn response [body]
  {:status 200
   :body body})



(def cdn-url
  (str "https://cdn.jsdelivr.net/gh/starfederation/datastar@"
       consts/version
       "/bundles/datastar.js"))



(defn page-scaffold [body]
  (h/html
    (h/raw "<!DOCTYPE html>")
    [:html
     [:head
      [:meta {:charset "UTF-8"}]
      [:script {:type "module"
                :src cdn-url}]]
     [:body body]]))
