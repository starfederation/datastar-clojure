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


(defn format-clj-cli-args [{:keys [cli X M main-ns args-str]}]
  (string/join " "
    (cond-> []
      X                        (conj (str "-X" (aliases->str X)))
      (or M (= "clojure" cli)) (conj (str "-M" (when M (aliases->str M))))
      main-ns                  (conj "-m" main-ns)
      args-str                 (conj args-str))))


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
  "Invoke the clojure cli.

  Arg keys:
  - `:dir`: directory in which to invoke the cli
  - `:X`: clojure `-X` option (seq of deps keywords aliases)
  - `:M`: clojure `-X` option (seq of deps keywords aliases)
  - `:main-ns`: namespace of the main function
  - `:args-str`: additional args for the cli
  "
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
  "Invoke the babashka cli.

  Arg keys:
  - `:dir`: directory in which to invoke the cli
  - `:main-ns`: namespace of the main function
  - `:args-str`: additional args for the cli
  "
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


(defn dev
  "Start a clojure repl with deps `aliases`."
  [& aliases]
  (let [aliases (-> dev-aliases
                    (into aliases)
                    (into (map arg->kw *command-line-args*)))]
    (clojure {:M aliases
              :main-ns 'nrepl.cmdline
              :args-str " --middleware \"[cider.nrepl/cider-middleware]\""})))


(defn dev-bb
  "Start a babashka repl."
  [& [addr]]
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
                       (string/join " " args)))})


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


(defn run-go-tests []
  (t/shell "go run github.com/starfederation/datastar/sdk/tests/cmd/datastar-sdk-tests@latest"))
