package ru.wertik.orca.compose.android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaTableAlignment
import ru.wertik.orca.core.OrcaTableCell

@Composable
internal fun TableBlockNode(
    block: OrcaBlock.Table,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
) {
    val columnCount = maxOf(
        block.header.size,
        block.rows.maxOfOrNull { row -> row.size } ?: 0,
    )
    if (columnCount == 0) return

    ColumnWithHorizontalScroll(
        style = style,
    ) {
        TableRowNode(
            cells = block.header,
            columnCount = columnCount,
            isHeader = true,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
        )
        block.rows.forEach { row ->
            TableRowNode(
                cells = row,
                columnCount = columnCount,
                isHeader = false,
                style = style,
                onLinkClick = onLinkClick,
                footnoteNumbers = footnoteNumbers,
            )
        }
    }
}

@Composable
private fun ColumnWithHorizontalScroll(
    style: OrcaStyle,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .border(style.table.borderWidth, style.table.borderColor),
    ) {
        content()
    }
}

@Composable
private fun TableRowNode(
    cells: List<OrcaTableCell>,
    columnCount: Int,
    isHeader: Boolean,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
    ) {
        repeat(columnCount) { index ->
            val cell = cells.getOrNull(index)
            val text = remember(cell, style, onLinkClick, footnoteNumbers) {
                if (cell == null) {
                    AnnotatedString("")
                } else {
                    buildInlineAnnotatedString(
                        inlines = cell.content,
                        style = style,
                        onLinkClick = onLinkClick,
                        footnoteNumbers = footnoteNumbers,
                    )
                }
            }
            val align = tableCellAlignment(cell?.alignment)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(style.table.columnWidth)
                    .border(style.table.borderWidth, style.table.borderColor)
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
