package ru.wertik.orca.core

data class OrcaDocument(
    val blocks: List<OrcaBlock>,
    val frontMatter: OrcaFrontMatter? = null,
)

sealed interface OrcaFrontMatter {
    val raw: String
    val entries: Map<String, String>

    data class Yaml(
        override val raw: String,
        override val entries: Map<String, String>,
    ) : OrcaFrontMatter

    data class Toml(
        override val raw: String,
        override val entries: Map<String, String>,
    ) : OrcaFrontMatter
}

sealed interface OrcaBlock {
    data class Heading(
        val level: Int,
        val content: List<OrcaInline>,
    ) : OrcaBlock

    data class Paragraph(
        val content: List<OrcaInline>,
    ) : OrcaBlock

    data class ListBlock(
        val ordered: Boolean,
        val items: List<OrcaListItem>,
        val startNumber: Int = 1,
    ) : OrcaBlock

    data class Quote(
        val blocks: List<OrcaBlock>,
    ) : OrcaBlock

    data class CodeBlock(
        val code: String,
        val language: String?,
    ) : OrcaBlock

    data class Image(
        val source: String,
        val alt: String?,
        val title: String?,
    ) : OrcaBlock

    data object ThematicBreak : OrcaBlock

    data class Table(
        val header: List<OrcaTableCell>,
        val rows: List<List<OrcaTableCell>>,
    ) : OrcaBlock

    data class Footnotes(
        val definitions: List<OrcaFootnoteDefinition>,
    ) : OrcaBlock

    data class HtmlBlock(
        val html: String,
    ) : OrcaBlock
}

data class OrcaListItem(
    val blocks: List<OrcaBlock>,
    val taskState: OrcaTaskState? = null,
)

enum class OrcaTaskState {
    CHECKED,
    UNCHECKED,
}

data class OrcaTableCell(
    val content: List<OrcaInline>,
    val alignment: OrcaTableAlignment?,
)

enum class OrcaTableAlignment {
    LEFT,
    CENTER,
    RIGHT,
}

data class OrcaFootnoteDefinition(
    val label: String,
    val blocks: List<OrcaBlock>,
)

sealed interface OrcaInline {
    data class Text(
        val text: String,
    ) : OrcaInline

    data class Bold(
        val content: List<OrcaInline>,
    ) : OrcaInline

    data class Italic(
        val content: List<OrcaInline>,
    ) : OrcaInline

    data class Strikethrough(
        val content: List<OrcaInline>,
    ) : OrcaInline

    data class InlineCode(
        val code: String,
    ) : OrcaInline

    data class Link(
        val destination: String,
        val content: List<OrcaInline>,
    ) : OrcaInline

    data class Image(
        val source: String,
        val alt: String?,
        val title: String?,
    ) : OrcaInline

    data class FootnoteReference(
        val label: String,
    ) : OrcaInline

    data class HtmlInline(
        val html: String,
    ) : OrcaInline
}
