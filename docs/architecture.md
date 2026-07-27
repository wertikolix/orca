# Orca Architecture Overview

## Module Structure

Orca is split into lightweight core/rendering modules plus optional integrations:

| Module | Responsibility |
|---|---|
| **orca-core** | Parsing: markdown string → `OrcaDocument` AST. Zero Compose dependencies. |
| **orca-compose** | Rendering: `OrcaDocument` → Compose UI. Owns styling, security, layout, and image content slots; no image/network stack. |
| **orca-compose-material3** | Optional Material 3 adapter for deriving `OrcaStyle` from the active `MaterialTheme`. |
| **orca-images-coil** | Optional Coil/Ktor implementation of block and inline image slots. |
| **orca-math-orcex** | Optional Orcex implementation of block and inline math slots. |
| **orca-benchmarks** | JVM-only, not published. Parser/streaming benchmarks with scaling checks, run in CI. |

**Dependency direction:** optional adapters (`orca-compose-material3`, `orca-images-coil`, `orca-math-orcex`) → `orca-compose` → `orca-core`. Consumers who only need parsing or chat rendering do not carry optional theme, network/image, or math dependencies.

The modules use `commonMain` for shared logic. The parser's platform-specific synchronization code is isolated in `OrcaLock` (see [Platform Targets](#platform-targets)).

---

## Parsing Pipeline

Entry point: `OrcaMarkdownParser.parse(input: String): OrcaDocument`

The pipeline runs through these stages:

```
markdown string
    │
    ▼
1. extractFrontMatter()          ── strips YAML (---) or TOML (+++) front matter
    │
    ▼
2. extractInlineGuardedRegions() ── pulls blocks with a pathological run of unmatched [
    │
    ▼
3. extractAbbreviations()        ── pulls *[ABBR]: Title definitions, stores map
    │
    ▼
4. extractDetailsBlocks()        ── pulls <details> regions, inserts placeholders
    │
    ▼
5. extractMathBlocks()           ── pulls $$ … $$ blocks, inserts placeholders
    │
    ▼
6. extractDefinitionLists()      ── pulls Term / : Definition blocks, inserts placeholders
    │
    ▼
7. extractFootnoteDefinitions()  ── pulls [^label]: blocks out of the body
    │
    ▼
8. MarkdownParser.buildMarkdownTreeFromString()   ── intellij-markdown AST
    │
    ▼
9. IntellijTreeMapper.mapBlock() ── recursive walk converting ASTNode → OrcaBlock/OrcaInline
    │  ├─ emoji shortcodes       ── replaceEmojiShortcodes() on OrcaInline.Text nodes
    │  ├─ footnote syntax        ── processFootnoteSyntax() parses [^ref] and ^[inline] from text
    │  └─ super/subscript        ── processSuperSubScript() parses ^text^ and ~text~
    │
    ▼
10. Placeholder resolution       ── deflist / details / math placeholders → real blocks
    │
    ▼
11. applyAbbreviations()         ── replaces abbreviation matches in inline content
    │
    ▼
12. resolveRawTextPlaceholders() ── guarded blocks → plain text paragraphs
    │
    ▼
13. OrcaDocument(blocks, frontMatter)
```

### Stage details

**1. Front matter extraction** (`IntellijMarkdownFrontMatter.kt`)
Runs before the markdown parser sees the input. Detects `---`/`...` (YAML) or `+++` (TOML) delimiters at the start of the source. Parses simple `key: value` / `key = value` entries into `OrcaFrontMatter.Yaml` or `OrcaFrontMatter.Toml`. The remaining markdown body is passed downstream.

**2. Inline guard** (`IntellijMarkdownInlineGuard.kt`)
The inline scanner resolves link openers by backtracking, so a block with *N* unmatched `[` costs O(N²): 25 600 of them is not a slow parse, it is a hang. The guard counts unclosed openers per block and swaps any block above `maxInlineBracketDepth` (default 512) for a `<!--orca:rawtext:N-->` placeholder, resolved in step 12 into a plain text paragraph and reported as `OrcaParseWarning.InlineBracketLimitExceeded`. Fenced code and `$$` math are skipped: their content never reaches the inline scanner.

