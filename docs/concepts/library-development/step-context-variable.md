# Step Context

[Steps](./library-steps.md) can resolve a `stepContext` variable.

| Field                 | Description                                                                                                         |
|-----------------------|---------------------------------------------------------------------------------------------------------------------|
| `stepContext.name`    | The name of the step being executed. May be an alias.                                                               |
| `stepContext.library` | The library that contributed the step. Will be `null` for no-op [placeholder steps](../advanced/placeholder-steps). |
| `stepContext.isAlias` | True when `stepContext.name` refers to an alias, False otherwise.                                                   |
