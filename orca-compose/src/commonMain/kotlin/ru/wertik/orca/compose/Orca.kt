package ru.wertik.orca.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaDocument
import ru.wertik.orca.core.OrcaParseError
import ru.wertik.orca.core.OrcaParseDiagnostics
import ru.wertik.orca.core.OrcaParser

private const val PARSE_LOG_TAG = "Orca"

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
) {
    val parserKey = remember(parser) { parser.cacheKey() }
    val emptyDocument = remember { OrcaDocument(emptyList()) }
    var latestDocument by remember(parserKey) { mutableStateOf(emptyDocument) }

    val document by produceState(
        initialValue = latestDocument,
        markdown,
        parserKey,
        parseCacheKey,
    ) {
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
            latestDocument
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

        latestDocument = parsed
        value = parsed
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
    return blocks.mapIndexed { index, block ->
        OrcaRenderBlock(
            key = "${block::class.simpleName}:$index",
            block = block,
        )
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
