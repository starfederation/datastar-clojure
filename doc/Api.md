# A tour of the API

## Core SDK

The core SDK mainly provides tools for working with SSE-gens in a generic
manner. To do so all specific SSE-Gens are implementations of the
[starfederation.datastar.clojure.protocols/SSEGenerator](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.protocols#SSEGenerator)
protocol. This way
you use the same namespace regardless of the specific adapter you are using.

### [starfederation.datastar.clojure.api](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api)

This is the main entry point for using the SDK.

#### SSE Generator utilities

- [starfederation.datastar.clojure.api/close-sse!](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#close-sse!)
- [starfederation.datastar.clojure.api/lock-sse!](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#lock-sse!)
- [starfederation.datastar.clojure.api/with-open-sse](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#with-open-sse)

#### Standard Datastar patching functions

- [starfederation.datastar.clojure.api/patch-elements!](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#patch-elements!)
- [starfederation.datastar.clojure.api/patch-elements-seq!](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#patch-elements-seq!)
- [starfederation.datastar.clojure.api/remove-element!](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#remove-element!)
- [starfederation.datastar.clojure.api/patch-signals!](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#patch-signals!)

#### Executing scripts

Building on the patching functions we have convenience functions to send
JavaScript scripts to the browser to execute:

- [starfederation.datastar.clojure.api/execute-script!](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#execute-script!)
- [starfederation.datastar.clojure.api/console-log!](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#console-log!)
- [starfederation.datastar.clojure.api/console-error!](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#console-error!)
- [starfederation.datastar.clojure.api/redirect!](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#redirect!)

#### Simple actions

There are also some simple utilities for generation Datastar actions to put in
HTML:

- [starfederation.datastar.clojure.api/sse-get](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#sse-get)
- [starfederation.datastar.clojure.api/sse-post](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#sse-post)
- [starfederation.datastar.clojure.api/sse-put](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#sse-put)
- [starfederation.datastar.clojure.api/sse-patch](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#sse-patch)
- [starfederation.datastar.clojure.api/sse-delete](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#sse-delete)

#### Helpers

- [starfederation.datastar.clojure.api/get-signals](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#get-signals)
- [starfederation.datastar.clojure.api/datastar-request?](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.api#datastar-request?)

### [starfederation.datastar.clojure.adapter.common](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.adapter.common)

This namespaces contains code shared between specific adapter. This is a more
Advanced API needed mostly if you start writing custom adapters or custom
[write profiles](/doc/Write-profiles.md)

It contains the definition for the keys used in the option maps of
`->sse-response` functions, utilities for working with output streams and
building [write profiles](/doc/Write-profiles.md) and default callbacks.

### [starfederation.datastar.clojure.adapter.test](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/api/starfederation.datastar.clojure.adapter.test)

This namespace contains a few mock SSEGenerator implementations.

## Ring

We try to provide a similar model for working with SSE in each adapter we
support by sticking as close as possible to the ring spec.

### General model

When using the SDK you will mainly use a `->sse-response` function. It returns a
ring response map that starts a SSE stream and is tailored to the adapter you
are using.

The response map will contain a response status, the required SSE headers and
a body.

> [!IMPORTANT]
> You can modify the response map except for the body and the SSE headers.

For every adapter this function takes 2 arguments:

- the ring request
- a map containing callbacks and options

You get access to the SSE-Gen as an argument to the callbacks yo pass to
`->sse-response`.

### SSE lifecycle

The SSE connection is open and ready when the `on-open` callback is run.
If you don't plan on keeping it open you must close it yourself here.

When the adapter detects the client disconnecting or when you close the
SSE-Gen yourself the `on-close` callback will be called. Note that when that
happens it is already no longer possible to send events.

When sending events to the client, if an exception is thrown a `on-exception`
callback will be called. If you don't provide it a default one will be in place.
The default behavior is to close the connection on `java.io.IOException` and
rethrow otherwise.

### Differences between adapters

We have tried to keep the differences between the adapters minimal. Still not
all ring adapters use the same mechanism to represent SSE streams and some
differences are unavoidable.

These differences revolve mainly around disconnect detection and how the ring
synchronous and asynchronous APIs behave.

Refer to the SDK adapter READMEs for more information.

### Option keys, default write profiles

Besides the `->sse-reponse` function the `starfederation.datastar.clojure.adapter.XXX`
namespaces provides aliases to the commonly used code from the
[starfederation.datastar.clojure.adapter.common](https://cljdoc.org/d/dev.data-star.clojure/sdk/1.0.0-RC2/api/starfederation.datastar.clojure.adapter.common)
namespace.
