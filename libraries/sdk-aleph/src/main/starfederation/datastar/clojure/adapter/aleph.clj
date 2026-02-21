(ns starfederation.datastar.clojure.adapter.aleph
  (:require
    [manifold.stream                                       :as stream]
    [starfederation.datastar.clojure.adapter.common        :as ac]
    [starfederation.datastar.clojure.adapter.aleph.impl    :as impl]
    [starfederation.datastar.clojure.utils                 :refer [def-clone]]))

(def-clone on-open ac/on-open)
(def-clone on-close ac/on-close)
(def-clone on-exception ac/on-exception)
(def-clone default-on-exception ac/default-on-exception)


(def-clone write-profile ac/write-profile)

(def-clone basic-profile                impl/basic-profile)
(def-clone buffered-writer-profile      ac/buffered-writer-profile)
(def-clone gzip-profile                 ac/gzip-profile)
(def-clone gzip-buffered-writer-profile ac/gzip-buffered-writer-profile)


(defn ->sse-response
  "Make a Ring response map that will start a SSE stream.

  BE CAREFUL with your `on-open` callback, it is called synchronously in the
  handler. If it blocks it will block the sending of the response.
  Offload work that takes time to a separate vthread for instance.

  Specific SSE headers are set automatically, the user provided ones will be
  merged.

  Note that the SSE connection stays opened until you close it.


  General options:
  - `:status`: status for the HTTP response, defaults to 200.
  - `:headers`: ring headers map to add to the response.
  - [[on-open]]: mandatory callback called when the generator is ready to send.
  - [[on-close]]: callback called when the underlying SSE-gen or the underlying
    manifold stream is closed.
  - [[on-exception]]: callback called when sending a SSE event throws.
  - [[write-profile]]: write profile for the connection.
    Defaults to [[basic-profile]]

  SDK provided write profiles:
  - [[basic-profile]]
  - [[buffered-writer-profile]]
  - [[gzip-profile]]
  - [[gzip-buffered-writer-profile]]

  You can also take a look at the `starfederation.datastar.clojure.adapter.common`
  namespace if you want to write your own profiles.
  "
  [ring-request {:keys [status] :as opts}]
  {:pre [(ac/on-open opts)]}
  (let [on-open-cb (ac/on-open opts)
        stream (stream/stream)
        sse-gen (impl/->sse-gen stream opts)]
    (on-open-cb sse-gen)
    {:status (or status 200)
     :headers (ac/headers ring-request opts)
     :body stream}))
