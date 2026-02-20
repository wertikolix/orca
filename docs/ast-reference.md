# AST Reference

`OrcaDocument` is the root node of every parsed markdown file. It contains an ordered list of `OrcaBlock` nodes representing block-level content, plus optional front matter metadata. Block nodes that carry textual content hold `OrcaInline` nodes for text-level formatting, links, images, etc.

All types are defined in `ru.wertik.orca.core.OrcaDocument.kt`.

---

## OrcaDocument

Root of the AST.

| Property | Type | Description |
|---|---|---|
| `blocks` | `List<OrcaBlock>` | Ordered list of top-level block nodes |
| `frontMatter` | `OrcaFrontMatter?` | Front matter metadata, or `null` if absent |

---

## Front Matter

Sealed interface `OrcaFrontMatter` with two variants.

### OrcaFrontMatter.Yaml

YAML front matter delimited by `---`.

```markdown
---
title: My Page
author: Alice
---
```

### OrcaFrontMatter.Toml

TOML front matter delimited by `+++`.

```markdown
+++
title = "My Page"
author = "Alice"
+++
```

### Properties (common to both variants)

| Property | Type | Description |
|---|---|---|
| `raw` | `String` | Unparsed front matter text (without delimiters) |
| `entries` | `Map<String, String>` | Parsed key-value pairs |

---

## Block Nodes

Sealed interface `OrcaBlock`. Each variant maps to a specific markdown construct.

### Heading

ATX or setext heading.

| Property | Type | Description |
|---|---|---|
| `level` | `Int` | Heading level (1–6) |
| `content` | `List<OrcaInline>` | Inline elements forming the heading text |

```markdown
# Heading 1
## Heading 2
### Heading 3
```

### Paragraph

A paragraph of inline content.

| Property | Type | Description |
|---|---|---|
| `content` | `List<OrcaInline>` | Inline elements forming the paragraph text |

```markdown
This is a paragraph with **bold** and *italic* text.
```

### ListBlock

Ordered or unordered list.

| Property | Type | Description |
|---|---|---|
| `ordered` | `Boolean` | `true` for ordered (numbered) lists |
| `items` | `List<OrcaListItem>` | List entries, possibly containing nested blocks |
| `startNumber` | `Int` | Starting number for ordered lists (default: `1`) |

```markdown
- Item one
- Item two
- Item three

1. First
2. Second
3. Third
```

### Quote

Block quote containing nested block-level content.

| Property | Type | Description |
|---|---|---|
| `blocks` | `List<OrcaBlock>` | Nested block content inside the quote |

```markdown
> This is a block quote.
> It can span multiple lines.
```

### Admonition

Callout block such as NOTE, TIP, or WARNING.

| Property | Type | Description |
|---|---|---|
| `type` | `OrcaAdmonitionType` | Admonition severity/category |
| `title` | `String?` | Custom title, or `null` to use the default for `type` |
| `blocks` | `List<OrcaBlock>` | Nested block content inside the admonition |

```markdown
> [!NOTE]
> This is a note admonition.

> [!WARNING] Custom Title
> Be careful here.
```

### CodeBlock

Fenced or indented code block.

| Property | Type | Description |
|---|---|---|
| `code` | `String` | Raw code content |
| `language` | `String?` | Info-string language hint, or `null` if unspecified |

````markdown
```kotlin
fun main() {
    println("Hello")
}
```
````

### Image (Block)

Block-level image (standalone image not wrapped in a paragraph).

| Property | Type | Description |
|---|---|---|
| `source` | `String` | Image URL or path |
| `alt` | `String?` | Alternative text, or `null` |
| `title` | `String?` | Optional title attribute |

```markdown
![Alt text](image.png "Optional title")
```

### ThematicBreak

Horizontal rule. Data object — no properties.

```markdown
---
***
___
```

### Table

Markdown table.

| Property | Type | Description |
|---|---|---|
| `header` | `List<OrcaTableCell>` | Header row cells |
| `rows` | `List<List<OrcaTableCell>>` | Body rows, each a list of `OrcaTableCell` |

```markdown
| Name  | Age | City   |
|-------|-----|--------|
| Alice |  30 | London |
| Bob   |  25 | Paris  |
```

### Footnotes

Collected footnote definitions referenced elsewhere in the document.

| Property | Type | Description |
|---|---|---|
| `definitions` | `List<OrcaFootnoteDefinition>` | List of footnote definitions |

```markdown
[^1]: This is the first footnote.
[^note]: This is another footnote.
```

### HtmlBlock

