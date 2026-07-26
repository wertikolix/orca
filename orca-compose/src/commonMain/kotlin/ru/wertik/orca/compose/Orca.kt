package ru.wertik.orca.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaDocument
import ru.wertik.orca.core.OrcaInline
import ru.wertik.orca.core.OrcaParseError
import ru.wertik.orca.core.OrcaParseDiagnostics
import ru.wertik.orca.core.OrcaParseResult
import ru.wertik.orca.core.OrcaParser
import kotlin.reflect.KClass

private const val PARSE_LOG_TAG = "Orca"
private const val DEFAULT_STREAMING_DEBOUNCE_MS = 80L

private val defaultStyle: OrcaStyle = OrcaDefaults.lightStyle()
private val noOpLinkClick: (String) -> Unit = {}

/**
 * Root layout strategy for the Orca composable.
 *
 * @see Orca
 */
enum class OrcaRootLayout {
    /** Uses a [LazyColumn] — efficient for long documents, renders items on demand. */
    LAZY_COLUMN,

    /** Uses a plain [Column] — measures all blocks upfront, suitable for short content or nested scrollable containers. */
    COLUMN,
}

/**
 * Renders Markdown text as Compose UI.
 *
 * Parses [markdown] using the supplied [parser] and renders the resulting document.
 * Parsing runs off the UI thread and paced updates keep streaming content responsive
 * without parsing every incoming token.
 *
 * @param markdown raw Markdown string to render.
 * @param modifier [Modifier] applied to the root layout.
 * @param parser [OrcaParser] implementation used to convert Markdown to an [OrcaDocument].
 * @param parseCacheKey optional cache key passed to [OrcaParser.parseCached]; when `null`, caching is bypassed.
 * @param style visual configuration for all rendered elements.
 * @param rootLayout whether to use a [LazyColumn][OrcaRootLayout.LAZY_COLUMN] or a [Column][OrcaRootLayout.COLUMN].
 * @param listState state of the root [LazyColumn]; expose it to synchronize scrollbars or external controls.
 * @param securityPolicy URL filter applied to links and images before rendering.
 * @param onLinkClick callback invoked when a user taps a link.
 * @param onParseDiagnostics optional callback receiving parse diagnostics (errors and warnings) after each parse.
 * @param streamingDebounceMs minimum pacing delay in milliseconds between streaming re-parses. Default is 80 ms.
 * @param blockOverride optional map of block types to custom composable renderers. When a block's class matches a key, the override is used instead of the default renderer.
 * @param imageContent optional composable for rendering allowed block images. Without it, block images render as fallback text.
 * @param inlineImageContent optional composable for rendering allowed inline images. Without it, inline images render their alt text.
 * @param onTaskToggle optional callback making task-list checkboxes interactive. Receives the
 * document-order task index and the requested state; the host should update the Markdown source.
 * @param streamingCursor optional glyph (e.g. `"\u258D"`) appended after the last block's text,
 * typically while an LLM response is still streaming. Applied to the parsed document, never to
 * the source, so incremental parser sessions keep their append-only fast path.
 * @param inlineOverride optional exact-class inline renderers producing custom annotated text.
 * @param taskCheckboxContent optional replacement for the default flat task-list checkbox.
 * @param highlight optional search query shaded in rendered inline text with
 * [OrcaInlineStyle.searchMatch]. Pair it with `OrcaDocument.findMatches()` for match navigation.
 * @see Orca
 * @see OrcaStyle
 * @see OrcaSecurityPolicy
 */
