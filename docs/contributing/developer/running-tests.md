# Running Tests

Unit tests for JTE are written using [Spock](https://spockframework.org/spock/docs/2.0/all_in_one.html).

To run all the tests, run:

``` bash
just test
```

The gradle test report is published to `build/reports/tests/test/index.html`

## Execute tests for a specific class

To run tests for a specific Class, `StepWrapperSpec` for example, run:

``` bash
just test '*.StepWrapperSpec'
```

## Code Coverage

By default, [JaCoCo](https://github.com/jacoco/jacoco) is enabled when running test.

Once executed, the JaCoCo coverage report can be found at: `build/reports/jacoco/test/html/index.html`

To disable this, run:

``` bash
just --set coverage false test
```
