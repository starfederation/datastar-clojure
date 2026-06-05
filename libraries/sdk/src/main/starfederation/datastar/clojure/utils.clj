(ns starfederation.datastar.clojure.utils
  (:refer-clojure :exclude [assert])
  (:require
    [clojure.string :as string])
  (:import
    [java.util.concurrent.locks ReentrantLock]))


(defmacro assert
  "Same as clojure's [[assert]] except that it throws a `clojure.lang.ExceptionInfo`."
  {:added "1.0"}
  ([x]
   (when *assert*
     `(when-not ~x
        (throw (ex-info (str "Assert failed: " (pr-str '~x)) {})))))
  ([x message]
   (when *assert*
     `(when-not ~x
        (throw (ex-info (str "Assert failed: " ~message "\n" (pr-str '~x)) {}))))))

(comment
  (assert (number? :a)))

;; -----------------------------------------------------------------------------
;; Locking utility
;; -----------------------------------------------------------------------------
(defn reantrant-lock? [l]
  (instance? ReentrantLock l))

;; Shamelessly adapted from https://github.com/clojure/clojure/blob/clojure-1.12.0/src/clj/clojure/core.clj#L1662
(defmacro lock!
  [x & body]
  `(let [lockee# ~x]
     (assert (reantrant-lock? ~x))
     (try
       (let [^ReentrantLock locklocal# lockee#]
         (.lock locklocal#)
         (try
           ~@body
           (finally
            (.unlock locklocal#)))))))

(comment
  (macroexpand-1 '(lock! x (do (stuff)))))

;; -----------------------------------------------------------------------------
;; Other
;; -----------------------------------------------------------------------------
(defmacro transient-> [v & body]
  `(-> ~v transient ~@body persistent!))

(comment
  (macroexpand-1 '(transient-> [] (conj! 1) (conj! 2))))


(defn not-empty-string? [s]
  (not (string/blank? s)))


(defn assert-no-newline!
  "Throw an [[ex-info]] if `(str v)` contains `\\n` or `\\r`.

  Used to defend against SSE event injection: option/id values are written
  to a single line of the SSE wire format, so a newline in user-controlled
  input would let an attacker append arbitrary lines (and forge whole
  events). `name` is included in the error for context. Returns `(str v)`."
  [v ^String name]
  (let [s (str v)]
    (when (or (.contains s "\n") (.contains s "\r"))
      (throw (ex-info (str name " must not contain newlines or carriage returns.")
                      {:value v :name name})))
    s))


(defn merge-transient!
  "Merge a map `m` into a transient map `tm`.
  Returns the transient map without calling [[persistent!]] on it."
  [tm m]
  (reduce-kv (fn [acc k v]
               (assoc! acc k v))
             tm
             m))


(defmacro def-clone
  "Little utility to clone simple var. It brings their docstring to the clone."
  ([src]
   (let [dest (-> src name symbol)]
    `(def-clone ~dest ~src)))
  ([dest src]
   (let [src-var (resolve src)
         doc (-> src-var meta :doc)]
    `(do
       (def ~dest ~(symbol src-var))
       (alter-meta! (resolve '~dest) assoc :doc ~doc)
       (var ~dest)))))

