package ru.wertik.orca.sample

internal fun sampleMarkdown(screen: SampleScreen): String = when (screen) {
    SampleScreen.OVERVIEW -> OVERVIEW_MARKDOWN
    SampleScreen.BLOCKS -> BLOCKS_MARKDOWN
    SampleScreen.TABLES -> TABLES_MARKDOWN
    SampleScreen.MEDIA -> MEDIA_MARKDOWN
    SampleScreen.MATH -> MATH_MARKDOWN
    SampleScreen.ADVANCED -> ADVANCED_MARKDOWN
    SampleScreen.RENDERERS -> RENDERERS_MARKDOWN
    else -> ""
}

private val TASK_MARKER_REGEX = Regex("""(?m)^(\s*(?:[-+*]|\d+[.)])\s+)\[( |x|X)]""")

internal fun toggleMarkdownTask(markdown: String, taskIndex: Int, checked: Boolean): String {
    var current = -1
    return TASK_MARKER_REGEX.replace(markdown) { match ->
        current += 1
        if (current == taskIndex) {
            "${match.groupValues[1]}[${if (checked) "x" else " "}]"
        } else {
            match.value
        }
    }
}

private val OVERVIEW_MARKDOWN = """
    # Orca 0.20

    A Compose Multiplatform Markdown renderer built for documents, chat responses, and live previews. The core stays small while image, Material, and native math integrations remain opt-in.

    ## Rendering contract

    Orca parses Markdown into a predictable AST, applies a URL policy, then renders stable Compose nodes. The first measured frame contains real content, and subsequent streaming updates run away from the UI thread.

    - **Portable:** Android, iOS, Desktop, and wasmJs
    - **Controlled:** host-owned links, images, tasks, and inline overrides
    - **Readable:** selectable text, anchors, footnotes, code actions, and responsive tables

    ## Start here

    ```kotlin
    val parser = remember { OrcaMarkdownParser() }

    Orca(
        markdown = source,
        parser = parser,
        style = rememberOrcaMaterialStyle(),
        securityPolicy = OrcaSecurityPolicies.Default,
    )
    ```

    ## Integration checklist

    - [x] Pick a stable parser instance
    - [x] Match the renderer to the app theme
    - [x] Keep remote media opt-in
    - [ ] Tap this task to test source rewriting

    ## Navigation

    [Jump to the rendering contract](#rendering-contract) or inspect the other suites for tables, native math, HTML media, streaming, and renderer overrides.
""".trimIndent()

private val BLOCKS_MARKDOWN = """
    # Blocks and states

    ## Admonitions

    > [!NOTE]
    > Callouts now use a full one-pixel outline, a quiet surface tint, and a semantic title. There is no elevation or decorative side stripe.

    > [!TIP]
    > Keep one `OrcaMarkdownParser` per document surface or share it across message rows to reuse the parser cache.

    > [!IMPORTANT]
    > Remote image loading still requires both an explicit security policy and an image content slot.

    > [!WARNING]
    > Treat Markdown input as untrusted even when the source comes from your own backend.

    > [!CAUTION]
    > Never open an allowed link without applying the host application's navigation policy.

    ## Quote

    > Good rendering feels quiet. Structure should remain obvious while the document, not its container, receives attention.
    >
    > Nested content, **emphasis**, and links continue to use the same renderer context.

    ## Code with actions

    ```kotlin
    suspend fun renderMessage(message: Message): OrcaDocument =
        withContext(Dispatchers.Default) {
            parser.parseCached(message.id, message.markdown)
        }
    ```

    ```json
    {
      "surface": "flat",
      "gradients": false,
      "shadows": false,
      "selection": true
    }
    ```

    ## Inline vocabulary

    Use **bold**, *italic*, ~~removed text~~, ++inserted text++, ==highlight==, `inline code`, x^2^, and H~2~O in the same paragraph.

    Keyboard hints also work through HTML: <kbd>Ctrl</kbd> + <kbd>K</kbd> opens a command surface.
""".trimIndent()

