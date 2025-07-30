# SDK tests

This is where the code for the [generic tests](/sdk/test) lives.

## Running the test app

- repl:

```bash
clojure -M:repl -m nrepl.cmdline --middleware "[cider.nrepl/cider-middleware]"

```

- main:

```bash
clojure -M -m starfederation.datastar.clojure.sdk-test.main
```

- start go binary running the tests

```bash
go run github.com/starfederation/datastar/sdk/tests/cmd/datastar-sdk-tests@latest
```

More information here: [SDKs' common tests](https://github.com/starfederation/datastar/tree/develop/sdk/tests).
