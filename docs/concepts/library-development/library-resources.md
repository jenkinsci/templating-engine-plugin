# Library Resources

Libraries can store static assets, such as shell scripts or YAML files, in a `resources` directory.

## Accessing A Library Resource

Within a [Library Step](./library-steps.md), the `resource(String relativePath)` method can be used to return the file contents of a resource as a `String`.

!!! example
    In the following example, a Library Step is created that executes a script from the root of the resources directory and then reads in data from a YAML file nested within the resources directory.
    === "Library Structure"
        ```
        exampleLibraryName
        ├── steps
        │   └── step.groovy
        ├── resources
        │   ├── doSomething.sh
        │   └── nested
        │       └── data.yaml
        └── library_config.groovy
        ```
    === "Library Step"
        ``` groovy title="step.groovy"
        void call(){
          String script = resource("doSomething.sh")
          def data = readYaml text: resource("nested/data.yaml")
        }
        ```

!!! important
    1. The path parameter passed to the `resource` method must be a *relative path* within the `resources` directory
    2. Only steps within a library can access the library's resources (no cross-library resource fetching)
    3. The `resource()` method is only available within Library Steps and can't be invoked from the Pipeline Template
