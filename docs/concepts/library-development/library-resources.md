# Library Resources

Libraries can store static assets, such as shell scripts or YAML files, in a `resources` directory.

## Accessing A Library Resource

Within a [Library Step](./library-steps.md), the `resource(String relativePath)` can be used to return the file contents of a resource as a `String`.

## Example

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
=== "step.groovy"
    ```groovy
    void call(){
      String script = resource("doSomething.sh")
      def data = readYaml text: resource("nested/data.yaml")
    }
    ```

!!! note
    *The path parameter passed to the `resource` method must be a*relative path*within the `resources` directory
    * Only steps within a library can access the library's resources (no cross-library resource fetching)
    * The `resource()` method is only available within library steps and can't be invoked from the pipeline template
