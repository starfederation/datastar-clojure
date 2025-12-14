(ns starfederation.datastar.clojure.adapter.ring.impl-test
  (:require
    [lazytest.core :as lt :refer [defdescribe it expect]]
    [ring.core.protocols :as p]
    [starfederation.datastar.clojure.adapter.common :as ac]
    [starfederation.datastar.clojure.adapter.ring.impl :as impl]
    [starfederation.datastar.clojure.adapter.test :as at]
    [starfederation.datastar.clojure.api :as d*]
    [starfederation.datastar.clojure.adapter.common-test :refer [read-bytes]])
  (:import
    [java.io ByteArrayOutputStream]))


;; -----------------------------------------------------------------------------
;; Basic sending of a SSE event without any server
;; -----------------------------------------------------------------------------
(def expected-event-result
  (d*/patch-elements! (at/->sse-gen) "msg"))

(defn send-SSE-event [response]
  (let [response (assoc-in response [::impl/opts ac/on-open] (fn [_sse]))
        baos (ByteArrayOutputStream.)]
    (with-open [sse-gen (impl/->sse-gen)
                baos baos]
      (p/write-body-to-stream sse-gen response baos)
      (d*/patch-elements! sse-gen "msg" {}))

    (expect
      (= (read-bytes baos (::impl/opts response))
         expected-event-result))))

(defdescribe simple-test
  (it "can send events with a using temp buffers"
    (send-SSE-event {}))

  (it "can send events with a using a persistent buffered reader"
    (send-SSE-event {::impl/opts {ac/write-profile ac/buffered-writer-profile}}))

  (it "can send gziped events with a using temp buffers"
    (send-SSE-event {::impl/opts {ac/write-profile ac/gzip-profile
                                  :gzip? true}}))

  (it "can send gziped events with a using a persistent buffered reader"
    (send-SSE-event {::impl/opts {ac/write-profile ac/gzip-buffered-writer-profile
                                  :gzip? true}})))




(comment
  (user/reload!)
  (require '[lazytest.repl :as ltr])
  (ltr/run-test-var #'simple-test))
