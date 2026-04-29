(ns starfederation.datastar.clojure.api
  "
Public api for the Datastar SDK.

The main api consists several functions that operate on SSE generators, see:
- [[patch-elements!]]
- [[patch-elements-seq!]]
- [[remove-element!]]
- [[patch-signals!]]
- [[execute-script!]]


These function take options map whose keys are:
- [[id]]
- [[retry-duration]]
- [[selector]]
- [[patch-mode]]
- [[use-view-transition]]
- [[element-ns]]
- [[only-if-missing]]
- [[auto-remove]]
- [[attributes]]

To help manage SSE generators's underlying connection there is:
- [[close-sse!]]
- [[lock-sse!]]
- [[with-open-sse]]

Helper to extract datastar specific data from ring requests:
- [[get-signals]]
- [[datastar-request?]]

Some common utilities for HTTP are also provided:
- [[sse-get]]
- [[sse-post]]
- [[sse-put]]
- [[sse-patch]]
- [[sse-delete]]

Some scripts are provided:
- [[console-log!]]
- [[console-error!]]
- [[redirect!]]"
  (:require
    [starfederation.datastar.clojure.api.common   :as common]
    [starfederation.datastar.clojure.api.elements :as elements]
    [starfederation.datastar.clojure.api.signals  :as signals]
    [starfederation.datastar.clojure.api.scripts  :as scripts]
    [starfederation.datastar.clojure.consts       :as consts]
    [starfederation.datastar.clojure.protocols    :as p]
    [starfederation.datastar.clojure.utils        :as u]))

;; -----------------------------------------------------------------------------
;; CDN
;; -----------------------------------------------------------------------------
(def CDN-url
  "URL for the Datastar js bundle tracking the latest Datastar, currently
  v1.0.1."

  "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.1/bundles/datastar.js")


(def CDN-map-url
  "URL for the Datastar source map going with [[CDN-url]]."
  "https://cdn.jsdelivr.net/gh/starfederation/datastar@1.0.1/bundles/datastar.js.map")

;; -----------------------------------------------------------------------------
;; SSE generator management
;; -----------------------------------------------------------------------------
(defmacro lock-sse!
  "Hold onto the lock of a `sse-gen` while executing `body`. This allows for
  preventing concurrent sending of sse events. Sse generators use
  [[java.util.concurrent.locks.ReentrantLock]] under the hood.

  Ex:
  ```clojure
  (lock-sse! my-sse-gen
             (patch-elements! sse frags)
             (patch-signals!  sse signals))
  ```
  "
  [sse-gen & body]
  `(u/lock! (p/get-lock ~sse-gen) ~@body))


(comment
  (macroexpand-1
    (macroexpand-1
      '(lock-sse! my-sse-gen
                  (patch-elements! sse frags)
                  (patch-signals!  sse signals)))))


(defn close-sse!
  "Close the connection of a sse generator.

  Return value:
  - true if `sse-gen` closed
  - false if it was already closed"
  [sse-gen]
  (p/close-sse! sse-gen))


(defmacro with-open-sse
  "Macro functioning similarly to [[clojure.core/with-open]]. It evaluates the
  `body` inside a try expression and closes the `sse-gen` at the end using
  [[close-sse!]] in a finally clause.

  Ex:
  ```
  (with-open-sse sse-gen
    (d*/patch-elements! sse-gen frag1)
    (d*/patch-signals!  sse-gen signals))
  ```
  "
  [sse-gen & body]
  `(try
     ~@body
     (finally
       (close-sse! ~sse-gen))))

(comment
  (macroexpand-1
    '(with-open-sse toto
       (do-stuff)
       (do-stuff))))


;; -----------------------------------------------------------------------------
;; Option names
;; -----------------------------------------------------------------------------
(def id
  "SSE option use in all event functions, string:

  Each event may include an eventId. This can be used by
  the backend to replay events. This is part of the SSE spec and is used to
  tell the browser how to handle the event. For more details see
  https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#id"
  common/id)

(def retry-duration
  "SSE option used in all event functions, number:

  Each event may include a retryDuration value. If one is
  not provided the SDK must default to 1000 milliseconds. This is part of the
  SSE spec and is used to tell the browser how long to wait before reconnecting
  if the connection is lost. For more details see
  https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#retry"
  common/retry-duration)

;; patch element opts
(def selector
  "[[patch-elements!]] & [[patch-elements-seq!]] option, string:

  The CSS selector to use to insert the elements. If not
  provided or empty, Datastar will default to using the id attribute of the
  element."
  common/selector)

