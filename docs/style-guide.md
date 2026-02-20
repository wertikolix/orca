# Style Guide

`OrcaStyle` is the single configuration object for all visual aspects of the renderer. Pass it to the `Orca` composable to customize appearance.

```kotlin
Orca(
    markdown = markdown,
    parser = OrcaMarkdownParser(),
    style = OrcaStyle(...),
)
```

Default: `OrcaStyle()` (light theme). Dark theme: `OrcaDefaults.darkStyle()`.

## OrcaStyle fields

| Field | Type | Description |
|---|---|---|
| `typography` | `OrcaTypographyStyle` | Heading and paragraph text styles |
| `inline` | `OrcaInlineStyle` | Span styles for inline elements |
| `layout` | `OrcaLayoutStyle` | Spacing and sizing |
| `quote` | `OrcaQuoteStyle` | Blockquote appearance |
| `code` | `OrcaCodeBlockStyle` | Code block appearance and syntax highlighting |
| `table` | `OrcaTableStyle` | Table appearance and layout mode |
| `thematicBreak` | `OrcaThematicBreakStyle` | Horizontal rule appearance |
| `image` | `OrcaImageStyle` | Standalone image appearance |
| `admonition` | `OrcaAdmonitionStyle` | Callout block colors and layout |
| `inlineImage` | `OrcaInlineImageStyle` | Inline image sizing |

---

## Typography (`OrcaTypographyStyle`)

Controls heading levels (H1-H6) and paragraph text.

| Property | Default |
|---|---|
| `heading1` | 30sp, Bold |
| `heading2` | 26sp, Bold |
| `heading3` | 22sp, SemiBold |
| `heading4` | 20sp, SemiBold |
| `heading5` | 18sp, Medium |
| `heading6` | 16sp, Medium |
| `paragraph` | 16sp, line height 24sp |

Helper: `heading(level: Int)` returns the style for a given level (1-6).

---

## Inline Styles (`OrcaInlineStyle`)

Span styles applied to inline markdown elements within text.

