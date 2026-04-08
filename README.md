# Orca

Compose Multiplatform Markdown renderer. Targets **Android**, **iOS**, **Desktop (JVM)**, and **wasmJs**.

[![CI](https://github.com/wertikolix/Orca/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/wertikolix/Orca/actions/workflows/ci.yml?query=branch%3Amain)
[![Maven Central](https://img.shields.io/maven-central/v/ru.wertik/orca-core)](https://central.sonatype.com/artifact/ru.wertik/orca-core)

## Status

- Current stable minor: `0.9.4`
- Maturity: lightweight production-ready core subset (Markdown-first)

## Documentation

- [Architecture Overview](docs/architecture.md) -- module structure, parsing pipeline, rendering pipeline
- [AST Reference](docs/ast-reference.md) -- complete list of all block and inline node types
- [Style Guide](docs/style-guide.md) -- full reference for OrcaStyle configuration
- [Cookbook](docs/cookbook.md) -- practical recipes for common use cases

## Why Orca

- Small API surface
- Predictable AST (`orca-core`) and Compose renderer (`orca-compose`)
- Safe defaults for links and images
- No mandatory heavy runtime dependencies
- Compose Multiplatform: single codebase for Android, iOS, Desktop, Web

## Modules

- `orca-core`
  - Kotlin Multiplatform
  - AST model (common)
  - parser interface + built-in parser (common)
  - backend: `org.jetbrains:markdown` (`intellij-markdown`, GFM flavour)
- `orca-compose`
  - Compose Multiplatform renderer for `OrcaDocument`
  - Targets: Android, iOS, Desktop (JVM), wasmJs
  - Style model (`OrcaStyle`)
  - Image loading via Coil 3 + Ktor (pluggable — bring your own image loader)
- `sample-app`
  - Android demo for manual checks

## Maven

```kotlin
// Kotlin Multiplatform (commonMain)
implementation("ru.wertik:orca-core:0.9.4")
implementation("ru.wertik:orca-compose:0.9.4")
```

Gradle resolves platform-specific artifacts automatically (`orca-core-jvm`, `orca-compose-android`, etc.).

## Quick Start

### Parse markdown

```kotlin
import ru.wertik.orca.core.OrcaMarkdownParser
import ru.wertik.orca.core.OrcaParser

val parser: OrcaParser = OrcaMarkdownParser()
val document = parser.parse(markdown)
```

> `OrcaMarkdownParser` uses `org.jetbrains:markdown` and is available in `commonMain` (Android, iOS, Desktop, wasmJs).

### Parse markdown with cache key

```kotlin
val parser = OrcaMarkdownParser()
val document = parser.parseCached(
    key = "message-42",
    input = markdown,
)
```

Use a stable key per message/item to avoid repeated AST rebuilds across recompositions and list reuse.

### Parse markdown with diagnostics

```kotlin
val parser = OrcaMarkdownParser(maxTreeDepth = 32)
val result = parser.parseWithDiagnostics(markdown)

val document = result.document
val warnings = result.diagnostics.warnings
val errors = result.diagnostics.errors
```

### Render from markdown

```kotlin
import ru.wertik.orca.compose.Orca
import ru.wertik.orca.compose.OrcaRootLayout
import ru.wertik.orca.core.OrcaMarkdownParser

Orca(
    markdown = markdown,
    parser = OrcaMarkdownParser(),
    parseCacheKey = "message-42",
    rootLayout = OrcaRootLayout.COLUMN, // use when parent already controls scrolling
    securityPolicy = OrcaSecurityPolicies.Default,
    onLinkClick = { url ->
        // open via your app policy
    },
    onParseDiagnostics = { diagnostics ->
        // observe warnings/errors if needed
    },
)
```

### Render from pre-parsed AST

```kotlin
import ru.wertik.orca.compose.Orca

Orca(
    document = document,
)
```

### Streaming / LLM chat

For token-by-token streaming (e.g. LLM responses), Orca debounces parse operations to avoid redundant work:

```kotlin
Orca(
    markdown = streamingMarkdown, // updated on every token
    parser = OrcaMarkdownParser(),
    parseCacheKey = "message-42",
    streamingDebounceMs = 80, // default; set 0 to disable
)
```

During fast updates, only the latest markdown value is parsed after the debounce window. The first render is always synchronous (no empty frame), so items in a scrollable list appear at their correct size immediately.

## Public API

```kotlin
fun interface OrcaParser {
    fun parse(input: String): OrcaDocument
    fun parseWithDiagnostics(input: String): OrcaParseResult
    fun parseCached(key: Any, input: String): OrcaDocument
    fun parseCachedWithDiagnostics(key: Any, input: String): OrcaParseResult
}
```

`OrcaMarkdownParser` options:

```kotlin
OrcaMarkdownParser(
    maxTreeDepth = 64,
    cacheSize = 64,
    enableSuperscript = true,  // set false to disable ^text^ parsing
    enableSubscript = true,    // set false to disable ~text~ parsing
    onDepthLimitExceeded = { depth ->
        // observe depth truncation if needed
    },
)
```

Diagnostics model:

```kotlin
data class OrcaParseResult(
    val document: OrcaDocument,
    val diagnostics: OrcaParseDiagnostics,
)
```

## Supported Syntax (`0.9.4`)

### Blocks

- heading
- paragraph
- bullet list
- ordered list (start number preserved)
- quote
- fenced code block
- indented code block
- thematic break (`---`)
- standalone image block
- GFM tables
- HTML blocks (styled rendering with tag support)
- footnote definitions
- **admonitions / callouts** (`> [!NOTE]`, `> [!TIP]`, `> [!IMPORTANT]`, `> [!WARNING]`, `> [!CAUTION]`)
- **definition lists** (`Term` + `: Definition`)
- **details / spoilers** (`<details>/<summary>` — collapsible blocks)

### Inlines

- text
- bold
- italic
- strikethrough
- **superscript** (`^text^`)
- **subscript** (`~text~`)
- **highlight** (`==text==`)
- inline code
- link (with title support)
- **inline image rendering** (actual images in text flow via InlineTextContent)
- inline HTML (rich styled rendering — `<kbd>`, `<mark>`, `<b>`, `<i>`, `<sup>`, `<sub>`, etc.)
- footnote references
- inline footnotes `^[...]`
- soft/hard line breaks (`\n`)
- **emoji shortcodes** (`:smile:`, `:rocket:`, `:fire:`, etc.)
- **abbreviations** (`*[ABBR]: Full Title`)

### GFM extensions enabled

- `GFMFlavourDescriptor` from `org.jetbrains:markdown`
- GFM tables
- strikethrough
- task list markers
- autolinks (including bare URLs like `https://example.com`)
- footnotes layer in Orca (`[^label]` and inline `^[...]`)

### Metadata

- front matter parsing:
  - YAML (`--- ... ---`)
  - TOML (`+++ ... +++`)

## Renderer Behavior

- `LazyColumn` root for long documents
- optional root layout switch: `OrcaRootLayout.LAZY_COLUMN` or `OrcaRootLayout.COLUMN`
- parsing off main thread (`Dispatchers.Default`)
- parse failure fallback to previous valid document (UI is not dropped); partial results with errors accepted when blocks are present (avoids streaming freeze on unclosed fences)
- deterministic block keys for better list state retention (FNV-1a hash with 256-char sampling + tail fold)
- **full document text selection** — all text (headings, paragraphs, lists, quotes, tables) is selectable
- footnotes rendered as superscript markers + numbered definitions block
- footnote navigation:
  - tap reference marker (`[n]`) to jump to definition
  - tap backlink (`↩`) to return to source block
- **accessibility** — semantic roles for headings, content descriptions for images and blocks
- **heading anchor links** — `[link](#heading-text)` scrolls to the corresponding heading (auto-generated GitHub-style slugs)
- **custom block renderers** — override rendering for any block type via `blockOverride` parameter
- **pluggable image loading** — supply custom `imageContent` composable to replace built-in Coil loader

### Admonition rendering

- GitHub-style callout blocks: NOTE, TIP, IMPORTANT, WARNING, CAUTION
- colored left stripe + icon + title
- full content block rendering inside admonition
- light and dark theme color presets
- **collapsible mode** — toggle content visibility with animated expand/collapse

### Details / spoiler rendering

- HTML `<details>/<summary>` blocks rendered as collapsible sections
- animated expand/collapse
- supports `<details open>` for initially expanded state
- nested markdown content inside details is fully rendered
- styled border + background, configurable via `OrcaDetailsStyle`

### Code block rendering

- monospace typography
- rounded container + subtle border
- optional language label (when language exists)
- syntax highlighting (enabled by default, configurable) — supports multiline strings, raw strings, template literals, decorators, type annotations
- selectable code text
- optional line numbers for multiline blocks
- horizontal scroll for long lines (no forced wrap)
- optional copy-to-clipboard button

### Image loading

- shimmer/skeleton placeholder while loading (no more raw text fallback)
- smooth crossfade transition on load
- error state with icon fallback

### Table rendering

- auto layout by content width (default)
- fallback fixed layout mode available via style
- horizontal scroll remains for wide tables

### HTML rendering

- block-level HTML rendered with styled AnnotatedString
- supported tags: `<b>`, `<i>`, `<s>`, `<u>`, `<code>`, `<a>`, `<sup>`, `<sub>`, `<mark>`, `<kbd>`, `<br>`, `<p>`, `<h1>`-`<h6>`, `<li>`, `<hr>`, `<blockquote>`, `<pre>`
- HTML entities decoded (`&amp;`, `&lt;`, `&gt;`, `&quot;`, `&nbsp;`, numeric `&#8212;`, `&#x2714;`, etc.)
- interleaved/malformed tags handled gracefully (e.g. `<b><i></b></i>` -- styles popped and re-pushed correctly)
- unknown tags gracefully stripped

## Styling

Use `OrcaStyle` as a single configuration object:

- `typography`
- `inline`
- `layout`
- `quote`
- `code`
- `table`
- `thematicBreak`
- `image`
- `inlineImage`
- `admonition`
- `definitionList`
- `details`

### Adaptive theme

```kotlin
// Automatically picks light or dark style based on system theme
val style = OrcaDefaults.adaptiveStyle() // @Composable
```

### Custom style

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.wertik.orca.compose.Orca
import ru.wertik.orca.compose.OrcaCodeBlockStyle
import ru.wertik.orca.compose.OrcaStyle

Orca(
    markdown = markdown,
    parser = OrcaMarkdownParser(),
    style = OrcaStyle(
        code = OrcaCodeBlockStyle(
            background = Color(0xFFF8F9FB),
            borderColor = Color(0xFFD0D7DE),
            borderWidth = 1.dp,
        ),
    ),
)
```

## Security Defaults

- Default policy allows:
  - links: `http`, `https`, `mailto`
  - images: `http`, `https`
- Unsafe URLs are rendered as plain text/fallback instead of clickable/loaded targets.
- You can fully override checks via `OrcaSecurityPolicy`.

Custom policy example:

```kotlin
val policy = OrcaSecurityPolicies.byAllowedSchemes(
    linkSchemes = setOf("https", "myapp"),
    imageSchemes = setOf("https"),
    allowRelativeLinks = true,
    allowRelativeImages = true,
)

Orca(
    markdown = markdown,
    parser = OrcaMarkdownParser(),
    securityPolicy = policy,
)
```

Always keep your own URL-opening policy in `onLinkClick`.

## Platform Support

| Platform | orca-core | orca-compose | Parser |
|---|---|---|---|
| Android | commonMain + jvmMain | full | `OrcaMarkdownParser` |
| Desktop (JVM) | commonMain + jvmMain | full | `OrcaMarkdownParser` |
| iOS | commonMain | full | `OrcaMarkdownParser` |
| wasmJs (Web) | commonMain | full | `OrcaMarkdownParser` |

## Extensibility

### Custom block renderers

Override how specific block types are rendered:

```kotlin
Orca(
    markdown = markdown,
    parser = OrcaMarkdownParser(),
    blockOverride = mapOf(
        OrcaBlock.CodeBlock::class to { block ->
            val code = block as OrcaBlock.CodeBlock
            MyCustomCodeBlock(code = code.code, language = code.language)
        },
    ),
)
```

### Custom image loader

Replace the built-in Coil image loader with your own:

```kotlin
Orca(
    markdown = markdown,
    parser = OrcaMarkdownParser(),
    imageContent = { url, contentDescription ->
        // Use Glide, Kamel, or any custom loader
        GlideImage(model = url, contentDescription = contentDescription)
    },
)
```

### Collapsible admonitions

```kotlin
Orca(
    markdown = markdown,
    parser = OrcaMarkdownParser(),
    style = OrcaStyle(
        admonition = OrcaAdmonitionStyle(
            collapsible = true,
            collapsedByDefault = false,
        ),
    ),
)
```

## Verification

```bash
./gradlew --no-daemon --build-cache :orca-core:jvmTest :orca-compose:testDebugUnitTest :sample-app:assembleDebug
```

For release-like check:

```bash
./gradlew --no-daemon --build-cache :sample-app:assembleRelease :sample-app:bundleRelease
```

## Versioning

- Stable releases use plain semver tags like `0.9.1`
- Pre-releases use `-alpha`, `-beta`, `-rc`
- Maven Central artifacts are immutable after publish

## Changelog

### 0.9.4

- **`==highlight==` syntax** -- inline text highlight with configurable `OrcaInlineStyle.highlight` (yellow background by default)
- **Heading anchor IDs** -- headings auto-generate GitHub-style slugs (`## My Heading` -> `id = "my-heading"`), duplicate headings get `-1`, `-2` suffixes
- **Fragment link scroll** -- `[link](#heading-slug)` clicks auto-scroll to the matching heading in both `LAZY_COLUMN` and `COLUMN` layouts
- **Fragment URLs allowed** -- `#fragment` URLs now pass security policy (previously blocked as schemeless)

### 0.9.3

- **Inline HTML rendering** -- `<kbd>`, `<mark>`, `<b>`, `<i>`, `<sup>`, `<sub>`, `<code>`, `<u>`, `<s>` tags in paragraphs now render with proper styles (previously stripped to plain text)
- **`<kbd>` tag** -- keyboard input tag rendered with monospace font + subtle background in both block and inline HTML
- **Numeric HTML entities** -- `&#8212;`, `&#x2714;` and all decimal/hex character references decoded correctly
- **Details summary inline markdown** -- `<summary>**bold** text</summary>` now renders rich inline formatting (was plain text)
- **KMP fix** -- numeric entity decoder uses cross-platform codepoint conversion instead of JVM-only `String(IntArray)`

### 0.9.2

- **`<details>/<summary>` support** -- collapsible blocks with animated expand/collapse, `<details open>`, nested markdown content
- **Coil warnings fix** -- replaced manual `when (painter.state)` with slot-based `loading`/`error`/`success` parameters
- **Kotlin expect/actual warnings** -- suppressed beta warnings via compiler opt-in

### 0.9.1

- **Cache lock fix** -- `OrcaParserCache` now parses outside the lock; concurrent callers no longer block each other (eliminates ANR risk on main thread)
- **HTML interleaved tags** -- malformed tag nesting like `<b><i></b></i>` is handled correctly by scanning the stack and re-pushing intervening styles
- **Table recomposition** -- `TableRowNode` uses `rememberUpdatedState` for callbacks, preventing unnecessary `AnnotatedString` rebuilds on every recomposition
- **Initial parse diagnostics** -- warnings and errors from the synchronous first-frame parse are now reported via `onParseDiagnostics`
- **Streaming freeze fix** -- parse results with errors but non-empty blocks are accepted instead of falling back to a stale document (fixes UI freeze on unclosed code fences during streaming)
- **Render depth guard** -- `OrcaBlockNode` enforces `MAX_RENDER_DEPTH = 32` to prevent stack overflow on deeply nested markdown from custom parsers
- **Hash distribution** -- `stableHash` samples 256 characters (was 128) and folds in tail content for strings >256 chars, reducing LazyColumn key collisions for code blocks with identical imports

## Contributing

1. Open an issue for substantial change.
2. Keep PR scope focused.
3. Add tests for parser/render regressions.
4. Update release notes and README support section when behavior changes.

## License

MIT. See [`LICENSE`](LICENSE).
