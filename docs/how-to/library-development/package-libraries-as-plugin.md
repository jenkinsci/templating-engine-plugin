# Package a Library Source as a Plugin

Users can package their Library Source as a stand-alone Jenkins Plugin. 

This has a couple advantages:

1. **Performance**: Avoid fetching from a remote repository on every Pipeline Run
2. **Plugin Dependencies**: Ensure the Jenkins Plugins your libraries depend upon are installed

## Gradle JTE Plugin

To package your Library Source, the simplest path is to use the [Gradle JTE Plugin](https://github.com/jenkinsci/gradle-jte-plugin).

```groovy
plugins{
  // used to packate these libraries as a jenkins plugin
  id "io.jenkins.jte" version "0.2.0"
}
```

You can use the configuraiton options defined in the [README](https://github.com/jenkinsci/gradle-jte-plugin/blob/main/README.md) to configure the plugin.

To package your Library Source, run `./gradlew jte`.
The `hpi` file will be output to your projects build directory.

from there, you can install the `hpi` file via the Jenkins UI.

!!! note
    To see an example Library Source, check out the [jte-library-scaffold](https://github.com/steven-terrana/jte-library-scaffold).