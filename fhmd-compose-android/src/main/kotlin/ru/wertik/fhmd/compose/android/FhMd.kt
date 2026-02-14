package ru.wertik.fhmd.compose.android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import ru.wertik.fhmd.core.CommonmarkFhMdParser
import ru.wertik.fhmd.core.FhMdBlock
import ru.wertik.fhmd.core.FhMdDocument
import ru.wertik.fhmd.core.FhMdParser
import ru.wertik.fhmd.core.FhMdTableAlignment
import ru.wertik.fhmd.core.FhMdTableCell

private val defaultParser: FhMdParser = CommonmarkFhMdParser()
private val defaultStyle: FhMdStyle = FhMdStyle()
private val noOpLinkClick: (String) -> Unit = {}

@Composable
fun FhMd(
    markdown: String,
    modifier: Modifier = Modifier,
    parser: FhMdParser = defaultParser,
    style: FhMdStyle = defaultStyle,
    onLinkClick: (String) -> Unit = noOpLinkClick,
) {
    val parserType = parser::class
    val document = remember(markdown, parserType) {
        parser.parse(markdown)
    }
    FhMd(
        document = document,
        modifier = modifier,
        style = style,
        onLinkClick = onLinkClick,
    )
}

@Composable
fun FhMd(
    document: FhMdDocument,
    modifier: Modifier = Modifier,
    style: FhMdStyle = defaultStyle,
    onLinkClick: (String) -> Unit = noOpLinkClick,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(style.blockSpacing),
    ) {
        document.blocks.forEach { block ->
            FhMdBlockNode(
                block = block,
                style = style,
                onLinkClick = onLinkClick,
            )
        }
    }
}

@Composable
private fun FhMdBlockNode(
    block: FhMdBlock,
    style: FhMdStyle,
    onLinkClick: (String) -> Unit,
) {
    when (block) {
        is FhMdBlock.Heading -> {
            val headingText = remember(block.content, style, onLinkClick) {
                buildInlineAnnotatedString(
                    inlines = block.content,
                    style = style,
                    onLinkClick = onLinkClick,
                )
            }
            InlineTextNode(
                text = headingText,
                textStyle = style.heading(block.level),
            )
        }

        is FhMdBlock.Paragraph -> {
            val paragraphText = remember(block.content, style, onLinkClick) {
                buildInlineAnnotatedString(
                    inlines = block.content,
                    style = style,
                    onLinkClick = onLinkClick,
                )
            }
            InlineTextNode(
                text = paragraphText,
                textStyle = style.paragraph,
            )
        }

        is FhMdBlock.ListBlock -> Column(
            verticalArrangement = Arrangement.spacedBy(style.nestedBlockSpacing),
        ) {
            block.items.forEachIndexed { index, item ->
                Row {
                    val marker = listMarkerText(
                        ordered = block.ordered,
                        startNumber = block.startNumber,
                        index = index,
                    )
                    Text(
                        text = marker,
                        style = style.paragraph,
                        modifier = Modifier.width(style.listMarkerWidth),
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(style.nestedBlockSpacing),
                    ) {
                        item.blocks.forEach { listItemBlock ->
                            FhMdBlockNode(
                                block = listItemBlock,
                                style = style,
                                onLinkClick = onLinkClick,
                            )
                        }
                    }
                }
            }
        }

        is FhMdBlock.Quote -> Row(
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(style.quoteStripeWidth)
                    .fillMaxHeight()
                    .background(style.quoteStripeColor),
            )
            Column(
                modifier = Modifier
                    .padding(start = style.quoteSpacing)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(style.nestedBlockSpacing),
            ) {
                block.blocks.forEach { nested ->
                    FhMdBlockNode(
                        block = nested,
                        style = style,
                        onLinkClick = onLinkClick,
                    )
                }
            }
        }

        is FhMdBlock.CodeBlock -> Text(
            text = block.code,
            style = style.codeBlock,
            modifier = Modifier
                .fillMaxWidth()
                .background(style.codeBlockBackground, style.codeBlockShape)
                .padding(style.codeBlockPadding),
        )

        is FhMdBlock.Image -> MarkdownImageNode(
            block = block,
            style = style,
        )

        is FhMdBlock.ThematicBreak -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(style.thematicBreakThickness)
                .background(style.thematicBreakColor),
        )

        is FhMdBlock.Table -> {
            val columnCount = maxOf(
                block.header.size,
                block.rows.maxOfOrNull { row -> row.size } ?: 0,
            )
            if (columnCount == 0) return

            Column(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .border(style.tableBorderWidth, style.tableBorderColor),
            ) {
                TableRowNode(
                    cells = block.header,
                    columnCount = columnCount,
                    isHeader = true,
                    style = style,
                    onLinkClick = onLinkClick,
                )
                block.rows.forEach { row ->
                    TableRowNode(
                        cells = row,
                        columnCount = columnCount,
                        isHeader = false,
                        style = style,
                        onLinkClick = onLinkClick,
                    )
                }
            }
        }
    }
}

internal fun listMarkerText(
    ordered: Boolean,
    startNumber: Int,
    index: Int,
): String {
    return if (ordered) {
        "${startNumber + index}."
    } else {
        "•"
    }
}

@Composable
private fun TableRowNode(
    cells: List<FhMdTableCell>,
    columnCount: Int,
    isHeader: Boolean,
    style: FhMdStyle,
    onLinkClick: (String) -> Unit,
) {
    Row {
        repeat(columnCount) { index ->
            val cell = cells.getOrNull(index)
            val text = remember(cell, style, onLinkClick) {
                if (cell == null) {
                    AnnotatedString("")
                } else {
                    buildInlineAnnotatedString(
                        inlines = cell.content,
                        style = style,
                        onLinkClick = onLinkClick,
                    )
                }
            }
            val align = tableCellAlignment(cell?.alignment)
            Box(
                modifier = Modifier
                    .width(style.tableColumnWidth)
                    .border(style.tableBorderWidth, style.tableBorderColor)
                    .background(if (isHeader) style.tableHeaderBackground else Color.Transparent)
                    .padding(style.tableCellPadding),
            ) {
                Text(
                    text = text,
                    style = if (isHeader) style.tableHeaderText else style.tableText,
                    textAlign = align,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MarkdownImageNode(
    block: FhMdBlock.Image,
    style: FhMdStyle,
) {
    val safeSource = remember(block.source) {
        block.source.takeIf(::isSafeImageSource)
    }
    if (safeSource == null) {
        Text(
            text = imageBlockFallbackText(block),
            style = style.paragraph,
        )
        return
    }

    AsyncImage(
        model = safeSource,
        contentDescription = block.alt,
        contentScale = style.imageContentScale,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = style.imageMaxHeight)
            .clip(style.imageShape)
            .background(style.imageBackground),
    )
}

private fun imageBlockFallbackText(block: FhMdBlock.Image): String {
    return block.alt?.takeIf { it.isNotBlank() } ?: block.source
}

private fun tableCellAlignment(alignment: FhMdTableAlignment?): TextAlign {
    return when (alignment) {
        FhMdTableAlignment.LEFT -> TextAlign.Start
        FhMdTableAlignment.CENTER -> TextAlign.Center
        FhMdTableAlignment.RIGHT -> TextAlign.End
        null -> TextAlign.Start
    }
}

@Composable
private fun InlineTextNode(
    text: AnnotatedString,
    textStyle: androidx.compose.ui.text.TextStyle,
) {
    Text(
        text = text,
        style = textStyle,
    )
}
