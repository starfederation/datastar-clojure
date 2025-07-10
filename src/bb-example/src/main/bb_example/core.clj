(ns bb-example.core
  (:require
    [org.httpkit.server :as hk]))



(defonce !server (atom nil))

(defn stop! []
  (when-let [server @!server]
    (hk/server-stop! server)
    (reset! !server nil)))

(defn start! [handler]
  (stop!)
  (reset! !server (hk/run-server handler {:port 8080
                                          :legacy-return-value? false})))

