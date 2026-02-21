(ns test.persistent-connection
  (:require
    [clojure.string                                 :as string]
    [lazytest.core                                  :as lt]
    [lazytest.extensions.matcher-combinators        :as mc]
    [org.httpkit.client                             :as http]
    [starfederation.datastar.clojure.adapter.test   :as test-gen]
    [starfederation.datastar.clojure.adapter.common :as ac]
    [starfederation.datastar.clojure.api            :as d*]
    [starfederation.datastar.clojure.api.sse        :as sse]
    [test.utils                                     :as u]))

(defn- ->persistent-sse-handler
  "Make a ring handler that puts a sse gen into an promise for use later."
  [->sse-response !conn]
  (fn handler
    ([req]
     (->sse-response req
       {ac/on-open (fn [sse-gen] (deliver !conn sse-gen))}))
    ([req respond _raise]
     (respond (handler req)))))


(defn ->server
  "Make the server used for the test
   Opts keys:
  - `:start!`: start server
  - `:stop!`: stop the server
  - `:->sse-response`: function for the specific adapter
  - `:wrap`: middleware for the handler
  - `:port`: optional port
  "
  [& {:keys [start! stop! ->sse-response wrap port] :as opts}]
  (let [!conn (promise)
        handler (cond-> (->persistent-sse-handler ->sse-response !conn) wrap wrap)
        port (or port (u/free-port!))
        server (start! handler (assoc opts :port port))]
    {:server server
     :port port
     :!conn !conn
     :stop #(stop! server)}))

(defn sse-interaction! [sse-gen]
  (d*/patch-elements! sse-gen "1")
  (d*/patch-elements! sse-gen "2")
  (d*/close-sse! sse-gen))


(defn run-test
  "Opts keys:
  - `:start!`: start server
  - `:stop!`: stop the server
  - `:->sse-response`: function for the specific adapter
  - `:wrap`: middleware for the handler
  - `:port`: optional port
  - other: options for the server
  "
  [opts]
  (let [{:keys [port !conn stop]} (->server opts)
        response (http/request {:url (u/url port "")})
        sse-gen (deref !conn 1000 nil)]
    (try
      (when-not sse-gen
        (throw (ex-info "The handler did not deliver the persistent sse-gen." {})))
      (sse-interaction! sse-gen)
      (deref response 1000 :timeout)
      (finally
        (stop)))))

(comment
  (require '[aleph.http                                    :as aleph]
           '[starfederation.datastar.clojure.adapter.aleph :as d*a])
  (def opts
    {:start! aleph/start-server
     :stop! #(do (.close %))
     :->sse-response d*a/->sse-response})

  (def response (run-test opts))

  (sse-status-ok? response)
  (sse-http1-headers-ok? response)
  (sse-body-ok? response))

;; -----------------------------------------------------------------------------
;; Expectations
;; -----------------------------------------------------------------------------
(defn sse-status-ok? [response]
  (lt/expect (= (:status response) 200)))


(defn ->headers [req]
  (-> req
      sse/headers
      (update-keys (comp keyword string/lower-case))))


(def SSE-headers-1-dot-1 (->headers {:protocol "HTTP/1.1"}))

(defn sse-http1-headers-ok? [response]
  (lt/expect (mc/match? SSE-headers-1-dot-1 (:headers response))))


(def expected-p-sse-res-body
  (-> (test-gen/->sse-recorder)
      (doto (sse-interaction!))
      :!rec
      deref
      (->> (apply str))))

(defn sse-body-ok? [response]
  (lt/expect (= (:body response) expected-p-sse-res-body)))
