# Contribute to these Docs

If you're here, it probably means you're interested in contributing to these docs.
So first off, thank you!

This page will walk you through everything you need to get started.

## Documentation Philosophy

The JTE documentation is organized into the following sections: [Concepts](../concepts/framework-overview/overview.md), [Reference](../reference/overview.md), [Tutorials](../tutorials/index.md), & [How-To Guides](../how-to/index.md).

!!! note
    Huge shout out to [Divio](https://divio.com) for formalizing this approach [here](https://documentation.divio.com/).

    The following table comes from the [introduction](https://documentation.divio.com/introduction/) to this documentation system.

|               | Concepts      | Reference              | Tutorials                  | How-To Guides               |
|---------------|:-------------:|:----------------------:|:--------------------------:|:---------------------------:|
| *oriented to* | understanding | information            | learning                   | a goal                      |
| *must*        | explain       | describe specifics     | help new users get started | solve a specific problem    |
| *its form*    | Article       | no-fluff specification | a hands-on lesson          | a step-by-step walk through |

## Prerequisites

JTE uses a few different tools to write, build, and publish the documentation.

Development activities take place within containers and are orchestrated using `Just`

| tool | description |
|------|-------------|
| [Just](https://github.com/casey/just) | a task runner similar to `Make` with a simpler syntax |
| [Docker](https://docs.docker.com/get-docker/) | runtime environments are encapsulated in container images |

## Documentation Framework

This documentation site is generated using the [MkDocs](https://www.mkdocs.org/) framework using the [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/) theme.

## Linting

JTE uses a few tools to lint the documentation and ensure consistency.

Run `just lint-docs` to run all the linters for the documentation.

### Markdown Formatting

JTE uses [markdownlint](https://github.com/DavidAnson/markdownlint) to enforce common Markdown formatting.

!!! note
    Markdownlint is a static analysis tool for Node.js with a library of rules to enforce standards and consistency for Markdown files.

    Check out the [Rules](https://github.com/DavidAnson/markdownlint/blob/v0.24.0/doc/Rules.md) for markdownlint. 

Run `just lint-markdown` to specifically lint the formatting of the markdown.

### Prose Style Guide

JTE uses [Vale](https://github.com/errata-ai/vale) to ensure consistency of prose style.

!!! note
    Vale is a syntax-aware linter for prose built with speed and extensibility in mind.

JTE conforms to the [Microsoft Writing Style Guide](https://docs.microsoft.com/en-us/style-guide/welcome/).

Run `just lint-prose` to specifically lint the documentation prose.

The [styles](https://docs.errata.ai/vale/styles) for Vale can be found in `docs/styles/Microsoft` and were taken from [here](https://github.com/errata-ai/Microsoft).

## Local Docs Development

MkDocs supports a local development server featuring hot-reloading capabilities.

To see changes in real-time, run `just serve`.
After a few seconds, a local version of the docs will be hosted at [http://localhost:8000](http://localhost:8000).

!!! note "IDE Integration"
    [Visual Studio Code](https://code.visualstudio.com/) has extensions for both [Vale](https://marketplace.visualstudio.com/items?itemName=errata-ai.vale-server) and [markdownlint](https://marketplace.visualstudio.com/items?itemName=DavidAnson.vscode-markdownlint).

## Versioning

Material for MkDocs integrates with [Mike](https://squidfunk.github.io/mkdocs-material/setup/setting-up-versioning) to support documentation versioning.
