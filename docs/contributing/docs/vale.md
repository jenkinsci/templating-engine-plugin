# Prose Linting with Vale

These docs use [Vale](https://github.com/errata-ai/vale) to ensure consistency of prose style.

## Perform Linting

Run `just lint-prose` to specifically lint the documentation prose.

## Style Guide

Vale uses the [Microsoft Writing Style Guide](https://docs.microsoft.com/en-us/style-guide/welcome/).

The [styles](https://docs.errata.ai/vale/styles) for Vale can be found in `docs/styles/Microsoft` and were taken from [here](https://github.com/errata-ai/Microsoft).

## Configuration

The `.vale.ini` file at the root of the repository is used to configure Vale for both IDE integration and for the checks that run during a Pull Request.

!!! note "IDE Integration"
    Check out the [Local Development](./local-development.md#integrated-development-environment-integration) page to learn more about IDE integration for Vale.
