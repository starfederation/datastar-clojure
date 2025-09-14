# Datastar Clojure SDK

We provide several libraries for working with [Datastar](https://data-star.dev/):

- A generic SDK to generate and send SSE events formatted for Datastar.
- A SSE generator abstraction defined by the
  `starfederation.datastar.clojure.protocols/SSEGenerator` protocol as well as
  several libraries implementing it to work with specific ring adapters.
- A library containing [malli schemas](https://github.com/metosin/malli)
  covering the generic API and our adapter implementations.
- A library providing the tools necessary to use Brotli compression in SSE Streams

There currently are adapter implementations for:

- [ring compliant adapters](https://github.com/ring-clojure/ring)
- [http-kit](https://github.com/http-kit/http-kit)

If you want to roll your own adapter implementation, see
[implementing-adapters](/doc/implementing-adapters.md).

## Library coordinates


| library       | deps coordinate                                     |
| ------------- | ----------------------------------------------------|
| SDK           | [![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/sdk.svg)](https://clojars.org/dev.data-star.clojure/sdk)                     |
| http-kit      | [![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/http-kit.svg)](https://clojars.org/dev.data-star.clojure/http-kit)           |
| ring          | [![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/ring.svg)](https://clojars.org/dev.data-star.clojure/ring)                   |
| brotli        | [![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/brotli.svg)](https://clojars.org/dev.data-star.clojure/brotli)               |
| Core SDK malli schemas | [![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/malli-schemas.svg)](https://clojars.org/dev.data-star.clojure/malli-schemas) |
| Http-kit malli schemas | [![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/http-kit-malli-schemas.svg)](https://clojars.org/dev.data-star.clojure/http-kit-malli-schemas) |
| Ring malli schemas | [![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/ring-malli-schemas.svg)](https://clojars.org/dev.data-star.clojure/ring-malli-schemas) |

- To get started you'll need either the http-kit library or the ring one.
- Other libraries are optional
- The ring library works with ring compliant adapters (adapter using the
  `ring.core.protocols/StreamableResponseBody`)
- Currently the Brotli library works only with http-kit

## Usage

### Basic Concepts

By convention SDK adapters provide a single `->sse-response` function. This
function returns a valid ring response tailored to work with the ring
adapter it is made for. This function will receive an implementation of
`SSEGenerator` protocol also tailored to the ring adapter used.

You then use the Datastar SDK functions with the SSE generator.

### Short example

Start by requiring the main API and an adapter. With Http-kit for instance:

```clojure
(require '[starfederation.datastar.clojure.api :as d*]
         '[starfederation.datastar.clojure.adapter.http-kit :as hk-gen])

```

Using the adapter you create ring responses in your handlers:

```clojure
(defn sse-handler [request]
  (hk-gen/->sse-response request
    {hk-gen/on-open
     (fn [sse-gen]
       (d*/patch-elements! sse-gen "<div>test</div>")
       (d*/close-sse! sse-gen))}))

```

In the callback we use the SSE generator `sse-gen` with the Datastar SDK functions.

Depending on the adapter you use, you can keep the SSE generator open by storing
it somewhere and use it later:

```clojure
(def !connections (atom #{}))


(defn sse-handler [request]
  (hk-gen/->sse-response request
    {hk-gen/on-open
     (fn [sse-gen]
       (swap! !connections conj sse-gen))

     hk-gen/on-close
     (fn [sse-gen status]
       (swap! !connections disj sse-gen))}))


(defn broadcast-elements! [elements]
  (doseq [c @!connections]
    (d*/patch-elements! c elements)))

```

### Advanced features

This SDK is essentially a tool to manage SSE connections with helpers to format
events the way the Datastar framework expects them on the front end.

It provides advanced functionality for managing several aspects of SSE.

You can find more information in several places:

- the docstings for the `->sse-response` function you are using.
- the [SSE design notes document](/doc/SSE-design-notes.md) details
  what considerations are taken into account in the SDK.
- the [write profiles document](/doc/Write-profiles.md) details the
  tools the SDK provides to control the buffering behaviors of a SSE stream and
  how to use compression.
- the [adapter implementation guide](/doc/implementing-adapters.md)
  lists the conventions by which SDK adapters are implemented if the need to
  implement your own ever arises.

## TODO:

- Streamlined release process (cutting releases and publish jar to a maven repo)
- Review the etaoin tests, there are some race conditions

## License

[![License](https://img.shields.io/github/license/starfederation/datastar)](https://github.com/starfederation/datastar/blob/main/LICENSE.md)
