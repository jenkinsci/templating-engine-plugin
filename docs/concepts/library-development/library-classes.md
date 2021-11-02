# Library Classes

Libraries have the option to provide groovy classes. These classes should be placed in the `src` directory of the library.

!!! tip
    Each loaded library's `src` directory contents are synchronized to a common directory. Take care not to load two libraries that provide the same class.

    One way to avoid this is to name the package after the contributing library. 

    For example, if the library's name was `example` then put the library's classes in the `src/example/` directory with `package example` at the top of the class files. 

## Class Serializability

Library classes should implement the `Serializable` interface whenever possible.

For example, a `Utility` class coming from an `example` library:

<!-- markdownlint-disable code-block-style-->
``` groovy title="example.groovy"
package example
class Utility implements Serializable {}
```

This is because Jenkins pipeline's implement a design pattern called [Continuation Passing Style (CPS)](https://github.com/jenkinsci/workflow-cps-plugin#technical-design) so that individual Pipeline Runs can resume progress.

!!! note
    To learn more, check out [Best Practices for Avoiding Serializability Exceptions](https://www.jenkins.io/doc/book/pipeline/pipeline-best-practices/#avoiding-notserializableexception)

## Classpath

Classes contributed by loaded libraries can be imported from the library's steps, steps from other libraries, and the Pipeline Template.

!!! warning
    Importing a library class from a Pipeline Template or from a step outside the library will lead to tight coupling.

    In general, library classes should be utilized *within* steps from the **same library**

## Jenkins Step Resolution

Library classes can not resolve Jenkins pipeline DSL functions such as `sh` or `echo`. A work around for this is to pass the `steps` variable to the class constructor to store on a field or through a method parameter.

For example, to use the `echo` pipeline step one could do the following:

=== "Utility Class"
    ``` groovy title="example.groovy"
    package example

    class Utility implements Serializable{
      void doThing(steps){
        steps.echo "message from the Utility class"
      }
    }
    ```
  
=== "Library Step"  
    ``` groovy title="echo_example.groovy"
    import example.Utility

    void call(){
      Utility u = new Utility()
      u.doThing(steps)
    }
    ```

## Accessing the Library Configuration

Unlike with library steps, the `config` and `pipelineConfig` variables aren't autowired to library classes.

To access these variables, they can be passed to the class through constructor or method parameters.

=== "Utility Class"
    ``` groovy title="example.groovy"
    package example

    class Utility implements Serializable{
      def config
      Utility(config){
        this.config = config
      }

      void doThing(steps){
        steps.echo "library config: ${config}"
      }
    }
    ```
=== "Library Step"
    ``` groovy title="get_config.groovy"
    import example.Utility

    void call(){
      Utility u = new Utility(config)
      u.doThing(steps)
    }
    ```
