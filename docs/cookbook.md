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

For token-by-token streaming where `markdown` updates frequently:

```kotlin
@Composable
fun ChatMessage(
    messageId: String,
    markdown: String, // updated on every token
) {
    Orca(
        markdown = markdown,
        parser = remember { OrcaMarkdownParser() },
        parseCacheKey = messageId, // stable key avoids redundant re-parses
        streamingDebounceMs = 80, // default; periodically renders the latest streamed value
        rootLayout = OrcaRootLayout.COLUMN, // parent LazyColumn handles scrolling
    )
}
```

Key points:
- `parseCacheKey` should be stable per message (e.g. message ID)
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

## Material3 theme integration

Match Orca colors to your Material3 theme:

```kotlin
val colorScheme = MaterialTheme.colorScheme

val style = OrcaStyle(
    typography = OrcaTypographyStyle(
        paragraph = TextStyle(color = colorScheme.onSurface),
        heading1 = TextStyle(color = colorScheme.onSurface, fontSize = 30.sp, fontWeight = FontWeight.Bold),
        // ... other headings
    ),
    inline = OrcaInlineStyle(
        link = SpanStyle(color = colorScheme.primary, textDecoration = TextDecoration.Underline),
    ),
    code = OrcaCodeBlockStyle(
        background = colorScheme.surfaceVariant,
        borderColor = colorScheme.outline,
    ),
    table = OrcaTableStyle(
        headerBackground = colorScheme.surfaceVariant,
        borderColor = colorScheme.outline,
    ),
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

```kotlin
val document = parser.parse(markdown)
val headings = document.blocks
    .filterIsInstance<OrcaBlock.Heading>()
    .map { heading ->
        val text = heading.content
            .filterIsInstance<OrcaInline.Text>()
            .joinToString("") { it.text }
        heading.level to text
    }
// headings: List<Pair<Int, String>>
// e.g. [(1, "Title"), (2, "Section"), (3, "Subsection")]
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
