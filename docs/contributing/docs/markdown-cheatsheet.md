# Markdown Cheatsheet

## Headers

```markdown
# H1
## H2
### H3
#### H4
##### H5
###### H6
```

## Emphasis

| Style                  | Markdown                 |
|------------------------|--------------------------|
| *Italics*              | `*Italics*`              |
| **Bold**               | `**Bold**`               |
| **_Bold and Italics_** | `**_Bold and Italics_**` |

## Links

There are 3 primary ways embed a hyperlink.

=== "Markdown"
    ```markdown
    1. you can use an [inline link](https://google.com)
    2. you can use a [link by reference][1]
    3. you can use the [link text itself] as the reference
    [1]: https://google.com
    [link text itself]: https://google.com
    ```
=== "Rendered"
    1. you can use an [inline link](http://google.com)
    2. you can use a [link by reference][1]
    3. you can use the [link text itself] as the reference

[1]: https://google.com
[link text itself]: https://google.com

## Tables

<!-- markdownlint-disable -->
!!! tip
    Tables are kind of a pain in markdown.
    This [tool](https://www.tablesgenerator.com/markdown_tables) can help to generate your markdown tables for you. 

    The [Markdown Table Prettifier](https://marketplace.visualstudio.com/items?itemName=darkriszty.markdown-table-prettify) extension for VS Code is also pretty great.
<!-- markdownlint-restore -->

=== "Markdown"
    ```markdown
    |  column 1  |   column 2  | column 3     |
    | ---------- | :---------: | -----------: |
    | column 1   | column 2    | column 3  is |
    | is left    |   is        | is right     |
    | aligned    | centered    | aligned      |
    ```
=== "Rendered"
    |  column 1  |   column 2  | column 3     |
    | ---------- | :---------: | -----------: |
    | column 1   | column 2    | column 3  is |
    | is left    |   is        | is right     |
    | aligned    | centered    | aligned      |

## Inline Code Snippets

=== "Markdown"
    ```markdown
    Inline `code` snippets use `backticks` around them
    ```
=== "Rendered"
    Inline `code` snippets use `backticks` around them

## Code Blocks

code blocks use three backticks and the language name for syntax highlighting:

=== "Markdown"
    `````markdown
    ```groovy
    def s = [ 1, 2, 3]
    s.each{ item ->
      println item
    }
    ```
    `````
=== "Rendered"
    ```groovy
    def s = [ 1, 2, 3]
    s.each{ item ->
      println item
    }
    ```

## Admonitions

Squidfunk covers this on the [Admonitions page](https://squidfunk.github.io/mkdocs-material/reference/admonitions/) of the Material for MkDocs docs site.

Please use consistent admonitions based upon the type of content being added.

| Admonition Type | Description                           |
|-----------------|---------------------------------------|
| `example`       | Examples of what's being discussed    |
| `tip`           | A recommendation from the maintainers |
| `danger`        | Call outs for common gotchas          |
| `info`          | Redirect users to more information    |

<!--markdownlint-disable-next-line-->
## Emojis :smile:

To embed an emoji, simply surround the emoji name with two colons: `:emoji-name:`.

A list of the available emojis can be found at [emojipedia](https://emojipedia.org/twitter/).

## Content Tabs

[Content Tabs](https://squidfunk.github.io/mkdocs-material/reference/content-tabs/) have been used throughout this page.

```markdown
=== "Tab Title A"
    some markdown content
=== "Tab Title B"
    some other markdown content
```

=== "Tab Title A"
    some markdown content
=== "Tab Title B"
    some other markdown content

## Footnotes

=== "Markdown"
    ```markdown
    Footnotes[^1] are supported.
    [^1]: some footnote information
    ```
=== "Rendered"
    Footnotes[^1] are supported.

[^1]: here's some footnote text