private val TABLES_MARKDOWN = """
    # Responsive data

    Tables estimate useful column widths, preserve alignment, expose collection semantics, and show a solid overflow position indicator only when horizontal scrolling is available.

    ## Module matrix

    | Module | Purpose | Android | iOS | Desktop | Web |
    |:--|:--|:--:|:--:|:--:|:--:|
    | `orca-core` | AST and parser | Ready | Ready | Ready | Ready |
    | `orca-compose` | Foundation renderer | Ready | Ready | Ready | Ready |
    | `orca-compose-material3` | Theme adapter | Ready | Ready | Ready | Ready |
    | `orca-images-coil` | Remote media | Ready | Ready | Ready | Ready |
    | `orca-math-orcex` | Native LaTeX | Ready | Ready | Ready | Planned |

    ## Alignment and rich cells

    | Input | Render | Confidence | Notes |
    |:--|:--:|--:|:--|
    | `**strong**` | **strong** | 100% | Nested styles stay intact |
    | `${'$'}x^2${'$'}` | ${'$'}x^2${'$'} | 100% | Inline math slots now reach table cells |
    | `<img>` | <img src="https://raw.githubusercontent.com/JetBrains/kotlin-web-site/master/static/images/kotlin-logo.png" alt="Kotlin"> | 100% | HTML media uses the same URL policy |
    | `[link](url)` | [docs](https://github.com/wertikolix/orca) | 100% | Host callback remains authoritative |

    ## Wide content

    | Event | Parser path | Stable prefix | Active tail | UI behavior | Security |
    |:--|:--|:--|:--|:--|:--|
    | Initial document | Exact parse | Complete | None | First frame is measured | URL policy applied |
    | Plain token delta | Incremental | Reused | Reparsed | Paced update | URL policy applied |
    | Open code fence | Exact fallback | Conservative | Reparsed | Source stays readable | URL policy applied |
""".trimIndent()

private val MEDIA_MARKDOWN = """
    # Safe media rendering

    Standard Markdown images and strict HTML media blocks share the same image slot, caption style, content description, and `OrcaSecurityPolicy` decision.

    ## Markdown image

    ![Kotlin logo](https://raw.githubusercontent.com/JetBrains/kotlin-web-site/master/static/images/kotlin-logo.png "A standard Markdown title becomes the visible caption.")

    ## HTML figure

    <figure>
      <img src="https://raw.githubusercontent.com/JetBrains/kotlin-web-site/master/static/images/kotlin-logo.png" alt="Kotlin logo rendered from HTML">
      <figcaption>Strict figure parsing routes this image through the existing secure renderer.</figcaption>
    </figure>

    ## Inline HTML image

    The inline pipeline can now place <img src="https://raw.githubusercontent.com/JetBrains/kotlin-web-site/master/static/images/kotlin-logo.png" alt="Kotlin"> inside text, including table cells and details summaries.

    > [!NOTE]
    > Mixed HTML such as a paragraph followed by an image is not collapsed into a media node. Unknown or complex markup keeps the readable text fallback.
""".trimIndent()

private val MATH_MARKDOWN = """
    # Native LaTeX

    Orca keeps formula nodes in the common AST. The optional Orcex adapter renders them natively on Android, iOS, and Desktop without adding a math engine to the base artifact.

    Inline formulas stay aligned with prose: ${'$'}E = mc^2${'$'}, ${'$'}\alpha + \beta = \gamma${'$'}, and ${'$'}\sqrt{x^2 + y^2}${'$'}.

    ## Display equation

    ${'$'}${'$'}
    \frac{1}{n} \sum_{i=1}^{n} x_i = \bar{x}
    ${'$'}${'$'}

    ## Matrix

    ${'$'}${'$'}
    A = \begin{bmatrix}
      1 & 2 \\
      3 & 4
    \end{bmatrix}
    ${'$'}${'$'}

    ## Framed result

    ${'$'}${'$'}
    \boxed{\int_0^1 x^2 \, dx = \frac{1}{3}}
    ${'$'}${'$'}

    Unclosed formulas remain readable source while tokens arrive, then switch to the native renderer only after the closing delimiter is present.
""".trimIndent()

