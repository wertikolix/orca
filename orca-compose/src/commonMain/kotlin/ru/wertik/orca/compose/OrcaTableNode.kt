package ru.wertik.orca.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaInline
import ru.wertik.orca.core.OrcaTableAlignment
import ru.wertik.orca.core.OrcaTableCell

@Composable
internal fun TableBlockNode(
    block: OrcaBlock.Table,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
) {
    val columnCount = maxOf(
        block.header.size,
        block.rows.maxOfOrNull { row -> row.size } ?: 0,
    )
    if (columnCount == 0) return

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

        ColumnWithHorizontalScroll(style = style) {
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
            )
            block.rows.forEach { row ->
                TableRowNode(
                    cells = row,
                    columnCount = columnCount,
                    columnWidths = columnWidths,
                    isHeader = false,
                    style = style,
                    onLinkClick = onLinkClick,
                    securityPolicy = securityPolicy,
                    footnoteNumbers = footnoteNumbers,
                    sourceBlockKey = sourceBlockKey,
                    onFootnoteReferenceClick = onFootnoteReferenceClick,
                )
            }
        }
    }
}

@Composable
private fun ColumnWithHorizontalScroll(
    style: OrcaStyle,
    content: @Composable () -> Unit,
) {
    val borderWidth = style.table.borderWidth
    val borderColor = style.table.borderColor
    Column(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .drawBehind {
                val strokePx = borderWidth.toPx()
                // Draw bottom border
                drawLine(
                    color = borderColor,
                    start = Offset(0f, size.height - strokePx / 2),
                    end = Offset(size.width, size.height - strokePx / 2),
                    strokeWidth = strokePx,
                )
                // Draw end (right) border
                drawLine(
                    color = borderColor,
                    start = Offset(size.width - strokePx / 2, 0f),
                    end = Offset(size.width - strokePx / 2, size.height),
                    strokeWidth = strokePx,
                )
            },
    ) {
        content()
    }
}

@Composable
private fun TableRowNode(
    cells: List<OrcaTableCell>,
    columnCount: Int,
    columnWidths: List<Dp>,
    isHeader: Boolean,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
    ) {
        repeat(columnCount) { index ->
            val cell = cells.getOrNull(index)
            val text = remember(
                cell,
                style,
                onLinkClick,
                securityPolicy,
                footnoteNumbers,
                sourceBlockKey,
                onFootnoteReferenceClick,
            ) {
                if (cell == null) {
                    AnnotatedString("")
                } else {
                    buildInlineAnnotatedString(
                        inlines = cell.content,
                        style = style,
                        onLinkClick = onLinkClick,
                        securityPolicy = securityPolicy,
                        footnoteNumbers = footnoteNumbers,
                        onFootnoteClick = { label -> onFootnoteReferenceClick(label, sourceBlockKey) },
                    )
                }
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
                    .background(if (isHeader) style.table.headerBackground else Color.Transparent)
                    .padding(style.table.cellPadding),
            ) {
                Text(
                    text = text,
                    style = if (isHeader) style.table.headerText else style.table.text,
                    textAlign = align,
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
        is OrcaInline.Link -> {
            val labelLength = inline.content.sumOf(::estimateInlineTextLength)
            if (labelLength > 0) labelLength else inline.destination.length
        }

        is OrcaInline.Image -> inline.alt?.length ?: inline.source.length
        is OrcaInline.FootnoteReference -> 4
        is OrcaInline.HtmlInline -> htmlInlineFallbackText(inline.html).length
        is OrcaInline.Superscript -> inline.content.sumOf(::estimateInlineTextLength)
        is OrcaInline.Subscript -> inline.content.sumOf(::estimateInlineTextLength)
    }
}

private fun List<Dp>.sumDp(): Dp = fold(0.dp) { acc, value -> acc + value }
