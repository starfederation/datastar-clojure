# Using Datastar

Datastar allows you to control a web page from the backend. HTTP responses are
used to patch either the current dom or the signals present in the
page.

There are 2 main ways to structure HTTP responses for Datastar:

- Return `text/html` or `application/json` HTTP response to patch the DOM or
  signals
- Start a Server Sent Events stream with a `text/event-stream` response and
  send SSE events to patch the page.

The Clojure SDK provides helpers when using the SSE option. It follows the
[Architecture Decision Record](https://github.com/starfederation/datastar/blob/develop/sdk/ADR.md)
shared by all official SDKs. This ADR describes a general mechanism to
manage SSE streams called a `ServerSentEventGenerator` and functions using this
SSE-Gen to send events formatted the way the Datastar expects them in the
browser.

## Brief overview of the API

When using the SDK you will invariably make use of 2 main namespaces, one for
sending Datastar event, the other to make SSE ring responses.

### `starfederation.datastar.clojure.api`

This is the main API of the core SDK. It provides several tools to work with
SSE-Gens such as:

- the patching functions specified in the ADR
- helpers for managing the SSE-Gen

### `starfederation.datastar.clojure.adapter.XXX`

The ring API provided by the adapter implementation you are using. It's main
role is to provide a `->sse-response` function that builds a ring response
tailored to your adapter.

In the following examples we'll be using
`starfederation.datastar.clojure.adapter.http-kit`.

## Examples

### Simple hello world

Let's start with a Datastar hello world. We start by requiring the 2 namespaces
we'll need:

```clojure
(require '[starfederation.datastar.clojure.api :as d*]                  ;; 1
         '[starfederation.datastar.clojure.adapter.http-kit :as hk-gen]);; 2

```

1. The core API
2. The specific API for a ring adapter, in this case Http-kit

We can imagine a page with the following HTML:

```HTML
<div>
  <button data-on-click="@get(/'say-hello')">Say hello</button>
  <p id="hello-field"></p>
</div>
```

Here we have a button that will call the `/'say-hello'` endpoint when clicked.
The handler for this endpoint would be:

```clojure
(require '[some.hiccup.library :refer [html]])

(defn simple-hello [request]                             ;; 1
  (hk-gen/->sse-response request                         ;; 2
    {hk-gen/on-open                                      ;; 3
     (fn [sse-gen]                                       ;; 4
       (d*/patch-elements! sse-gen
         (html [:p {:id "hello-field"} "Hello world!"])) ;; 5
       (d*/close-sse! sse-gen))}))                       ;; 6

```

1. We declare a standard ring handler which is a function of the HTTP request
2. The handler returns a SSE response
3. We setup a callback that will be called once the SSE stream is opened
4. The callback receives a `sse-gen` which is the SSE-gen for this response
5. Using `patch-elements` we send a HTML targeting the `"hello-field"` element
6. We close the connection

In the browser, when Datastar gets the patch it will morph the DOM to be:

```HTML
<div>
  <button data-on-click="@get(/'say-hello')">Say hello</button>
  <p id="hello-field">Hello world!</p>
</div>
```

### Chunked hello world

The previous example could have been accomplished without the use of the SDK
since we only sent one patch. However, using SSE we could just as well chunk the
response. Consider this handler:

```clojure
(defn chunked-hello [request]
  (hk-gen/->sse-response request
    {hk-gen/on-open
     (fn [sse-gen]

       (d*/patch-elements! sse-gen
         (html [:p {:id "hello-field"} "Hello"]))

       (Thread/sleep 1000)

       (d*/patch-elements! sse-gen
         (html [:p {:id "hello-field"} "Hello world!"]))

       (d*/close-sse! sse-gen))}))

```

Here we send 2 events. The first will morph the DOM into:

```HTML
<div>
  <button data-on-click="@get(/'say-hello')">Say hello</button>
  <p id="hello-field">Hello</p>
</div>
```

The second:

```HTML
<div>
  <button data-on-click="@get(/'say-hello')">Say hello</button>
  <p id="hello-field">Hello world!</p>
</div>
```

The example on [the datastar homepage](https://data-star.dev/) is build
similarly to this. It helps illustrate the possibilities using SSE. We
can sent multiple patches, do work between patches and we can keep the
connection alive for however long we want.

### Barebones broadcast

Speaking of keeping the connection alive, a simple broadcast system can be
implemented with the following code:

```clojure
(def !connections (atom #{}))                ;; 1


(defn subscribe-handler [request]
  (hk-gen/->sse-response request
    {hk-gen/on-open
     (fn [sse-gen]
       (swap! !connections conj sse-gen))    ;; 2

     hk-gen/on-close
     (fn [sse-gen status]
       (swap! !connections disj sse-gen))})) ;; 3


(defn broadcast-elements! [elements]         ;; 4
  (doseq [c @!connections]
    (d*/patch-elements! c elements)))

```

1. We keep an atom that contains the open connections
2. When the handler is called the `sse-gen` is added to `!connections`
3. When the connection is closed we remove `sse-gen` from `!connections`
4. The broadcast function will send `elements` to all connected browsers

In this example we do not automatically close the `sse-gen`. It will be kept
alive until either the client closes the SSE connection or your code does
it somewhere else.

### Fat updates and compression

Long lived connections open interesting possibilities up. A common pattern when
using Datastar is to keep one SSE stream opened and push updates when relevant
events occurred on the server.

We can have a page setup this way:

```HTML
<body data-on-load="/updates">
  <div id="main">
    Imagine a complex UI here
  </div>
</body>
```

And code similar to our broadcast example:

```clojure
;; Broadcasting logic
(def !connections (atom #{}))


(defn updates-handler [request]
  (hk-gen/->sse-response request
    {hk-gen/on-open
     (fn [sse-gen]
       (swap! !connections conj sse-gen))

     hk-gen/on-close
     (fn [sse-gen status]
       (swap! !connections disj sse-gen))}))


(defn broadcast-frame! [frame]
  (doseq [c @!connections]
    (d*/patch-elements! c frame)))


;; Renders the whole main content of the page
(defn render-frame [state]
  (html
    [:div#main "Do something with the state here"])


;; The state the rendering is based on
(def !state (atom {:some-complex "state"}))


(add-watch !state ::watch
  (fn [_k _ref old new]
    (when-not (identical? old new)
      (let [frame (render-frame new)]
        (broadcast-frame! frame))))))

```

When this page loads, the `/updates` endpoint is called setting up a long lived
SSE connection. When `!state` changes we broadcast a re-render of the whole
content of the page.

Using fat updates instead of fine grained ones might seem wasteful at first.
It however has several advantages:

- We don't need to keep track of which fine grained updates may not have gone
  through risking a page only partially updated. Instead each update of the page
  a client receives is internally consistent.
- SSE streams compresses really well, especially with algorithms like brotli
  that keep a shared compression window between client and server opened for
  the whole duration of the stream.

To use compression in this example we just need to use an option of the
`->sse-response` function. Our `update-handler` would look like this:

```clojure
(defn update-handler [request]
  (hk-gen/->sse-response request
    {hk-gen/on-open
     (fn [sse-gen]
       (swap! !connections conj sse-gen))

     hk-gen/on-close
     (fn [sse-gen status]
       (swap! !connections disj sse-gen))

     ;; We add a write profile here to enable gzip compression
     hk-gen/write-profile hk-gen/gzip-profile}))

```

For more about the compression option and this write profile concept,
checkout the [write profiles docs](/doc/Write-profiles.md).

## Going further

The examples presented here are contrived on purpose. For instance, you won't
keep you app state in an atom and use a watch to broadcast changes. However
these may help you get a feel for what is possible.

You can now learn more about each specific library we provide using the rest of
the docs as well as the API docs.

There are already several open source projects using Clojure and Datastar
that refine the patterns we presented here. I would encourage you to explore
these projects, use or take inspiration from them.
