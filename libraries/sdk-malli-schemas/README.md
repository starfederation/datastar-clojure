# Malli schemas for the SDK

## Installation

Install using clojars deps coordinates:

[![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/malli-schemas.svg)](https://clojars.org/dev.data-star.clojure/malli-schemas)

[![cljdoc badge](https://cljdoc.org/badge/dev.data-star.clojure/malli-schemas)](https://cljdoc.org/d/dev.data-star.clojure/malli-schemas/CURRENT)

## Overview

This library provides Malli schemas for the core SDK.

Require the namespaces for which you want schema and/or instrumentation. Then
use malli's instrumentation facilities.

Notable schema namespaces:

- `starfederation.datastar.clojure.api-schemas` for the general d\* API
- `starfederation.datastar.clojure.api.*-schemas` for more specific code
  underlying the main API
- `starfederation.datastar.clojure.adapter.common-schemas` for the common
  adapter machinery (write profiles)
