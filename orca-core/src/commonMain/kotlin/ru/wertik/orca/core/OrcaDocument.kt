package ru.wertik.orca.core

/** Parsed markdown document containing block-level content and optional front matter. */
data class OrcaDocument(
    /** Ordered list of top-level blocks in the document. */
    val blocks: List<OrcaBlock>,
    /** Front matter metadata, if present at the start of the source. Defaults to `null`. */
    val frontMatter: OrcaFrontMatter? = null,
)

/**
 * Front matter metadata extracted from the beginning of a markdown source.
 *
 * @property raw The unparsed front matter text (without delimiters).
 * @property entries Key-value pairs parsed from the front matter.
 */
sealed interface OrcaFrontMatter {
    val raw: String
    val entries: Map<String, String>

    /** YAML front matter delimited by `---`. */
    data class Yaml(
        override val raw: String,
        override val entries: Map<String, String>,
    ) : OrcaFrontMatter

    /** TOML front matter delimited by `+++`. */
    data class Toml(
        override val raw: String,
        override val entries: Map<String, String>,
    ) : OrcaFrontMatter
}

/** Block-level markdown element. Each variant maps to a specific markdown construct. */
sealed interface OrcaBlock {
    /**
     * ATX or setext heading.
     *
     * @property level Heading level (1–6).
     * @property content Inline elements forming the heading text.
     */
    data class Heading(
        val level: Int,
        val content: List<OrcaInline>,
    ) : OrcaBlock

    /** Paragraph containing inline elements. */
    data class Paragraph(
        val content: List<OrcaInline>,
    ) : OrcaBlock

    /**
     * Ordered or unordered list.
     *
     * @property ordered `true` for ordered (numbered) lists.
     * @property items List entries, possibly containing nested blocks.
     * @property startNumber Starting number for ordered lists. Defaults to `1`.
     */
    data class ListBlock(
        val ordered: Boolean,
        val items: List<OrcaListItem>,
        val startNumber: Int = 1,
    ) : OrcaBlock

    /** Block quote containing nested block-level content. */
    data class Quote(
        val blocks: List<OrcaBlock>,
    ) : OrcaBlock

    /**
     * Admonition (callout) block such as NOTE, TIP, or WARNING.
     *
     * @property type Admonition severity/category.
     * @property title Optional custom title; `null` uses the default for [type].
     * @property blocks Nested block content inside the admonition.
     */
    data class Admonition(
        val type: OrcaAdmonitionType,
        val title: String?,
        val blocks: List<OrcaBlock>,
    ) : OrcaBlock

    /**
     * Fenced or indented code block.
     *
     * @property code The raw code content.
     * @property language Info-string language hint, or `null` if unspecified.
     */
    data class CodeBlock(
        val code: String,
        val language: String?,
    ) : OrcaBlock

    /**
     * Block-level image (standalone image not wrapped in a paragraph).
     *
     * @property source Image URL or path.
     * @property alt Alternative text, or `null`.
     * @property title Optional title attribute.
     */
    data class Image(
        val source: String,
        val alt: String?,
        val title: String?,
    ) : OrcaBlock

    /** Horizontal rule / thematic break (`---`, `***`, or `___`). */
    data object ThematicBreak : OrcaBlock

    /**
     * Markdown table.
     *
     * @property header Header row cells.
     * @property rows Body rows, each a list of [OrcaTableCell].
     */
    data class Table(
        val header: List<OrcaTableCell>,
        val rows: List<List<OrcaTableCell>>,
    ) : OrcaBlock

    /** Collected footnote definitions referenced elsewhere in the document. */
    data class Footnotes(
        val definitions: List<OrcaFootnoteDefinition>,
    ) : OrcaBlock

    /** Raw HTML block passed through verbatim. */
    data class HtmlBlock(
        val html: String,
    ) : OrcaBlock

