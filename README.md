# FhMd

Markdown renderer for Compose (Android-first, multiplatform-ready).

[![CI](https://github.com/wertikolix/FhMd/actions/workflows/ci.yml/badge.svg)](https://github.com/wertikolix/FhMd/actions/workflows/ci.yml)

## Status

`0.1.0-alpha02` (in progress, early alpha, API may change).

Next draft notes: [`0.1.0-alpha02`](docs/releases/0.1.0-alpha02.md).

## Goals

- Clean Markdown -> Compose rendering pipeline
- Simple API for common app use cases
- Lightweight integration
- Extensible rendering hooks

## Modules

- `fhmd-core`: AST model + parser API + `commonmark-java` mapping
- `fhmd-compose-android`: Compose renderer for `FhMdDocument`
- `sample-app`: Android sample for manual verification

## Supported (`0.1.0-alpha01`)

- Parser: `commonmark-java` (`0.27.1`)
- Public parser API:
  - `interface FhMdParser { fun parse(input: String): FhMdDocument }`
- Blocks:
  - heading
  - paragraph
  - bullet/ordered list
  - quote
  - fenced/indented code block
- Inlines:
  - text
  - bold
  - italic
  - inline code
  - link
  - soft/hard line breaks (mapped to `\n`)
- Renderer:
  - Compose renderer for `FhMdDocument`
  - basic default styles (`FhMdStyle`)
  - link click callback: `onLinkClick: (String) -> Unit`

## Not Supported Yet

- Tables
- Images
- HTML blocks/inline HTML
- Strikethrough
- Task lists
- Thematic breaks (`---`)
- Footnotes/front matter/syntax extensions
- Rich theming system (beyond basic style object)
- Compose Multiplatform targets (architecture is prepared, Android module implemented first)

## Usage

```kotlin
val parser: FhMdParser = CommonmarkFhMdParser()
val document = parser.parse(markdown)
```

```kotlin
FhMd(
    markdown = markdown,
    onLinkClick = { link -> /* handle link */ },
)
```

## Verification

```bash
./gradlew :fhmd-core:test :fhmd-compose-android:testDebugUnitTest :sample-app:assembleDebug
```

## Contributing

Contributions are welcome.

1. Open an issue for significant changes.
2. Create a focused branch.
3. Submit a PR with clear rationale and test notes.

## License

MIT. See `LICENSE`.
