(ns bb-example.animation
  (:require
    [bb-example.animation.broadcast                   :as broadcast]
    [bb-example.animation.handlers                    :as handlers]
    [bb-example.animation.rendering                   :as rendering]
    [bb-example.animation.state                       :as state]
    [bb-example.common                                :as c]
    [bb-example.core                                  :as core]
    [ruuter.core                                      :as ruuter]
    [ring.middleware.params                           :as r-params]
    [starfederation.datastar.clojure.adapter.http-kit :as hk-gen]

    [starfederation.datastar.clojure.adapter.http-kit-schemas]
    [starfederation.datastar.clojure.api-schemas]))

;; This example let's use play with fat updates and compression
;; to get an idea of the gains compression can help use achieve
;; in terms of network usage.
(broadcast/install-watch!)

(def routes
  [(c/GET "/"          handlers/home-handler)
   (c/GET "/ping/:id"  (r-params/wrap-params handlers/ping-handler))
   (c/GET "/random-10" handlers/random-pings-handler)
   (c/GET "/reset"     handlers/reset-handler)
   (c/GET "/step1"     handlers/step-handler)
   (c/GET "/play"      handlers/play-handler)
   (c/GET "/pause"     handlers/pause-handler)
   (c/GET "/refresh"   handlers/refresh-handler)
   (c/POST "/resize"   (r-params/wrap-params handlers/resize-handler))
   (c/GET "/updates"   (handlers/->updates-handler
                         {hk-gen/write-profile hk-gen/gzip-profile}))])

(defn handler [req]
  (ruuter/route routes req))


(defn clear-terminal! []
  (binding [*out* (java.io.PrintWriter. System/out)]
    (print "\033c")
    (flush)))



(comment
  (clear-terminal!)
  (core/start! #'handler)
  *e
  state/!state
  state/!conns
  (reset! state/!conns #{})

  (-> state/!state
      deref
      rendering/page)
  (state/resize! 10 10)
  (state/resize! 20 20)
  (state/resize! 25 25)
  (state/resize! 30 30)
  (state/resize! 50 50)
  (state/reset-state!)
  (state/add-random-pings!)
  (state/step-state!)
  (state/start-animating!))


