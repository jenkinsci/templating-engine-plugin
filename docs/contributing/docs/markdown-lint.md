# Markdownlint

[Markdownlint](https://github.com/DavidAnson/markdownlint) is used to ensure consistency in the structure of the Markdown files across the docs pages.

## Perform Linting

To run the markdownlint linter run: `just lint-markdown`.

## Rules

The [Rules](https://github.com/markdownlint/markdownlint/blob/master/docs/RULES.md) for markdownlint explain what's enforced with examples.

## Ignoring Violations

In rare cases, it's necessary to [ignore markdown lint violations](https://github.com/DavidAnson/markdownlint#configuration).

## Configuration

The `.markdownlint-cli2.yaml` configuration file is used for IDE integration and for the checks that run during a Pull Request.

!!! note "IDE Integration"
    Check out the [Local Development](./local-development.md#integrated-development-environment-integration) page to learn more about IDE integration for markdownlint.
