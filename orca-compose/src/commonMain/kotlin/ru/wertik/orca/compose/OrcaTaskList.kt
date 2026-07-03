package ru.wertik.orca.compose

import androidx.compose.runtime.staticCompositionLocalOf
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaListItem

/**
 * Callback invoked when the user toggles a task-list checkbox.
 *
 * @param taskIndex zero-based index of the task item in document order
 * (counting every `- [ ]` / `- [x]` item, including nested ones).
 * @param checked the requested new state — `true` when the user checks the box.
 */
typealias OrcaTaskToggle = (taskIndex: Int, checked: Boolean) -> Unit

/**
 * Internal lookup that maps rendered task-list items to their document-order index.
 *
 * Orca renders checkboxes statelessly: toggling never mutates the document. The host
 * application receives the tap through [onTaskToggle] and is expected to update the
 * Markdown source (or its own model) and recompose.
 */
internal class OrcaTaskListInteraction(
    private val indices: Map<TaskItemKey, Int>,
    val onTaskToggle: OrcaTaskToggle,
) {
    fun indexOf(item: OrcaListItem): Int? = indices[TaskItemKey(item)]
}

/**
 * Identity-based key so that two structurally equal task items occurring in
 * different places in the document keep distinct indices.
 */
internal class TaskItemKey(private val item: OrcaListItem) {
    override fun equals(other: Any?): Boolean = other is TaskItemKey && other.item === item
    override fun hashCode(): Int = item.hashCode()
}

/**
 * Pre-order traversal assigning a document-order index to every task-list item.
 * The traversal order matches the render order, so indices are stable regardless
 * of lazy composition order.
 */
internal fun buildTaskIndices(blocks: List<OrcaBlock>): Map<TaskItemKey, Int> {
    val result = mutableMapOf<TaskItemKey, Int>()
    var next = 0

    fun walkBlocks(blocks: List<OrcaBlock>) {
        for (block in blocks) {
            when (block) {
                is OrcaBlock.ListBlock -> block.items.forEach { item ->
                    if (item.taskState != null) {
                        result[TaskItemKey(item)] = next
                        next += 1
                    }
                    walkBlocks(item.blocks)
                }

                is OrcaBlock.Quote -> walkBlocks(block.blocks)
                is OrcaBlock.Admonition -> walkBlocks(block.blocks)
                is OrcaBlock.Details -> walkBlocks(block.blocks)
                is OrcaBlock.DefinitionList -> block.items.forEach { item ->
                    item.definitions.forEach { definition -> walkBlocks(definition) }
                }

                is OrcaBlock.Footnotes -> block.definitions.forEach { definition ->
                    walkBlocks(definition.blocks)
                }

                else -> Unit
            }
        }
    }

    walkBlocks(blocks)
    return result
}

internal val LocalOrcaTaskInteraction = staticCompositionLocalOf<OrcaTaskListInteraction?> { null }
