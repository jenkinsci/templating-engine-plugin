# Bottom-Up Explanation

Welcome :wave:!

This page is going to take you on a journey from how pipelines are typically built today (on a per-application basis), meander through the challenges that causes at scale, and then reverse-engineer together how the Jenkins Templating Engine works to remediate those pain points. 

This will be one of the lengthier pages in the documentation.
If you can stick to it, you'll come out the other side seeing pipeline development differently.

## Let's Get Started Without JTE

Let's start by imagining that there are **three** applications that need a pipeline. 

Click through the tabs below to see a pipeline for a Gradle application, a Maven application, and an NPM application. 

=== "Gradle"
    ```groovy
    // a basic Gradle pipeline
    stage("Unit Test"){
      node{
        sh "gradle verify"
      }
    }
    stage("Build"){
      node{
        sh "gradle build"
      }
    }
    ```
=== "Maven"
    ```groovy
    // a basic Maven pipeline
    stage("Unit Test"){
      node{
        sh "mvn verify"
      }
    }
    stage("Build"){
      node{
        sh "mvn build"
      }
    }
    ```
=== "NPM"
    ```groovy
    // a basic NPM pipeline
    stage("Unit Test"){
      node{
        sh "npm ci && npm run test"
      }
    }
    stage("Build"){
      node{
        sh "npm ci && npm build"
      }
    }
    ```

Traditionally, these pipelines would be within the source code repository for the application.  

### Why This Approach Doesn't Scale

Defining pipelines on a per-application basis works when you have a *small number of teams you're supporting*. 

Over time, though, "DevOps Teams" at organizations find themselves responsible for administering internal tools and scaling the adoption of those tools across the organization. 

Why is creating bespoke pipelines such a big deal? It doesn't scale. 

1. Individual pipelines means work needs to be done to integrate each application with a pipeline
2. Duplicated pipipelines introduces uncertainty that common processes are followed
3. Complexity becomes difficult to manage  

#### Requires Onboarding Each Team Individually

When pipelines are built on a per-application basis, it leaves organizations with two choices.
Either you need a developer on every team that knows how to write pipelines aligned with organizational standards or you need a centralized team onboarding teams to a pipeline. 

The first choice often isn't super viable - developers should focus on the problem that they're best suited to solve: developing applications. 
Even if all the software developers *could* write their own automated pipelines, it becomes very challenging to ensure these pipelines follow required compliance checks.

The second choice requires that you scale the centralized team to meet the number of teams that need support.
This is often prohibitively expensive.

#### Standardization & Compliance

When pipelines are built on a per-application basis, it becomes extremely challenging to be confident that teams throughout the organization are following approved software delivery processes aligned with cyber compliance requirements. 

Furthermore, for more tightly governed environments, the idea of a developer being able to modify the Jenkinsfile on their branch to perform a deployment to production is just not an acceptable threat vector.
Mitigating this possibility requires very mature monitoring and correlating across systems. 

#### Complexity

There's an old adage, "running one container is easy, running many is really really hard."
The same applies for pipelines within an organization. 

Creating a pipeline that runs unit tests, builds artifacts, deploys those artifacts, and runs security scans gets exponentially more complex the more teams and technical stacks need to be supported.

## A Better Way: Pipeline Templating

These challenges are all caused by one thing: *building pipelines on a per-application basis*.

Even if you're modularizing your pipeline code for reuse through Jenkins Shared Libraries, you still need to duplicate the Jenkinsfile across every branch of every source code repository. 

!!! warning 
    Don't even get me started on how tricky it can be to propagate a *change* to the pipeline across teams when the representation of the pipeline lives across each branch independently.

Taking a step back, you may have noticed that each of the three pipelines above follow the same structure.
First they execute unit tests, then they perform a build. 

In our experience, this pattern holds true most of the times (*especially* when working with microservices).
While the specific *tools* that get used to perform a **step** of the pipeline may change, the workflow remains the same. 

### Writing Our First Template

The entire philosophy behind the Jenkins Template Engine stems from this concept of common workflows with interchangeable tools. 

