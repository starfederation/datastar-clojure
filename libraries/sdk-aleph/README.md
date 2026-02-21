
# Datastar Aleph adapter

## Installation

Install using clojars deps coordinates:

[![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/aleph.svg)](https://clojars.org/dev.data-star.clojure/aleph)

[![cljdoc badge](https://cljdoc.org/badge/dev.data-star.clojure/aleph)](https://cljdoc.org/d/dev.data-star.clojure/aleph/CURRENT)

This library already depends on the core SDK lib.


## Overview

This library provides an implementation of the
`starfederation.datastar.clojure.protocols/SSEGenerator` for the [Aleph adapter](https://github.com/clj-commons/aleph).


## Specific behavior

### API behavior
- Only use this library with the ring synchronous API, to my knowledge Aleph does not really implement it.
- The Datastar patch functions (`patch-elements`, etc...) return manifold deferred containing a boolean instead
  of returning the boolean itself
- The `on-open` callback from the `->sse-response` is called synchronously.
  In other words the ring response won't be sent to the client until this callback is done.
  To circumvent this you can offload work to a future/vthread from here. That way
  the callback ends, the initial SSE response (status + headers) is sent and the SSE stream
  opens immediately.

    
> [!IMPORTANT]
> Just to be sure, be really careful not to block in the `on-open` callback. This is the
> main difference from the Ring and Http-kit implementations.


### Detecting a closed connection

Aleph detects closed connections by itself. When it does the `on-close`
callback of `->sse-response` will be called.

### SSE connection lifetime

The connection stays alive until the client or your code explicitly closes it
server side regardless of the ring API (sync vs async) you are using.
