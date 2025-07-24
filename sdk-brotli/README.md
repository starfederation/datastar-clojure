# Datastar Brotli write profile

This library contains some utilities to work with Brotli.

Credits to [Anders](https://andersmurphy.com/) and his work on [Hyperlith](https://github.com/andersmurphy/hyperlith)
from which this library takes it's code.

## Installation

Install using clojars deps coordinates:

[![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/brotli.svg)](https://clojars.org/dev.data-star.clojure/brotli)

## Supported ring adapters

At this moment only Http-kit is supported.

## Usage

This library provides brotil write profiles you can use like this:

```clojure
(require
  '[starfederation.datastar.clojure.api              :as d*]
  '[starfederation.datastar.clojure.adapter.Http-kit :as hk-gen]
  '[starfederation.datastar.clojure.brotli           :as brotli])

(defn handler [req]
  (hk-gen/->sse-response
    {hk-gen/write-profile (brotli/->brotli-profile)
     hk-gen/on-open
      (fn [sse]
        (d*/with-open-sse sse
          (do ...))
```

See docstrings in the `starfederation.datastar.clojure.brotli` namespace for
more information.