private val ADVANCED_MARKDOWN = """
    # Extended documents

    ## Details

    <details>
    <summary>**Open** the implementation notes with ${'$'}x^2${'$'}</summary>

    Details preserve rich summary content, accessible state, nested lists, images, and math slots.

    - The summary controls expansion
    - The body accepts regular Markdown blocks
    - The initial `open` attribute is respected

    </details>

    <details open>
    <summary>Initially expanded</summary>

    This section starts open and remains fully selectable.

    </details>

    ## Definition lists

    Orca
    : A Markdown AST and Compose renderer with explicit extension points.

    Render context
    : The style, security policy, media slots, math slots, and inline overrides shared by nested nodes.

    ## Footnotes

    Kotlin first appeared publicly in 2011[^kotlin]. Compose Multiplatform shares declarative UI across targets[^compose].

    [^kotlin]: JetBrains designed Kotlin for pragmatic interoperability and maintainable application code.
    [^compose]: The same Compose model now targets Android, iOS, Desktop, and Web.

    ## Abbreviations

    *[KMP]: Kotlin Multiplatform
    *[AST]: Abstract Syntax Tree

    KMP consumers can parse once, inspect the AST, and render the same document with different visual styles.
""".trimIndent()

private val RENDERERS_MARKDOWN = """
    # Renderer extensions

    Version 0.20 adds an exact-class `inlineOverride` API beside the existing composable `blockOverride` map. Overrides receive the source AST node and return annotated text; every nested renderer uses the same map.

    *[AST]: Abstract Syntax Tree
    *[KMP]: Kotlin Multiplatform
    *[DSL]: Domain-Specific Language

    In this suite, AST, KMP, and DSL use a live custom renderer that expands each abbreviation inline. The Markdown source remains unchanged.

    ## Inline override

    ```kotlin
    Orca(
        markdown = source,
        parser = parser,
        inlineOverride = mapOf(
            OrcaInline.Abbreviation::class to { node ->
                val value = node as OrcaInline.Abbreviation
                AnnotatedString("${'$'}{value.text} (${ '$' }{value.title})")
            },
        ),
    )
    ```

    ## Block override

    `blockOverride` remains composable and is useful for diagrams, domain-specific code fences, embeds, or product-owned callouts. Both maps use exact classes so fallback behavior stays deterministic.

    > [!TIP]
    > Keep custom rendering narrow. Let Orca handle links, security, selection, nested layout, and stable block identity whenever the built-in node already fits.
""".trimIndent()

internal val STREAMING_DEMO_MARKDOWN = """
    # Streaming without jitter

    Token deltas arrive frequently. Orca publishes a paced snapshot and reuses completed segments whenever the syntax remains safe.

    ## Data path

    ```text
    network delta
      -> OrcaStreamingState.append
      -> paced snapshot
      -> OrcaIncrementalParserSession
      -> stable Compose blocks
    ```

    ## Compose usage

    ```kotlin
    val stream = rememberOrcaStreamingState(frameIntervalMs = 80)
    val parser = remember { OrcaIncrementalParserSession(OrcaMarkdownParser()) }

    LaunchedEffect(messageId) {
        chunks.collect(stream::append)
        stream.finish()
    }

    Orca(state = stream, parser = parser, streamingCursor = "▍")
    ```

    > [!TIP]
    > Pause this lab, change the playback speed, or replay it. The source is append-only and the cursor never enters the parser input.

    ## Stable completion

    The final document is exact. During generation, conservative fallbacks keep open fences, tables, and formulas readable instead of guessing at incomplete syntax.
""".trimIndent()

internal val PLAYGROUND_DEFAULT_MARKDOWN = """
    # Orca 0.20 playground

    Edit the source and inspect a live preview. Wide screens keep both panes visible; compact screens preserve space with a focused Source/Preview switch.

    ## Try the renderer

    - [x] Flat checkbox renderer
    - [ ] Interactive source rewrite
    - [ ] Add your own Markdown

    > [!NOTE]
    > Orca uses outlines, solid surfaces, and typography. The sample contains no gradients or shadows.

    | Surface | State |
    |:--|:--:|
    | Editor | Live |
    | Preview | Live |

    Use **bold**, `code`, ==highlight==, ++underline++, or [open the project](https://github.com/wertikolix/orca).
""".trimIndent()
