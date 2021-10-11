# Local Development

## Local Docs Server

MkDocs supports a local development server so that changes can be viewed in the browser in real-time as they're made.

To see changes in real-time, run `just serve`.

After a few seconds, a local version of the docs will be hosted at [http://localhost:8000](http://localhost:8000).

!!! note "Prerequisites"
    Check out the [Prerequisites](./getting-started.md#tools) to make sure you've got the required tools installed.

## Integrated Development Environment Integration

[Visual Studio Code](https://code.visualstudio.com/) is the recommended IDE for updating the docs as it has extensions for both linting tools used:

* [markdownlint](https://marketplace.visualstudio.com/items?itemName=DavidAnson.vscode-markdownlint)
* [Vale](https://marketplace.visualstudio.com/items?itemName=errata-ai.vale-server)

!!! note "Local Tool Installation"
    To use the VS Code extensions for markdownlint and vale you'll have to install those tools locally.
