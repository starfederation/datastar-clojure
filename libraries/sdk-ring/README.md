# Datastar ring adapter

## Installation

Install using clojars deps coordinates:

[![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/ring.svg)](https://clojars.org/dev.data-star.clojure/ring)

[![cljdoc badge](https://cljdoc.org/badge/dev.data-star.clojure/ring)](https://cljdoc.org/d/dev.data-star.clojure/ring/CURRENT)

This library already depends on the core SDK lib.

## Overview

Datastar SDK adapter for [ring](https://github.com/ring-clojure/ring). It is currently
tested only with
[ring-jetty-adapter](https://github.com/ring-clojure/ring/tree/master/ring-jetty-adapter).

This SDK adapter is based on the `ring.core.protocols/StreamableResponseBody` protocol.
Any ring adapter using this protocol should work with this library.

## Specific behavior

### Detecting a closed connection

With the [ring-jetty-adapter](https://github.com/ring-clojure/ring/tree/master/ring-jetty-adapter),
sending events on a closed connection will fail at some point throwing an
`IOException`. By default the SSE-Gen will catch this exception, close itself
then call the `on-close` callback.

> [!Note]
> At this moment, when using the ring adapter and Jetty, our SSE-Gen needs
> to send 2 small events or 1 big event to detect a closed connection.
> There must be some buffering happening independent of our implementation.

### SSE connection lifetime

|Api| connection lifetime|
|-|--|
|Ring sync| same as the thread carrying the initial response|
|Ring async| alive until the client or the server closes it|

> [!IMPORTANT]
> This is standard behavior as specified in the Ring spec. It implies that you
> can't keep a SSE connection opened beyond the lifetime of the thread making
> the initial response when using the synchronous API.

In other words, the [barebones broadcast example](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT/doc/sdk-docs/using-datastar#barebones-broadcast)
from the docs will work with the ring asynchronous API, not the synchronous one
when using this library.
