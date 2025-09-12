# Generic Clojure SDK for Datastar

## Installation

The SDK provided adapter libraries already depend on this library. However,
if you want to [develop your own SSEGenerator](/doc/implementing-adapters.md)
you'll need to depend on:

[![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/sdk.svg)](https://clojars.org/dev.data-star.clojure/sdk)
[![cljdoc badge](https://cljdoc.org/badge/dev.data-star.clojure/sdk)](https://cljdoc.org/d/dev.data-star.clojure/sdk/CURRENT)

## Overview

Datastar SDKs in each language follow an
[Architecture Decision Record](https://github.com/starfederation/datastar/blob/develop/sdk/ADR.md)
and the Clojure SDK is no exception. This ADR describes a general mechanism to
manage SSE streams called a ServerSentEventGenerator and functions using it to
send SSE event formatted the way the Datastar expect them in the browser.

This library is a generic implementation of the ADR. It contains the code for:

- building blocks to manage SSE streams
- a clojure protocol `starfederation.datastar.clojure.protocols/SSEGenerator`
  allowing for the implementation of SSE generators for different Ring adapters.
- functions based on the protocol for working with SSE generators.
- helpers allowing to send Javascript scripts to run in the browser
- a generic mechanism called [write profiles](/doc/Write-profiles.md) to manage
  the buffering behavior and compression of SSE streams
- several write profiles providing gzip compression
