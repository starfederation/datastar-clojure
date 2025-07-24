# Datastar ring adapter

Datastar SDK adapter for [ring](https://github.com/ring-clojure/ring). It is currently
tested with
[ring-jetty-adapter](https://github.com/ring-clojure/ring/tree/master/ring-jetty-adapter)

This SDK adapter is based on the `ring.core.protocols/StreamableResponseBody` protocol.
Any ring adapter using this protocol should work with this library.

## Installation

Install using clojars deps coordinates:

[![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/ring.svg)](https://clojars.org/dev.data-star.clojure/ring)

Don't forget, you need the base SDK also:
[![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/sdk.svg)](https://clojars.org/dev.data-star.clojure/sdk)
