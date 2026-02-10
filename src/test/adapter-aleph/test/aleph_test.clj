(ns test.aleph-test
  (:require
    [test.common                                   :as common]
    [test.smoketests                               :as st]
    [test.persistent-connection                    :as pc]
    [test.examples.aleph-handler                   :as aleph-handler]
    [lazytest.core                                 :as lt :refer [defdescribe expect it]]
    [aleph.http                                    :as aleph]
    [starfederation.datastar.clojure.adapter.aleph :as aleph-gen])
  (:import
    java.io.Closeable))

;; -----------------------------------------------------------------------------
;; Aleph config
;; -----------------------------------------------------------------------------
(def aleph-opts
  {:start! aleph/start-server
   :stop!  (fn [^Closeable server] (.close server))})


(defdescribe smoke-tests
  {:webdriver true
   :context [(st/with-server-f (assoc aleph-opts
                                      :handler aleph-handler/handler))]}

  (it "manages signals"
    (doseq [[driver-type driver] common/drivers]
      (let [res (st/run-counters! @driver st/*port*)]
        (expect (= res st/expected-counters) (str driver-type)))))

  (it "manages forms"
    (doseq [[driver-type driver] common/drivers]
      (let [res (st/run-forms! @driver st/*port*)]
        (expect (= res st/expected-form-vals) (str driver-type))))))


;; -----------------------------------------------------------------------------
;; Tests
;; -----------------------------------------------------------------------------

(defdescribe persistent-sse-test
  (it "handles persistent connections"
    (let [res (pc/run-test (assoc aleph-opts
                                  :->sse-response aleph-gen/->sse-response))]
      (expect (map? res))
      (expect (pc/sse-status-ok? res))
      (expect (pc/sse-http1-headers-ok? res))
      (expect (pc/sse-body-ok? res)))))

(comment
  (user/reload!)
  (require '[lazytest.repl :as ltr])
  (ltr/run-tests '[test.aleph-test])
  (ltr/run-test-var #'persistent-sse-test)
  :dbg
  :rec)
