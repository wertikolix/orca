package ru.wertik.orca.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaInline
import ru.wertik.orca.core.OrcaTableAlignment
import ru.wertik.orca.core.OrcaTableCell
import kotlin.reflect.KClass

@Composable
internal fun TableBlockNode(
    block: OrcaBlock.Table,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
    inlineImageContent: OrcaImageContent? = null,
    inlineMathContent: OrcaMathContent? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
) {
    val columnCount = maxOf(
        block.header.size,
        block.rows.maxOfOrNull { row -> row.size } ?: 0,
    )
    if (columnCount == 0) return
    val inlineMathPlaceholder = LocalOrcaInlineMathPlaceholder.current

    val contentLengths = remember(block, columnCount) {
        tableContentLengths(
            block = block,
            columnCount = columnCount,
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnWidths = remember(
            contentLengths,
            columnCount,
            style.table,
            maxWidth,
        ) {
            computeTableColumnWidths(
                columnCount = columnCount,
                contentLengths = contentLengths,
                tableStyle = style.table,
                availableWidth = maxWidth.takeIf { width -> width > 0.dp },
            )
        }

        ColumnWithHorizontalScroll(
            style = style,
            rowCount = block.rows.size + 1,
            columnCount = columnCount,
        ) {
            TableRowNode(
                cells = block.header,
                columnCount = columnCount,
                columnWidths = columnWidths,
                isHeader = true,
                style = style,
                onLinkClick = onLinkClick,
                securityPolicy = securityPolicy,
                footnoteNumbers = footnoteNumbers,
                sourceBlockKey = sourceBlockKey,
                onFootnoteReferenceClick = onFootnoteReferenceClick,
                inlineImageContent = inlineImageContent,
                inlineMathContent = inlineMathContent,
                inlineMathPlaceholder = inlineMathPlaceholder,
                inlineOverride = inlineOverride,
            )
            block.rows.forEachIndexed { index, row ->
                TableRowNode(
                    cells = row,
                    columnCount = columnCount,
                    columnWidths = columnWidths,
                    isHeader = false,
                    rowIndex = index,
                    style = style,
                    onLinkClick = onLinkClick,
                    securityPolicy = securityPolicy,
                    footnoteNumbers = footnoteNumbers,
                    sourceBlockKey = sourceBlockKey,
                    onFootnoteReferenceClick = onFootnoteReferenceClick,
                    inlineImageContent = inlineImageContent,
                    inlineMathContent = inlineMathContent,
                    inlineMathPlaceholder = inlineMathPlaceholder,
                    inlineOverride = inlineOverride,
                )
            }
        }
    }
}

@Composable
private fun ColumnWithHorizontalScroll(
    style: OrcaStyle,
    rowCount: Int,
    columnCount: Int,
    content: @Composable () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                collectionInfo = CollectionInfo(rowCount = rowCount, columnCount = columnCount)
            },
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .clip(style.table.containerShape)
                .border(style.table.borderWidth, style.table.outerBorderColor, style.table.containerShape),
        ) {
            content()
        }
        if (style.table.showScrollIndicator && scrollState.maxValue > 0) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = style.table.scrollIndicatorSpacing)
                    .height(style.table.scrollIndicatorHeight)
                    .drawBehind {
                        drawRect(color = style.table.scrollTrackColor)
                        val viewportWidth = size.width
                        val totalWidth = viewportWidth + scrollState.maxValue
                        val thumbWidth = (viewportWidth * viewportWidth / totalWidth)
                            .coerceAtLeast(style.table.scrollIndicatorMinWidth.toPx())
                            .coerceAtMost(viewportWidth)
                        val availableTravel = viewportWidth - thumbWidth
                        val progress = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                        drawRect(
                            color = style.table.scrollIndicatorColor,
                            topLeft = Offset(x = availableTravel * progress, y = 0f),
                            size = Size(width = thumbWidth, height = size.height),
                        )
                    },
            )
        }
    }
}