**3. Abbreviation extraction** (`IntellijMarkdownAbbreviations.kt`)
Scans for `*[ABBR]: Full Title` definition lines. Removes them from the body and stores a `Map<String, String>` of abbreviation → expansion. The map is applied as a post-processing step after all blocks are parsed (step 11).

**6. Definition list extraction** (`IntellijMarkdownDefinitionList.kt`)
Scans for `Term` + `: Definition` patterns. Replaces them with HTML comment placeholders (`<!--orca:deflist:N-->`) so the intellij-markdown parser doesn't misinterpret them. After tree mapping, placeholders are resolved back into `OrcaBlock.DefinitionList` nodes with fully parsed inline terms and block-level definitions.

The scan is linear. Definition lines are located in one pass (`DefinitionLineIndex`), and a list is only attempted on the single line that could open one binding to the next definition line. Probing every line, as the first implementation did, is quadratic on a document that is one long paragraph — which is most documents.

**7. Footnote extraction** (`IntellijMarkdownFootnotes.kt`)
Scans for `[^label]: content` definition blocks (with continuation-indent support). Removes them from the body so the intellij-markdown parser doesn't misinterpret them. Extracted `FootnoteSourceDefinition`s are parsed into `OrcaFootnoteDefinition`s after the main tree mapping completes.

**8. IntelliJ markdown AST**
Uses `MarkdownParser(GFMFlavourDescriptor())` — GitHub-Flavored Markdown with tables, task lists, strikethrough, and autolinks.

**9. Tree mapping** (`IntellijMarkdownTreeMapper.kt`)
`IntellijTreeMapper` walks the intellij-markdown `ASTNode` tree and produces `OrcaBlock`/`OrcaInline` nodes. Key post-processing steps applied during inline mapping:

- **Emoji shortcodes** — `replaceEmojiShortcodes()` converts `:rocket:` → 🚀 on `OrcaInline.Text` nodes. Uses a static map of ~150 common shortcodes (`OrcaEmojiShortcodes.kt`).
- **Footnote syntax** — `processFootnoteSyntax()` parses `[^label]` references and `^[inline content]` inline footnotes from text nodes that the upstream parser treats as plain text.
- **Superscript / subscript** — `processSuperSubScript()` parses `^text^` → `OrcaInline.Superscript` and `~text~` → `OrcaInline.Subscript` via regex on text nodes.
- **Admonition detection** — `tryMapAdmonition()` checks if a blockquote's first paragraph starts with `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, or `[!CAUTION]` and converts the quote into `OrcaBlock.Admonition`.

A configurable `maxTreeDepth` (default: 64) guards against pathological nesting. When exceeded, subtrees are dropped and a `OrcaParseWarning.DepthLimitExceeded` diagnostic is emitted.

---

## AST Model

Defined in `OrcaDocument.kt`. The model is a two-level tree:

```
OrcaDocument
├── frontMatter: OrcaFrontMatter?  (Yaml | Toml)
└── blocks: List<OrcaBlock>
    └── (each block may contain List<OrcaInline> or nested List<OrcaBlock>)
```

### Block types (`OrcaBlock` — sealed interface)

`Heading`, `Paragraph`, `ListBlock`, `Quote`, `Admonition`, `CodeBlock`, `Math`, `Image`, `ThematicBreak`, `Table`, `Footnotes`, `HtmlBlock`, `DefinitionList`, `Details`

### Inline types (`OrcaInline` — sealed interface)

`Text`, `Bold`, `Italic`, `Strikethrough`, `Superscript`, `Subscript`, `Highlight`, `Underline`, `InlineCode`, `Math`, `Link`, `Image`, `FootnoteReference`, `HtmlInline`, `Abbreviation`

Both are **sealed interfaces**, enabling exhaustive `when` handling — the compiler enforces that all variants are covered. This is used throughout the rendering layer (see `OrcaBlockNode`).

