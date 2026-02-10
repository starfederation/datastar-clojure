(ns test.common
  (:require
    [clojure.java.io                                :as io]
    [clojure.edn                                    :as edn]
    [etaoin.api                                     :as ea]))


(def default-drivers-config
  {:chrome {:headless true
            :args ["-incognito"]}
   :firefox {:headless true
             :args ["-private"]}})


(def default-drivers
  [:firefox :chrome])


(def custom-config
  "Config from \"test-resources/test.config.edn\".

  Keys:
  - `:drivers`: vector of driver types to run (kws like `:firefox`... from etaoin)
  - `:webdriver-opts`: map of drivers types to drivers opts
    (options passed to etaoin driver starting opts)
  "
  (some-> "test.config.edn"
          io/resource
          slurp
          edn/read-string))


(def drivers-configs
  "Merge of the default drivers opts and the custom ones."
  (let [custom-opts (:webdriver-opts custom-config)]
    (reduce-kv
      (fn [acc type opts]
        (assoc acc type (merge opts
                               (get custom-opts type))))
      {}
      default-drivers-config)))


(defonce drivers
  (reduce
    (fn [acc driver]
      (assoc acc driver (delay (ea/boot-driver driver (get drivers-configs driver)))))
    {}
    (:drivers custom-config default-drivers)))


(defn install-shutdown-hooks! []
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               ; Killing web drivers
                               (println "Killing web drivers")
                               (doseq [d (vals drivers)]
                                 (when (realized? d)
                                   (try
                                     (ea/quit @d)
                                     (catch Exception _
                                       (println "Exception killing webdriver")))))

                               ; Killing agents
                               (println "Killing agents")
                               (shutdown-agents)))))

(defonce _ (install-shutdown-hooks!))
