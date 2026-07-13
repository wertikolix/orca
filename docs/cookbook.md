# Cookbook

Practical recipes for common Orca use cases.

## Basic rendering

```kotlin
@Composable
fun MarkdownView(markdown: String) {
    Orca(
        markdown = markdown,
        parser = remember { OrcaMarkdownParser() },
    )
}
```

## Chat / LLM streaming

For token-by-token streaming, push deltas into paced state instead of replacing a whole string on every token:

```kotlin
@Composable
fun ChatMessage(
    messageId: String,
    chunks: Flow<String>,
) {
    val stream = rememberOrcaStreamingState(frameIntervalMs = 80)
    val parser = remember { OrcaIncrementalParserSession(OrcaMarkdownParser()) }

    LaunchedEffect(messageId) {
        parser.reset()
        stream.clear()
        chunks.collect(stream::append)
        stream.finish()
    }

    Orca(
        state = stream,
        parser = parser,
        parseCacheKey = messageId, // stable key avoids redundant re-parses
        rootLayout = OrcaRootLayout.COLUMN, // parent LazyColumn handles scrolling
    )
}
```

Key points:
- `parseCacheKey` should be stable per message (e.g. message ID)
- `frameIntervalMs` controls how frequently buffered deltas publish a renderable snapshot
- `OrcaIncrementalParserSession` reuses completed prose paragraphs; complex Markdown automatically falls back to exact full parsing
- `OrcaRootLayout.COLUMN` when the parent already scrolls (e.g. chat list)
- parsing starts off the UI thread, including first render
- parser instance should be `remember`ed or shared across messages

## Shared parser instance

Create one parser and share it across all messages to share the LRU cache:

```kotlin
val sharedParser = remember { OrcaMarkdownParser(cacheSize = 128) }

LazyColumn {
    items(messages) { message ->
        Orca(
            markdown = message.text,
            parser = sharedParser,
            parseCacheKey = message.id,
            rootLayout = OrcaRootLayout.COLUMN,
        )
    }
}
```

## Pre-parsed AST

Parse once, render multiple times (e.g. preview + full view):

```kotlin
val parser = remember { OrcaMarkdownParser() }
val document = remember(markdown) { parser.parse(markdown) }

// render in multiple places
Orca(document = document, style = previewStyle)
Orca(document = document, style = fullStyle)
```

## Material 3 theme and external scrollbar

Reuse the root `LazyListState` when the host app renders a fast scrollbar or external scroll controls. In Material 3 apps, `rememberOrcaMaterialStyle()` automatically follows the active `MaterialTheme` color scheme, typography, and shapes.

Add `implementation("ru.wertik:orca-compose-material3:0.20.0")` and import `ru.wertik.orca.compose.material3.rememberOrcaMaterialStyle`.

```kotlin
val listState = rememberLazyListState()

Orca(
    document = document,
    listState = listState,
    style = rememberOrcaMaterialStyle(),
)
```

## Custom task checkbox

Keep the source rewrite callback and replace only the visual checkbox when your product has its own control vocabulary:

```kotlin
Orca(
    markdown = markdown,
    parser = parser,
    onTaskToggle = ::rewriteTask,
    taskCheckboxContent = { checked, enabled, onCheckedChange ->
        ProductCheckbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    },
)
```

## Custom inline renderer

`inlineOverride` uses exact AST classes and applies at every supported nesting depth, including table cells and details summaries:

```kotlin
Orca(
    markdown = markdown,
    parser = parser,
    inlineOverride = mapOf(
        OrcaInline.Abbreviation::class to { inline ->
            val abbreviation = inline as OrcaInline.Abbreviation
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append(abbreviation.text)
                }
                append(" (${abbreviation.title})")
            }
        },
    ),
)
```

## Custom link handling

```kotlin
Orca(
    markdown = markdown,
    parser = remember { OrcaMarkdownParser() },
    onLinkClick = { url ->
        when {
            url.startsWith("myapp://") -> handleDeepLink(url)
            url.startsWith("mailto:") -> openEmail(url)
            else -> openBrowser(url)
        }
    },
)
```