Supporting types: `OrcaListItem` (with optional `OrcaTaskState`), `OrcaTableCell` (with `OrcaTableAlignment`), `OrcaFootnoteDefinition`, `OrcaDefinitionListItem`, `OrcaAdmonitionType`, `OrcaFrontMatter`.

> For the complete node reference with all properties, see `ast-reference.md`.

---

## Rendering Pipeline

Entry point: the `Orca` composable in `Orca.kt`.

```
OrcaDocument
    │
    ▼
buildRenderBlocks()        ── assigns a stable content-based key to each block
    │
    ▼
LazyColumn / Column        ── root layout (OrcaRootLayout.LAZY_COLUMN or .COLUMN)
    │
    ▼
OrcaBlockNode()            ── exhaustive when-dispatch on OrcaBlock sealed variants
    │
    ├── HeadingNode, ParagraphNode, ListBlockNode, QuoteBlockNode,
    │   CodeBlockNode, TableBlockNode, AdmonitionNode, FootnotesNode,
    │   DefinitionListNode, DetailsNode, MarkdownImageNode, MarkdownMathNode, ...
    │
    └── buildInlineAnnotatedString()  ── OrcaInline list → AnnotatedString
        (OrcaInlineText.kt)              with SpanStyles, LinkAnnotations, inline media/math,
                                         and exact-class inline overrides
```

### Key generation (`buildRenderBlocks`)

Each `OrcaBlock` gets a deterministic string key derived from its content (type prefix + content hash). Duplicate keys get a `#n` suffix. These keys drive `LazyColumn`'s `key` parameter for stable item identity during streaming updates.

### Root layout

`OrcaRootLayout.LAZY_COLUMN` (default) — efficient for long documents, renders items on demand via `LazyColumn` with `items(key = ...)`.

`OrcaRootLayout.COLUMN` — measures all blocks upfront. Use for short content or when nested inside another scrollable container. Uses `BringIntoViewRequester` for footnote navigation.

### Block rendering (`OrcaBlockNode.kt`)

A single `@Composable OrcaBlockNode(block, style, ...)` function dispatches via `when (block)` to dedicated composables. Lists, quotes, admonitions, footnotes, definition lists, and details call back into `OrcaBlockNode` for nested blocks. The complete render context is threaded through recursion: style, URL policy, media slots, math slots, footnote navigation, task interaction, and inline overrides.

### Inline rendering (`OrcaInlineText.kt`)

`buildInlineAnnotatedString()` walks a `List<OrcaInline>` and builds a Compose `AnnotatedString` with:
- `SpanStyle` for bold, italic, strikethrough, inline code, superscript, subscript
- `LinkAnnotation.Url` for links (with `OrcaSecurityPolicy` check — disallowed URLs render as plain text)
- `appendInlineContent()` placeholders for inline images (resolved only when `inlineImageContent` is supplied)
- inline math placeholders resolved by `inlineMathContent` and an optional measured `OrcaInlineMathPlaceholder`
- strict inline HTML `<img>` conversion through the same image policy and slot
- exact-class `inlineOverride` replacements returning `AnnotatedString`
- Footnote references rendered as superscript clickable annotations

The same inline pipeline is used by paragraphs, headings, table cells, definition terms, and details summaries. This prevents nested content from silently losing media, math, security filtering, or host overrides.

### Renderer extension points

- `blockOverride`: exact-class composable replacement for top-level blocks.
- `inlineOverride`: exact-class annotated-text replacement for inline nodes at any nesting depth.
- `imageContent` / `inlineImageContent`: host-owned image loading after `OrcaSecurityPolicy` approval.
- `blockMathContent` / `inlineMathContent`: optional math engines without a base dependency.
- `taskCheckboxContent`: complete replacement for the default semantic task checkbox.

---

## Caching

`OrcaParserCache` (`OrcaParserCache.kt`) is an LRU cache internal to `OrcaMarkdownParser`.

```kotlin
class OrcaParserCache(maxEntries: Int = 64)
```

### `parseCached()` flow

