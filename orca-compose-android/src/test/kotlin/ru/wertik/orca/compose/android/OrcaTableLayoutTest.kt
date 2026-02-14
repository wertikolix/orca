package ru.wertik.orca.compose.android

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaInline
import ru.wertik.orca.core.OrcaTableCell

class OrcaTableLayoutTest {

    @Test
    fun `fixed mode keeps configured column width`() {
        val widths = computeTableColumnWidths(
            columnCount = 3,
            contentLengths = listOf(1, 100, 5),
            tableStyle = OrcaTableStyle(
                layoutMode = OrcaTableLayoutMode.FIXED,
                columnWidth = 180.dp,
            ),
            availableWidth = 400.dp,
        )

        assertEquals(listOf(180.dp, 180.dp, 180.dp), widths)
    }

    @Test
    fun `auto mode assigns wider columns for longer content`() {
        val widths = computeTableColumnWidths(
            columnCount = 2,
            contentLengths = listOf(4, 24),
            tableStyle = OrcaTableStyle(
                layoutMode = OrcaTableLayoutMode.AUTO,
                minColumnWidth = 80.dp,
                maxColumnWidth = 260.dp,
                autoColumnCharacterWidth = 8.dp,
                fillAvailableWidth = false,
            ),
            availableWidth = null,
        )

        assertEquals(2, widths.size)
        assertTrue(widths[1] > widths[0])
    }

    @Test
    fun `auto mode can stretch to available width`() {
        val widths = computeTableColumnWidths(
            columnCount = 2,
            contentLengths = listOf(3, 3),
            tableStyle = OrcaTableStyle(
                layoutMode = OrcaTableLayoutMode.AUTO,
                minColumnWidth = 50.dp,
                maxColumnWidth = 200.dp,
                autoColumnCharacterWidth = 7.dp,
                fillAvailableWidth = true,
            ),
            availableWidth = 160.dp,
        )

        assertEquals(160.dp, widths[0] + widths[1])
    }

    @Test
    fun `table content length estimation accounts for inline nodes`() {
        val table = OrcaBlock.Table(
            header = listOf(
                OrcaTableCell(
                    content = listOf(OrcaInline.Text("short")),
                    alignment = null,
                ),
                OrcaTableCell(
                    content = listOf(OrcaInline.Text("name")),
                    alignment = null,
                ),
            ),
            rows = listOf(
                listOf(
                    OrcaTableCell(
                        content = listOf(
                            OrcaInline.Text("value "),
                            OrcaInline.FootnoteReference("note"),
                        ),
                        alignment = null,
                    ),
                    OrcaTableCell(
                        content = listOf(
                            OrcaInline.Link(
                                destination = "https://example.com",
                                content = listOf(OrcaInline.Text("longer-name")),
                            ),
                        ),
                        alignment = null,
                    ),
                ),
            ),
        )

        val lengths = tableContentLengths(table, columnCount = 2)

        assertEquals(2, lengths.size)
        assertTrue(lengths[0] >= "value [1]".length)
        assertTrue(lengths[1] >= "longer-name".length)
    }
}
