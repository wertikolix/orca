package ru.wertik.orca.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaDocument
import ru.wertik.orca.core.OrcaParseError
import ru.wertik.orca.core.OrcaParseDiagnostics
import ru.wertik.orca.core.OrcaParser

private const val PARSE_LOG_TAG = "Orca"
private const val DEFAULT_STREAMING_DEBOUNCE_MS = 80L

private val defaultStyle: OrcaStyle = OrcaStyle()
private val noOpLinkClick: (String) -> Unit = {}

enum class OrcaRootLayout {
    LAZY_COLUMN,
    COLUMN,
}

@Composable
fun Orca(
    markdown: String,
    modifier: Modifier = Modifier,
    parser: OrcaParser,
    parseCacheKey: Any? = null,
    style: OrcaStyle = defaultStyle,
    rootLayout: OrcaRootLayout = OrcaRootLayout.LAZY_COLUMN,
    securityPolicy: OrcaSecurityPolicy = OrcaSecurityPolicies.Default,
    onLinkClick: (String) -> Unit = noOpLinkClick,
    onParseDiagnostics: ((OrcaParseDiagnostics) -> Unit)? = null,
    streamingDebounceMs: Long = DEFAULT_STREAMING_DEBOUNCE_MS,
) {
    val parserKey = remember(parser) { parser.cacheKey() }
    val emptyDocument = remember { OrcaDocument(emptyList()) }
    var document by remember(parserKey) { mutableStateOf(emptyDocument) }

    LaunchedEffect(markdown, parserKey, parseCacheKey) {
        if (streamingDebounceMs > 0) {
            delay(streamingDebounceMs)
        }

        var parseError: Throwable? = null
        val parsedResult = try {
            withContext(Dispatchers.Default) {
                if (parseCacheKey == null) {
                    parser.parseWithDiagnostics(markdown)
                } else {
                    parser.parseCachedWithDiagnostics(
                        key = parseCacheKey,
                        input = markdown,
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

        val parsed = if (parsedResult == null || parsedResult.diagnostics.hasErrors) {
            if (parsedResult?.diagnostics?.hasErrors == true) {
                println("W/$PARSE_LOG_TAG: parser reported errors, using previous document")
            }
            document
        } else {
            parsedResult.document
        }
        onParseDiagnostics?.invoke(
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

    Orca(
        document = document,
        modifier = modifier,
        style = style,
        rootLayout = rootLayout,
        securityPolicy = securityPolicy,
        onLinkClick = onLinkClick,
    )
}

@Composable
fun Orca(
    document: OrcaDocument,
    modifier: Modifier = Modifier,
    style: OrcaStyle = defaultStyle,
    rootLayout: OrcaRootLayout = OrcaRootLayout.LAZY_COLUMN,
    securityPolicy: OrcaSecurityPolicy = OrcaSecurityPolicies.Default,
    onLinkClick: (String) -> Unit = noOpLinkClick,
) {
    val renderBlocks = remember(document.blocks) {
        buildRenderBlocks(document.blocks)
    }
    val footnoteNumbers = remember(document.blocks) {
        buildFootnoteNumbers(document.blocks)
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

    fun findFootnoteBlockIndex(label: String): Int? {
        for ((renderBlock, index) in footnoteBlockIndices) {
            val footnotes = renderBlock.block as OrcaBlock.Footnotes
            if (footnotes.definitions.any { it.label == label }) return index
        }
        return footnoteBlockIndices.firstOrNull()?.second
    }

    var activeFootnoteLabel by remember(document.blocks) { mutableStateOf<String?>(null) }
    val footnoteSourceStack = remember(document.blocks) {
        mutableStateMapOf<String, MutableList<String>>()
    }
    val scope = rememberCoroutineScope()

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

    when (rootLayout) {
        OrcaRootLayout.LAZY_COLUMN -> {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(style.layout.blockSpacing),
            ) {
                items(
                    items = renderBlocks,
                    key = { item -> item.key },
                ) { item ->
                    OrcaBlockNode(
                        block = item.block,
                        style = style,
                        onLinkClick = onLinkClick,
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
                    )
                }
            }
        }

        OrcaRootLayout.COLUMN -> {
            val blockRequesters = remember(renderBlocks) {
                renderBlocks.associate { item -> item.key to BringIntoViewRequester() }
            }

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
                        OrcaBlockNode(
                            block = item.block,
                            style = style,
                            onLinkClick = onLinkClick,
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
                        )
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
        is OrcaBlock.CodeBlock -> "Code:${block.language.orEmpty()}:${block.code.take(64).hashCode()}"
        is OrcaBlock.ListBlock -> "List:${if (block.ordered) "ol" else "ul"}:${block.items.size}"
        is OrcaBlock.Quote -> "Quote:${block.blocks.size}"
        is OrcaBlock.Table -> "Table:${block.header.size}x${block.rows.size}"
        is OrcaBlock.Image -> "Img:${block.source.take(64)}"
        is OrcaBlock.ThematicBreak -> "HR"
        is OrcaBlock.Footnotes -> "FN:${block.definitions.size}"
        is OrcaBlock.HtmlBlock -> "Html:${block.html.take(32).hashCode()}"
    }
}

private fun inlineContentDigest(inlines: List<ru.wertik.orca.core.OrcaInline>): String {
    if (inlines.isEmpty()) return ""
    val text = buildString {
        for (inline in inlines) {
            appendInlineText(inline)
            if (length > 64) break
        }
    }
    return text.take(64).hashCode().toUInt().toString(36)
}

private fun StringBuilder.appendInlineText(inline: ru.wertik.orca.core.OrcaInline) {
    when (inline) {
        is ru.wertik.orca.core.OrcaInline.Text -> append(inline.text)
        is ru.wertik.orca.core.OrcaInline.Bold -> inline.content.forEach { appendInlineText(it) }
        is ru.wertik.orca.core.OrcaInline.Italic -> inline.content.forEach { appendInlineText(it) }
        is ru.wertik.orca.core.OrcaInline.Strikethrough -> inline.content.forEach { appendInlineText(it) }
        is ru.wertik.orca.core.OrcaInline.InlineCode -> append(inline.code)
        is ru.wertik.orca.core.OrcaInline.Link -> inline.content.forEach { appendInlineText(it) }
        is ru.wertik.orca.core.OrcaInline.Image -> append(inline.alt.orEmpty())
        is ru.wertik.orca.core.OrcaInline.FootnoteReference -> append("[^${inline.label}]")
        is ru.wertik.orca.core.OrcaInline.HtmlInline -> append(inline.html)
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
