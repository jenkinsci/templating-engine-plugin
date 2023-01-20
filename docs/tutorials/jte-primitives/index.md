# JTE: Pipeline Primitives

In this lab, we're going to walk through each of the JTE pipeline primitives and describe their usage.

## What Are Primitives?

When writing reusable Pipeline Templates through the Jenkins Templating Engine, one of the primary goals is to keep the template as easy-to-read as humanly possible.

JTE pipeline primitives are defined in your pipeline configuration file and primarily serve to aid in this endeavor by providing "syntactic sugar" during the runtime execution of the template. "Sugar" in that it has no functional benefit but makes the code easier to read, and therefore maintain.

Throughout this lab, we will cover each of the JTE pipeline primitives and describe when to use them and how they make writing templates even easier.

!!! important
    Pipeline primitives are defined in the aggregated pipeline configuration and make writing and reading Pipeline Templates easier.

## What You'll Learn

* The Application Environment Primitive
* The Stage Primitive
* The Keyword Primitive
