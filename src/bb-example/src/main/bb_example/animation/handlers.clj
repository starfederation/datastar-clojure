(ns bb-example.animation.handlers
  (:require
    [bb-example.animation.rendering                 :as rendering]
    [bb-example.animation.state                     :as state]
    [bb-example.common                              :as c]
    [starfederation.datastar.clojure.adapter.http-kit :as hk-gen]))


(defn home-handler
  ([_]
   (c/response (str (rendering/page @state/!state))))
  ([req respond _raise]
   (respond
     (home-handler req))))


(defn ->updates-handler [{:as opts}]
  (fn updates-handler [req]
   (hk-gen/->sse-response req
     (merge opts
       {hk-gen/on-open
        (fn [sse]
          (state/add-conn! sse))
        hk-gen/on-close
        (fn on-close
          ([sse]
           (state/remove-conn! sse))
          ([sse _status]
           (on-close sse)))}))))


(def id-regex #"[^-]*-(\d*)-(\d*)")


(defn recover-coords [req]
  (when-let [[_ x y] (-> req
                       :params
                       :id
                       (->> (re-find id-regex)))]
    {:x (Integer/parseInt x)
     :y (Integer/parseInt y)}))


(defn ping-handler
  ([req]
   (when-let [coords (recover-coords req)]
     (println "-- ping " coords)
     (state/add-ping! coords))
   {:status 204})
  ([req respond _raise]
   (respond (ping-handler req))))


(defn random-pings-handler
  ([_req]
   (println "-- add pixels")
   (state/add-random-pings!)
   {:status 204})
  ([req respond _raise]
   (respond
     (random-pings-handler req))))


(defn reset-handler
  ([_req]
   (println "-- reseting state")
   (state/reset-state!)
   {:status 204})
  ([req respond _raise]
   (respond (reset-handler req))))

(defn step-handler
  ([_req]
   (println "-- Step 1")
   (state/step-state!)
   {:status 204})
  ([req respond _raise]
   (respond
     (step-handler req))))



(defn play-handler
  ([_req]
   (println "-- play animation")
   (state/start-animating!)
   {:status 204})
  ([req respond _raise]
   (respond (play-handler req))))


(defn pause-handler
  ([_req]
   (println "-- pause animation")
   (state/stop-animating!)
   {:status 204})
  ([req respond _raise]
   (respond (pause-handler req))))

(defn resize-handler
  ([req]
   (let [{x "rows" y "columns"} (c/get-signals req)]
     (println "-- resize" x y)
     (state/resize! x y)
     {:status 204}))
  ([req respond _raise]
   (respond
     (resize-handler req))))


(defn refresh-handler
  ([_req]
   {:status 200
    :headers {"Content-Type" "text/html"}
    :body (rendering/render-content @state/!state)})
  ([req respond _raise]
   (respond (refresh-handler req))))


