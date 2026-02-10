(ns test.ring-jetty-test
  (:require
    [test.common                                  :as common]
    [test.persistent-connection                   :as pc]
    [test.smoketests                              :as st]
    [test.examples.ring-handler                   :as rh]
    [lazytest.core                                :as lt :refer [defdescribe expect it]]
    [ring.adapter.jetty                           :as jetty]
    [starfederation.datastar.clojure.adapter.ring :as jetty-gen])
  (:import
    [org.eclipse.jetty.server Server ServerConnector]))

;; -----------------------------------------------------------------------------
;; Ring Jetty stuff
;; -----------------------------------------------------------------------------
(defn stop-jetty! [^Server server]
  (.stop server))


(def ring-jetty-opts
  {:start! jetty/run-jetty
   :stop!  stop-jetty!
   :join? false})


;; -----------------------------------------------------------------------------
;; Tests
;; -----------------------------------------------------------------------------
(defdescribe smoke-tests
  {:webdriver true
   :context [(st/with-server-f (assoc ring-jetty-opts
                                      :handler rh/handler))]}
  (it "manages signals"
    (doseq [[driver-type driver] common/drivers]
      (let [res (st/run-counters! @driver st/*port*)]
        (expect (= res st/expected-counters) (str driver-type)))))

  (it "manages forms"
    (doseq [[driver-type driver] common/drivers]
      (let [res (st/run-forms! @driver st/*port*)]
        (expect (= res st/expected-form-vals) (str driver-type))))))


(defdescribe counters-async-test
  {:webdriver true
   :context [(st/with-server-f (assoc ring-jetty-opts
                                      :handler rh/handler
                                      :async? true))]}
  (it "manages signals"
    (doseq [[driver-type driver] common/drivers]
      (let [res (st/run-counters! @driver st/*port*)]
        (expect (= res st/expected-counters) (str driver-type)))))

  (it "manages forms"
    (doseq [[driver-type driver] common/drivers]
      (let [res (st/run-forms! @driver st/*port*)]
        (expect (= res st/expected-form-vals) (str driver-type))))))


;; -----------------------------------------------------------------------------
(defdescribe persistent-sse-test
  "Testing persistent connection, events are sent from outside the ring handler."
  (it "handles persistent connections"
    (let [res (pc/run-test (assoc ring-jetty-opts
                                  :->sse-response jetty-gen/->sse-response
                                  :async? true))]
      (expect (map? res))
      (pc/sse-status-ok? res)
      (pc/sse-http1-headers-ok? res)
      (pc/sse-body-ok? res))))


(comment
  (require '[lazytest.repl :as ltr])
  (ltr/run-tests '[test.ring-jetty-test]))
