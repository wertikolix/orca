# Orca

Compose Multiplatform Markdown renderer. Targets **Android**, **iOS**, **Desktop (JVM)**, and **wasmJs**.

[![CI](https://github.com/wertikolix/Orca/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/wertikolix/Orca/actions/workflows/ci.yml?query=branch%3Amain)
[![Maven Central](https://img.shields.io/maven-central/v/ru.wertik/orca-core)](https://central.sonatype.com/artifact/ru.wertik/orca-core)

## Status

- Current stable minor: `0.6.3`
- Release notes: [`docs/releases/0.6.3.md`](docs/releases/0.6.3.md)
- Maturity: lightweight production-ready core subset (Markdown-first)

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
implementation("ru.wertik:orca-core:0.6.3")
implementation("ru.wertik:orca-compose:0.6.3")
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

### Render from markdown

```kotlin
import ru.wertik.orca.compose.Orca
import ru.wertik.orca.compose.OrcaRootLayout
import ru.wertik.orca.core.OrcaMarkdownParser

Orca(
    markdown = markdown,
    parser = OrcaMarkdownParser(),
    rootLayout = OrcaRootLayout.COLUMN, // use when parent already controls scrolling
    onLinkClick = { url ->
        // open via your app policy
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

## Public API

```kotlin
fun interface OrcaParser {
    fun parse(input: String): OrcaDocument
}
```

`OrcaMarkdownParser` options:

```kotlin
OrcaMarkdownParser(
    maxTreeDepth = 64,
    onDepthLimitExceeded = { depth ->
        // observe depth truncation if needed
    },
)
```

Compatibility alias:

- `IntellijMarkdownOrcaParser` is still available but deprecated.
- Prefer `OrcaMarkdownParser` for new code.

## Supported Syntax (`0.6.3`)

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
- HTML blocks (safe text fallback rendering)
- footnote definitions

### Inlines

- text
- bold
- italic
- strikethrough
- inline code
- link
- inline image AST (rendered as fallback text inside inline flow)
- inline HTML (safe text fallback rendering)
- footnote references
- inline footnotes `^[...]`
- soft/hard line breaks (`\n`)

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

- Links are treated safe only for: `http`, `https`, `mailto`
- Images are treated safe only for: `http`, `https`
- Unsafe URLs are rendered as plain text/fallback instead of clickable/loaded targets

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

## Migration from 0.5.x

### Parser rename and backend switch

```diff
- import ru.wertik.orca.core.CommonmarkOrcaParser
+ import ru.wertik.orca.core.OrcaMarkdownParser

- val parser: OrcaParser = CommonmarkOrcaParser()
+ val parser: OrcaParser = OrcaMarkdownParser()
```

`orca-core` parser backend moved from `commonmark-java` to `org.jetbrains:markdown` (`GFMFlavourDescriptor`).

## Migration from 0.6.0

### Parser naming cleanup

```diff
- import ru.wertik.orca.core.IntellijMarkdownOrcaParser
+ import ru.wertik.orca.core.OrcaMarkdownParser

- val parser: OrcaParser = IntellijMarkdownOrcaParser()
+ val parser: OrcaParser = OrcaMarkdownParser()
```

`IntellijMarkdownOrcaParser` remains as a deprecated compatibility alias.

## Migration from 0.4.x

### Package rename

```diff
- import ru.wertik.orca.compose.android.Orca
- import ru.wertik.orca.compose.android.OrcaStyle
+ import ru.wertik.orca.compose.Orca
+ import ru.wertik.orca.compose.OrcaStyle
```

### Parser is now a required parameter

```diff
  Orca(
      markdown = markdown,
+     parser = OrcaMarkdownParser(),
  )
```

### Maven artifact

```diff
- implementation("ru.wertik:orca-compose:0.4.5")
+ implementation("ru.wertik:orca-compose:0.6.3")
```

Gradle resolves the correct platform artifact automatically.

## Versioning

- Stable releases use plain semver tags like `0.6.3`
- Pre-releases use `-alpha`, `-beta`, `-rc`
- Maven Central artifacts are immutable after publish

## Contributing

1. Open an issue for substantial change.
2. Keep PR scope focused.
3. Add tests for parser/render regressions.
4. Update release notes and README support section when behavior changes.

## License

MIT. See [`LICENSE`](LICENSE).
