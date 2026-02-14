package ru.wertik.orca.compose.android

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.wertik.orca.core.CommonmarkOrcaParser
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaDocument
import ru.wertik.orca.core.OrcaParser

private const val PARSE_LOG_TAG = "Orca"

private val defaultParser: OrcaParser = CommonmarkOrcaParser()
private val defaultStyle: OrcaStyle = OrcaStyle()
private val noOpLinkClick: (String) -> Unit = {}

@Composable
fun Orca(
    markdown: String,
    modifier: Modifier = Modifier,
    parser: OrcaParser = defaultParser,
    style: OrcaStyle = defaultStyle,
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
        onLinkClick = onLinkClick,
    )
}

@Composable
fun Orca(
    document: OrcaDocument,
    modifier: Modifier = Modifier,
    style: OrcaStyle = defaultStyle,
    onLinkClick: (String) -> Unit = noOpLinkClick,
) {
    val renderBlocks = remember(document.blocks) {
        buildRenderBlocks(document.blocks)
    }

    LazyColumn(
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
            )
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