@Composable
fun Orca(
    markdown: String,
    modifier: Modifier = Modifier,
    parser: OrcaParser,
    parseCacheKey: Any? = null,
    style: OrcaStyle = defaultStyle,
    rootLayout: OrcaRootLayout = OrcaRootLayout.LAZY_COLUMN,
    listState: LazyListState = rememberLazyListState(),
    securityPolicy: OrcaSecurityPolicy = OrcaSecurityPolicies.Default,
    onLinkClick: (String) -> Unit = noOpLinkClick,
    onParseDiagnostics: ((OrcaParseDiagnostics) -> Unit)? = null,
    streamingDebounceMs: Long = DEFAULT_STREAMING_DEBOUNCE_MS,
    blockOverride: Map<KClass<out OrcaBlock>, @Composable (OrcaBlock) -> Unit> = emptyMap(),
    imageContent: OrcaImageContent? = null,
    inlineImageContent: OrcaImageContent? = null,
    blockMathContent: OrcaMathContent? = null,
    inlineMathContent: OrcaMathContent? = null,
    inlineMathPlaceholder: OrcaInlineMathPlaceholder? = null,
    onTaskToggle: OrcaTaskToggle? = null,
    streamingCursor: String? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
    taskCheckboxContent: OrcaTaskCheckboxContent? = null,
    highlight: OrcaTextHighlight? = null,
) {
    val parserKey = remember(parser) { parser.cacheKey() }
    val latestMarkdown by rememberUpdatedState(markdown)
    val latestOnParseDiagnostics by rememberUpdatedState(onParseDiagnostics)

    // Establish the first measured layout synchronously. Rendering an empty document first makes
    // outer lazy lists measure a zero-height message and jump when async parsing fills it in.
    val initialParseResult = remember(parser, parserKey) {
        runCatching {
            if (parseCacheKey == null) {
                parser.parseWithDiagnostics(markdown)
            } else {
                parser.parseCachedWithDiagnostics(key = parseCacheKey, input = markdown)
            }
        }.getOrElse {
            OrcaParseResult(document = OrcaDocument(emptyList()))
        }
    }
    var document by remember(parser, parserKey) { mutableStateOf(initialParseResult.document) }

    LaunchedEffect(initialParseResult) {
        if (initialParseResult.diagnostics.hasWarnings || initialParseResult.diagnostics.hasErrors) {
            latestOnParseDiagnostics?.invoke(initialParseResult.diagnostics)
        }
    }

    // Re-parse updates off the UI thread after the synchronous first measured frame.
    // Conflation keeps the newest stream value without starving rendering until the stream stops.
    LaunchedEffect(parser, parserKey, parseCacheKey, streamingDebounceMs) {
        var hasParsedInput = false
        var lastParsedInput: String? = null
        snapshotFlow { latestMarkdown }
            .conflate()
            .collect {
                if (hasParsedInput && streamingDebounceMs > 0) {
                    delay(streamingDebounceMs)
                }
                val input = latestMarkdown
                if (input == lastParsedInput) return@collect
                lastParsedInput = input
                hasParsedInput = true

                var parseError: Throwable? = null
                val parsedResult = try {
                    withContext(Dispatchers.Default) {
                        if (parseCacheKey == null) {
                            parser.parseWithDiagnostics(input)
                        } else {
                            parser.parseCachedWithDiagnostics(
                                key = parseCacheKey,
                                input = input,
                            )
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    println("W/$PARSE_LOG_TAG: failed to parse markdown, using previous document: ${error.message}")
                    parseError = error
                    null
                }

                val parsed = if (parsedResult == null) {
                    document
                } else if (parsedResult.diagnostics.hasErrors && parsedResult.document.blocks.isEmpty()) {
                    println("W/$PARSE_LOG_TAG: parser reported errors with empty result, using previous document")
                    document
                } else {
                    parsedResult.document
                }
                latestOnParseDiagnostics?.invoke(
                    parsedResult?.diagnostics ?: OrcaParseDiagnostics(
                        errors = listOf(
                            OrcaParseError.ParserFailure(
                                message = parseError?.message ?: "Unknown parse failure",
                            ),
                        ),
                    ),
                )
                document = parsed
            }
    }

    Orca(
        document = document,
        modifier = modifier,
        style = style,
        rootLayout = rootLayout,
        listState = listState,
        securityPolicy = securityPolicy,
        onLinkClick = onLinkClick,
        blockOverride = blockOverride,
        imageContent = imageContent,
        inlineImageContent = inlineImageContent,
        blockMathContent = blockMathContent,
        inlineMathContent = inlineMathContent,
        inlineMathPlaceholder = inlineMathPlaceholder,
        onTaskToggle = onTaskToggle,
        streamingCursor = streamingCursor,
        inlineOverride = inlineOverride,
        taskCheckboxContent = taskCheckboxContent,
        highlight = highlight,
    )
}

/**
 * Renders Markdown from a delta-buffered [OrcaStreamingState].
 *
 * The state already paces published snapshots, so this overload parses each published value
 * immediately rather than applying a second debounce interval. When [streamingCursor] is
 * supplied, the glyph is shown after the last block only while [OrcaStreamingState.isStreaming].
 */
@Composable
fun Orca(
    state: OrcaStreamingState,
    modifier: Modifier = Modifier,
    parser: OrcaParser,
    parseCacheKey: Any? = null,
    style: OrcaStyle = defaultStyle,
    rootLayout: OrcaRootLayout = OrcaRootLayout.LAZY_COLUMN,
    listState: LazyListState = rememberLazyListState(),
    securityPolicy: OrcaSecurityPolicy = OrcaSecurityPolicies.Default,
    onLinkClick: (String) -> Unit = noOpLinkClick,
    onParseDiagnostics: ((OrcaParseDiagnostics) -> Unit)? = null,
    blockOverride: Map<KClass<out OrcaBlock>, @Composable (OrcaBlock) -> Unit> = emptyMap(),
    imageContent: OrcaImageContent? = null,
    inlineImageContent: OrcaImageContent? = null,
    blockMathContent: OrcaMathContent? = null,
    inlineMathContent: OrcaMathContent? = null,
    inlineMathPlaceholder: OrcaInlineMathPlaceholder? = null,
    onTaskToggle: OrcaTaskToggle? = null,
    streamingCursor: String? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
    taskCheckboxContent: OrcaTaskCheckboxContent? = null,
    highlight: OrcaTextHighlight? = null,
) {
    Orca(
        markdown = state.markdown,
        modifier = modifier,
        parser = parser,
        parseCacheKey = parseCacheKey,
        style = style,
        rootLayout = rootLayout,
        listState = listState,
        securityPolicy = securityPolicy,
        onLinkClick = onLinkClick,
        onParseDiagnostics = onParseDiagnostics,
        streamingDebounceMs = 0,
        blockOverride = blockOverride,
        imageContent = imageContent,
        inlineImageContent = inlineImageContent,
        blockMathContent = blockMathContent,
        inlineMathContent = inlineMathContent,
        inlineMathPlaceholder = inlineMathPlaceholder,
        onTaskToggle = onTaskToggle,
        streamingCursor = streamingCursor?.takeIf { state.isStreaming },
        inlineOverride = inlineOverride,
        taskCheckboxContent = taskCheckboxContent,
        highlight = highlight,
    )
}

/**
 * Renders a pre-parsed [OrcaDocument] as Compose UI.
 *
 * Use this overload when you already have a parsed AST (e.g. from a custom parser pipeline
 * or server-side pre-processing). For raw Markdown input, prefer the [Orca] overload that
 * accepts a `String`.
 *
 * @param document pre-parsed Markdown AST to render.
 * @param modifier [Modifier] applied to the root layout.
 * @param style visual configuration for all rendered elements.
 * @param rootLayout whether to use a [LazyColumn][OrcaRootLayout.LAZY_COLUMN] or a [Column][OrcaRootLayout.COLUMN].
 * @param listState state of the root [LazyColumn]; expose it to synchronize scrollbars or external controls.
 * @param securityPolicy URL filter applied to links and images before rendering.
 * @param onLinkClick callback invoked when a user taps a link.
 * @param blockOverride optional map of block types to custom composable renderers.
 * @param imageContent optional composable for rendering allowed block images.
 * @param inlineImageContent optional composable for rendering allowed inline images.
 * @param onTaskToggle optional callback making task-list checkboxes interactive. Receives the
 * document-order task index and the requested state; the host should update the Markdown source.
 * @param streamingCursor optional glyph appended after the last block's text.
 * @param inlineOverride optional exact-class inline renderers producing custom annotated text.
 * @param taskCheckboxContent optional replacement for the default flat task-list checkbox.
 * @param highlight optional search query shaded in rendered inline text.
 * @see OrcaDocument
 * @see OrcaStyle
 */
@Composable
fun Orca(
    document: OrcaDocument,
    modifier: Modifier = Modifier,
    style: OrcaStyle = defaultStyle,
    rootLayout: OrcaRootLayout = OrcaRootLayout.LAZY_COLUMN,
    listState: LazyListState = rememberLazyListState(),
    securityPolicy: OrcaSecurityPolicy = OrcaSecurityPolicies.Default,
    onLinkClick: (String) -> Unit = noOpLinkClick,
    blockOverride: Map<KClass<out OrcaBlock>, @Composable (OrcaBlock) -> Unit> = emptyMap(),
    imageContent: OrcaImageContent? = null,
    inlineImageContent: OrcaImageContent? = null,
    blockMathContent: OrcaMathContent? = null,
    inlineMathContent: OrcaMathContent? = null,
    inlineMathPlaceholder: OrcaInlineMathPlaceholder? = null,
    onTaskToggle: OrcaTaskToggle? = null,
    streamingCursor: String? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
    taskCheckboxContent: OrcaTaskCheckboxContent? = null,
    highlight: OrcaTextHighlight? = null,
) {
    val displayDocument = remember(document, streamingCursor) {
        if (streamingCursor.isNullOrEmpty()) document else document.withTrailingCursor(streamingCursor)
    }
    val renderBlocks = remember(displayDocument.blocks) {
        buildRenderBlocks(displayDocument.blocks)
    }
    val footnoteNumbers = remember(displayDocument.blocks) {
        buildFootnoteNumbers(displayDocument.blocks)
    }
    val blockIndexByKey = remember(renderBlocks) {
        renderBlocks.mapIndexed { index, renderBlock ->
            renderBlock.key to index
        }.toMap()
    }
    val footnoteBlockIndices = remember(renderBlocks, blockIndexByKey) {
        renderBlocks
            .filter { renderBlock -> renderBlock.block is OrcaBlock.Footnotes }
            .mapNotNull { renderBlock -> blockIndexByKey[renderBlock.key]?.let { renderBlock to it } }
    }
    val headingAnchorIndex = remember(renderBlocks) {
        buildMap {
            for ((index, rb) in renderBlocks.withIndex()) {
                val heading = rb.block as? OrcaBlock.Heading ?: continue
                val id = heading.id?.takeIf { it.isNotEmpty() } ?: continue
                if (id !in this) put(id, index)
            }
        }
    }

    fun findFootnoteBlockIndex(label: String): Int? {
        for ((renderBlock, index) in footnoteBlockIndices) {
            val footnotes = renderBlock.block as OrcaBlock.Footnotes
            if (footnotes.definitions.any { it.label == label }) return index
        }
        return footnoteBlockIndices.firstOrNull()?.second
    }

    var activeFootnoteLabel by remember(displayDocument.blocks) { mutableStateOf<String?>(null) }
    val footnoteSourceStack = remember(displayDocument.blocks) {
        mutableStateMapOf<String, MutableList<String>>()
    }
    val scope = rememberCoroutineScope()

    val latestOnTaskToggle by rememberUpdatedState(onTaskToggle)
    val taskInteraction = remember(displayDocument.blocks, onTaskToggle != null) {
        if (onTaskToggle == null) {
            null
        } else {
            OrcaTaskListInteraction(
                indices = buildTaskIndices(displayDocument.blocks),
                onTaskToggle = { index, checked -> latestOnTaskToggle?.invoke(index, checked) },
            )
        }
    }

    fun onFootnoteReferenceClick(label: String, sourceBlockKey: String, scrollToFootnotes: (() -> Unit)?) {
        footnoteSourceStack.getOrPut(label) { mutableListOf() }.add(sourceBlockKey)
        activeFootnoteLabel = label
        scrollToFootnotes?.invoke()
    }

    fun onFootnoteBackClick(label: String, scrollToSource: ((String) -> Unit)?) {
        val stack = footnoteSourceStack[label]
        val sourceBlockKey = stack?.removeLastOrNull() ?: return
        activeFootnoteLabel = null
        scrollToSource?.invoke(sourceBlockKey)
    }

    CompositionLocalProvider(
        LocalOrcaInlineMathPlaceholder provides inlineMathPlaceholder,
        LocalOrcaTaskInteraction provides taskInteraction,
        LocalOrcaTaskCheckboxContent provides taskCheckboxContent,
        LocalOrcaTextHighlight provides highlight?.takeIf { it.isActive },
    ) {
        when (rootLayout) {
        OrcaRootLayout.LAZY_COLUMN -> {
            val wrappedLinkClick: (String) -> Unit = { url ->
                if (url.startsWith("#")) {
                    val fragment = url.removePrefix("#")
                    val targetIndex = headingAnchorIndex[fragment]
                    if (targetIndex != null) {
                        scope.launch { listState.animateScrollToItem(targetIndex) }
                    } else {
                        onLinkClick(url)
                    }
                } else {
                    onLinkClick(url)
                }
            }
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(style.layout.blockSpacing),
                ) {
                items(
                    items = renderBlocks,
                    key = { item -> item.key },
                ) { item ->
                    val override = blockOverride[item.block::class]
                    if (override != null) {
                        override(item.block)
                    } else {
                        OrcaBlockNode(
                            block = item.block,
                            style = style,
                            onLinkClick = wrappedLinkClick,
                            securityPolicy = securityPolicy,
                            footnoteNumbers = footnoteNumbers,
                            sourceBlockKey = item.key,
                            activeFootnoteLabel = activeFootnoteLabel,
                            onFootnoteReferenceClick = { label, sourceBlockKey ->
                                onFootnoteReferenceClick(
                                    label = label,
                                    sourceBlockKey = sourceBlockKey,
                                    scrollToFootnotes = {
                                        val targetIndex = findFootnoteBlockIndex(label)
                                        if (targetIndex != null) {
                                            scope.launch {
                                                listState.animateScrollToItem(targetIndex)
                                            }
                                        }
                                    },
                                )
                            },
                            onFootnoteBackClick = { label ->
                                onFootnoteBackClick(
                                    label = label,
                                    scrollToSource = { sourceBlockKey ->
                                        val targetIndex = blockIndexByKey[sourceBlockKey]
                                        if (targetIndex != null) {
                                            scope.launch {
                                                listState.animateScrollToItem(targetIndex)
                                            }
                                        }
                                    },
                                )
                            },
                            imageContent = imageContent,
                            inlineImageContent = inlineImageContent,
                            blockMathContent = blockMathContent,
                            inlineMathContent = inlineMathContent,
                            inlineOverride = inlineOverride,
                        )
                    }
                }
                }
            }
        }

        OrcaRootLayout.COLUMN -> {
            val blockRequesters = remember(renderBlocks) {
                renderBlocks.associate { item -> item.key to BringIntoViewRequester() }
            }
            val wrappedLinkClickColumn: (String) -> Unit = { url ->
                if (url.startsWith("#")) {
                    val fragment = url.removePrefix("#")
                    val targetRb = renderBlocks.firstOrNull { rb ->
                        (rb.block as? OrcaBlock.Heading)?.id == fragment
                    }
                    val targetRequester = targetRb?.key?.let { blockRequesters[it] }
                    if (targetRequester != null) {
                        scope.launch { targetRequester.bringIntoView() }
                    } else {
                        onLinkClick(url)
                    }
                } else {
                    onLinkClick(url)
                }
            }

            SelectionContainer {
                Column(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(style.layout.blockSpacing),
                ) {
                    renderBlocks.forEach { item ->
                        val requester = blockRequesters[item.key]
                        val itemModifier = if (requester != null) {
                            Modifier.bringIntoViewRequester(requester)
                        } else {
                            Modifier
                        }

                        androidx.compose.foundation.layout.Box(modifier = itemModifier) {
                            val override = blockOverride[item.block::class]
                            if (override != null) {
                                override(item.block)
                            } else {
                                OrcaBlockNode(
                                    block = item.block,
                                    style = style,
                                    onLinkClick = wrappedLinkClickColumn,
                                    securityPolicy = securityPolicy,
                                    footnoteNumbers = footnoteNumbers,
                                    sourceBlockKey = item.key,
                                    activeFootnoteLabel = activeFootnoteLabel,
                                    onFootnoteReferenceClick = { label, sourceBlockKey ->
                                        onFootnoteReferenceClick(
                                            label = label,
                                            sourceBlockKey = sourceBlockKey,
                                            scrollToFootnotes = {
                                                val footnoteBlock = renderBlocks.firstOrNull { rb ->
                                                    val block = rb.block
                                                    block is OrcaBlock.Footnotes && block.definitions.any { it.label == label }
                                                } ?: renderBlocks.firstOrNull { rb -> rb.block is OrcaBlock.Footnotes }
                                                val targetRequester = footnoteBlock?.key?.let { blockRequesters[it] }
                                                if (targetRequester != null) {
                                                    scope.launch { targetRequester.bringIntoView() }
                                                }
                                            },
                                        )
                                    },
                                    onFootnoteBackClick = { label ->
                                        onFootnoteBackClick(
                                            label = label,
                                            scrollToSource = { sourceBlockKey ->
                                                val targetRequester = blockRequesters[sourceBlockKey]
                                                if (targetRequester != null) {
                                                    scope.launch { targetRequester.bringIntoView() }
                                                }
                                            },
                                        )
                                    },
                                    imageContent = imageContent,
                                    inlineImageContent = inlineImageContent,
                                    blockMathContent = blockMathContent,
                                    inlineMathContent = inlineMathContent,
                                    inlineOverride = inlineOverride,
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

internal data class OrcaRenderBlock(
    val key: String,
    val block: OrcaBlock,
)

internal fun buildRenderBlocks(blocks: List<OrcaBlock>): List<OrcaRenderBlock> {
    val seenKeys = mutableMapOf<String, Int>()
    return blocks.map { block ->
        val base = blockContentKey(block)
        val occurrence = seenKeys[base] ?: 0
        seenKeys[base] = occurrence + 1
        val key = if (occurrence == 0) base else "$base#$occurrence"
        OrcaRenderBlock(key = key, block = block)
    }
}

private fun blockContentKey(block: OrcaBlock): String {
    return when (block) {
        is OrcaBlock.Heading -> "H${block.level}:${inlineContentDigest(block.content)}"
        is OrcaBlock.Paragraph -> "P:${inlineContentDigest(block.content)}"
        is OrcaBlock.CodeBlock -> "Code:${block.language.orEmpty()}:${stableHash(block.code)}"
        is OrcaBlock.ListBlock -> {
            val firstItemDigest = block.items.firstOrNull()
                ?.blocks
                ?.firstOrNull()
                ?.let { firstBlock ->
                    when (firstBlock) {
                        is OrcaBlock.Paragraph -> inlineContentDigest(firstBlock.content)
                        is OrcaBlock.Heading -> inlineContentDigest(firstBlock.content)
                        else -> firstBlock::class.simpleName.orEmpty()
                    }
                }
                .orEmpty()
            "List:${if (block.ordered) "ol" else "ul"}:${block.items.size}:$firstItemDigest"
        }
        is OrcaBlock.Quote -> {
            val firstBlockDigest = block.blocks.firstOrNull()
                ?.let { first ->
                    when (first) {
                        is OrcaBlock.Paragraph -> inlineContentDigest(first.content)
                        is OrcaBlock.Heading -> inlineContentDigest(first.content)
                        else -> first::class.simpleName.orEmpty()
                    }
                }
                .orEmpty()
            "Quote:${block.blocks.size}:$firstBlockDigest"
        }
        is OrcaBlock.Table -> "Table:${block.header.size}x${block.rows.size}:${block.header.firstOrNull()?.content?.let { inlineContentDigest(it) }.orEmpty()}"
        is OrcaBlock.Math -> "Math:${stableHash(block.source)}"
        is OrcaBlock.Image -> "Img:${block.source.take(64)}"
        is OrcaBlock.ThematicBreak -> "HR"
        is OrcaBlock.Footnotes -> "FN:${block.definitions.size}:${block.definitions.firstOrNull()?.label.orEmpty()}"
        is OrcaBlock.HtmlBlock -> "Html:${stableHash(block.html)}"
        is OrcaBlock.Admonition -> "Adm:${block.type.name}:${block.blocks.size}"
        is OrcaBlock.DefinitionList -> "DL:${block.items.size}:${block.items.firstOrNull()?.let { inlineContentDigest(it.term) }.orEmpty()}"
        is OrcaBlock.Details -> "Det:${inlineContentDigest(block.summary)}:${block.blocks.size}"
    }
}

/**
 * FNV-1a 32-bit hash — distributes much better than [String.hashCode] for short
 * prefixes, dramatically reducing key collisions in the LazyColumn.
 *
 * Samples up to 256 leading characters and, for strings longer than 256 chars,
 * folds in characters from the tail as well. Combined with the length xor,
 * this virtually eliminates collisions for code blocks with identical imports.
 */
private fun stableHash(value: String): String {
    var hash = 0x811c9dc5.toInt()
    val limit = value.length.coerceAtMost(256)
    for (i in 0 until limit) {
        hash = hash xor value[i].code
        hash = hash * 0x01000193
    }
    // For long strings, fold in characters from the tail for better discrimination.
    if (value.length > 256) {
        val tailStart = (value.length - 64).coerceAtLeast(256)
        for (i in tailStart until value.length) {
            hash = hash xor value[i].code
            hash = hash * 0x01000193
        }
    }
    // Include length so that strings sharing a prefix but differing
    // in length still produce different hashes.
    hash = hash xor value.length
    return hash.toUInt().toString(36)
}

private fun inlineContentDigest(inlines: List<ru.wertik.orca.core.OrcaInline>): String {
    if (inlines.isEmpty()) return ""
    val text = buildString {
        for (inline in inlines) {
            appendInlineText(inline)
            if (length > 128) break
        }
    }
    return stableHash(text)
}

private fun StringBuilder.appendInlineText(inline: ru.wertik.orca.core.OrcaInline) {
    when (inline) {
        is ru.wertik.orca.core.OrcaInline.Text -> append(inline.text)
        is ru.wertik.orca.core.OrcaInline.Bold -> inline.content.forEach { appendInlineText(it) }
        is ru.wertik.orca.core.OrcaInline.Italic -> inline.content.forEach { appendInlineText(it) }
        is ru.wertik.orca.core.OrcaInline.Strikethrough -> inline.content.forEach { appendInlineText(it) }
        is ru.wertik.orca.core.OrcaInline.InlineCode -> append(inline.code)
        is ru.wertik.orca.core.OrcaInline.Math -> append("\$${inline.source}\$")
        is ru.wertik.orca.core.OrcaInline.Link -> inline.content.forEach { appendInlineText(it) }
        is ru.wertik.orca.core.OrcaInline.Image -> append(inline.alt.orEmpty())
        is ru.wertik.orca.core.OrcaInline.FootnoteReference -> append("[^${inline.label}]")
        is ru.wertik.orca.core.OrcaInline.HtmlInline -> append(inline.html)
        is ru.wertik.orca.core.OrcaInline.Superscript -> inline.content.forEach { appendInlineText(it) }
        is ru.wertik.orca.core.OrcaInline.Subscript -> inline.content.forEach { appendInlineText(it) }
        is ru.wertik.orca.core.OrcaInline.Highlight -> inline.content.forEach { appendInlineText(it) }
        is ru.wertik.orca.core.OrcaInline.Underline -> inline.content.forEach { appendInlineText(it) }
        is ru.wertik.orca.core.OrcaInline.Abbreviation -> append(inline.text)
    }
}

internal fun buildFootnoteNumbers(blocks: List<OrcaBlock>): Map<String, Int> {
    val numbers = linkedMapOf<String, Int>()
    var nextNumber = 1
    blocks.asSequence()
        .filterIsInstance<OrcaBlock.Footnotes>()
        .flatMap { footnotes -> footnotes.definitions.asSequence() }
        .forEach { definition ->
            if (definition.label !in numbers) {
                numbers[definition.label] = nextNumber
                nextNumber += 1
            }
        }
    return numbers
}