What if instead of defining pipelines on a *per-application basis* we could define tool-agnostic pipeline templates in a central location? 

Said another way, what if we could translate those three separate pipelines above into:

```groovy
unit_test()
build()
```

This kind of works! 
We've got our first pipeline template.

Through JTE, we can define this pipeline template in one place for use across multiple repositories. 

!!! note
    For the sake of sticking to concepts, let's just skip over how JTE knows where to find this template. 

    You can learn more about how to configure this in [template selection](/concepts/pipeline-templates/pipeline-template-selection.md)


We'll need to create implementations of those `unit_test` and `build` steps though if this template is going to do anything.

### Writing Our First Pipeline Steps

Let's refactor those pipelines above into `unit_test` and `build` **steps**.
We'll need a version of those methods for each of the three build tools we need to support: gradle, maven, and npm. 

=== "Gradle: `unit_test`"
    ```groovy
    // file: gradle/steps/unit_test.groovy
    void call(){
      stage("Unit Test"){
        node{
          sh "gradle verify"
        }
      }
    }
    ```
=== "Gradle: `build`"
    ```groovy
    // file: gradle/steps/build.groovy
    void call(){
      stage("Build"){
        node{
          sh "gradle build"
        }
      }
    }
    ```
<br>
=== "Maven: `unit_test`"
    ```groovy
    // file: maven/steps/unit_test.groovy
    void call(){
      stage("Unit Test"){
        node{
          sh "mvn verify"
        }
      }
    }
    ```
=== "Maven: `build`"
    ```groovy
    // file: maven/steps/build.groovy
    void call(){
      stage("Build"){
        node{
          sh "mvn build"
        }
      }
    }
    ```
<br>
=== "NPM: `unit_test`"
    ```groovy
    // file: npm/steps/unit_test.groovy
    void call(){
      stage("Unit Test"){
        node{
          sh "npm ci && npm run test"
        }
      }
    }
    ```
=== "NPM: `build`"
    ```groovy
    // file: npm/steps/build.groovy
    void call(){
      stage("Build"){
        node{
          sh "npm ci && npm build"
        }
      }
    }
    ```

!!! note
    These **steps** are the *exact same pipeline code* we had written at the start.
    
    We just wrapped it in a `call` method for Reasons :tm: you can learn about over on the [library steps](/concepts/library-development/library-steps.md) page.

### Understanding Libraries

If you paid attention to the comments indicating the files those steps were placed in, you'll have noticed the following file structure: 

```
.
├── gradle
│   └── steps
│       ├── build.groovy
│       └── unit_test.groovy
├── maven
│   └── steps
│       ├── build.groovy
│       └── unit_test.groovy
└── npm
    └── steps
        ├── build.groovy
        └── unit_test.groovy
```

A **library** in JTE is a collection of **steps** (in a directory) that can be loaded at runtime.

Earlier, we defined a common pipeline template that executes unit tests then performs a build.
This template can be shared across teams. 

Next, we defined three *libraries*: `npm`, `gradle`, and `maven`. 
Each of these libraries implemented the steps required by the template. 

Next, we'll need a way to tell an individual team's pipeline which library to load. 

!!! note
    We'll need to tell Jenkins where it can find libraries.
    This is where [library sources](/concepts/library-development/library-source.md) come in.
    
    For the sake of focusing on the **concepts**, let's assume JTE knows where to find these libraries.

### Pipeline Configuration

We have a template that invokes steps.
We have libraries that provide steps. 
Now we need a way to link the two. 

This is where the pipeline configuration comes in.
The pipeline configuration uses a groovy-based configuration language.

Let's create a pipeline configuration that specifies the `npm` library should be loaded. 

```groovy
libraries{
  npm
}
```

When a pipeline using JTE runs with this configuration and template, the steps from the `npm` library are loaded. 

This means that the `unit_test` and `build` steps within the template will now resolve to the `unit_test` and `build` steps provided by the `npm` library!

By swapping which library gets loaded, we can use the **same template** across multiple teams while still supporting **multiple tech stacks**. 

--8<-- "concepts/framework-overview/snippets/design-patterns.md"