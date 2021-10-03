# Library Source

Library Sources are a reference to a location where one or more libraries can be loaded.

## Library Source Structure

Within a configured library source, a library is a *directory*.

The name of the directory is the identifier that would be declared in the `libraries{}` block of the pipeline configuration.

## Library Providers

The Jenkins Templating Engine plugin provides an interface to create library sources.

The plugin comes with two types of library sources built-in: SCM Libary Sources and Plugin Library Sources

### Source Code Management Library Source

The Source Code Managemetn (SCM) Library Source is used to fetch libraries from a source code repository.
This repository can be a local directory with a `.git` directory accessible from the Jenkins Controller or a remote repository.

!!! tip
    To learn how to configure an SCM Library Source, check out the [how-to guide](../../how-to/scm-library-source.md).

### Plugin Library Source

The Plugin Library Source is used when libraries have been bundled into a separate plugin.
This option will only be available in the Jenkins UI when a plugin has been installed that can serve as a library-providing plugin.

!!! tip
    To learn how to create a library-providing plugin, check out the [how-to guide](../../how-to/library-providing-plugin.md)
