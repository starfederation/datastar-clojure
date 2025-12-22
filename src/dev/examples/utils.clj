(ns examples.utils
  (:require
    [charred.api                         :as charred]
    [clojure.java.io                     :as io]
    [fireworks.core                      :refer [?]]
    [puget.printer                       :as pp]
    [starfederation.datastar.clojure.api :as d*]))


;; -----------------------------------------------------------------------------
;; Misc utils
;; -----------------------------------------------------------------------------
(defn- safe-requiring-resolve [sym]
  (try
    (deref (requiring-resolve sym))
    (catch Exception _ nil)))


(def original-out
  (or
    (:out (safe-requiring-resolve 'cider.nrepl.middleware.out/original-output))
    System/out))


(def original-err
  (or
    (:err (safe-requiring-resolve 'cider.nrepl.middleware.out/original-output))
    System/err))


(defmacro force-out
  "Binds [[*out*]] to the original system's out."
  [& body]
  `(binding [*out* (io/writer original-out)]
     ~@body))


(defn clear-terminal!
  "Clear the terminal of all text"
  []
  (force-out
    (print "\033c")
    (flush)))


(defn pp-request [req]
 (-> req
     (dissoc :reitit.core/match :reitit.core/router)
     pp/pprint
     pp/with-color))


(defn ?req [req]
  (? (dissoc req :reitit.core/match :reitit.core/router)))


(def ^:private bufSize 1024)
(def read-json (charred/parse-json-fn {:async? false :bufsize bufSize}))



(defn get-signals [req]
  (some-> req d*/get-signals read-json))


(defn rr [sym]
  (try
    (requiring-resolve sym)
    (catch Exception _
      nil)))

;; -----------------------------------------------------------------------------
;; httpkit server
;; -----------------------------------------------------------------------------
(defonce !hk-server (atom nil))

(def http-kit-run! (rr 'org.httpkit.server/run-server))
(def http-kit-stop! (rr 'org.httpkit.server/server-stop!))

(defn reboot-hk-server! [handler]
  (if-not http-kit-run!
    (println "http kit isn't in the classpath")
    (swap! !hk-server
           (fn [server]
             (when server
               (http-kit-stop! server))
             (http-kit-run! handler
                             {:port 8080
                              :legacy-return-value? false})))))

;; -----------------------------------------------------------------------------
;; ring jetty server
;; -----------------------------------------------------------------------------
(defonce !jetty-server (atom nil))

(def ring-jetty-run! (rr 'ring.adapter.jetty/run-jetty))


(defn reboot-jetty-server! [handler & {:as opts}]
  (if-not ring-jetty-run!
    (println "Ring jetty isn't in the classpath")
    (swap! !jetty-server
           (fn [server]
             (when server
               (.stop server))
             (ring-jetty-run! handler
                             (merge
                               {:port 8081
                                :join? false}
                               opts))))))

;; -----------------------------------------------------------------------------
;; rj9a server
;; -----------------------------------------------------------------------------
(defonce !rj9a-server (atom nil))

(def rj9a-run! (rr 'ring.adapter.jetty9/run-jetty))


(defn reboot-rj9a-server! [handler & {:as opts}]
  (if-not rj9a-run!
    (println "Ring jetty isn't in the classpath")
    (swap! !rj9a-server
           (fn [server]
             (when server
               (.stop server))
             (rj9a-run! handler
                        (merge
                          {:port 8082
                           :join? false}
                          opts))))))


