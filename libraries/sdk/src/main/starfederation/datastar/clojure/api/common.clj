(ns starfederation.datastar.clojure.api.common)

;; -----------------------------------------------------------------------------
;; Option names
;; -----------------------------------------------------------------------------

;; SSE Options
(def id                  :d*.sse/id)
(def retry-duration      :d*.sse/retry-duration)

;; Merge fragment opts
(def selector            :d*.elements/selector)
(def patch-mode          :d*.elements/patch-mode)
(def use-view-transition :d*.elements/use-view-transition)
(def element-namespace   :d*.elements/namespace)

;;Signals opts
(def only-if-missing     :d*.signals/only-if-missing)


;; Script opts
(def auto-remove         :d*.scripts/auto-remove)
(def attributes          :d*.scripts/attributes)



;; -----------------------------------------------------------------------------
;; Data lines construction helpers
;; -----------------------------------------------------------------------------
(defn add-opt-line!
  "Add an option `v` line to the transient `data-lines!` vector.

  Args:
  - `data-lines!`: a transient vector of data-lines that will be written in a sse
    event
  - `prefix`: The Datastar specific preffix for that line
  - `v`: the value for that line
  "
  [data-lines! prefix v]
  (conj! data-lines! (str prefix v)))


(defn add-data-lines!
  "Add several data-lines to the `data-lines!` transient vector."
  [data-lines! prefix ^String text]
  (let [stream (.lines text)
        i (.iterator stream)]
    (loop [acc data-lines!]
      (if (.hasNext i)
        (recur (conj! acc (str prefix (.next i))))
        acc))))

(defn add-boolean-option?
  "Utility used to test whether an boolean option should result in a sse event
  data-line. Returns true if `val` a boolean and isn't the `default-val`."
  [default-val val]
  (and
    (boolean? val)
    (not= val default-val)))
