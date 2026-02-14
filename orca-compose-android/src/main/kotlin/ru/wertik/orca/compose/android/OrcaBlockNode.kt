package ru.wertik.orca.compose.android

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaTaskState

@Composable
internal fun OrcaBlockNode(
    block: OrcaBlock,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
) {
    when (block) {
        is OrcaBlock.Heading -> HeadingNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
        )

        is OrcaBlock.Paragraph -> ParagraphNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
        )

        is OrcaBlock.ListBlock -> ListBlockNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
        )

        is OrcaBlock.Quote -> QuoteBlockNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
        )

        is OrcaBlock.CodeBlock -> CodeBlockNode(
            block = block,
            style = style,
        )

        is OrcaBlock.Image -> MarkdownImageNode(
            block = block,
            style = style,
        )

        is OrcaBlock.ThematicBreak -> ThematicBreakNode(style = style)

        is OrcaBlock.Table -> TableBlockNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
        )

        is OrcaBlock.Footnotes -> FootnotesNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
        )
    }
}

@Composable
private fun HeadingNode(
    block: OrcaBlock.Heading,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
) {
    val headingText = remember(block.content, style, onLinkClick, footnoteNumbers) {
        buildInlineAnnotatedString(
            inlines = block.content,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
        )
    }
    InlineTextNode(
        text = headingText,
        textStyle = style.heading(block.level),
    )
}

@Composable
private fun ParagraphNode(
    block: OrcaBlock.Paragraph,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
) {
    val paragraphText = remember(block.content, style, onLinkClick, footnoteNumbers) {
        buildInlineAnnotatedString(
            inlines = block.content,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
        )
    }
    InlineTextNode(
        text = paragraphText,
        textStyle = style.typography.paragraph,
    )
}

@Composable
private fun ListBlockNode(
    block: OrcaBlock.ListBlock,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
    ) {
        block.items.forEachIndexed { index, item ->
            Row {
                val marker = listMarkerText(
                    ordered = block.ordered,
                    startNumber = block.startNumber,
                    index = index,
                    taskState = item.taskState,
                )
                Text(
                    text = marker,
                    style = style.typography.paragraph,
                    modifier = Modifier.width(style.layout.listMarkerWidth),
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
                ) {
                    item.blocks.forEach { listItemBlock ->
                        OrcaBlockNode(
                            block = listItemBlock,
                            style = style,
                            onLinkClick = onLinkClick,
                            footnoteNumbers = footnoteNumbers,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteBlockNode(
    block: OrcaBlock.Quote,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .width(style.quote.stripeWidth)
                .fillMaxHeight()
                .background(style.quote.stripeColor),
        )
        Column(
            modifier = Modifier
                .padding(start = style.quote.spacing)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
        ) {
            block.blocks.forEach { nested ->
                OrcaBlockNode(
                    block = nested,
                    style = style,
                    onLinkClick = onLinkClick,
                    footnoteNumbers = footnoteNumbers,
                )
            }
        }
    }
}

@Composable
private fun FootnotesNode(
    block: OrcaBlock.Footnotes,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
    ) {
        block.definitions.forEach { definition ->
            Row {
                Text(
                    text = footnoteListMarkerText(definition.label, footnoteNumbers),
                    style = style.typography.paragraph,
                    modifier = Modifier.width(style.layout.listMarkerWidth),
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
                ) {
                    definition.blocks.forEach { blockItem ->
                        OrcaBlockNode(
                            block = blockItem,
                            style = style,
                            onLinkClick = onLinkClick,
                            footnoteNumbers = footnoteNumbers,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockNode(
    block: OrcaBlock.CodeBlock,
    style: OrcaStyle,
) {
    val languageLabel = remember(block.language) { codeLanguageLabel(block.language) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(style.code.shape)
            .background(style.code.background, style.code.shape)
            .border(style.code.borderWidth, style.code.borderColor, style.code.shape)
            .padding(style.code.padding),
        verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
    ) {
        if (languageLabel != null) {
            Text(
                text = languageLabel,
                style = style.code.languageLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(
                        color = style.code.languageLabelBackground,
                        shape = style.code.shape,
                    )
                    .padding(style.code.languageLabelPadding),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            Text(
                text = block.code,
                style = style.code.text,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun ThematicBreakNode(style: OrcaStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(style.thematicBreak.thickness)
            .background(style.thematicBreak.color),
    )
}

internal fun listMarkerText(
    ordered: Boolean,
    startNumber: Int,
    index: Int,
    taskState: OrcaTaskState?,
): String {
    return when (taskState) {
        OrcaTaskState.CHECKED -> "☑"
        OrcaTaskState.UNCHECKED -> "☐"
        null -> if (ordered) {
            "${startNumber + index}."
        } else {
            "•"
        }
    }
}

internal fun footnoteListMarkerText(
    label: String,
    footnoteNumbers: Map<String, Int>,
): String {
    val number = footnoteNumbers[label]
    return if (number != null) {
        "$number."
    } else {
        "[$label]"
    }
}

internal fun codeLanguageLabel(language: String?): String? {
    return language
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

@Composable
private fun InlineTextNode(
    text: AnnotatedString,
    textStyle: TextStyle,
) {
    Text(
        text = text,
        style = textStyle,
    )
}