Raw HTML block passed through verbatim.

| Property | Type | Description |
|---|---|---|
| `html` | `String` | Raw HTML content |

```markdown
<div class="custom">
  <p>Raw HTML block</p>
</div>
```

---

## Inline Nodes

Sealed interface `OrcaInline`. Each variant maps to a specific inline construct.

### Text

Plain text span.

| Property | Type | Description |
|---|---|---|
| `text` | `String` | The text content |

```markdown
Just plain text.
```

### Bold

Strong emphasis wrapping nested inlines.

| Property | Type | Description |
|---|---|---|
| `content` | `List<OrcaInline>` | Nested inline elements |

```markdown
**bold text**
__bold text__
```

### Italic

Emphasis wrapping nested inlines.

| Property | Type | Description |
|---|---|---|
| `content` | `List<OrcaInline>` | Nested inline elements |

```markdown
*italic text*
_italic text_
```

### Strikethrough

Strikethrough wrapping nested inlines.

| Property | Type | Description |
|---|---|---|
| `content` | `List<OrcaInline>` | Nested inline elements |

```markdown
~~deleted text~~
```

### Superscript

Superscript wrapping nested inlines.

| Property | Type | Description |
|---|---|---|
| `content` | `List<OrcaInline>` | Nested inline elements |

```markdown
^superscript^
```

### Subscript

Subscript wrapping nested inlines.

| Property | Type | Description |
|---|---|---|
| `content` | `List<OrcaInline>` | Nested inline elements |

```markdown
~subscript~
```

### InlineCode

Inline code span.

| Property | Type | Description |
|---|---|---|
| `code` | `String` | The code content |

```markdown
`inline code`
```

### Link

Hyperlink.

| Property | Type | Description |
|---|---|---|
| `destination` | `String` | URL or anchor target |
| `content` | `List<OrcaInline>` | Inline elements forming the link text |
| `title` | `String?` | Optional title attribute (default: `null`) |

```markdown
[link text](https://example.com)
[link text](https://example.com "Title")
```

### Image (Inline)

Inline image.

| Property | Type | Description |
|---|---|---|
| `source` | `String` | Image URL or path |
| `alt` | `String?` | Alternative text, or `null` |
| `title` | `String?` | Optional title attribute |

```markdown
![alt text](image.png "title")
```

### FootnoteReference

Reference to a footnote definition.

| Property | Type | Description |
|---|---|---|
| `label` | `String` | Reference label (e.g. `"1"` for `[^1]`) |

```markdown
Some text[^1] with a footnote[^note].
```

### HtmlInline

Raw inline HTML passed through verbatim.

| Property | Type | Description |
|---|---|---|
| `html` | `String` | Raw HTML content |

```markdown
This has <em>inline HTML</em> in it.
```

---

## Supporting Types

### OrcaListItem

Single item in a `ListBlock`.

| Property | Type | Description |
|---|---|---|
| `blocks` | `List<OrcaBlock>` | Block-level content of this list item |
| `taskState` | `OrcaTaskState?` | Checkbox state for task lists, or `null` for regular items |

```markdown
- [x] Completed task
- [ ] Incomplete task
- Regular item
```

### OrcaTaskState

Checkbox state for task-list items.

| Value | Syntax | Description |
|---|---|---|
| `CHECKED` | `[x]` | Completed task |
| `UNCHECKED` | `[ ]` | Incomplete task |

### OrcaTableCell

Single cell in a markdown table.

| Property | Type | Description |
|---|---|---|
| `content` | `List<OrcaInline>` | Inline elements inside the cell |
| `alignment` | `OrcaTableAlignment?` | Column alignment, or `null` if unspecified |

### OrcaTableAlignment

Column alignment in a markdown table.

| Value | Syntax | Description |
|---|---|---|
| `LEFT` | `:---` | Left-aligned column |
| `CENTER` | `:---:` | Center-aligned column |
| `RIGHT` | `---:` | Right-aligned column |

### OrcaAdmonitionType

Admonition category/severity.

| Value | Description |
|---|---|
| `NOTE` | General note |
| `TIP` | Helpful tip |
| `IMPORTANT` | Important information |
| `WARNING` | Warning |
| `CAUTION` | Caution / danger |

### OrcaFootnoteDefinition

Footnote definition mapping a label to its block content.

| Property | Type | Description |
|---|---|---|
| `label` | `String` | Reference label (e.g. `"1"` for `[^1]`) |
| `blocks` | `List<OrcaBlock>` | Block-level content of the footnote |