| Property | Applied to | Default |
|---|---|---|
| `inlineCode` | `` `code` `` | Monospace, semi-transparent background |
| `strikethrough` | `~~text~~` | LineThrough decoration |
| `link` | `[text](url)` | Blue (#1565C0), underline |
| `footnoteReference` | `[^ref]` | Superscript, 12sp, grey (#455A64) |
| `superscript` | `^text^` | Superscript baseline shift, 12sp |
| `subscript` | `~text~` | Subscript baseline shift, 12sp |

---

## Layout (`OrcaLayoutStyle`)

| Property | Default | Description |
|---|---|---|
| `blockSpacing` | 12dp | Vertical space between top-level blocks |
| `nestedBlockSpacing` | 8dp | Vertical space inside nested blocks (lists, quotes) |
| `listMarkerWidth` | 22dp | Width of list bullet/number column |

---

## Quote (`OrcaQuoteStyle`)

| Property | Default | Description |
|---|---|---|
| `stripeColor` | #B0BEC5 | Left border color |
| `stripeWidth` | 3dp | Left border width |
| `spacing` | 10dp | Space between stripe and content |

---

## Code Block (`OrcaCodeBlockStyle`)

| Property | Default | Description |
|---|---|---|
| `text` | 14sp, Monospace | Code text style |
| `languageLabel` | 12sp, Monospace, Medium | Language tag style |
| `lineNumber` | 12sp, Monospace, grey | Line number style |
| `background` | #F3F3F3 | Container background |
| `languageLabelBackground` | semi-transparent | Language tag background |
| `borderColor` | #D0D7DE | Container border color |
| `borderWidth` | 1dp | Container border width |
| `shape` | RoundedCornerShape(8dp) | Container shape |
| `padding` | 12dp | Content padding |
| `showLineNumbers` | `true` | Show line numbers for multiline blocks |
| `lineNumberMinWidth` | 28dp | Minimum width for line number column |
| `lineNumberEndPadding` | 12dp | Space after line numbers |
| `languageLabelPadding` | 8dp horizontal, 4dp vertical | Language tag padding |
| `showCopyButton` | `true` | Show copy-to-clipboard button |
| `syntaxHighlightingEnabled` | `true` | Enable keyword/string/comment coloring |
| `highlightKeyword` | Blue (#0B57D0), SemiBold | Keyword token style |
| `highlightString` | Green (#2E7D32) | String literal token style |
| `highlightComment` | Grey (#6D6D6D), Italic | Comment token style |
| `highlightNumber` | Purple (#8E24AA) | Number literal token style |

---

## Table (`OrcaTableStyle`)

| Property | Default | Description |
|---|---|---|
| `text` | 14sp | Cell text style |
| `headerText` | 14sp, SemiBold | Header cell text style |
| `columnWidth` | 160dp | Fixed column width (when `FIXED` mode) |
| `layoutMode` | `AUTO` | `FIXED` or `AUTO` column sizing |
| `minColumnWidth` | 120dp | Minimum column width in AUTO mode |
| `maxColumnWidth` | 320dp | Maximum column width in AUTO mode |
| `autoColumnCharacterWidth` | 7dp | Estimated width per character for AUTO sizing |
| `fillAvailableWidth` | `true` | Expand columns to fill available width |
| `cellPadding` | 10dp horizontal, 8dp vertical | Cell content padding |
| `borderColor` | #D0D7DE | Cell border color |
| `borderWidth` | 1dp | Cell border width |
| `headerBackground` | #F7F9FB | Header row background |

### Layout modes

- **`AUTO`** -- column widths calculated from content length, bounded by min/max, expanded to fill available width
- **`FIXED`** -- all columns use `columnWidth`, horizontal scroll for overflow

---

## Thematic Break (`OrcaThematicBreakStyle`)

| Property | Default | Description |
|---|---|---|
| `color` | #D0D7DE | Line color |
| `thickness` | 1dp | Line thickness |

---

## Image (`OrcaImageStyle`)

Standalone images (single image in a paragraph).

| Property | Default | Description |
|---|---|---|
| `shape` | RoundedCornerShape(8dp) | Image clip shape |
| `background` | #F7F9FB | Placeholder background |
| `maxHeight` | 360dp | Maximum image height |
| `contentScale` | `ContentScale.Fit` | How image scales within bounds |

---

## Inline Image (`OrcaInlineImageStyle`)

Images embedded within text flow.

| Property | Default | Description |
|---|---|---|
| `size` | 20dp | Image width and height |
| `shape` | RoundedCornerShape(2dp) | Image clip shape |
| `widthSp` | 18sp | Placeholder width in text units |
| `heightSp` | 18sp | Placeholder height in text units |

---

## Admonition (`OrcaAdmonitionStyle`)

GitHub-style callout blocks (`> [!NOTE]`, `> [!WARNING]`, etc.).

| Property | Default | Description |
|---|---|---|
| `stripeWidth` | 3dp | Left border width |
| `spacing` | 10dp | Space between stripe and content |
| `titleStyle` | 14sp, SemiBold | Title text style |

### Per-type colors (light theme)

| Type | Stripe color | Background |
|---|---|---|
| NOTE | #1565C0 (blue) | #0D1565C0 |
| TIP | #2E7D32 (green) | #0D2E7D32 |
| IMPORTANT | #7B1FA2 (purple) | #0D7B1FA2 |
| WARNING | #EF6C00 (orange) | #0DEF6C00 |
| CAUTION | #C62828 (red) | #0DC62828 |

---

## Dark Theme

```kotlin
Orca(
    markdown = markdown,
    parser = OrcaMarkdownParser(),
    style = OrcaDefaults.darkStyle(),
)
```

`OrcaDefaults.darkStyle()` adjusts colors for dark backgrounds:
- Light text colors for headings and paragraphs
- Darker code block and table backgrounds
- Lighter admonition stripe colors
- Adjusted link and inline code colors

`OrcaDefaults.lightStyle()` returns the default `OrcaStyle()`.

---

## Customization Examples

### Custom code block

```kotlin
OrcaStyle(
    code = OrcaCodeBlockStyle(
        background = Color(0xFF1E1E1E),
        borderColor = Color(0xFF333333),
        text = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color.White),
        showLineNumbers = false,
        showCopyButton = false,
    ),
)
```

### Custom link color

```kotlin
OrcaStyle(
    inline = OrcaInlineStyle(
        link = SpanStyle(color = Color(0xFFFF6600), textDecoration = TextDecoration.None),
    ),
)
```

### Dark theme with custom admonitions

```kotlin
OrcaDefaults.darkStyle().copy(
    admonition = OrcaAdmonitionStyle(
        noteColor = Color(0xFF90CAF9),
        warningColor = Color(0xFFFFCC02),
        cautionColor = Color(0xFFFF5252),
    ),
)
```

### Compact layout

```kotlin
OrcaStyle(
    layout = OrcaLayoutStyle(
        blockSpacing = 8.dp,
        nestedBlockSpacing = 4.dp,
        listMarkerWidth = 18.dp,
    ),
    typography = OrcaTypographyStyle(
        paragraph = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    ),
)
```