(def patch-mode
  "[[patch-elements!]] & [[patch-elements-seq!]] option, string:

  The mode to use when merging elements into the DOM.
  If not provided the Datastar client side will default to morph.

  The set of valid values is:
  - [[pm-outer]] default
  - [[pm-inner]]
  - [[pm-remove]]
  - [[pm-prepend]]
  - [[pm-append]]
  - [[pm-before]]
  - [[pm-after]]
  - [[pm-replace]]
  "
  common/patch-mode)

(def use-view-transition
  "[[patch-elements!]] / [[remove-element!]  option, boolean:

  Whether to use view transitions, if not provided the
  Datastar client side will default to false."
  common/use-view-transition)


(def element-ns
  "[[patch-elements!]] & [[patch-elements-seq!]] option, boolean:

  Use a namespace when patching elements.
  Possible values are:
  - [[ns-html]] default
  - [[ns-svg]]
  - [[ns-mathml]]"
  common/element-namespace)

;;Signals opts
(def only-if-missing
  "[[patch-signals!]] option, boolean:

  Whether to patch the signal only if it does not already
  exist. If not provided, the Datastar client side will default to false, which
  will cause the data to be patched into the signals."
  common/only-if-missing)

;; Script opts
(def auto-remove
  "[[execute-script!]] option, boolean:

  Whether to remove the script after execution, if not
  provided the Datastar client side will default to true."
  common/auto-remove)

(def attributes
  "[[execute-script!]] option, map:

  A map of attributes to add to the script element."
  common/attributes)


;; -----------------------------------------------------------------------------
;; Data-star base api
;; -----------------------------------------------------------------------------
(def pm-outer
  "patch mode: replaces the outer HTML of the existing element."
  consts/element-patch-mode-outer)

(def pm-inner
  "patch mode: replaces the inner HTML of the existing element."
  consts/element-patch-mode-inner)

(def pm-remove
  "patch mode: remove the existing element from the dom."
  consts/element-patch-mode-remove)

(def pm-prepend
  "patch mode: prepends the element to the existing element."
  consts/element-patch-mode-prepend)

(def pm-append
  "patch mode: appends the element to the existing element."
  consts/element-patch-mode-append)

(def pm-before
  "patch mode: inserts the element before the existing element."
  consts/element-patch-mode-before)

(def pm-after
  "patch mode: inserts the element after the existing element."
  consts/element-patch-mode-after)

(def pm-replace
  "patch mode: Do not morph, simply replace the whole element and reset any
  related state."
  consts/element-patch-mode-replace)

(def ns-html
  "element namespace: default html namespace"
  consts/element-namespace-html)

(def ns-svg
  "element namespace: svg namespace"
  consts/element-namespace-svg)

(def ns-mathml
  "element namespace: mathMl namespace"
  consts/element-namespace-mathml)

;; -----------------------------------------------------------------------------
;; Sanitizers
;; -----------------------------------------------------------------------------
;; The atomic helpers backing the safe-by-default validation. They are
;; public so callers can also validate at a different boundary (e.g.
;; before passing a value into one of the `unsafe-*` variants below).
(defn assert-sse-line-safe!
  "Throw an [[clojure.core/ex-info]] if `(str v)` contains `\\n` or `\\r`,
  otherwise return `(str v)`.

  Use this on values that will reach an SSE option line — [[id]],
  [[selector]], [[patch-mode]], [[element-ns]] and friends — when those
  values can come from user input. A newline in any of those would let
  the caller append arbitrary SSE lines to the stream and forge events.

  `name` is the field name and is included in the thrown error for
  context (e.g. `\"selector\"`)."
  [v name]
  (u/assert-no-newline! v name))


