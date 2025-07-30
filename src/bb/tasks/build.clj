(ns tasks.build
  (:require
    [babashka.fs          :as fs]
    [babashka.tasks       :as t]
    [borkdude.rewrite-edn :as r]
    [clojure.edn          :as edn]
    [clojure.set          :as set]))

;; -----------------------------------------------------------------------------
;; Util
;; -----------------------------------------------------------------------------
(def sdk-dir                                "libraries/sdk")
(def sdk-brotli-dir                         "libraries/sdk-brotli")
(def sdk-adapter-http-kit-dir               "libraries/sdk-http-kit")
(def sdk-adapter-http-kit-malli-schemas-dir "libraries/sdk-http-kit-malli-schemas")
(def sdk-adapter-ring-dir                   "libraries/sdk-ring")
(def sdk-adapter-ring-malli-schemas-dir     "libraries/sdk-ring-malli-schemas")
(def sdk-malli-schemas-dir                  "libraries/sdk-malli-schemas")

;; Carrefull order matters because of interdepencies
(def sdk-lib-dirs
  [sdk-dir
   sdk-brotli-dir
   sdk-adapter-http-kit-dir
   sdk-adapter-ring-dir

   sdk-malli-schemas-dir
   sdk-adapter-http-kit-malli-schemas-dir
   sdk-adapter-ring-malli-schemas-dir])


(def sdk-lib-maven-names
  '#{dev.data-star.clojure/sdk
     dev.data-star.clojure/brotli
     dev.data-star.clojure/http-kit
     dev.data-star.clojure/http-kit-malli-schemas
     dev.data-star.clojure/malli-schemas
     dev.data-star.clojure/ring
     dev.data-star.clojure/ring-malli-schemas})

(def lib-dir->deps
  {sdk-brotli-dir
   ['dev.data-star.clojure/sdk]

   sdk-adapter-http-kit-dir
   ['dev.data-star.clojure/sdk]

   sdk-adapter-http-kit-malli-schemas-dir
   ['dev.data-star.clojure/sdk
    'dev.data-star.clojure/malli-schemas
    'dev.data-star.clojure/http-kit]

   sdk-adapter-ring-dir
   ['dev.data-star.clojure/sdk]

   sdk-adapter-ring-malli-schemas-dir
   ['dev.data-star.clojure/sdk
    'dev.data-star.clojure/malli-schemas
    'dev.data-star.clojure/http-kit]

   sdk-malli-schemas-dir
   ['dev.data-star.clojure/sdk]})


(def maven-dir
  (str (fs/path (fs/home) ".m2" "repository" "dev" "data-star" "clojure")))


(defn clean-maven-dir!
  "Deletes `~/.m2/repository/dev/data-star/clojure`."
  []
  (println "-------------")
  (println "Deleting:" maven-dir)
  (println "-------------")
  (fs/delete-tree maven-dir))


;; -----------------------------------------------------------------------------
;; Tasks
;; -----------------------------------------------------------------------------
(defn update-deps-version [deps-map lib-dir version]
  (if-let [deps (lib-dir->deps lib-dir)]
    (reduce (fn [deps-map dep]
              (r/assoc-in deps-map [:deps dep] {:mvn/version version}))
            deps-map
            deps)
    deps-map))


(defn get-interdependencies [deps-map]
  (-> deps-map
      r/sexpr
      (get :deps)
      keys
      set
      (set/intersection sdk-lib-maven-names)))


(defn update-inter-deps-versions [deps-map version]
  (reduce (fn [deps-map lib-maven-name]
            (r/assoc-in deps-map [:deps lib-maven-name] {:mvn/version version}))
          deps-map
          (get-interdependencies deps-map)))


(defn ->deps-file [lib-dir]
  (-> lib-dir
      (fs/path "deps.edn")
      str))


(defn assoc-deps!
  "Adds libraries interdependencies in deps.edn files"
  [lib-dir version]
  (let [deps-file (->deps-file lib-dir)]
    (-> deps-file
        slurp
        r/parse-string
        (update-inter-deps-versions version)
        str
        (->> (spit deps-file)))))


(defn current-version []
  (-> (t/shell {:dir sdk-dir :out :string} "neil" "version")
      :out
      edn/read-string
      :project))


(defn lib-bump! [dir component]
  (when-not (contains? #{"major" "minor" "patch"} component)
    (println (str "ERROR: First argument must be one of: major, minor, patch. Got: " (or component "nil")))
    (System/exit 1))

  (t/shell {:dir dir} (str "neil version " component " --no-tag"))

  (assoc-deps! dir (current-version)))


(defn lib-set-version! [dir version]
  (t/shell {:dir dir} (str "neil version set " version " --no-tag"))

  (assoc-deps! dir version))


(defn lib-clean! [dir]
  (t/clojure {:dir dir} "-T:build clean"))


(defn lib-jar! [dir]
  (println "----------------")
  (println "Building" dir)
  (println "----------------")
  (t/clojure {:dir dir} "-T:build jar"))


(defn lib-install! [dir]
  (println "----------------")
  (println "Installing" dir)
  (println "----------------")
  (t/clojure {:dir dir} "-T:build install"))


(defn lib-publish! [dir]
  (t/clojure {:dir dir} "-T:build deploy"))

