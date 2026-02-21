(ns test.smoketests
  (:require
    [test.utils         :as u]
    [etaoin.api         :as ea]
    [test.examples.form :as ef]
    [lazytest.core      :as lt]))

;; -----------------------------------------------------------------------------
;; Fixture: Web server serving the site to smoke test
;; -----------------------------------------------------------------------------
(defn ->server
  "Make the server used for the test

  Opts keys:
  - `:start!`: start server
  - `:stop!`: stop the server
  - `:handler`: handler for the server
  - `:port`: optional port
  "
  [& {:keys [start! stop! handler port] :as opts}]
  (let [port (or port (u/free-port!))
        server (start! handler (assoc opts :port port))]
    {:server server
     :stop! #(stop! server)
     :port port}))


(def ^:dynamic *port* nil)


(defn with-server-f
  "Fixture starting and stopping a server. Binds [[*port*]] for the durations.

  opts keys:
  - `:start!`: start server
  - `:stop!`: stop the server
  - `:handler`: handler for the server
  - `:port`: optional port
   "
  [opts]
  (lt/around [f]
    (let [{:keys [stop! port]} (->server opts)]
      (binding [*port* port]
        (try
          (f)
          (finally
            (stop!)))))))


;; -----------------------------------------------------------------------------
;; Smoke test interactions and expected results
;; -----------------------------------------------------------------------------
(defn run-counters! [driver port]

  (ea/go driver (u/url port "counters/"))

  (ea/click driver :increment-1)
  (ea/click driver :increment-1)
  (ea/click driver :decrement-2)
  (ea/click driver :increment-3)


  {:get (ea/get-element-text driver :counter1)
   :post (ea/get-element-text driver :counter2)
   :signal (ea/get-element-text driver :counter3)})


(def expected-counters {:get "2"
                        :post "-1"
                        :signal "1"})


;; -----------------------------------------------------------------------------
(defn form-interactions! [driver msg button port]
  (ea/go driver (u/url port "form"))
  (ea/fill driver ef/input-id msg)
  (ea/click driver button)
  (ea/clear driver ef/input-id)
  (ea/get-element-text driver ef/form-result-id))


(defn run-forms! [driver port]
  {:get (form-interactions! driver "get" ef/get-button-id port)
   :post (form-interactions! driver "post" ef/post-button-id port)})


(def expected-form-vals
  {:get "get"
   :post "post"})
