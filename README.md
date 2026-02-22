# Orca

Compose Multiplatform Markdown renderer. Targets **Android**, **iOS**, **Desktop (JVM)**, and **wasmJs**.

[![CI](https://github.com/wertikolix/Orca/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/wertikolix/Orca/actions/workflows/ci.yml?query=branch%3Amain)
[![Maven Central](https://img.shields.io/maven-central/v/ru.wertik/orca-core)](https://central.sonatype.com/artifact/ru.wertik/orca-core)

## Status

- Current stable minor: `0.8.2`
- Release notes: [`docs/releases/0.8.2.md`](docs/releases/0.8.2.md)
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
  - Image loading via Coil 3 + Ktor
- `sample-app`
  - Android demo for manual checks

## Maven

```kotlin
// Kotlin Multiplatform (commonMain)
implementation("ru.wertik:orca-core:0.8.2")
implementation("ru.wertik:orca-compose:0.8.2")
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

## Supported Syntax (`0.8.2`)

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

### Inlines

- text
- bold
- italic
- strikethrough
- **superscript** (`^text^`)
- **subscript** (`~text~`)
- inline code
- link (with title support)
- **inline image rendering** (actual images in text flow via InlineTextContent)
- inline HTML (styled rendering)
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
- parse failure fallback to previous valid document (UI is not dropped)
- deterministic block keys for better list state retention
- footnotes rendered as superscript markers + numbered definitions block
- footnote navigation:
  - tap reference marker (`[n]`) to jump to definition
  - tap backlink (`↩`) to return to source block

### Admonition rendering

- GitHub-style callout blocks: NOTE, TIP, IMPORTANT, WARNING, CAUTION
- colored left stripe + icon + title
- full content block rendering inside admonition
- light and dark theme color presets

### Code block rendering

- monospace typography
- rounded container + subtle border
- optional language label (when language exists)
- syntax highlighting (enabled by default, configurable)
- selectable code text
- optional line numbers for multiline blocks
- horizontal scroll for long lines (no forced wrap)
- optional copy-to-clipboard button

### Table rendering

- auto layout by content width (default)
- fallback fixed layout mode available via style
- horizontal scroll remains for wide tables

### HTML rendering

- block-level HTML rendered with styled AnnotatedString
- supported tags: `<b>`, `<i>`, `<s>`, `<u>`, `<code>`, `<a>`, `<sup>`, `<sub>`, `<mark>`, `<br>`, `<p>`, `<h1>`-`<h6>`, `<li>`, `<hr>`, `<blockquote>`, `<pre>`
- HTML entities decoded (`&amp;`, `&lt;`, `&gt;`, `&quot;`, `&nbsp;`, etc.)
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

Example:

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

## Not Supported Yet

- built-in LaTeX math rendering

## Verification

```bash
./gradlew --no-daemon --build-cache :orca-core:jvmTest :orca-compose:testDebugUnitTest :sample-app:assembleDebug
```

For release-like check:

```bash
./gradlew --no-daemon --build-cache :sample-app:assembleRelease :sample-app:bundleRelease
```

## Versioning

- Stable releases use plain semver tags like `0.8.0`
- Pre-releases use `-alpha`, `-beta`, `-rc`
- Maven Central artifacts are immutable after publish

## Contributing

1. Open an issue for substantial change.
2. Keep PR scope focused.
3. Add tests for parser/render regressions.
4. Update release notes and README support section when behavior changes.

## License

MIT. See [`LICENSE`](LICENSE).