## Interactive task lists

Task checkboxes are static by default. Pass `onTaskToggle` to make them tappable. The callback
receives the zero-based document-order index of the task item and the requested state; rewrite
your Markdown source and recompose — rendering stays stateless.

```kotlin
var markdown by rememberSaveable { mutableStateOf(initialMarkdown) }
val taskMarker = Regex("""(?m)^(\s*(?:[-+*]|\d+[.)])\s+)\[( |x|X)]""")

Orca(
    markdown = markdown,
    parser = remember { OrcaMarkdownParser() },
    onTaskToggle = { index, checked ->
        var current = -1
        markdown = taskMarker.replace(markdown) { match ->
            current += 1
            if (current == index) "${match.groupValues[1]}[${if (checked) "x" else " "}]" else match.value
        }
    },
)
```

## Custom security policy

Remote images are blocked by default. For trusted content, enable only the schemes you need together with any custom links:

```kotlin
val policy = OrcaSecurityPolicies.byAllowedSchemes(
    linkSchemes = setOf("https", "myapp", "mailto"),
    imageSchemes = setOf("https", "data"),
    allowRelativeLinks = true,
    allowRelativeImages = true,
)

Orca(
    markdown = markdown,
    parser = remember { OrcaMarkdownParser() },
    securityPolicy = policy,
    imageContent = { url, description -> OrcaCoilImage(url, description, style) },
    inlineImageContent = { url, description -> OrcaCoilInlineImage(url, description, style) },
)
```

## Safe HTML media

Standalone HTML media uses the same policy and slots as Markdown images. Orca recognizes strict `<img>` blocks, `<figure>` with `<figcaption>`, and inline `<img>` tags. Mixed or complex HTML stays in the readable HTML fallback.

```markdown
<figure>
  <img src="https://example.com/diagram.png" alt="Architecture diagram">
  <figcaption>Request flow through the renderer.</figcaption>
</figure>

Status <img src="https://example.com/status.png" alt="Ready"> ready.
```

```kotlin
Orca(
    markdown = markdown,
    parser = parser,
    securityPolicy = OrcaSecurityPolicies.RemoteImages,
    imageContent = { url, description -> BlockImage(url, description) },
    inlineImageContent = { url, description -> InlineImage(url, description) },
)
```

## Dark theme

```kotlin
val style = if (isSystemInDarkTheme()) {
    OrcaDefaults.darkStyle()
} else {
    OrcaDefaults.lightStyle()
}

Orca(
    markdown = markdown,
    parser = remember { OrcaMarkdownParser() },
    style = style,
)
```

## Material 3 theme integration

Follow your active Material 3 color scheme, typography, and shapes:

```kotlin
Orca(
    document = document,
    style = rememberOrcaMaterialStyle(),
)
```

## Monitoring parse errors

```kotlin
Orca(
    markdown = markdown,
    parser = remember { OrcaMarkdownParser() },
    onParseDiagnostics = { diagnostics ->
        if (diagnostics.hasErrors) {
            Log.w("Orca", "Parse errors: ${diagnostics.errors}")
        }
        if (diagnostics.hasWarnings) {
            Log.d("Orca", "Parse warnings: ${diagnostics.warnings}")
        }
    },
)
```

## Depth limit for untrusted input

Prevent stack overflow from deeply nested markdown:

```kotlin
val parser = OrcaMarkdownParser(
    maxTreeDepth = 16, // default is 64
    onDepthLimitExceeded = { depth ->
        Log.w("Orca", "Depth limit exceeded: $depth")
    },
)
```

## Accessing front matter

```kotlin
val parser = OrcaMarkdownParser()
val document = parser.parse("""
    ---
    title: My Document
    author: John
    ---
    
    # Content here
""".trimIndent())

val frontMatter = document.frontMatter
if (frontMatter is OrcaFrontMatter.Yaml) {
    val title = frontMatter.entries["title"] // "My Document"
    val author = frontMatter.entries["author"] // "John"
}
```

## Extracting headings for table of contents

