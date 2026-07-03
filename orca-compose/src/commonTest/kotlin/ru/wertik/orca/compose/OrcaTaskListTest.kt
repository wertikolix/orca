package ru.wertik.orca.compose

import androidx.compose.ui.text.style.TextDecoration
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaDefinitionListItem
import ru.wertik.orca.core.OrcaInline
import ru.wertik.orca.core.OrcaListItem
import ru.wertik.orca.core.OrcaTaskState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrcaTaskListTest {

    private fun taskItem(text: String, state: OrcaTaskState): OrcaListItem = OrcaListItem(
        blocks = listOf(OrcaBlock.Paragraph(content = listOf(OrcaInline.Text(text)))),
        taskState = state,
    )

    private fun plainItem(text: String): OrcaListItem = OrcaListItem(
        blocks = listOf(OrcaBlock.Paragraph(content = listOf(OrcaInline.Text(text)))),
    )

    @Test
    fun assignsDocumentOrderIndices() {
        val first = taskItem("one", OrcaTaskState.UNCHECKED)
        val second = taskItem("two", OrcaTaskState.CHECKED)
        val third = taskItem("three", OrcaTaskState.UNCHECKED)
        val blocks = listOf(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("intro"))),
            OrcaBlock.ListBlock(ordered = false, items = listOf(first, plainItem("plain"), second)),
            OrcaBlock.Quote(blocks = listOf(OrcaBlock.ListBlock(ordered = false, items = listOf(third)))),
        )

        val indices = buildTaskIndices(blocks)

        assertEquals(3, indices.size)
        assertEquals(0, indices[TaskItemKey(first)])
        assertEquals(1, indices[TaskItemKey(second)])
        assertEquals(2, indices[TaskItemKey(third)])
    }

    @Test
    fun identicalItemsKeepDistinctIndices() {
        val first = taskItem("same", OrcaTaskState.UNCHECKED)
        val second = taskItem("same", OrcaTaskState.UNCHECKED)
        val blocks = listOf(
            OrcaBlock.ListBlock(ordered = false, items = listOf(first, second)),
        )

        val indices = buildTaskIndices(blocks)

        assertEquals(0, indices[TaskItemKey(first)])
        assertEquals(1, indices[TaskItemKey(second)])
    }

    @Test
    fun nestedListsInsideTaskItemsAreCounted() {
        val nested = taskItem("nested", OrcaTaskState.UNCHECKED)
        val parent = OrcaListItem(
            blocks = listOf(
                OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("parent"))),
                OrcaBlock.ListBlock(ordered = false, items = listOf(nested)),
            ),
            taskState = OrcaTaskState.CHECKED,
        )
        val blocks = listOf(OrcaBlock.ListBlock(ordered = false, items = listOf(parent)))

        val indices = buildTaskIndices(blocks)

        assertEquals(0, indices[TaskItemKey(parent)])
        assertEquals(1, indices[TaskItemKey(nested)])
    }

    @Test
    fun definitionListsAndFootnotesAreTraversed() {
        val inDefinition = taskItem("def", OrcaTaskState.UNCHECKED)
        val blocks = listOf(
            OrcaBlock.DefinitionList(
                items = listOf(
                    OrcaDefinitionListItem(
                        term = listOf(OrcaInline.Text("term")),
                        definitions = listOf(
                            listOf(OrcaBlock.ListBlock(ordered = false, items = listOf(inDefinition))),
                        ),
                    ),
                ),
            ),
        )

        val indices = buildTaskIndices(blocks)

        assertEquals(0, indices[TaskItemKey(inDefinition)])
    }

    @Test
    fun nonTaskItemsAreNotIndexed() {
        val plain = plainItem("plain")
        val blocks = listOf(OrcaBlock.ListBlock(ordered = false, items = listOf(plain)))

        val indices = buildTaskIndices(blocks)

        assertTrue(indices.isEmpty())
        assertNull(indices[TaskItemKey(plain)])
    }

    @Test
    fun underlineInlineRendersWithUnderlineStyle() {
        val style = OrcaStyle()
        val text = buildInlineAnnotatedString(
            inlines = listOf(
                OrcaInline.Text("before "),
                OrcaInline.Underline(content = listOf(OrcaInline.Text("inserted"))),
            ),
            style = style,
            onLinkClick = {},
        )

        assertEquals("before inserted", text.text)
        val underlined = text.spanStyles.filter { it.item.textDecoration == TextDecoration.Underline }
        assertEquals(1, underlined.size)
        assertEquals("inserted", text.text.substring(underlined.single().start, underlined.single().end))
    }
}
