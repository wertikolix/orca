# Orca

Android-first Markdown renderer for Jetpack Compose with architecture prepared for future Compose Multiplatform split.

[![CI](https://github.com/wertikolix/Orca/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/wertikolix/Orca/actions/workflows/ci.yml?query=branch%3Amain)

## Status

- Current stable minor: `0.3.0`
- Release notes: [`docs/releases/0.3.0.md`](docs/releases/0.3.0.md)
- Maturity: lightweight production-ready core subset (Markdown-first)

## Why Orca

- Small API surface
- Predictable AST (`orca-core`) and Compose renderer (`orca-compose-android`)
- Safe defaults for links and images
- No mandatory heavy runtime dependencies

## Modules

- `orca-core`
  - AST model
  - parser interface
  - `commonmark-java` mapping
- `orca-compose-android`
  - Compose renderer for `OrcaDocument`
  - style model (`OrcaStyle`)
- `sample-app`
  - Android demo for manual checks

## Maven

```kotlin
dependencies {
    implementation("ru.wertik:orca-core:<version>")
    implementation("ru.wertik:orca-compose:<version>")
}
```

## Quick Start

### Parse markdown

```kotlin
import ru.wertik.orca.core.CommonmarkOrcaParser
import ru.wertik.orca.core.OrcaParser

val parser: OrcaParser = CommonmarkOrcaParser()
val document = parser.parse(markdown)
```

### Render from markdown

```kotlin
import ru.wertik.orca.compose.android.Orca
import ru.wertik.orca.compose.android.OrcaRootLayout

Orca(
    markdown = markdown,
    rootLayout = OrcaRootLayout.COLUMN, // use when parent already controls scrolling
    onLinkClick = { url ->
        // open via your app policy
    },
)
```

### Render from pre-parsed AST

```kotlin
import ru.wertik.orca.compose.android.Orca

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

`CommonmarkOrcaParser` options:

```kotlin
CommonmarkOrcaParser(
    maxTreeDepth = 64,
    onDepthLimitExceeded = { depth ->
        // observe depth truncation if needed
    },
)
```

## Supported Syntax (`0.3.0`)

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
- footnote definitions

### Inlines

- text
- bold
- italic
- strikethrough
- inline code
- link
- inline image AST (rendered as fallback text inside inline flow)
- footnote references
- soft/hard line breaks (`\n`)

### GFM extensions enabled

- `commonmark-ext-gfm-tables`
- `commonmark-ext-gfm-strikethrough`
- `commonmark-ext-task-list-items`
- `commonmark-ext-autolink` (bare URLs like `https://example.com`)
- `commonmark-ext-footnotes`

## Renderer Behavior

- `LazyColumn` root for long documents
- optional root layout switch: `OrcaRootLayout.LAZY_COLUMN` or `OrcaRootLayout.COLUMN`
- parsing off main thread (`Dispatchers.Default`)
- parse failure fallback to previous valid document (UI is not dropped)
- deterministic block keys for better list state retention
- footnotes rendered as superscript markers + numbered definitions block

### Code block rendering

- monospace typography
- rounded container + subtle border
- optional language label (when language exists)
- selectable code text
- optional line numbers for multiline blocks
- horizontal scroll for long lines (no forced wrap)

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
import ru.wertik.orca.compose.android.Orca
import ru.wertik.orca.compose.android.OrcaCodeBlockStyle
import ru.wertik.orca.compose.android.OrcaStyle

Orca(
    markdown = markdown,
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

## Not Supported Yet

- HTML blocks and inline HTML
- front matter
- built-in LaTeX math rendering
- Compose Multiplatform target modules (architecture ready, Android implementation first)

## Verification

```bash
./gradlew --no-daemon --build-cache :orca-core:test :orca-compose-android:testDebugUnitTest :sample-app:assembleDebug
```

For release-like check:

```bash
./gradlew --no-daemon --build-cache :sample-app:assembleRelease :sample-app:bundleRelease
```

## Versioning

- Stable releases use plain semver tags like `0.3.0`, `0.3.1`
- Pre-releases use `-alpha`, `-beta`, `-rc`
- Maven Central artifacts are immutable after publish

## Contributing

1. Open an issue for substantial change.
2. Keep PR scope focused.
3. Add tests for parser/render regressions.
4. Update release notes and README support section when behavior changes.

## License

MIT. See [`LICENSE`](LICENSE).
