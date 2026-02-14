package ru.wertik.orca.compose.android

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaTaskState

@Composable
internal fun OrcaBlockNode(
    block: OrcaBlock,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
) {
    when (block) {
        is OrcaBlock.Heading -> HeadingNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
        )

        is OrcaBlock.Paragraph -> ParagraphNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
        )

        is OrcaBlock.ListBlock -> ListBlockNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
        )

        is OrcaBlock.Quote -> QuoteBlockNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
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
        )
    }
}

@Composable
private fun HeadingNode(
    block: OrcaBlock.Heading,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
) {
    val headingText = remember(block.content, style, onLinkClick) {
        buildInlineAnnotatedString(
            inlines = block.content,
            style = style,
            onLinkClick = onLinkClick,
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
) {
    val paragraphText = remember(block.content, style, onLinkClick) {
        buildInlineAnnotatedString(
            inlines = block.content,
            style = style,
            onLinkClick = onLinkClick,
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
                )
            }
        }
    }
}

@Composable
private fun CodeBlockNode(
    block: OrcaBlock.CodeBlock,
    style: OrcaStyle,
) {
    Text(
        text = block.code,
        style = style.code.text,
        modifier = Modifier
            .fillMaxWidth()
            .background(style.code.background, style.code.shape)
            .padding(style.code.padding),
    )
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