Since 0.15.0 the core module ships a TOC API, and the Compose module maps anchor
ids to lazy-list indices for scroll-to-section UIs:

```kotlin
val document = parser.parse(markdown)
val toc = document.tableOfContents(maxLevel = 2) // List<OrcaTocEntry>(level, title, id)
val anchors = orcaHeadingBlockIndex(document)    // id -> top-level block index

val listState = rememberLazyListState()
val scope = rememberCoroutineScope()

Row {
    toc.forEach { entry ->
        TextButton(onClick = {
            anchors[entry.id]?.let { index ->
                scope.launch { listState.animateScrollToItem(index) }
            }
        }) { Text(entry.title) }
    }
}

Orca(markdown = markdown, parser = parser, listState = listState)
```

## Streaming cursor for chat bubbles

Show a cursor glyph after the last block while an LLM response streams. The glyph
is applied to the parsed document — never to the source — so
`OrcaIncrementalParserSession` keeps its append-only fast path:

```kotlin
Orca(
    state = stream,                      // OrcaStreamingState
    parser = remember { OrcaIncrementalParserSession(OrcaMarkdownParser()) },
    streamingCursor = "\u258D",          // shown only while stream.isStreaming
)
```

## Custom parser implementation

Implement `OrcaParser` for a custom backend:

```kotlin
class MyCustomParser : OrcaParser {
    override fun parse(input: String): OrcaDocument {
        // your parsing logic here
        return OrcaDocument(blocks = myBlocks)
    }
}

Orca(
    markdown = markdown,
    parser = MyCustomParser(),
)
```

## Embedding in ScrollView

Use `COLUMN` layout when Orca is inside a scrollable parent:

```kotlin
Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
    Text("Header")
    Orca(
        markdown = markdown,
        parser = remember { OrcaMarkdownParser() },
        rootLayout = OrcaRootLayout.COLUMN, // no nested LazyColumn
    )
    Text("Footer")
}
```

Use `LAZY_COLUMN` (default) when Orca is the root scrollable:

```kotlin
Orca(
    markdown = longDocument,
    parser = remember { OrcaMarkdownParser() },
    rootLayout = OrcaRootLayout.LAZY_COLUMN, // efficient for long content
)
```

## Definition lists

Orca supports PHP Markdown Extra / Pandoc definition list syntax:

```kotlin
val markdown = """
    Apple
    : A fruit that grows on trees.
    : Used in pies, cider, and juice.

    Kotlin
    : A modern programming language for the JVM.
""".trimIndent()

Orca(
    markdown = markdown,
    parser = remember { OrcaMarkdownParser() },
)
```

Customize definition list appearance:

```kotlin
OrcaStyle(
    definitionList = OrcaDefinitionListStyle(
        termStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0)),
        definitionIndent = 24.dp,
        termSpacing = 12.dp,
        definitionSpacing = 6.dp,
    ),
)
```

## Abbreviations

Define abbreviations once, and all occurrences in the document get styled automatically:

```kotlin
val markdown = """
    *[HTML]: Hyper Text Markup Language
    *[CSS]: Cascading Style Sheets
    *[API]: Application Programming Interface

    The HTML specification defines how browsers render pages.
    Use CSS for styling and the REST API for data.
""".trimIndent()

Orca(
    markdown = markdown,
    parser = remember { OrcaMarkdownParser() },
)
```

Abbreviation definitions are removed from the rendered output. Matched text gets an underline style by default. Customize via:

```kotlin
OrcaStyle(
    inline = OrcaInlineStyle(
        abbreviation = SpanStyle(
            textDecoration = TextDecoration.Underline,
            color = Color(0xFF6A1B9A),
        ),
    ),
)
```

## Accessing definition lists from the AST

```kotlin
val parser = OrcaMarkdownParser()
val document = parser.parse(markdown)

document.blocks
    .filterIsInstance<OrcaBlock.DefinitionList>()
    .flatMap { it.items }
    .forEach { item ->
        val termText = item.term.filterIsInstance<OrcaInline.Text>().joinToString("") { it.text }
        println("Term: $termText, definitions: ${item.definitions.size}")
    }
```