@Composable
private fun TableRowNode(
    cells: List<OrcaTableCell>,
    columnCount: Int,
    columnWidths: List<Dp>,
    isHeader: Boolean,
    rowIndex: Int = 0,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
    inlineImageContent: OrcaImageContent?,
    inlineMathContent: OrcaMathContent?,
    inlineMathPlaceholder: OrcaInlineMathPlaceholder?,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer>,
) {
    // Use rememberUpdatedState for callbacks so that the AnnotatedString
    // cache (keyed on cell data + style) doesn't invalidate every recomposition
    // due to a new lambda reference.
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val currentSecurityPolicy by rememberUpdatedState(securityPolicy)
    val currentOnFootnoteReferenceClick by rememberUpdatedState(onFootnoteReferenceClick)

    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
    ) {
        repeat(columnCount) { index ->
            val cell = cells.getOrNull(index)
            val text = remember(cell, style, footnoteNumbers, sourceBlockKey, inlineOverride) {
                if (cell == null) {
                    AnnotatedString("")
                } else {
                    buildInlineAnnotatedString(
                        inlines = cell.content,
                        style = style,
                        onLinkClick = currentOnLinkClick,
                        securityPolicy = currentSecurityPolicy,
                        footnoteNumbers = footnoteNumbers,
                        onFootnoteClick = { label -> currentOnFootnoteReferenceClick(label, sourceBlockKey) },
                        inlineOverride = inlineOverride,
                    )
                }
            }
            val inlineImages = remember(cell, style, securityPolicy, inlineImageContent) {
                buildInlineImageMap(
                    inlines = cell?.content.orEmpty(),
                    style = style,
                    securityPolicy = securityPolicy,
                    inlineImageContent = inlineImageContent,
                )
            }
            val inlineMath = remember(cell, inlineMathContent, inlineMathPlaceholder) {
                buildInlineMathMap(cell?.content.orEmpty(), inlineMathContent, inlineMathPlaceholder)
            }
            val align = tableCellAlignment(cell?.alignment)
            val cellBorderWidth = style.table.borderWidth
            val cellBorderColor = style.table.borderColor
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(columnWidths.getOrElse(index) { style.table.columnWidth })
                    .drawBehind {
                        val strokePx = cellBorderWidth.toPx()
                        // Draw top border
                        drawLine(
                            color = cellBorderColor,
                            start = Offset(0f, strokePx / 2),
                            end = Offset(size.width, strokePx / 2),
                            strokeWidth = strokePx,
                        )
                        // Draw start (left) border
                        drawLine(
                            color = cellBorderColor,
                            start = Offset(strokePx / 2, 0f),
                            end = Offset(strokePx / 2, size.height),
                            strokeWidth = strokePx,
                        )
                    }
                    .background(
                        when {
                            isHeader -> style.table.headerBackground
                            rowIndex % 2 == 0 -> style.table.rowBackground
                            else -> style.table.alternateRowBackground
                        },
                    )
                    .padding(style.table.cellPadding)
                    .semantics {
                        collectionItemInfo = CollectionItemInfo(
                            rowIndex = if (isHeader) 0 else rowIndex + 1,
                            rowSpan = 1,
                            columnIndex = index,
                            columnSpan = 1,
                        )
                        if (isHeader) heading()
                    },
            ) {
                Text(
                    text = text,
                    style = (if (isHeader) style.table.headerText else style.table.text).copy(textAlign = align),
                    inlineContent = inlineImages + inlineMath,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun tableCellAlignment(alignment: OrcaTableAlignment?): TextAlign {
    return when (alignment) {
        OrcaTableAlignment.LEFT -> TextAlign.Start
        OrcaTableAlignment.CENTER -> TextAlign.Center
        OrcaTableAlignment.RIGHT -> TextAlign.End
        null -> TextAlign.Start
    }
}

internal fun computeTableColumnWidths(
    columnCount: Int,
    contentLengths: List<Int>,
    tableStyle: OrcaTableStyle,
    availableWidth: Dp?,
): List<Dp> {
    if (columnCount <= 0) return emptyList()

    if (tableStyle.layoutMode == OrcaTableLayoutMode.FIXED) {
        return List(columnCount) { tableStyle.columnWidth }
    }

    val widths = (0 until columnCount)
        .map { index ->
            val contentLength = contentLengths.getOrElse(index) { 1 }.coerceAtLeast(1)
            val estimated = tableStyle.autoColumnCharacterWidth * contentLength.toFloat()
            estimated.coerceIn(
                minimumValue = tableStyle.minColumnWidth,
                maximumValue = tableStyle.maxColumnWidth,
            )
        }
        .toMutableList()

    if (availableWidth != null && tableStyle.fillAvailableWidth) {
        var remaining = availableWidth - widths.sumDp()
        var expandable = widths.indices
            .filter { index -> widths[index] < tableStyle.maxColumnWidth }
            .toMutableList()

        while (remaining > 0.dp && expandable.isNotEmpty()) {
            val chunk = remaining / expandable.size.toFloat()
            if (chunk <= 0.dp) break

            var consumed = 0.dp
            val stillExpandable = mutableListOf<Int>()
            expandable.forEach { index ->
                val capacity = tableStyle.maxColumnWidth - widths[index]
                val delta = minOf(chunk, capacity)
                widths[index] = widths[index] + delta
                consumed += delta
                if (widths[index] < tableStyle.maxColumnWidth) {
                    stillExpandable += index
                }
            }
            if (consumed <= 0.dp) break

            remaining -= consumed
            expandable = stillExpandable
        }
    }

    return widths
}

internal fun tableContentLengths(
    block: OrcaBlock.Table,
    columnCount: Int,
): List<Int> {
    val lengths = MutableList(columnCount) { 1 }
    fun applyRow(cells: List<OrcaTableCell>) {
        repeat(columnCount) { index ->
            val length = estimateCellTextLength(cells.getOrNull(index))
            if (length > lengths[index]) {
                lengths[index] = length
            }
        }
    }

    applyRow(block.header)
    block.rows.forEach(::applyRow)
    return lengths
}

private fun estimateCellTextLength(cell: OrcaTableCell?): Int {
    if (cell == null) return 1
    val length = cell.content.sumOf(::estimateInlineTextLength)
    return length.coerceAtLeast(1)
}

private fun estimateInlineTextLength(inline: OrcaInline): Int {
    return when (inline) {
        is OrcaInline.Text -> inline.text.length
        is OrcaInline.Bold -> inline.content.sumOf(::estimateInlineTextLength)
        is OrcaInline.Italic -> inline.content.sumOf(::estimateInlineTextLength)
        is OrcaInline.Strikethrough -> inline.content.sumOf(::estimateInlineTextLength)
        is OrcaInline.InlineCode -> inline.code.length
        is OrcaInline.Math -> inline.source.length + 2
        is OrcaInline.Link -> {
            val labelLength = inline.content.sumOf(::estimateInlineTextLength)
            if (labelLength > 0) labelLength else inline.destination.length
        }

        is OrcaInline.Image -> inline.alt?.length ?: inline.source.length
        is OrcaInline.FootnoteReference -> 4
        is OrcaInline.HtmlInline -> htmlInlineFallbackText(inline.html).length
        is OrcaInline.Superscript -> inline.content.sumOf(::estimateInlineTextLength)
        is OrcaInline.Subscript -> inline.content.sumOf(::estimateInlineTextLength)
        is OrcaInline.Highlight -> inline.content.sumOf(::estimateInlineTextLength)
        is OrcaInline.Underline -> inline.content.sumOf(::estimateInlineTextLength)
        is OrcaInline.Abbreviation -> inline.text.length
    }
}

private fun List<Dp>.sumDp(): Dp = fold(0.dp) { acc, value -> acc + value }
