(ns tasks
  (:require
    [babashka.tasks :as t]
    [clojure.string :as string]
    [clojure.edn :as edn]))

;; -----------------------------------------------------------------------------
;; Clojure cli invocation helpers
;; -----------------------------------------------------------------------------
(defn aliases->str [aliases]
  (->> aliases
       (map str)
       (string/join "")))


(defn format-clj-cli-args [{:keys [X M main-ns args-str]}]
  (string/join " "
    (cond-> []
      X        (conj (str "-X" (aliases->str X)))
      true     (conj (str "-M" (when M (aliases->str M))))
      main-ns  (conj "-m" main-ns)
      args-str (conj args-str))))


(defn print-cli [{:keys [cli X M main-ns args-str dir] :as args}]
  (println "--------------------------------")
  (println "Running " cli)
  (when dir
    (println "in dir: " dir))
  (println "--------------------------------")
  (when X        (println "X:       " X))
  (when M        (println "M:       " M))
  (when main-ns  (println "main ns: " main-ns))
  (when args-str (println "args:    " args-str))
  (println "--------------------------------")
  args)


(defn clojure
  {:arglists '([{:keys [X M main-ns args-str dir]}])}
  [{:as args}]
  (let [invocation (-> args
                       (assoc :cli "clojure")
                       print-cli
                       format-clj-cli-args)]
    (if-let [dir (:dir args)]
      (t/clojure {:dir dir} invocation)
      (t/clojure  invocation))))


(defn bb
  {:arglists '([{:keys [main-ns args-str]}])}
  [{:as args}]
  (-> args
      (dissoc :M :X)
      (assoc :cli "bb")
      print-cli
      format-clj-cli-args
      (->> (str "bb ")
           t/shell)))


;; -----------------------------------------------------------------------------
;; Starting repls
;; -----------------------------------------------------------------------------
(def dev-aliases
  [:test
   :repl
   :malli-schemas
   :sdk-brotli])

(defn arg->kw [s]
  (if (string/starts-with? s ":")
    (keyword (subs s 1))
    (keyword s)))


(defn dev [& aliases]
  (let [aliases (-> dev-aliases
                    (into aliases)
                    (into (map arg->kw *command-line-args*)))]
    (clojure {:M aliases
              :main-ns 'nrepl.cmdline
              :args-str " --middleware \"[cider.nrepl/cider-middleware]\""})))

(defn dev-bb [& [addr]]
  (let [addr (or addr (first *command-line-args*))]
    (bb {:args-str (str " nrepl-server"
                        (when addr (str " " addr)))})))



;; -----------------------------------------------------------------------------
;; Helpers tasks for tests
;; -----------------------------------------------------------------------------
(defn deps-aliases []
  (-> "deps.edn"
      slurp
      edn/read-string
      :aliases))


(def all-aliases (delay (deps-aliases)))


(defn named-paths->dirs [named-paths]
  (->> named-paths
       (mapcat #(get @all-aliases %))
       (mapv #(str "-d " %))))


(defn lazytest-invocation [{:keys [aliases named-paths args]}]
  {:M (into [:test] aliases)
   :main-ns 'lazytest.main
   :args-str (string/join " "
                 (conj (named-paths->dirs named-paths)
                       args))})


(defn lazytest [aliases paths-aliases & args]
  (-> {:aliases aliases
       :named-paths paths-aliases
       :args args}
      lazytest-invocation
      clojure))


(defn bb-lazytest [named-paths]
  (-> {:named-paths named-paths}
      lazytest-invocation
      bb))

(defn start-test-server []
  (clojure {:dir "sdk-tests"
            :main-ns 'starfederation.datastar.clojure.sdk-test.main}))

;; -----------------------------------------------------------------------------
;; Build tasks
;; -----------------------------------------------------------------------------
(def sdk-dir                  "sdk")
(def sdk-adapter-http-kit-dir "sdk-adapter-http-kit")
(def sdk-adapter-ring-dir     "sdk-adapter-ring")
(def sdk-brotli-dir           "sdk-brotli")
(def sdk-malli-schemas-dir    "sdk-malli-schemas")

(def sdk-lib-dirs
  [sdk-dir
   sdk-adapter-ring-dir
   sdk-adapter-http-kit-dir
   sdk-brotli-dir
   sdk-malli-schemas-dir])


(defn lib-jar! [dir]
  (t/clojure {:dir dir} "-T:build jar"))


(defn lib-install! [dir]
  (t/clojure {:dir dir} "-T:build install"))


(defn lib-clean! [dir]
  (t/clojure {:dir dir} "-T:build clean"))


(defn lib-bump! [dir component]
  (when-not (contains? #{"major" "minor" "patch"} component)
    (println (str "ERROR: First argument must be one of: major, minor, patch. Got: " (or component "nil")))
    (System/exit 1))
  (t/shell {:dir dir} (str "neil version " component " --no-tag")))


(defn lib-set-version! [dir version]
  (t/shell {:dir dir} (str "neil version set " version " --no-tag")))


(defn lib-publish! [dir]
  (t/clojure {:dir dir} "-T:build deploy"))
