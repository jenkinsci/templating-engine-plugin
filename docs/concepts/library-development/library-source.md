# Library Source

A Library Source is a reference to a location where one or more libraries can be fetched.

## Library Source Structure

Within a configured Library Source, a library is a *directory*.

The name of the directory is the identifier that would be declared in the `libraries{}` block of the Pipeline Configuration.

## Library Providers

The Jenkins Templating Engine plugin provides an interface to create Library Sources.

The plugin comes with two types of Library Sources built-in: SCM Library Sources and Plugin Library Sources.

### Source Code Library Source

The SCM Library Source is used to fetch libraries from a source code repository.
This repository can be a local directory with a `.git` directory accessible from the Jenkins Controller or a remote repository.

### Plugin Library Source

The Plugin Library Source is used when libraries have been bundled into a separate plugin.
This option will only be available in the Jenkins UI when a plugin has been installed that can serve as a library-providing plugin.

!!! note "Learn More"
    To learn more, check out [How to Package a Library Source as a Plugin](../../../how-to/library-development/package-libraries-as-plugin)
