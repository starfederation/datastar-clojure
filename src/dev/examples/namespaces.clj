(ns examples.namespaces
  (:require
    [clojure.java.io :as io]
    [examples.common :as c]
    [dev.onionpancakes.chassis.core :as h]
    [dev.onionpancakes.chassis.compiler :as hc]
    [examples.utils :as u]
    [ring.util.response :as rur]
    [reitit.ring :as rr]
    [reitit.ring.middleware.parameters :as params]
    [starfederation.datastar.clojure.adapter.http-kit :refer [->sse-response on-open]]
    [starfederation.datastar.clojure.api :as d*]))

;; example taken from https://github.com/zigster64/datastar.http.zig/blob/d142e841620769fde0647fd032025b71622e70f4/examples/01_basic.zig
;; Here we patch svg/math elements using the d*/element-ns options
(defn r-int [low high]
  (+ low (rand-int (- high low))))


(defn random-rect []
  [:rect {:id :svg-rect
          :x (r-int 10 100)
          :y (r-int 10 100)
          :width (r-int 10 80)
          :height 80
          :fill "red" :stroke "black" :stroke-width 4
          :class "animate"}])


(defn random-circle []
   [:circle {:id :svg-circle
             :cx (r-int 10 100)
             :cy (r-int 10 100)
             :r (r-int 10 80)
             :fill "green" :stroke "black" :stroke-width 4
             :class "animate"}])


(defn random-polygon []
  (let [v #(r-int 50 300)]
    [:polygon {:id :svg-triangle
               :points (str (v) "," (v) " "
                            (v) "," (v) " "
                            (v) "," (v) " ")
               :fill "blue"
               :stroke "black"
               :stroke-width 4
               :class "animate"}]))

(defn random-img []
  [:svg {:width 500
         :height 300
         :viewBox "0 0 500 300"
         :xmlns "http://www.w3.org/2000/svg"}
   (random-rect)
   (random-polygon)
   (random-circle)])

(def math
  [:math {:id "math-ml" :display "block"}
    [:mrow
      [:mi "E"]
      [:mo "="]
      [:mi "m"]
      [:msup
        [:mi "c"]
        [:mn {:id "math-factor"} "2"]]]])

(def page
  (h/html
    (c/page-scaffold
      [:div
       [:style ".animate {transition: all 0.4s ease;}"]
       [:h1 "Namespaces tests"]
       [:div
        [:h2 "svg"]
        [:button {:data-on:click (d*/sse-post "/rand")} "random!!!"]
        (random-img)]
       [:div
        [:h2 "math"]
        [:button {:data-on:click (d*/sse-post "/rand-math")} "random!!!"]
        math]])))



(defn home
  ([_]
   (rur/response page))
  ([req respond _]
   (respond (home req))))


(defn merge-svg [req]
  (->sse-response req
    {on-open (fn [sse]
               (d*/with-open-sse sse
                 (d*/patch-elements-seq! sse
                                         [(h/html (random-rect))
                                          (h/html (random-circle))
                                          (h/html (random-polygon))]
                                         {d*/element-ns d*/ns-svg})))}))



(defn merge-math [req]
  (->sse-response req
    {on-open (fn [sse]
               (d*/with-open-sse sse
                 (d*/patch-elements! sse
                                     (h/html [:mn {:id "math-factor"} (rand-int 5)])
                                     {d*/element-ns d*/ns-svg})))}))


(def router
  (rr/router
    [["/" {:handler #'home}]
     ["/rand" {:handler #'merge-svg}]
     ["/rand-math" {:handler #'merge-math}]]))


(def handler
  (rr/ring-handler router
                   (rr/create-default-handler)
                   {:middleware [params/parameters-middleware]}))

(comment
  (u/reboot-hk-server! #'handler)
  (user/clear-terminal!))
