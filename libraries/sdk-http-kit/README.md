# Datastar http-kit adapter

## Installation

Install using clojars deps coordinates:

[![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/http-kit.svg)](https://clojars.org/dev.data-star.clojure/http-kit)

[![cljdoc badge](https://cljdoc.org/badge/dev.data-star.clojure/http-kit)](https://cljdoc.org/d/dev.data-star.clojure/http-kit/CURRENT)

This library already depends on the core SDK lib.

> [!IMPORTANT]
> This library adds (and needs) a dependency to Http-kit as recent as the current
> `v2.9.0-beta2`. We do not recommend using older versions (`v2.8.1` being the
> current stable) as they do not work properly with SSE.

## Overview

This library provides an implementation of the
`starfederation.datastar.clojure.protocols/SSEGenerator` for Http-kit and an API
to return ring responses using it.

It provides 2 APIs to create ring SSE response tailored to Http-kit.

### `starfederation.datastar.clojure.adapter.http-kit`

This this the original API, it is mostly one function: `->sse-response`.

Using this namespace is straightforward but it has a downside.
The way it works, the response's status and headers will be sent as soon has
your ring handler is done. It means that middleware that would modify the
response (or interceptor having a `:leave` function) will have no effect.

### `starfederation.datastar.clojure.adapter.http-kit2`

This is the latest API for using the SDK with Http-kit. It was designed to fix
the middleware (and interceptor) incompatibilities of the first. It is inspired
by the way [Pedestal](https://github.com/pedestal/pedestal) uses Http-kit.

Using this API, `->sse-reponse` will not start the SSE stream. It returns a
response map that can be modified by middleware and interceptors after the
handler (and thus `->sse-reponse`) is done.

The SSE stream is initiated either by the `wrap-start-responding` middleware or
the `start-responding-interceptor`. To be sure all other middleware you may use
work properly, `wrap-start-responding` should be the first in the chain and thus
the last to finish before handing the response to the adapter.

## Specific behavior

### Detecting a closed connection

Http-kit detects closed connection by itself. When so the `on-close` callback of
`->sse-response` will be called.

### SSE connection lifetime

The connection stays alive until the client or the server closes it regardless
of the ring API (sync vs async) you are using.
