# Malli schemas for the SDK

## Installation

Install using clojars deps coordinates:

[![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/http-kit-malli-schemas.svg)](https://clojars.org/dev.data-star.clojure/http-kit-malli-schemas)

[![cljdoc badge](https://cljdoc.org/badge/dev.data-star.clojure/malli-schemas)](https://cljdoc.org/d/dev.data-star.clojure/malli-schemas/CURRENT)

## Overview

This library provides Malli schemas for Http-kit adapter APIs.

Require the namespaces for which you want schema and/or instrumentation. Then
use malli's instrumentation facilities.

Notable schema namespaces:

- `starfederation.datastar.clojure.adapter.http-kit-schemas` for the
  original http-kit adapter
- `starfederation.datastar.clojure.adapter.http-kit2-schemas` for the new
  http-kit adapter API
