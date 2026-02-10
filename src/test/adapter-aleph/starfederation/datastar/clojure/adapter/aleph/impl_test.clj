(ns starfederation.datastar.clojure.adapter.aleph.impl-test
  (:require
    [manifold.stream                                     :as s]
    [starfederation.datastar.clojure.api                 :as d*]
    [starfederation.datastar.clojure.adapter.common-test :as ct :refer [read-bytes]]
    [starfederation.datastar.clojure.adapter.aleph       :as d*a]
    [starfederation.datastar.clojure.adapter.aleph.impl  :as impl]
    [starfederation.datastar.clojure.adapter.test        :as at]
    [lazytest.core                                       :as lt :refer [defdescribe describe it expect]]))


(def expected-event-result
  (d*/patch-elements! (at/->sse-gen) "event"))


(defn concat-byte-arrays [byte-arrays]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (doseq [v byte-arrays]
      (.writeBytes baos v))
    (ct/->ba baos)))


(defn get-res [!result opts]
  (if (or (d*a/write-profile opts) (:gzip? opts))
    (-> !result deref concat-byte-arrays (read-bytes opts))
    (-> !result deref first)))

(defn send-SSE-event [opts]
  (let [stream (s/stream)
        sse-gen (impl/->sse-gen stream opts)
        !result (atom [])]
    (s/consume (fn [v] (swap! !result conj v))stream)
    (with-open [_ sse-gen]
      (d*/patch-elements! sse-gen "event"))
    (expect (= expected-event-result
               (get-res !result opts)))))


(defdescribe simple-test
  (it "can send events with a using temp buffers"
    (send-SSE-event {}))

  (it "can send events with a using a persistent buffered reader"
    (send-SSE-event {d*a/write-profile d*a/buffered-writer-profile}))

  (it "can send gziped events with a using temp buffers"
    (send-SSE-event {d*a/write-profile d*a/gzip-profile
                     :gzip? true}))

  (it "can send gziped events with a using a persistent buffered reader"
    (send-SSE-event {d*a/write-profile d*a/gzip-buffered-writer-profile
                     :gzip? true})))


(comment
  (require '[lazytest.repl :as ltr])
  (ltr/run-test-var #'simple-test))
