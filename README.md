# FhMd

Markdown renderer for Compose (Android-first, multiplatform-ready).

[![CI](https://github.com/wertikolix/FhMd/actions/workflows/ci.yml/badge.svg)](https://github.com/wertikolix/FhMd/actions/workflows/ci.yml)

## Status

`0.1.0-alpha07` (in progress, early alpha, API may change).

Next draft notes: [`0.1.0-alpha07`](docs/releases/0.1.0-alpha07.md).

## Goals

- Clean Markdown -> Compose rendering pipeline
- Simple API for common app use cases
- Lightweight integration
- Extensible rendering hooks

## Modules

- `fhmd-core`: AST model + parser API + `commonmark-java` mapping
- `fhmd-compose-android`: Compose renderer for `FhMdDocument`
- `sample-app`: Android sample for manual verification

## Maven Coordinates

- `ru.wertik:fhmd-core:<version>`
- `ru.wertik:fhmd-compose-android:<version>` (current published coordinate)
- `ru.wertik:fhmd-compose:<version>` (short alias configured in publishing from `0.1.0-alpha07`)

## Supported (`0.1.0-alpha07`)

- Parser: `commonmark-java` (`0.27.1`)
- Extensions:
  - `commonmark-ext-gfm-tables`
  - `commonmark-ext-gfm-strikethrough`
  - `commonmark-ext-task-list-items`
- Public parser API:
  - `interface FhMdParser { fun parse(input: String): FhMdDocument }`
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
  - Compose renderer for `FhMdDocument`
  - basic default styles (`FhMdStyle`)
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
import ru.wertik.fhmd.core.CommonmarkFhMdParser
import ru.wertik.fhmd.core.FhMdParser

val parser: FhMdParser = CommonmarkFhMdParser()
val document = parser.parse(markdown)
```

```kotlin
import ru.wertik.fhmd.compose.android.FhMd

FhMd(
    markdown = markdown,
    onLinkClick = { link -> /* handle link */ },
)
```

## Verification

```bash
./gradlew :fhmd-core:test :fhmd-compose-android:testDebugUnitTest :sample-app:assembleDebug
```

`sample-app` release APK is debug-signed for installation testing only.

## Contributing

Contributions are welcome.

1. Open an issue for significant changes.
2. Create a focused branch.
3. Submit a PR with clear rationale and test notes.

## License

MIT. See `LICENSE`.
