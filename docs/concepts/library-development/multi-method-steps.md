# Multi-Method Library Steps

Typically, [Library Steps](./library-steps.md) define a `call()` method that allows the step to be invoked via its name (such as `build()`).

This isn't required. Groovy's Call Operator[^1] means that invoking `build()` functionally equivalent to invoking `build.call()`.

Steps, therefore, can define alternative methods beyond just the `call()` method.

## Use Case: Utility Steps

Multi-Method Library Steps are most useful when creating Library Steps that wrap a particular utility.
The methods on the step can then represent different actions the utility can take.

!!! example "Utility Step Example: Git"

    === "Git Utility Step"

        ``` groovy title="git.groovy"
        void add(String files){
          sh "git add ${files}"
        }

        void commit(String message){
          sh "git commit -m ${message}"
        }

        void push(){
          sh "git push" 
        }
        ```

    === "Usage"

        ``` groovy title="Jenkinsfile"
        node{
          checkout scm
          writeFile file: 'test.txt', text: 'hello, world'
          git.add('test.txt')
          git.commit('add test file')
          git.push()
        }
        ```

## Groovy Command Chain

Expressive DSLs can be created when coupling multi-method steps with Groovy's [Command Chain](http://docs.groovy-lang.org/docs/latest/html/documentation/core-domain-specific-languages.html#_command_chains) feature.

!!! example "Using Command Chains With The Git Utility"
    Command Chains could be used to improve upon the previous example.

    === "Without Command Chains"
        ``` groovy title="Jenkinsfile"
        node{
          checkout scm
          writeFile file: 'test.txt', text: 'hello, world'
          git.add('test.txt')
          git.commit('add test file')
          git.push()
        }
        ```
    === "**With** Command Chains"
        ``` groovy title="Jenkinsfile"
        node{
          checkout scm
          writeFile file: 'test.txt', text: 'hello, world'
          git add 'test.txt'
          git commit 'add test file'
          git.push()
        }
        ```

[^1]: [Groovy Call Operator](https://groovy-lang.org/operators.html#_call_operator)
