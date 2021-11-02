# Linting

This project uses [Spotless](https://github.com/diffplug/spotless) and [CodeNarc](https://github.com/CodeNarc/CodeNarc) to perform linting.
The CodeNarc rule sets for `src/main` and `src/test` can be found in `config/codenarc/rules.groovy` and `config/codenarc/rulesTest.groovy`, respectively.

To execute linting, run:

``` bash
just lint-code
```

Once executed, the reports can be found at `build/reports/codenarc/main.html` and `build/reports/codenarc/test.html`.
