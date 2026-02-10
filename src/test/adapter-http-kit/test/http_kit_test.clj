(ns test.http-kit-test
  (:require
    [test.common                                      :as common]
    [test.persistent-connection                       :as pc]
    [test.smoketests                                  :as st]
    [test.examples.http-kit-handler                   :as hkh]
    [lazytest.core                                    :as lt :refer [defdescribe expect it]]
    [org.httpkit.server                               :as hk-server]
    [starfederation.datastar.clojure.adapter.http-kit :as hk-gen]))

;; -----------------------------------------------------------------------------
;; HTTP-Kit stuff
;; -----------------------------------------------------------------------------
(def http-kit-opts
  {:start! hk-server/run-server
   :stop!  hk-server/server-stop!
   :legacy-return-value? false})

;; -----------------------------------------------------------------------------
;; Tests
;; -----------------------------------------------------------------------------
(defdescribe smoke-tests
  {:webdriver true
   :context [(st/with-server-f (assoc http-kit-opts
                                      :handler hkh/handler))]}
  (it "manages signals"
    (doseq [[driver-type driver] common/drivers]
      (let [res (st/run-counters! @driver st/*port*)]
        (expect (= res st/expected-counters) (str driver-type)))))

  (it "manages forms"
    (doseq [[driver-type driver] common/drivers]
      (let [res (st/run-forms! @driver st/*port*)]
        (expect (= res st/expected-form-vals) (str driver-type))))))



(defdescribe smoke-tests-async
  {:webdriver true
   :context [(st/with-server-f (assoc http-kit-opts
                                      :handler hkh/handler
                                      :ring-async? true))]}
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
  (it "handles persistent connections"
    (let [res (pc/run-test (assoc http-kit-opts
                                  :->sse-response hk-gen/->sse-response))]
      (expect (map? res))
      (expect (pc/sse-status-ok? res))
      (expect (pc/sse-http1-headers-ok? res))
      (expect (pc/sse-body-ok? res)))))


(defdescribe persistent-sse-test-async
  (it "handles persistent connections"
    (let [res (pc/run-test (assoc http-kit-opts
                                  :ring-async? true
                                  :->sse-response hk-gen/->sse-response))]
      (expect (map? res))
      (pc/sse-status-ok? res)
      (pc/sse-http1-headers-ok? res)
      (pc/sse-body-ok? res))))
