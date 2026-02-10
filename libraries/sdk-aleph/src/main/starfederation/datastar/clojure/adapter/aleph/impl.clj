(ns starfederation.datastar.clojure.adapter.aleph.impl
  (:require
    [manifold.stream                                :as s]
    [starfederation.datastar.clojure.adapter.common :as ac]
    [starfederation.datastar.clojure.protocols      :as p]
    [starfederation.datastar.clojure.utils          :as u])
  (:import
    java.lang.Exception
    [java.io ByteArrayOutputStream Closeable]
    [java.util.concurrent.locks ReentrantLock]))

(def basic-profile
  "Basic write profile using temporary [[StringBuilder]]s, no output stream and
  no compression."
  {ac/write! (ac/->build-event-str)})


;; -----------------------------------------------------------------------------
;; Sending events machinery
;; -----------------------------------------------------------------------------
(defn ->send-simple [stream write-profile]
  (let [write! (ac/write! write-profile)]
    (fn
      ([])
      ([event-type data-lines opts]
       (let [event (write! event-type data-lines opts)]
         (s/put! stream event))))))



(defn flush-baos! [^ByteArrayOutputStream baos stream]
  (let [msg (.toByteArray baos)]
    (.reset baos)
    (s/put! stream msg)))


(defn ->send-with-output-stream [stream write-profile]
  (let [^ByteArrayOutputStream baos (ByteArrayOutputStream.)
        {wrap-os ac/wrap-output-stream
         write! ac/write!} write-profile
        writer (wrap-os baos)]
    (fn
      ([]
       ;; Close the writer first to finish the gzip process
       (.close ^Closeable writer)
       ;; Flush towards SSE out
       (flush-baos! baos stream))
      ([event-type data-lines opts]
       (write! writer event-type data-lines opts)
       (ac/flush writer)
       (flush-baos! baos stream)))))



(defn ->send! [stream opts]
  (let [write-profile (or (ac/write-profile opts)
                          basic-profile)]
    (if (ac/wrap-output-stream write-profile)
      (->send-with-output-stream stream write-profile)
      (->send-simple stream write-profile))))


;; -----------------------------------------------------------------------------
;; SSE gen
;; -----------------------------------------------------------------------------
(deftype SSEGenerator [stream
                       lock
                       send!
                       on-exception
                       ^:unsynchronized-mutable on-close]
  p/SSEGenerator
  (send-event! [this event-type data-lines opts]
    (u/lock! lock
      (try
        (send! event-type data-lines opts)
        (catch Exception e
          (when (on-exception this e {:sse-gen this
                                      :event-type event-type
                                      :data-lines data-lines
                                      :opts opts})
            (p/close-sse! this))
          false))))

  (get-lock [_] lock)

  (close-sse! [this]
    (u/lock! lock
      (if on-close
        (let [closing-res (ac/close-sse! #(do (send!))
                                         #(when on-close (on-close this)))]

          ;; on-close serves as a sentinel, that way when
          ;; the user close the SSEGenerator is closed (as opposed to the browser)
          ;; we don't close again when the manifold stream on-closed callback is called
          (set! on-close nil)
          (s/close! stream)
          (if (instance? Exception closing-res)
            (throw closing-res)
            closing-res))
        false)))

  (sse-gen? [_] true)

  Closeable
  (close [this]
    (p/close-sse! this)))


(defn ->sse-gen
  {:tag SSEGenerator}
  [stream opts]
  (let [{user-on-close     ac/on-close
         on-exception ac/on-exception
         :or {on-exception ac/default-on-exception
              user-on-close (constantly false)}} opts
        sse-gen (SSEGenerator. stream
                               (ReentrantLock.)
                               (->send! stream opts)
                               on-exception
                               user-on-close)]
    (s/on-closed stream #(p/close-sse! sse-gen))
    sse-gen))
