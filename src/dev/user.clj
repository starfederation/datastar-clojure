(ns user
  (:require
    [clojure.java.io   :as io]
    [clojure.repl.deps :as crdeps]
    [clojure+.hashp    :as hashp]
    [clj-reload.core   :as reload]
    [malli.dev         :as mdev]))


(alter-var-root #'*warn-on-reflection* (constantly true))


(hashp/install!)

(reload/init
  {:no-reload ['user]})


(defn reload! []
  (reload/reload))

;; -----------------------------------------------------------------------------
;; Printing helpers
;; -----------------------------------------------------------------------------
(defn- safe-requiring-resolve [sym]
  (try
    (deref (requiring-resolve sym))
    (catch Exception _ nil)))


(def original-out
  (or
    (:out (safe-requiring-resolve 'cider.nrepl.middleware.out/original-output))
    System/out))


(def original-err
  (or
    (:err (safe-requiring-resolve 'cider.nrepl.middleware.out/original-output))
    System/err))


(defmacro force-out
  "Binds [[*out*]] to the original system's out."
  [& body]
  `(binding [*out* (io/writer original-out)]
     ~@body))


(defn clear-terminal!
  "Clear the terminal of all text"
  []
  (force-out
    (print "\033c")
    (flush)))


(comment
  (mdev/start! {:exception true})
  (mdev/stop!)
  (reload!)
  *e
  (crdeps/sync-deps)

  (-> (System/getProperties)
      keys
      sort)

  (require '[clojure.tools.build.api :as b])
  b/write-pom
  b/copy-dir)
