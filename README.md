# Orca

Compose Multiplatform Markdown renderer. Targets **Android**, **iOS**, **Desktop (JVM)**, and **wasmJs**.

[![CI](https://github.com/wertikolix/Orca/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/wertikolix/Orca/actions/workflows/ci.yml?query=branch%3Amain)
[![Maven Central](https://img.shields.io/maven-central/v/ru.wertik/orca-core)](https://central.sonatype.com/artifact/ru.wertik/orca-core)

## Status

- Current stable minor: `0.30.0`
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
  - Lightweight base renderer with no mandatory image/network runtime
- `orca-compose-material3`
  - Optional Material 3 theme adapter for `orca-compose`
  - Provides `rememberOrcaMaterialStyle(density = …)` without adding Material 3 to the base renderer
- `orca-images-coil`
  - Optional Coil 3 + Ktor image renderer for trusted Markdown content
  - Provides block and inline image content slots without making chat-only apps pay for them
- `orca-math-orcex` *(optional, Android / Desktop / iOS)*
  - Optional Compose Multiplatform LaTeX renderer backed by [Orcex](https://github.com/wertikolix/Orcex)
  - Leaves `orca-core` and `orca-compose` free from a bundled font or math engine
- `sample-app`
  - Android demo for manual checks

## Maven

```kotlin
// Kotlin Multiplatform (commonMain)
implementation("ru.wertik:orca-core:0.30.0")
implementation("ru.wertik:orca-compose:0.30.0")
implementation("ru.wertik:orca-compose-material3:0.30.0") // optional Material 3 theme adapter
implementation("ru.wertik:orca-images-coil:0.30.0") // optional images
implementation("ru.wertik:orca-math-orcex:0.30.0") // optional multiplatform math renderer
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
import androidx.compose.runtime.remember

Orca(
    markdown = markdown,
    parser = remember { OrcaMarkdownParser() },
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

For token-by-token streaming (e.g. LLM responses), use `OrcaStreamingState`: it accepts deltas and publishes paced snapshots instead of forcing your UI to replace the entire string on every token.

```kotlin
val stream = rememberOrcaStreamingState(frameIntervalMs = 80)
val parser = remember {
    OrcaIncrementalParserSession(OrcaMarkdownParser())
}

LaunchedEffect(messageId) {
    parser.reset()
    stream.clear()
    responseChunks.collect { delta -> stream.append(delta) }
    stream.finish()
}

Orca(
    state = stream,
    parser = parser,
    parseCacheKey = "message-42",
)
```

`OrcaIncrementalParserSession` reuses stable completed paragraph blocks and reparses only the active tail for ordinary prose streams. Rich constructs that can affect earlier content (lists, tables, headings, fences, definitions, footnotes, and HTML blocks) conservatively fall back to the full parser. The initial parse and subsequent parses run on `Dispatchers.Default`.

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

Document utilities (`orca-core`):

```kotlin
val document = parser.parse(markdown)

document.tableOfContents(maxLevel = 2)   // headings with anchors
document.stats()                         // words, reading time, block and task counts
document.findMatches("streaming")        // hits with top-level block indices
document.plainText()                     // markup-free projection
```

## Supported Syntax (`0.30.0`)

### Blocks

- heading
- paragraph
- bullet list
- ordered list (start number preserved)
- quote
- fenced code block
- indented code block
- display math (`$$...$$`)
- thematic break (`---`)
- standalone image block
- **image captions** (the standard Markdown image title renders below the image)
- GFM tables
- HTML blocks (styled rendering with tag support)
- **safe HTML media** (standalone `<img>` and `<figure>/<figcaption>` through the image slot and URL policy)
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
- **inserted / underline** (`++text++`)
- inline code
- inline math (`$...$`)
- link (with title support)
- **inline image rendering** (actual images in text flow via InlineTextContent)
- inline HTML (rich styled rendering — `<kbd>`, `<mark>`, `<b>`, `<i>`, `<sup>`, `<sub>`, etc.)
- **inline HTML images** (`<img>` through the same inline image slot and URL policy)
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
- initial and subsequent raw-Markdown parsing off main thread (`Dispatchers.Default`)
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
- **custom inline renderers** — replace exact inline node classes with annotated text via `inlineOverride`
- **interactive task lists** — pass `onTaskToggle` to receive checkbox taps (document-order index + requested state) and update your source; rendering stays stateless
- **custom task checkbox slot** — replace the default flat, semantic checkbox via `taskCheckboxContent`
- **table of contents** — `OrcaDocument.tableOfContents()` + `orcaHeadingBlockIndex()` map headings to lazy-list indices for scroll-to-section UIs
- **streaming cursor** — optional `streamingCursor` glyph rendered after the last block while a response streams
- **zero-cost optional images** — base `orca-compose` displays fallback/alt text; supply `imageContent` and `inlineImageContent` only when image rendering is needed

### Admonition rendering

- GitHub-style callout blocks: NOTE, TIP, IMPORTANT, WARNING, CAUTION
- full one-pixel outline, solid surface tint, icon, and semantic title
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

- flat solid pulse placeholder while loading
- no gradient, shadow, or mandatory Material dependency
- readable error state using the configured caption style

### Table rendering

- auto layout by content width (default)
- fallback fixed layout mode available via style
- horizontal scroll remains for wide tables
- collection semantics for rows, columns, and headers
- a solid position indicator appears only when content overflows
- inline image and math slots work inside cells

### HTML rendering

- block-level HTML rendered with styled AnnotatedString
- supported tags: `<b>`, `<i>`, `<s>`, `<u>`, `<code>`, `<a>`, `<sup>`, `<sub>`, `<mark>`, `<kbd>`, `<br>`, `<p>`, `<h1>`-`<h6>`, `<li>`, `<hr>`, `<blockquote>`, `<pre>`
- standalone `<img>` and `<figure>/<figcaption>` blocks route through `OrcaSecurityPolicy` and `imageContent`
- inline `<img>` tags route through `OrcaSecurityPolicy` and `inlineImageContent`
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
- `task`
- `headingRule`

### Flat design system

Since `0.30`, every built-in style is generated from flat color tokens. There is no elevation,
gradient, or shadow anywhere in the render tree: structure comes from solid fills, one-pixel
outlines, and typography.

```kotlin
import ru.wertik.orca.compose.OrcaDensity
import ru.wertik.orca.compose.OrcaPalettes
import ru.wertik.orca.compose.orcaFlatStyle

val style = orcaFlatStyle(
    palette = OrcaPalettes.FlatDark,      // FlatLight, FlatDark, ContrastLight, ContrastDark
    density = OrcaDensity.COMPACT,        // COMPACT, COMFORTABLE, SPACIOUS
    headingRuleLevels = setOf(1, 2),      // one-pixel rules under H1/H2
)
```

`OrcaPalette` is the token surface: `background`, `surface`, `surfaceMuted`, `surfaceStrong`,
`outline`, `outlineMuted`, `text`, `textMuted`, `accent`, `onAccent`, `accentSurface`,
`highlight`, `searchMatch`, plus a `syntax` palette for code and a `signal` palette with one color
per admonition type. Copy a preset to brand it:

```kotlin
val brand = OrcaPalettes.FlatLight.copy(accent = Color(0xFF1F5FA8))
```

Density scales spacing and padding only; text metrics stay identical across the three modes.

### Adaptive theme

```kotlin
// Automatically picks the flat light or flat dark style based on the system theme
val style = OrcaDefaults.adaptiveStyle()                       // @Composable
val dense = OrcaDefaults.adaptiveStyle(OrcaDensity.COMPACT)    // @Composable
val a11y = OrcaDefaults.adaptiveContrastStyle()                // @Composable
```

`OrcaDefaults.legacyLightStyle()` and `OrcaDefaults.legacyDarkStyle()` keep the pre-0.30 visuals
for applications that pinned screenshots to them.

For Material 3 apps, derive colors, typography, and shapes directly from the active theme:

```kotlin
import ru.wertik.orca.compose.material3.rememberOrcaMaterialStyle

val style = rememberOrcaMaterialStyle(density = OrcaDensity.COMFORTABLE)
```

### Search highlighting

```kotlin
import ru.wertik.orca.compose.OrcaTextHighlight
import ru.wertik.orca.core.findMatches

val matches = document.findMatches(query)

Orca(
    document = document,
    listState = listState,
    highlight = OrcaTextHighlight(query),
)

// matches[i].blockIndex maps directly to listState.animateScrollToItem(...)
```

Matches are shaded with `OrcaInlineStyle.searchMatch` across headings, paragraphs, list items,
table cells, definition terms, details summaries, and footnote bodies. Code blocks keep their
syntax colors.

### External scrollbar

Pass the same `LazyListState` to Orca and your scrollbar or external controls:

```kotlin
val listState = rememberLazyListState()

Orca(
    document = document,
    listState = listState,
    style = rememberOrcaMaterialStyle(),
)
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
    parser = remember { OrcaMarkdownParser() },
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

- Default policy allows links using `http`, `https`, `mailto`, and local `#fragment` targets.
- Images are blocked by default so rendering untrusted Markdown cannot trigger network requests.
- Unsafe URLs are rendered as plain text/fallback instead of clickable/loaded targets.
- You can fully override checks via `OrcaSecurityPolicy`.

For trusted content that should load remote images, opt into both URL permission and an image renderer. With the optional Coil module:

```kotlin
import ru.wertik.orca.images.coil.OrcaCoilImage
import ru.wertik.orca.images.coil.OrcaCoilInlineImage

Orca(
    document = document,
    securityPolicy = OrcaSecurityPolicies.RemoteImages,
    imageContent = { url, description -> OrcaCoilImage(url, description, style) },
    inlineImageContent = { url, description -> OrcaCoilInlineImage(url, description, style) },
)
```

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
    parser = remember { OrcaMarkdownParser() },
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
    parser = remember { OrcaMarkdownParser() },
    blockOverride = mapOf(
        OrcaBlock.CodeBlock::class to { block ->
            val code = block as OrcaBlock.CodeBlock
            MyCustomCodeBlock(code = code.code, language = code.language)
        },
    ),
)
```

### Custom inline renderers

Replace exact inline node classes with custom annotated text. The same map is threaded through paragraphs, headings, tables, definition terms, and details summaries.

```kotlin
Orca(
    markdown = markdown,
    parser = remember { OrcaMarkdownParser() },
    inlineOverride = mapOf(
        OrcaInline.Abbreviation::class to { inline ->
            val abbreviation = inline as OrcaInline.Abbreviation
            AnnotatedString("${abbreviation.text} (${abbreviation.title})")
        },
    ),
)
```

### Optional image loader

`orca-compose` intentionally ships without an image/network stack. Add `orca-images-coil` for the provided Coil/Ktor slots, or provide your own slots:

```kotlin
Orca(
    markdown = markdown,
    parser = remember { OrcaMarkdownParser() },
    securityPolicy = OrcaSecurityPolicies.RemoteImages,
    imageContent = { url, contentDescription ->
        GlideImage(model = url, contentDescription = contentDescription)
    },
    inlineImageContent = { url, contentDescription ->
        GlideInlineImage(model = url, contentDescription = contentDescription)
    },
)
```

### Collapsible admonitions

```kotlin
Orca(
    markdown = markdown,
    parser = remember { OrcaMarkdownParser() },
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
./gradlew --no-daemon --build-cache :orca-core:jvmTest :orca-compose:testDebugUnitTest :orca-compose-material3:testDebugUnitTest :sample-app:assembleDebug
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

### 0.30.0

- **Flat design system** — `OrcaPalette`, `OrcaPalettes` (flat light/dark plus high-contrast light/dark), `OrcaSyntaxPalette`, `OrcaSignalPalette`, and `orcaFlatStyle()` build a complete `OrcaStyle` from tokens. No gradients, shadows, or elevation overlays exist in the render tree.
- **Density scale** — `OrcaDensity.COMPACT | COMFORTABLE | SPACIOUS` scales spacing and padding without touching text metrics. Accepted by `orcaFlatStyle`, `OrcaDefaults.*Style()`, and `rememberOrcaMaterialStyle()`.
- **New defaults** — `OrcaDefaults.lightStyle()` / `darkStyle()` now return the flat styles, and `adaptiveContrastStyle()` is available for accessibility surfaces. `legacyLightStyle()` / `legacyDarkStyle()` preserve the pre-0.30 look.
- **Heading rules** — `OrcaHeadingRuleStyle` draws a one-pixel rule under selected heading levels (H1/H2 by default in flat styles).
- **Document search** — `OrcaDocument.findMatches()` / `countMatches()` with case, whole-word, limit, and snippet options; each match carries its top-level block index and nearest heading anchor.
- **Search highlighting** — `OrcaTextHighlight` on every `Orca` overload shades matches with `OrcaInlineStyle.searchMatch` across all inline surfaces.
- **Document statistics** — `OrcaDocument.stats()` returns words, characters, reading time, per-block-type counts, and task progress in one pass.
- **Plain text export** — `OrcaDocument.plainText()`, `OrcaBlock.plainText()`, and `List<OrcaInline>.plainText()` are public.
- **Material 3 adapter refresh** — `rememberOrcaMaterialStyle()` maps the color scheme into an `OrcaPalette` via `OrcaDefaults.materialPalette()` and builds the style through `orcaFlatStyle`, with `density` and `headingRules` options.
- **Render lab** — ten suites, a shared token system between app chrome and renderer, in-document search with match navigation, live document statistics, palette/density switches, a design-token suite, and snippet insertion in the playground.
- **Release workflow** — longer Maven Central publish window: job and step timeouts raised, exponential retry on the staging publish trigger, and a single bounded sync-verification window.

### 0.20.0

- **Renderer context completeness:** inline images, inline math, security decisions, and inline overrides now reach table cells, definition terms, and details summaries.
- **Safe HTML media:** strict standalone `<img>` and `<figure>/<figcaption>` blocks use the existing image slots and URL policy; inline `<img>` uses the inline image slot.
- **Inline renderer API:** every `Orca` overload accepts an exact-class `inlineOverride` map returning `AnnotatedString` content.
- **Task renderer API:** task lists use a platform-neutral flat checkbox with proper semantics and a 40 dp interaction target; `taskCheckboxContent` allows full replacement.
- **Flat renderer refresh:** quotes and admonitions use full outlines and solid tinted surfaces. Coil loading uses a solid pulse instead of a shimmer gradient.
- **Table UX:** collection semantics, nested media/math, responsive auto sizing, and a solid overflow position indicator.
- **Render lab redesign:** adaptive wide navigation, compact suite tabs, nine feature suites, controllable streaming, and a responsive source/preview playground.
- **Release hardening:** tags must match the project version, Maven credentials and signing keys are required, and tag-only GitHub release steps are enforced.

### 0.15.0

- **Incremental streaming v2** — `OrcaIncrementalParserSession` now freezes a growing prefix of blank-line separated segments (headings, closed code fences, lists, quotes, admonitions, tables, thematic breaks) instead of plain paragraphs only. Only the active tail is re-parsed per update; heading anchor slugs are re-derived so duplicate titles keep full-parse numbering. Verified by prefix-equivalence property tests against the full parser.
- **Text selection in `LAZY_COLUMN`** — the default lazy root layout is now wrapped in a `SelectionContainer`, matching the `COLUMN` mode.
- **Table of contents API** — `OrcaDocument.tableOfContents()` in `orca-core` plus `orcaHeadingBlockIndex()` in `orca-compose` for scroll-to-heading UIs on top of `LazyListState`.
- **Streaming cursor** — optional `streamingCursor` glyph on all `Orca` overloads; the streaming overload shows it only while `OrcaStreamingState.isStreaming`. Applied to the parsed document, keeping incremental sessions append-only.
- **Admonition icons** — theme-tinted monochrome glyphs before callout titles, configurable/disableable via `OrcaAdmonitionStyle` (`showIcons`, per-type icon strings).
- **Orcex 0.5.0** — the optional math module picks up LaTeX colors (`\textcolor`, `\color`), framed results (`\boxed`) and stacked annotations (`\overset`/`\underset`), plus wasmJs artifacts of the Orcex runtime.

### 0.14.0

- **Inserted/underline inline** — new `++text++` syntax produces `OrcaInline.Underline`, styled via `OrcaInlineStyle.underline`.
- **Image captions** — the standard Markdown image title (`![alt](url "title")`) now renders as a caption below block images; configurable via `OrcaImageStyle.showCaption`, `captionText`, and `captionSpacing`.
- **Interactive task lists** — new optional `onTaskToggle` callback on all `Orca` overloads makes `- [ ]` checkboxes tappable; the host receives the document-order task index and requested state. Rendering stays stateless and dependency-free.
- **Theme-aware inline HTML** — `<mark>`, `<kbd>`, `<u>`/`<ins>`, `<sup>`, and `<sub>` now follow `OrcaStyle` instead of hardcoded light-theme colors, fixing unreadable spans in dark themes. `OrcaInlineStyle` gains `underline` and `kbd` fields.
- **Material 3 adapter** — `materialStyle()` maps the new kbd and image-caption styles to color-scheme tokens.
- **Sample app dark theme fix** — the status bar no longer stays white in dark theme: the sample follows the system theme on launch, re-applies `enableEdgeToEdge` system-bar styles on toggle, and ships a `values-night` window background.

### 0.13.0

- **Material 3 styles** — new optional `orca-compose-material3` module with `rememberOrcaMaterialStyle()` deriving an `OrcaStyle` from the active `MaterialTheme`.
- **Scroll state API** — expose the root `LazyListState` for external scroll control.

### 0.12.1

- **Stable first layout** -- restores synchronous initial Markdown parsing so messages nested in an outer `LazyColumn` no longer appear empty before gaining their real height and displacing scroll position.
- **Streaming preserved** -- subsequent text updates remain asynchronously parsed and paced; only the first measured frame is stabilised.

### 0.12.0

- **Multiplatform Orcex math** -- migrates `orca-math-orcex` from the Android Canvas bridge to Orcex `0.4.0`'s Compose Multiplatform renderer for Android, Desktop, and supported iOS targets.
- **Android compatibility** -- keeps the existing `Typeface` convenience overloads so current Android applications can upgrade without rewriting their formula slots.
- **Explicit verification** -- CI and release builds now compile the math adapter on Desktop and iOS Simulator as well as Android.

### 0.11.2

- **Orcex 0.3.0** -- updates the optional Android native math renderer to the current published Orcex release.

### 0.11.1

- **Valid Maven POM** -- removes a redundant versionless AndroidX Compose UI dependency from `orca-math-orcex`; Compose UI remains supplied transitively by `orca-compose`.

### 0.11.0

- **Math AST and slots** -- parses conservative inline `$...$` and display `$$...$$` formulas with readable source fallback.
- **Optional Orcex renderer** -- adds `orca-math-orcex` for native Android Canvas math rendering; the STIX font remains opt-in.
- **Streaming-safe formulas** -- incomplete formula delimiters stay text until the closing delimiter arrives.
- **Orcex 0.2.1** -- consumes the published renderer release with corrected matrix delimiter layout.

### 0.10.0

- **Ultra-light base renderer** -- moves Coil/Ktor image loading from `orca-compose` into opt-in `orca-images-coil`.
- **Explicit image slots** -- block and inline Markdown images render only through `imageContent` / `inlineImageContent`; without a loader, safe alt/fallback text remains visible.
- **Streaming state API** -- `rememberOrcaStreamingState()` accepts token deltas and publishes paced snapshots for chat rendering without caller-side full-string updates per token.
- **Conservative tail parsing** -- `OrcaIncrementalParserSession` reuses completed prose blocks and safely falls back to full parsing for document-scoped/rich Markdown constructs.
- **Readable dark tables** -- `OrcaDefaults.darkStyle()` now provides explicit light table body/header colors instead of inheriting black text.

### 0.9.5

- **Smaller Android releases** -- removes keep-all consumer rules so R8 strips unused Orca code; in the Fish release APK this reduced output by about 116 KiB.
- **Safer image defaults** -- remote images are blocked by default; opt in with `OrcaSecurityPolicies.RemoteImages` or a custom scheme policy.
- **No initial UI-thread parse** -- raw Markdown is parsed on `Dispatchers.Default` from the first composition onward.
- **Responsive token streaming** -- continuous updates are conflated and rendered at the pacing interval instead of waiting for a pause.
- **Lighter renderer dependencies** -- renderer uses Foundation text instead of Material3 and drops unused direct Android/Ktor dependencies.
- **Consumer ABI metadata** -- public Compose and Markdown parser types are now published through `api` dependencies.

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
