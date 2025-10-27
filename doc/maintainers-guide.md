# Maintainers Guide

## Directory structure

- `libraries/sdk`: the source folder for the main SDK
- `libraries/sdk-ring`: source folders for the ring adapter specific code
- `libraries/sdk-http-kit`: source folders for the http-kit adapter specific code
- `libraries/sdk-malli-schemas`: self explanatory...
- `libraries/sdk-brotli`: brotli write profiles
- `src/bb`: tasks used run a repl, tests...
- `src/bb-example`: bb examples
- `src/dev`: dev utils, examples
- `src/test`: centralized tests for all the libraries
- `test-resources`: self explanatory

## bb tasks

### Release tasks

- `bb bump-version patch/minor/major`: to bump a version component across all libs
- `bb set-version x.x.x`: to set the version component across all libs
- `bb jar:all`: Build jars artifacts for all of the libs
- `bb clean`: Clean all build artifacts
- `bb install:all`: Build jars artifacts for all of the libs and installs them locally
- `bb install:clean` will delete  `~/.m2/repository/dev/data-star/clojure`
- `bb jar:<lib-name>`: Build jars artifacts for one of the libs
- `bb publish:all`: Publish the artifacts to clojars.org

### Development tasks `bb run dev`

- `bb run dev`: start a repl with the dev nss, the test nss the malli schemas,
  ring-jetty and Http-kit on the classpath
- `bb run dev:rj9a`: same as basic dev task expect for ring-jetty being replaced
  with rj9a.
- `bb run dev:bb`: start a bb repl with the core SDK, http-kit adapter and
  malli-schemas in the classpath.

> [!note]
> You can add additional deps aliases when calling these tasks:
> `bb run dev :debug` will add a Flowstorm setup

### Test tasks `bb run test`

- `bb run test:all`: run all test for the SDK, the Http-kit adapter and the
  ring adapter using ring-jetty.
- `bb run test:rj9a`: run all test for the SDK and the ring adapter using rj9a.
- `bb run test:bb`: run unit tests for the SDK in Babashka.
- `bb run test:sdk-common`: start the server used to run the
  [SDKs' common tests](https://github.com/starfederation/datastar/tree/develop/sdk/tests).
- `bb run test:sdk-common-go`: run the common SDK test (go program)

### Cljdoc tasks

In order to test how [cljdoc](https://cljdoc.org/) will ingest our libraries
there are babashka to run a local instance.

To do so you need to have either [docker](https://www.docker.com/)
or [podman](https://podman.io/) installed, the babashka tasks we provide will
work with either one.

Next you need to run:

```bash
docker pull cljdoc/cljdoc
```

Once that is done you can use the following bb tasks:

- `bb run cljdoc:server` to start a cljdoc local instance on port `8000`
- `bb run install:all` to install locally the jars to ingest
- `bb run cljdoc:ingest <SDK-lib-name>` to tell cljdoc to ingest a sdk library.
  The possible values for `<SDK-lib-name>` are:
  `:sdk :http-kit :ring :malli-schemas :brotli`
- `bb run cljdoc:clean` will delete `./.cljdoc-preview`, the directory
  where the local cljdoc server stores its data.

## Release

- The library artifacts are published to [Clojars](http://clojars.org) under the
  `dev.data-star.clojure` namespace.
- The Clojars account and deploy token are managed by Ben Croker, and added to
  this repo as GitHub action secrets:
  - Secret name: `CLOJARS_USERNAME`
    Value: _the clojars account username_
  - Secret name: `CLOJARS_PASSWORD`
    Value: _the clojars deploy token_
- The libraries' versions are bumped in lockstep so that there is no confusion
  over which version of the common lib should be used with an adapter lib.

The Github Actions [CI workflow for clojure](../.github/workflows/release-sdk.yml)
will always run the tests and produce jar artifacts.

Triggering a deployment to clojars is a manual process. A Datastar core
contributor must trigger the `Release Clojure SDK` workflow with the `publish`
input boolean set to `true`.

**Release process:**

1. Use `bb set-version` or `bb bump-version` to update the library versions in lockstep
2. Commit those changes and push to GitHub
3. A core contributor must trigger the workflow manually setting `publish` to `true`

## Test

### Running tests

- for the unit and smoke tests see the bb tasks.
- for the generic go SDK tests:
  1. Start the test server with `bb run test:sdk-common`
  2. Run `bb run test:sdk-common-go`

### webdriver config

You can modify the webdriver config when testing by creating a
`test-resources/test.config.edn` file. It contains a map whose keys
are:

- `:drivers`: [etaoin](https://github.com/clj-commons/etaoin) webdriver types
  to run
- `:webdriver-opts`: a map of webdriver type to webriver specific options

For instance on my machine:

```clojure
{:drivers [:firefox :chrome]
 :webdriver-opts {:chrome {:path-driver "/snap/bin/chromium.chromedriver"}}}
```
