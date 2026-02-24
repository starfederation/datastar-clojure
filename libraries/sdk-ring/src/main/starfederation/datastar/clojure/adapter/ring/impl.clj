(ns starfederation.datastar.clojure.adapter.ring.impl
  (:require
    [starfederation.datastar.clojure.adapter.common :as ac]
    [starfederation.datastar.clojure.protocols :as p]
    [starfederation.datastar.clojure.utils :as u]
    [ring.core.protocols :as rp])
  (:import
    [java.io  Closeable OutputStream]
    java.util.concurrent.locks.ReentrantLock))


(def default-write-profile ac/basic-profile)


(defn ->send [raw-os opts]
  (let [{wrap ac/wrap-output-stream
         write! ac/write!
         custom-flush ac/custom-flush} (ac/write-profile opts default-write-profile)
        wrapped-os (wrap raw-os)
        flush (if custom-flush
                custom-flush
                ac/simple-flush)]
    (fn
      ([]
       (.close ^Closeable wrapped-os))
     ([event-type data-lines event-opts]
      (write! wrapped-os event-type data-lines event-opts)
      (flush wrapped-os raw-os)))))


;; Note that the send! field has 2 usages:
;; - it stores the sending function
;; - it acts as a `is-open?` flag
;; Also the on-close not being nil means the callback hasn't been called yet.
(deftype SSEGenerator [^:unsynchronized-mutable send!
                       ^ReentrantLock lock
                       ^:unsynchronized-mutable on-close
                       ^:unsynchronized-mutable on-exception]
  rp/StreamableResponseBody
  (write-body-to-stream [this response output-stream]
    (let [opts (::opts response)
          on-open (ac/on-open opts)]

      ;; Set the SSEGenerator's state
      (set! send! (->send output-stream opts))
      (set! on-exception (or (ac/on-exception opts)
                             ac/default-on-exception))
      (when-let [cb (ac/on-close opts)]
        (set! on-close cb))

      ;; flush the HTTP headers as soon as possible
      (.flush ^OutputStream output-stream)

      ;; SSE connection is ready we call user code
      (on-open this)))

  p/SSEGenerator
  (send-event! [this event-type data-lines opts]
    (u/lock! lock
      (if send! ;; still open?
        (try
          (send! event-type data-lines opts)
          true ;; successful send
          (catch Exception e
            (when (on-exception this e {:sse-gen this
                                        :event-type event-type
                                        :data-lines data-lines
                                        :opts opts})
              (set! send! nil)
              (p/close-sse! this))
            false)) ;; the event wasn't sent
        false))) ; closed return false

  (get-lock [_] lock)

  (close-sse! [this]
    (u/lock! lock
      ;; If either send! or on-close are here we try to close them
      (if (or send! on-close)
        (let [res (ac/close-sse! #(when send! (send!))
                                 #(when on-close (on-close this)))]
          ;; We make sure to clean them up after closing
          (set! send! nil)
          (set! on-close nil)
          (if (instance? Exception res)
            (throw res)
            true))
        false)))

  (sse-gen? [_] true)

  Closeable
  (close [this]
    (p/close-sse! this)))

(defn ->sse-gen
  {:tag SSEGenerator}
  []
  (SSEGenerator. nil
                 (ReentrantLock.)
                 nil
                 nil))
