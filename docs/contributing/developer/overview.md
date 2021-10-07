# Overview

## Local Environment

| Tool                                         | Purpose                                                                       |
|----------------------------------------------|-------------------------------------------------------------------------------|
| [Gradle]( https://gradle.org)                | Used to run unit tests, package the JPI, and publish the plugin               |
| [Just](https://github.com/casey/just)        | A task runner. Used here to automate common commands used during development. |
| [Docker](https://www.docker.com/get-started) | Used to build the documentation for local preview                             |

## Running Tests

To run all the tests, run:

```bash
just test
```

The gradle test report is published to `build/reports/tests/test/index.html`

### Execute tests for a specific class

To run tests for a specific Class, `StepWrapperSpec` for example, run:

```bash
just test '*.StepWrapperSpec'
```

### Code Coverage

By default, [JaCoCo](https://github.com/jacoco/jacoco) is enabled when running test.

Once executed, the JaCoCo coverage report can be found at: `build/reports/jacoco/test/html/index.html`

To disable this, run:

```bash
just --set coverage false test
```

## Linting

This project uses [Spotless](https://github.com/diffplug/spotless) and [CodeNarc](https://github.com/CodeNarc/CodeNarc) to perform linting.
The CodeNarc rule sets for `src/main` and `src/test` can be found in `config/codenarc/rules.groovy` and `config/codenarc/rulesTest.groovy`, respectively.

To execute linting, run:

```bash
just lint-code
```

Once executed, the reports can be found at `build/reports/codenarc/main.html` and `build/reports/codenarc/test.html`.

## Building the Plugin

To build the JPI, run:

```bash
just jpi
```

Once built, the JPI will be located at `build/libs/templating-engine.jpi`

## Run a containerized Jenkins

It's often helpful to run Jenkins in a container locally to test various scenarios with JTE during development.

```bash
just run 
```

With the default settings, this will expose jenkins on [http://localhost:8080](http://localhost:8080)

### Change the container name

```bash
just --set container someName run
```

### Change the port forwarding target

```bash
just --set port 9000 run
```

### Pass arbitrary flags to the container

Parameters passed to `just run` are sent as flags to the `docker run` command.

```bash
just run -e SOMEVAR="some var"
```

### Mounting local libraries for testing

Local directories can be configured as Git SCM library sources even if they don't have a remote repository.

For example, if `~/local-libraries` is a directory containing a local git repository then to mount it to the container you would run:

```bash
just run -v ~/local-libraries:/local-libraries 
```

You could then configure a library source using the file protocol to specify the repository location at `file:///local-libraries`

<!-- markdownlint-disable -->
!!! tip 
    When using this technique, changes to the libraries must be committed to be found. In a separate terminal, run:

    ```bash
    just watch ~/local-libraries
    ```

    to automatically commit changes to the libraries. 
<!-- markdownlint-restore -->