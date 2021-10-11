# Getting Started

Presumably you're here because you want to help by updating the JTE documentation, so thank you!

## Tools

These docs are written in Markdown[^1] and compiled using [MkDocs](https://mkdocs.org) with the [Material for MkDocs](http://squidfunk.github.io/mkdocs-material) theme.

Development activities take place within containers and are orchestrated using [Just](https://github.com/casey/just).

|                               Tool                                | Description                                               |
|:-----------------------------------------------------------------:|-----------------------------------------------------------|
|               [Just](https://github.com/casey/just)               | a task runner similar to `Make` with a simpler syntax     |
|           [Docker](https://docs.docker.com/get-docker/)           | runtime environments are encapsulated in container images |
|                   [MkDocs](https://mkdocs.org)                    | Documentation framework                                   |
| [Material for MkDocs](http://squidfunk.github.io/mkdocs-material) | Documentation styling                                     |
|             [Mike](https://github.com/jimporter/mike)             | Documentation versioning                                  |
|    [Markdownlint](https://github.com/DavidAnson/markdownlint)     | Markdown Linter                                           |
|             [Vale](https://github.com/errata-ai/vale)             | Prose Linter                                              |
|       [Visual Studio Code](https://code.visualstudio.com/)        | Recommended IDE for docs development                      |

!!! info "Required Prerequisites"
    You can get by with only Just and Docker.
    Local installations of the other tools may prove useful for development but aren't required.

## Learn More

| Topic                                                   | Description                                    |
|---------------------------------------------------------|------------------------------------------------|
| [Documentation Structure](./documentation-structure.md) | Learn how JTE's docs are organized             |
| [Local Development](./local-development.md)             | Learn how to make changes to the docs locally  |
| [Markdownlint](./markdown-lint.md)                      | Learn more about the Markdown linter           |
| [Prose Linting with Vale](./vale.md)                    | Learn more about the Prose linter              |
| [Add or Remove Pages](./add-or-remove-pages.md)         | Learn how to update the page tree              |
| [Markdown Cheatsheet](./markdown-cheatsheet.md)         | Learn more about Markdown Syntax for this site |

[^1]: [Markdown](https://en.wikipedia.org/wiki/Markdown)