    /**
     * Definition list containing term-definition pairs.
     *
     * Syntax (PHP Markdown Extra / Pandoc):
     * ```
     * Term
     * : Definition one
     * : Definition two
     * ```
     *
     * @property items Ordered list of term-definition entries.
     */
    data class DefinitionList(
        val items: List<OrcaDefinitionListItem>,
    ) : OrcaBlock
}

/**
 * Single item in an [OrcaBlock.ListBlock].
 *
 * @property blocks Block-level content of this list item.
 * @property taskState Checkbox state for task lists, or `null` for regular items.
 */
data class OrcaListItem(
    val blocks: List<OrcaBlock>,
    val taskState: OrcaTaskState? = null,
)

/** Checkbox state for task-list items. */
enum class OrcaTaskState {
    /** `[x]` — completed task. */
    CHECKED,
    /** `[ ]` — incomplete task. */
    UNCHECKED,
}

/**
 * Single cell in a markdown table.
 *
 * @property content Inline elements inside the cell.
 * @property alignment Column alignment for this cell, or `null` if unspecified.
 */
data class OrcaTableCell(
    val content: List<OrcaInline>,
    val alignment: OrcaTableAlignment?,
)

/** Column alignment in a markdown table. */
enum class OrcaTableAlignment {
    LEFT,
    CENTER,
    RIGHT,
}

/** Admonition category/severity used in [OrcaBlock.Admonition]. */
enum class OrcaAdmonitionType {
    NOTE,
    TIP,
    IMPORTANT,
    WARNING,
    CAUTION,
}

/**
 * Footnote definition mapping a label to its block content.
 *
 * @property label Reference label (e.g. `"1"` for `[^1]`).
 * @property blocks Block-level content of the footnote.
 */
data class OrcaFootnoteDefinition(
    val label: String,
    val blocks: List<OrcaBlock>,
)

/**
 * Single entry in a [OrcaBlock.DefinitionList].
 *
 * @property term Inline elements forming the term being defined.
 * @property definitions One or more definitions, each containing block-level content.
 */
data class OrcaDefinitionListItem(
    val term: List<OrcaInline>,
    val definitions: List<List<OrcaBlock>>,
)

/** Inline-level markdown element. Each variant maps to a specific inline construct. */
sealed interface OrcaInline {
    /** Plain text span. */
    data class Text(
        val text: String,
    ) : OrcaInline

    /** Bold (**strong**) emphasis wrapping nested inlines. */
    data class Bold(
        val content: List<OrcaInline>,
    ) : OrcaInline

    /** Italic (*emphasis*) wrapping nested inlines. */
    data class Italic(
        val content: List<OrcaInline>,
    ) : OrcaInline

    /** Strikethrough (~~deleted~~) wrapping nested inlines. */
    data class Strikethrough(
        val content: List<OrcaInline>,
    ) : OrcaInline

    /** Superscript (^text^) wrapping nested inlines. */
    data class Superscript(
        val content: List<OrcaInline>,
    ) : OrcaInline

    /** Subscript (~text~) wrapping nested inlines. */
    data class Subscript(
        val content: List<OrcaInline>,
    ) : OrcaInline

    /** Inline code span (`` `code` ``). */
    data class InlineCode(
        val code: String,
    ) : OrcaInline

    /**
     * Hyperlink.
     *
     * @property destination URL or anchor target.
     * @property content Inline elements forming the link text.
     * @property title Optional title attribute. Defaults to `null`.
     */
    data class Link(
        val destination: String,
        val content: List<OrcaInline>,
        val title: String? = null,
    ) : OrcaInline

    /**
     * Inline image.
     *
     * @property source Image URL or path.
     * @property alt Alternative text, or `null`.
     * @property title Optional title attribute.
     */
    data class Image(
        val source: String,
        val alt: String?,
        val title: String?,
    ) : OrcaInline

    /** Reference to a footnote definition (e.g. `[^label]`). */
    data class FootnoteReference(
        val label: String,
    ) : OrcaInline

    /** Raw inline HTML passed through verbatim. */
    data class HtmlInline(
        val html: String,
    ) : OrcaInline
}
