(ns hello-httpkit
  (:require
   [charred.api]
   [clojure.java.io :as io]
   [dev.onionpancakes.chassis.compiler :as hc]
   [dev.onionpancakes.chassis.core :as h]
   [org.httpkit.server]
   [reitit.ring.middleware.parameters]
   [reitit.ring]
   [ring.util.response]
   [starfederation.datastar.clojure.adapter.http-kit :refer [->sse-response on-open]]
   [starfederation.datastar.clojure.api :as d*]))

(def read-json (charred.api/parse-json-fn {:async? false :bufsize 1024}))

(defn get-signals [req]
  (-> req d*/get-signals read-json))

(def home-page
  (slurp (io/resource "hello-world.html")))

(defn home [_]
  (-> home-page
      (ring.util.response/response)
      (ring.util.response/content-type "text/html")))

(def message "Hello, world!")

(defn ->frag [i]
  (h/html
   (hc/compile
    [:div {:id "message"}
     (subs message 0 (inc i))])))

(defn hello-world [request]
  (let [d (-> request get-signals (get "delay") int)]
    (->sse-response request
                    {on-open
                     (fn [sse]
                       (d*/with-open-sse sse
                         (dotimes [i (count message)]
                           (d*/patch-elements! sse (->frag i))
                           (Thread/sleep d))))})))

(def routes
  [["/" {:handler home}]
   ["/hello-world" {:handler hello-world
                    :middleware [reitit.ring.middleware.parameters/parameters-middleware]}]])

(def router (reitit.ring/router routes))

(def handler (reitit.ring/ring-handler router))

;; ------------------------------------------------------------
;; Server
;; ------------------------------------------------------------
(defonce !server (atom nil))

(defn stop! []
  (if-let [s @!server]
    (do (org.httpkit.server/server-stop! s)
        (reset! !server nil))
    (throw (ex-info "Server not running" {}))))

(defn start! [handler opts]
  (when-not (nil? @!server)
    (stop!))
  (reset! !server
          (org.httpkit.server/run-server
           handler
           (merge {:port 8080}
                  opts
                  {:legacy-return-value? false}))))

(comment
  (stop!)
  (start! #'handler {})
  )

;; ------------------------------------------------------------
;; Main
;; ------------------------------------------------------------
(defn -main [& _]
  (start! #'handler {:port 8080})
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(do (stop!) (shutdown-agents)))))
