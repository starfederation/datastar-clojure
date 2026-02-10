(ns examples.broadcast-aleph
  (:require
    [examples.utils :as u]
    [reitit.ring :as rr]
    [starfederation.datastar.clojure.api :as d*]
    [starfederation.datastar.clojure.adapter.aleph :refer [->sse-response on-open on-close]]))


;; Tiny setup for that allows broadcasting events to several curl processes

(defonce !conns (atom #{}))


(defn long-connection [req]
  (->sse-response req
    {on-open
     (fn [sse]
       (u/vthread
         (swap! !conns conj sse)
         (d*/console-log! sse "'connected'")))
     on-close
     (fn on-close [sse]
       (swap! !conns disj sse)
       (println "remove connection from pool"))}))


(def routes
  [["/persistent" {:handler #'long-connection}]])


(def router
  (rr/router routes))


(def default-handler (rr/create-default-handler))


(def handler
  (rr/ring-handler router
                   default-handler))

(defn broadcast-number! [n]
  (doseq [conn @!conns]
    (try
      (d*/console-log! conn (str "n: " n))
      (catch Exception e
        (println "Error: " e)))))


;; open several clients:
;; curl -vv http://localhost:8083/persistent
(comment
  (-> !conns deref first d*/close-sse!)
  (-> !conns deref first (d*/console-log! "toto"))
  (broadcast-number! (rand-int 25))
  (user/reload!)
  (u/clear-terminal!)
  (u/reboot-aleph-server! #'handler))
