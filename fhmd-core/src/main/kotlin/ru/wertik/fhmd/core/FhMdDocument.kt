package ru.wertik.fhmd.core

data class FhMdDocument(
    val blocks: List<FhMdBlock>,
)

sealed interface FhMdBlock {
    data class Heading(
        val level: Int,
        val content: List<FhMdInline>,
    ) : FhMdBlock

    data class Paragraph(
        val content: List<FhMdInline>,
    ) : FhMdBlock

    data class ListBlock(
        val ordered: Boolean,
        val items: List<FhMdListItem>,
        val startNumber: Int = 1,
    ) : FhMdBlock

    data class Quote(
        val blocks: List<FhMdBlock>,
    ) : FhMdBlock

    data class CodeBlock(
        val code: String,
        val language: String?,
    ) : FhMdBlock

    data class Image(
        val source: String,
        val alt: String?,
        val title: String?,
    ) : FhMdBlock

    data object ThematicBreak : FhMdBlock

    data class Table(
        val header: List<FhMdTableCell>,
        val rows: List<List<FhMdTableCell>>,
    ) : FhMdBlock
}

data class FhMdListItem(
    val blocks: List<FhMdBlock>,
    val taskState: FhMdTaskState? = null,
)

enum class FhMdTaskState {
    CHECKED,
    UNCHECKED,
}

data class FhMdTableCell(
    val content: List<FhMdInline>,
    val alignment: FhMdTableAlignment?,
)

enum class FhMdTableAlignment {
    LEFT,
    CENTER,
    RIGHT,
}

sealed interface FhMdInline {
    data class Text(
        val text: String,
    ) : FhMdInline

    data class Bold(
        val content: List<FhMdInline>,
    ) : FhMdInline

    data class Italic(
        val content: List<FhMdInline>,
    ) : FhMdInline

    data class Strikethrough(
        val content: List<FhMdInline>,
    ) : FhMdInline

    data class InlineCode(
        val code: String,
    ) : FhMdInline

    data class Link(
        val destination: String,
        val content: List<FhMdInline>,
    ) : FhMdInline

    data class Image(
        val source: String,
        val alt: String?,
        val title: String?,
    ) : FhMdInline
}
