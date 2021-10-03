<!-- markdownlint-disable first-line-heading -->
!!! tip "Tip: Design Patterns in Action"
    There are two general software architecture design patterns that might help in understanding the way JTE works.

    The first is the [Template Method Design Pattern](https://refactoring.guru/design-patterns/template-method). You could think of the pipeline template as the `AbstractClass` that defines the scaffold of an algorithm (pipeline) and libraries as various implementations of a `ConcreteClass` that bring implementations of steps in the algorithm.

    The second is [Dependency Injection](https://en.wikipedia.org/wiki/Dependency_injection). You could think of the JTE framework as the `Injector`, the pipeline template the `Client`, and each step a `Dependency` being injected.
