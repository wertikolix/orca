# Orca

Markdown renderer for Compose (Android-first, multiplatform-ready).

[![CI](https://github.com/wertikolix/Orca/actions/workflows/ci.yml/badge.svg)](https://github.com/wertikolix/Orca/actions/workflows/ci.yml)

## Status

`0.2.0` (Android-first minor release).

Release notes: [`0.2.0`](docs/releases/0.2.0.md).

## Goals

- Clean Markdown -> Compose rendering pipeline
- Simple API for common app use cases
- Lightweight integration
- Extensible rendering hooks

## Modules

- `orca-core`: AST model + parser API + `commonmark-java` mapping
- `orca-compose-android`: Compose renderer for `OrcaDocument`
- `sample-app`: Android sample for manual verification

## Maven Coordinates

- `ru.wertik:orca-core:<version>`
- `ru.wertik:orca-compose:<version>`

## Supported (`0.2.0`)

- Parser: `commonmark-java` (`0.27.1`)
- Extensions:
  - `commonmark-ext-gfm-tables`
  - `commonmark-ext-gfm-strikethrough`
  - `commonmark-ext-task-list-items`
- Public parser API:
  - `interface OrcaParser { fun parse(input: String): OrcaDocument }`
  - `CommonmarkOrcaParser(maxTreeDepth = 64, onDepthLimitExceeded = { ... })`
- Blocks:
  - heading
  - paragraph
  - bullet/ordered list (with ordered list start number)
  - quote
  - fenced/indented code block
  - standalone image block (`![alt](url "title")`)
  - thematic breaks (`---`)
  - tables (GFM pipe tables)
- Inlines:
  - text
  - bold
  - italic
  - strikethrough
  - inline code
  - link
  - inline image AST (rendered as fallback text inside text flows)
  - soft/hard line breaks (mapped to `\n`)
- Lists:
  - ordered start number
  - task list items (`[x]` / `[ ]`)
- Renderer:
  - Compose renderer for `OrcaDocument`
  - root rendering via `LazyColumn` for long documents
  - markdown parsing off main thread (`Dispatchers.Default`)
  - parser failure fallback to last successful `OrcaDocument` (no UI crash)
  - renderer split by responsibilities (`Orca.kt`, `OrcaBlockNode`, `OrcaTableNode`, `OrcaImageNode`)
  - grouped style model (`OrcaStyle.typography`, `inline`, `layout`, `quote`, `code`, `table`, `thematicBreak`, `image`)
  - compatibility accessors for v0.1 flat style fields remain available
  - link click callback: `onLinkClick: (String) -> Unit`
  - link safety filter (`http`, `https`, `mailto`)
  - image safety filter (`http`, `https`; unsafe schemes fallback to text)

## Not Supported Yet

- HTML blocks/inline HTML
- Footnotes/front matter/syntax extensions
- Rich theming system (beyond basic style object)
- Compose Multiplatform targets (architecture is prepared, Android module implemented first)

## Usage

```kotlin
import ru.wertik.orca.core.CommonmarkOrcaParser
import ru.wertik.orca.core.OrcaParser

val parser: OrcaParser = CommonmarkOrcaParser()
val document = parser.parse(markdown)
```

```kotlin
import ru.wertik.orca.compose.android.Orca

Orca(
    markdown = markdown,
    onLinkClick = { link -> /* handle link */ },
)
```

## Verification

```bash
./gradlew :orca-core:test :orca-compose-android:testDebugUnitTest :sample-app:assembleDebug
```

`sample-app` release APK is debug-signed for installation testing only.

## Contributing

Contributions are welcome.

1. Open an issue for significant changes.
2. Create a focused branch.
3. Submit a PR with clear rationale and test notes.

## License

MIT. See `LICENSE`.