(defn assert-script-body-safe!
  "Throw if `script-text` contains `</script` (case-insensitive); otherwise
  return `script-text`.

  HTML5 raw-text element parsing closes a `<script>` element as soon as
  it sees `</script`, regardless of the trailing characters. Escaping
  the occurrence would change the JS that runs, so the only safe option
  is to reject it."
  [script-text]
  (when (and (string? script-text)
             (re-find #"(?i)</script" script-text))
    (throw (ex-info "Script content must not contain '</script' (would close the tag)."
                    {:script script-text})))
  script-text)


(defn escape-script-attribute-value
  "Return `v` (coerced to a string) with the four HTML attribute-context
  characters escaped: `&`, `\"`, `<`, `>`. Use it on values you put into
  the [[attributes]] map of [[execute-script!]] when those values come
  from untrusted input — without escaping, a value containing `\"` can
  break out of the attribute and inject extra attributes."
  [v]
  (-> (str v)
      (.replace "&" "&amp;")
      (.replace "\"" "&quot;")
      (.replace "<" "&lt;")
      (.replace ">" "&gt;")))


(def ^:private valid-attr-name-re #"[A-Za-z_][A-Za-z0-9_:.-]*")

(defn assert-script-attribute-name-safe!
  "Throw if `(name k)` doesn't match `[A-Za-z_][A-Za-z0-9_:.-]*`; otherwise
  return `(name k)`."
  [k]
  (let [n (name k)]
    (when-not (re-matches valid-attr-name-re n)
      (throw (ex-info (str "Invalid script attribute name: " (pr-str n))
                      {:attribute n})))
    n))


;; -----------------------------------------------------------------------------
;; Patch elements / signals / scripts
;; -----------------------------------------------------------------------------
;; Each user-facing function comes in two flavors:
;; - the canonical name (e.g. `patch-elements!`) is **safe by default** —
;;   values that get written raw onto the SSE wire or into a `<script>`
;;   tag are validated/escaped before being sent, throwing on injection.
;; - an `unsafe-*` twin skips that validation, for the case where the
;;   developer has already validated the input or knows the value is
;;   trusted.
;; The atomic helpers backing the safe path ([[assert-sse-line-safe!]],
;; [[assert-script-body-safe!]], [[escape-script-attribute-value]],
;; [[assert-script-attribute-name-safe!]]) are public above so that
;; callers can also validate at a different boundary.

(defn- validate-element-line-opts! [opts]
  (when-let [v (common/id opts)]                 (assert-sse-line-safe! v "id"))
  (when-let [v (common/selector opts)]           (assert-sse-line-safe! v "selector"))
  (when-let [v (common/patch-mode opts)]         (assert-sse-line-safe! v "patch-mode"))
  (when-let [v (common/element-namespace opts)]  (assert-sse-line-safe! v "element-ns"))
  opts)


(defn unsafe-patch-elements!
  "Like [[patch-elements!]] but skips the safe-by-default validation of
  option-line values. Use only when you've already validated [[id]],
  [[selector]], [[patch-mode]] and [[element-ns]] yourself, or know they
  are trusted.

  > [!WARNING]
  > [[id]], [[selector]], [[patch-mode]] and [[element-ns]] are written
  > verbatim onto a single line of the SSE wire format. A `\\n` or `\\r`
  > in any of these lets the caller inject arbitrary SSE lines (and
  > forge whole events) into the stream."
  ([sse-gen elements]
   (unsafe-patch-elements! sse-gen elements {}))
  ([sse-gen elements opts]
   (elements/patch-elements! sse-gen elements opts)))


(defn patch-elements!
  "Send HTML elements to the browser to be patched into the DOM.

  Args:
  - `sse-gen`: the sse generator to send from
  - `elements`: A string of HTML elements.
  - `opts`: An options map

  Options keys:
  - [[id]]
  - [[retry-duration]]
  - [[selector]]
  - [[patch-mode]]
  - [[use-view-transition]]
  - [[element-ns]]

  Return value:
  - `false` if the connection is closed
  - `true` otherwise

  Safe by default: throws if [[id]], [[selector]], [[patch-mode]] or
  [[element-ns]] contains a `\\n` or `\\r`. Use
  [[unsafe-patch-elements!]] to skip the check."
  ([sse-gen elements]
   (patch-elements! sse-gen elements {}))
  ([sse-gen elements opts]
   (validate-element-line-opts! opts)
   (elements/patch-elements! sse-gen elements opts)))


(defn unsafe-patch-elements-seq!
  "Like [[patch-elements-seq!]] but skips the safe-by-default validation.
  See [[unsafe-patch-elements!]]'s warning."
  ([sse-gen elements]
   (unsafe-patch-elements-seq! sse-gen elements {}))
  ([sse-gen elements opts]
   (elements/patch-elements-seq! sse-gen elements opts)))


(defn patch-elements-seq!
  "Same as [[patch-elements!]] except that it takes a seq of elements.

  Safe by default; see [[unsafe-patch-elements-seq!]] to skip validation."
  ([sse-gen elements]
   (patch-elements-seq! sse-gen elements {}))
  ([sse-gen elements opts]
   (validate-element-line-opts! opts)
   (elements/patch-elements-seq! sse-gen elements opts)))


(defn unsafe-remove-element!
  "Like [[remove-element!]] but skips the safe-by-default validation.
  See [[unsafe-patch-elements!]]'s warning."
  ([sse-gen selector]
   (unsafe-remove-element! sse-gen selector {}))
  ([sse-gen selector opts]
   (elements/remove-element! sse-gen selector opts)))


(defn remove-element!
  "Remove element(s) from the dom. It is a convenience function using
  [[patch-elements!]] with the [[patch-mode]] options set to [[pm-remove]]
  and a [[selector]] set to `selector`.

  Args:
  - `sse-gen`: the sse generator to send from
  - `selector`: string, CSS selector that represents the elements to be
    removed from the DOM.
  - `opts`: options map

  Options keys:
  - [[id]]
  - [[retry-duration]]
  - [[use-view-transition]]

  Return value:
  - `false` if the connection is closed
  - `true` otherwise

  Safe by default: throws if `selector` or [[id]] contains a `\\n` or
  `\\r`. Use [[unsafe-remove-element!]] to skip the check."
  ([sse-gen selector]
   (remove-element! sse-gen selector {}))
  ([sse-gen selector opts]
   (assert-sse-line-safe! selector "selector")
   (when-let [v (common/id opts)] (assert-sse-line-safe! v "id"))
   (elements/remove-element! sse-gen selector opts)))


(defn unsafe-patch-signals!
  "Like [[patch-signals!]] but skips the safe-by-default validation of
  [[id]]. See [[unsafe-patch-elements!]]'s warning."
  ([sse-gen signals-content]
   (unsafe-patch-signals! sse-gen signals-content {}))
  ([sse-gen signals-content opts]
   (signals/patch-signals! sse-gen signals-content opts)))


(defn patch-signals!
  "
  Send signals to the browser using
  [RFC 7386 JSON Merge Patch](https://datatracker.ietf.org/doc/html/rfc7386)
  semantics.

   Args:
   - `sse-gen`: the sse generator to send from
   - `signals-content`: a JavaScript object or JSON string that will be sent to
      the browser to update signals. The data must evaluate to a
      valid JavaScript Object. `null` values for keys in this JSON object mean
      that the signal at these keys are to be removed.
   - `opts`: An options map

  Options keys:
  - [[id]]
  - [[retry-duration]]
  - [[only-if-missing]]

  Return value:
  - `false` if the connection is closed
  - `true` otherwise

  Safe by default: throws if [[id]] contains a `\\n` or `\\r`. Use
  [[unsafe-patch-signals!]] to skip the check."
  ([sse-gen signals-content]
   (patch-signals! sse-gen signals-content {}))
  ([sse-gen signals-content opts]
   (when-let [v (common/id opts)] (assert-sse-line-safe! v "id"))
   (signals/patch-signals! sse-gen signals-content opts)))


(defn get-signals
  "Extract datastar signals from a ring request map.

  This function returns either a string or an InputStream depending on the
  HTTP method of the request.

  - For GET and DELETE requests a string is returned (the signals are found in
    the `:query-params` map of the request)
  - For all other HTTP methods an `InputStream` is returned (the signals are the
    `:body` of the request)

  We do not impose any json parsing library. This means that you need to bring
  your own to parse the returned value into Clojure data.
  "
  [ring-request]
  (signals/get-signals ring-request))


(defn unsafe-execute-script!
  "Like [[execute-script!]] but skips the safe-by-default validation /
  escaping of `script-text` and [[attributes]].

  > [!WARNING]
  > `script-text` is interpolated as raw HTML inside the `<script>` tag,
  > and each value in [[attributes]] is interpolated raw inside a
  > double-quoted attribute. A `</script` substring in the body or a
  > `\\\"` in an attribute value lets the caller close the tag and
  > inject HTML/JS."
  ([sse-gen script-text]
   (unsafe-execute-script! sse-gen script-text {}))
  ([sse-gen script-text opts]
   (scripts/execute-script! sse-gen script-text opts)))


(defn- sanitize-script-attributes [opts]
  (if-let [attrs (common/attributes opts)]
    (assoc opts common/attributes
           (reduce-kv (fn [m k v]
                        (assoc m
                               (assert-script-attribute-name-safe! k)
                               (escape-script-attribute-value v)))
                      {}
                      attrs))
    opts))


(defn execute-script!
  "
  Construct a HTML script tag using `script-text` as its content. Then sends it
  to the brower using [[patch-elements!]] with [[patch-mode]] set to
  [[pm-append]] and [[selector]] set to `\"body\"`.

  The default behavior is to auto remove the script after it has run.

   Args:
  - `sse-gen`: the sse generator to send from
  - `script-text`: string that represents the JavaScript to be executed
    by the browser.
  - `opts`: An options map

  Options keys:
  - [[id]]
  - [[retry-duration]]
  - [[auto-remove]] defaults to true
  - [[attributes]]

  Return value:
  - `false` if the connection is closed
  - `true` otherwise

  Safe by default:
  - throws if `script-text` contains `</script` (any case);
  - throws if [[id]] contains `\\n`/`\\r`;
  - validates each attribute name in [[attributes]] and HTML-escapes
    each value (`& \" < >`) before they reach the script tag.

  Use [[unsafe-execute-script!]] if you need to skip the validation."
  ([sse-gen script-text]
   (execute-script! sse-gen script-text {}))
  ([sse-gen script-text opts]
   (assert-script-body-safe! script-text)
   (when-let [v (common/id opts)] (assert-sse-line-safe! v "id"))
   (scripts/execute-script! sse-gen script-text (sanitize-script-attributes opts))))



;; -----------------------------------------------------------------------------
;; SSE helpers
;; -----------------------------------------------------------------------------
(defn- sse
  ([method url]
   (str "@" method "('" url "')"))
  ([method url opts-string]
   (str "@" method "('" url "', " opts-string ")")))


(defn sse-get
  "Helper making a @get(...) action."
  ([url]
   (sse "get" url))
  ([url opts-string]
   (sse "get" url opts-string)))


(defn sse-post
  "Helper making a @post(...) action."
  ([url]
   (sse "post" url))
  ([url opts-string]
   (sse "post" url opts-string)))


(defn sse-put
  "Helper making a @put(...) action."
  ([url]
   (sse "put" url))
  ([url opts-string]
   (sse "put" url opts-string)))


(defn sse-patch
  "Helper making a @patch(...) action."
  ([url]
   (sse "patch" url))
  ([url opts-string]
   (sse "patch" url opts-string)))


(defn sse-delete
  "Helper making a @delete(...) action."
  ([url]
   (sse "delete" url))
  ([url opts-string]
   (sse "delete" url opts-string)))


(comment
  (sse-get "/a/b")
  := "@get('/a/b')"

  (sse-put "/a/b" "{includeLocal: true}")
  := "@put('/a/b', {includeLocal: true})")


;; -----------------------------------------------------------------------------
;; Scripts common
;; -----------------------------------------------------------------------------
(defn console-log!
  "Log msg in the browser console.

  Same behavior as [[execute-script!]].

  > [!WARNING]
  > `msg` is interpolated verbatim into a JavaScript double-quoted string
  > literal. The caller is responsible for ensuring `msg` is safe to embed
  > there (no unescaped `\"`, `\\`, `</script>`, newlines, or other
  > characters that would break out of the literal). Never pass
  > unsanitized user input directly.
  "
  ([sse-gen msg]
   (console-log! sse-gen msg {}))
  ([sse-gen msg opts]
   (execute-script! sse-gen (str "console.log(\"" msg "\")") opts)))


(defn console-error!
  "Log error msg in the browser console.

  Same behavior as [[execute-script!]].

  > [!WARNING]
  > `msg` is interpolated verbatim into a JavaScript double-quoted string
  > literal. The caller is responsible for ensuring `msg` is safe to embed
  > there (no unescaped `\"`, `\\`, `</script>`, newlines, or other
  > characters that would break out of the literal). Never pass
  > unsanitized user input directly.
  "
  ([sse-gen msg]
   (console-error! sse-gen msg {}))
  ([sse-gen msg opts]
   (execute-script! sse-gen (str "console.error(\"" msg "\")") opts)))


(defn redirect!
  "Redirect a page using a script.

  Same behavior as [[execute-script!]].

  > [!WARNING]
  > `url` is interpolated verbatim into a JavaScript double-quoted string
  > literal. The caller is responsible for ensuring `url` is safe to embed
  > there (no unescaped `\"`, `\\`, `</script>`, newlines, or other
  > characters that would break out of the literal). Never pass
  > unsanitized user input directly.
  "
  ([sse-gen url]
   (redirect! sse-gen url {}))
  ([sse-gen url opts]
   (execute-script! sse-gen
                    (str "setTimeout(() => window.location.href =\"" url "\")")
                    opts)))


;; -----------------------------------------------------------------------------
;; Misc
;; -----------------------------------------------------------------------------
(defn datastar-request?
  "Test for the presence of the datastar header in a ring request. The presence
  of the header means the request is issued from a datastar action."
  [request]
  (= "true" (get-in request [:headers "datastar-request"])))