```
parseCached(key, input)
    │
    ▼
cache.getOrPut(key, input) {
    parseWithDiagnostics(input)   // cache miss → full parse
}
    │
    ├── HIT:  key exists AND stored input == current input → return cached OrcaParseResult
    │         (entry is moved to end of LinkedHashMap for LRU ordering)
    │
    └── MISS: parse, store result, evict oldest if size > maxEntries
```

Cache keys are caller-provided (e.g. file path, message ID). The cache also stores the raw `input` string and only returns a hit when the input matches exactly — no stale results.

Thread safety is provided by `OrcaLock.withLock {}` around all map operations. The parse itself runs outside the lock.

---

## Streaming

For LLM token streams, `OrcaStreamingState` accepts delta chunks and publishes renderable snapshots at `frameIntervalMs`. `Orca(state = ..., ...)` avoids a second debounce interval because pacing already happened at the state boundary. `OrcaIncrementalParserSession` supplies a conservative parser fast path: completed blocks are retained as stable AST nodes while only the active tail is reparsed; document-scoped Markdown constructs use the exact full-parser path.

The session is built on three ideas:

- **Segmentation is append-only.** Segments are only ever completed on a terminated line, which makes the completed prefix final and lets the scan resume from the last cut instead of re-reading the stream on every token. Cut points are blank lines *and* column-zero fenced code blocks: the text before an open ``` block is frozen while the block is still streaming, and the block itself is frozen the moment its closing line arrives.
- **An open fence tail is rebuilt, not re-parsed.** While a fence is open the tail is exactly one code block, so the session constructs it from the raw text. Because the delegate is an interface, the shortcut is checked against it once per fence header before it is used, and it is refused for content the delegate's raw-source pre-passes would react to.
- **A cut has to be safe for the pre-passes too.** `<details>` and `$$` extraction run over the raw source before the parser and do not share the scanner's view of fenced code, so the scanner mirrors their state and refuses to cut while either is mid-region.

Heading slugs are assigned from a running counter as blocks freeze, so duplicate titles get the same `-N` suffixes a full parse produces while frozen blocks keep their identity (and therefore their Compose recomposition scopes).

The underlying `Orca(markdown: String, ...)` composable handles each published snapshot as follows:

1. **Stable initial parse** — the first document is parsed synchronously inside `remember` so the first measured frame has the correct height and outer lazy containers do not jump.

2. **Paced background re-parse** — a long-lived `snapshotFlow` conflates rapid `markdown` changes. It:
   - Waits at most one `streamingDebounceMs` interval (default: 80ms) between subsequent parses while retaining the latest input.
   - Runs `parseWithDiagnostics()` on `Dispatchers.Default`.
   - Continues updating during uninterrupted token streams instead of waiting for an idle gap.
   - If parsing fails or returns errors, the previous `document` is kept (graceful degradation).
   - On success, `document` state is updated, triggering recomposition.

3. **Cache synergy** — when `parseCacheKey` is provided, background parses go through `parseCachedWithDiagnostics()`, avoiding repeated work for identical inputs.

---

## Security

`OrcaSecurityPolicy` (`OrcaSecurityPolicy.kt`) is a `fun interface` that gates URL rendering:

```kotlin
fun interface OrcaSecurityPolicy {
    fun isAllowed(type: OrcaUrlType, value: String): Boolean
}
```

`OrcaUrlType` is either `LINK` or `IMAGE`. The policy is checked before creating `LinkAnnotation`s and before Markdown or strict HTML image URLs are handed to supplied content slots.

**If a URL is disallowed**, links render as plain text and images are not passed to a loader. If no image slot is provided, block images render fallback text and inline images render their alt text even under an opt-in policy.

### Built-in policies (`OrcaSecurityPolicies`)

| Policy | Behavior |
|---|---|
| `Default` | Links: `http`, `https`, `mailto`, fragments. Images blocked. |
| `RemoteImages` | Default links plus `http`/`https` images; explicit opt-in for trusted content. |
| `byAllowedSchemes(...)` | Custom scheme sets with optional `allowRelativeLinks` / `allowRelativeImages` flags. |

The policy is passed as a parameter to the `Orca` composable and threaded through to all block/inline renderers.

---

## Styling

`OrcaStyle` (`OrcaStyle.kt`) is a single immutable data class aggregating all visual configuration:

```kotlin
data class OrcaStyle(
    val typography: OrcaTypographyStyle,   // H1–H6 + paragraph TextStyles
    val inline: OrcaInlineStyle,           // SpanStyles for code, links, strikethrough, footnotes, super/sub
    val layout: OrcaLayoutStyle,           // blockSpacing, nestedBlockSpacing, listMarkerWidth
    val quote: OrcaQuoteStyle,             // stripe color/width, spacing
    val code: OrcaCodeBlockStyle,          // text style, background, borders, line numbers, syntax highlighting tokens
    val table: OrcaTableStyle,             // column sizing, cell padding, borders, header background
    val thematicBreak: OrcaThematicBreakStyle,
    val image: OrcaImageStyle,             // shape, background, maxHeight, contentScale
    val admonition: OrcaAdmonitionStyle,   // per-type colors and backgrounds
    val inlineImage: OrcaInlineImageStyle, // size for images embedded in text
    val definitionList: OrcaDefinitionListStyle,
    val details: OrcaDetailsStyle,
    val task: OrcaTaskStyle,               // flat semantic checkbox styling
    val headingRule: OrcaHeadingRuleStyle, // one-pixel rules under selected heading levels
)
```

Since 0.30 the built-in styles are generated rather than hand-written: `orcaFlatStyle(palette,
density, …)` (`OrcaFlatStyle.kt`) maps an `OrcaPalette` — surfaces, outlines, text, accent, plus
nested `OrcaSyntaxPalette` and `OrcaSignalPalette` — onto every sub-style. The token set has no
elevation, gradient, or shadow entry, so no renderer can draw one.

### How it flows

1. Passed to the root `Orca(... style = style ...)` composable.
2. Threaded as a parameter to every `OrcaBlockNode` and from there to individual block composables (`HeadingNode`, `CodeBlockNode`, etc.).
3. Inline rendering reads `style.inline.*` for `SpanStyle`s and `style.typography.paragraph` for base text style.
4. Style itself is passed explicitly throughout. A small set of cross-cutting render options —
   inline math placeholders, task interaction, task checkbox slot, and the active
   `OrcaTextHighlight` — travel as CompositionLocals provided once by the root composable.

### Presets

`OrcaDefaults.lightStyle()`, `darkStyle()`, `contrastLightStyle()`, and `contrastDarkStyle()` are
thin wrappers over `orcaFlatStyle` with the matching `OrcaPalettes` preset and an optional
`OrcaDensity`. `legacyLightStyle()` / `legacyDarkStyle()` keep the pre-0.30 visuals. All sub-styles
still have default values, so `OrcaStyle()` works out of the box.

### Document utilities

`orca-core` exposes read-only projections over a parsed document: `tableOfContents()`, `stats()`,
`findMatches()` / `countMatches()`, and `plainText()`. They are pure functions over the AST with a
depth guard, so they are safe to call on untrusted input and cheap enough to recompute per edit.

---

## Platform Targets

Orca is a Kotlin Multiplatform project targeting:

- **JVM** (Android, Desktop)
- **iOS** (via Kotlin/Native)
- **wasmJs** (Kotlin/Wasm for browser)

### Source set structure

All parsing logic (`orca-core`) and all rendering logic (`orca-compose`) live in `commonMain`. The **only** `expect`/`actual` declaration is `OrcaLock`:

| Source set | `OrcaLock` implementation |
|---|---|
| `commonMain` | `internal expect class OrcaLock` with `fun <T> withLock(block: () -> T): T` |
| `jvmMain` | `synchronized(monitor) { block() }` |
| `iosMain` | Spinlock via `AtomicInt.compareAndSet` |
| `wasmJsMain` | No-op — single-threaded, `block()` called directly |

`OrcaLock` is used exclusively by `OrcaParserCache` to guard the LRU `LinkedHashMap` during concurrent `parseCached()` calls.
