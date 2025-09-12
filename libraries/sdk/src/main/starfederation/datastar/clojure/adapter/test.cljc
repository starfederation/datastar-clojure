(ns starfederation.datastar.clojure.adapter.test
  "Utilities providing a test SSEGenerator and a mock `->sse-response`
  function."
  (:require
    [starfederation.datastar.clojure.adapter.common :as ac]
    [starfederation.datastar.clojure.api.sse :as sse]
    [starfederation.datastar.clojure.protocols :as p]
    [starfederation.datastar.clojure.utils :as u])
 (:import
    #?(:clj [java.io Closeable])
    java.lang.StringBuilder
    [java.util.concurrent.locks ReentrantLock]))



(deftype ReturnMsgGen []
  p/SSEGenerator
  (send-event! [_ event-type data-lines opts]
    (-> (StringBuilder.)
        (sse/write-event! event-type data-lines opts)
        str))

  (get-lock [_])

  (close-sse! [_])
  (sse-gen? [_] true))



(defn ->sse-gen
  "Returns a SSEGenerator that whose [[starfederation.datastar.clojure.protocols/send-event!]] implementation doesn't
  send anything but return the string event that be sent instead."
  [& _]
  (->ReturnMsgGen))




(deftype RecordMsgGen [lock !rec !open?]
  p/SSEGenerator
  (send-event! [_ event-type data-lines opts]
    (u/lock! lock
      (vswap! !rec conj (-> (StringBuilder.)
                            (sse/write-event! event-type data-lines opts)
                            str))))

  (get-lock [_] lock)

  (close-sse! [_]
    (u/lock! lock
      (vreset! !open? false)))

  (sse-gen? [_] true)

  #?@(:bb  []
      :clj [Closeable
            (close [this]
              (p/close-sse! this))]))


(defn ->sse-response
  "Fake a sse-response, it returns the ring response map with
  the `:status`, `:headers` and `:body`  keys sent.

  The events sent with sse-gen during the `on-open` callback are recorded in a
  vector stored in an atom. This atom is provided as the value for the
  response's `:body`."
  [req {on-open ac/on-open
        :keys [status headers]}]
  (let [
        !rec (volatile! [])
        sse-gen (->RecordMsgGen (ReentrantLock.)
                                !rec
                                (volatile! true))]
    (on-open sse-gen)
    {:status (or status 200)
     :headers (merge headers (sse/headers req))
     :body !rec}))

