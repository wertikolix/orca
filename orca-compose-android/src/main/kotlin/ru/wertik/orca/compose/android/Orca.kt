package ru.wertik.orca.compose.android

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
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
import ru.wertik.orca.core.CommonmarkOrcaParser
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaDocument
import ru.wertik.orca.core.OrcaParser

private const val PARSE_LOG_TAG = "Orca"

private val defaultParser: OrcaParser = CommonmarkOrcaParser()
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
    parser: OrcaParser = defaultParser,
    style: OrcaStyle = defaultStyle,
    rootLayout: OrcaRootLayout = OrcaRootLayout.LAZY_COLUMN,
    onLinkClick: (String) -> Unit = noOpLinkClick,
) {
    val parserKey = remember(parser) { parser.cacheKey() }
    val emptyDocument = remember { OrcaDocument(emptyList()) }
    var latestDocument by remember(parserKey) { mutableStateOf(emptyDocument) }

    val document by produceState(
        initialValue = latestDocument,
        markdown,
        parserKey,
    ) {
        val parsed = try {
            withContext(Dispatchers.Default) {
                parser.parse(markdown)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Log.w(PARSE_LOG_TAG, "failed to parse markdown, using previous document", error)
            latestDocument
        }

        latestDocument = parsed
        value = parsed
    }

    Orca(
        document = document,
        modifier = modifier,
        style = style,
        rootLayout = rootLayout,
        onLinkClick = onLinkClick,
    )
}

@Composable
fun Orca(
    document: OrcaDocument,
    modifier: Modifier = Modifier,
    style: OrcaStyle = defaultStyle,
    rootLayout: OrcaRootLayout = OrcaRootLayout.LAZY_COLUMN,
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
    val footnoteBlockIndex = remember(renderBlocks, blockIndexByKey) {
        val footnoteBlockKey = renderBlocks
            .firstOrNull { renderBlock -> renderBlock.block is OrcaBlock.Footnotes }
            ?.key
        footnoteBlockKey?.let { key -> blockIndexByKey[key] }
    }

    var activeFootnoteLabel by remember(document.blocks) { mutableStateOf<String?>(null) }
    val referenceBlockByFootnote = remember(document.blocks) {
        mutableStateMapOf<String, String>()
    }
    val scope = rememberCoroutineScope()

    fun onFootnoteReferenceClick(label: String, sourceBlockKey: String, scrollToFootnotes: (() -> Unit)?) {
        referenceBlockByFootnote[label] = sourceBlockKey
        activeFootnoteLabel = label
        scrollToFootnotes?.invoke()
    }

    fun onFootnoteBackClick(label: String, scrollToSource: ((String) -> Unit)?) {
        val sourceBlockKey = referenceBlockByFootnote[label] ?: return
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
                        footnoteNumbers = footnoteNumbers,
                        sourceBlockKey = item.key,
                        activeFootnoteLabel = activeFootnoteLabel,
                        onFootnoteReferenceClick = { label, sourceBlockKey ->
                            onFootnoteReferenceClick(
                                label = label,
                                sourceBlockKey = sourceBlockKey,
                                scrollToFootnotes = {
                                    val targetIndex = footnoteBlockIndex
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
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(style.layout.blockSpacing),
            ) {
                renderBlocks.forEach { item ->
                    OrcaBlockNode(
                        block = item.block,
                        style = style,
                        onLinkClick = onLinkClick,
                        footnoteNumbers = footnoteNumbers,
                        sourceBlockKey = item.key,
                        activeFootnoteLabel = activeFootnoteLabel,
                        onFootnoteReferenceClick = { label, sourceBlockKey ->
                            onFootnoteReferenceClick(
                                label = label,
                                sourceBlockKey = sourceBlockKey,
                                scrollToFootnotes = null,
                            )
                        },
                        onFootnoteBackClick = { label ->
                            onFootnoteBackClick(
                                label = label,
                                scrollToSource = null,
                            )
                        },
                    )
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
    val seenByHash = mutableMapOf<Int, Int>()
    return blocks.map { block ->
        val hash = block.hashCode()
        val occurrence = seenByHash.getOrDefault(hash, 0)
        seenByHash[hash] = occurrence + 1
        OrcaRenderBlock(
            key = "${block::class.simpleName}:$hash:$occurrence",
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
            if (numbers.putIfAbsent(definition.label, nextNumber) == null) {
                nextNumber += 1
            }
        }
    return numbers
}
