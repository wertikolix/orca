package ru.wertik.orca.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.wertik.orca.core.OrcaAdmonitionType
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaTaskState

@Composable
internal fun OrcaBlockNode(
    block: OrcaBlock,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    activeFootnoteLabel: String?,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
    onFootnoteBackClick: (label: String) -> Unit,
) {
    when (block) {
        is OrcaBlock.Heading -> HeadingNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            onFootnoteReferenceClick = onFootnoteReferenceClick,
            sourceBlockKey = sourceBlockKey,
        )

        is OrcaBlock.Paragraph -> ParagraphNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            onFootnoteReferenceClick = onFootnoteReferenceClick,
            sourceBlockKey = sourceBlockKey,
        )

        is OrcaBlock.ListBlock -> ListBlockNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            sourceBlockKey = sourceBlockKey,
            activeFootnoteLabel = activeFootnoteLabel,
            onFootnoteReferenceClick = onFootnoteReferenceClick,
            onFootnoteBackClick = onFootnoteBackClick,
        )

        is OrcaBlock.Quote -> QuoteBlockNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            sourceBlockKey = sourceBlockKey,
            activeFootnoteLabel = activeFootnoteLabel,
            onFootnoteReferenceClick = onFootnoteReferenceClick,
            onFootnoteBackClick = onFootnoteBackClick,
        )

        is OrcaBlock.CodeBlock -> CodeBlockNode(
            block = block,
            style = style,
        )

        is OrcaBlock.Image -> MarkdownImageNode(
            block = block,
            style = style,
            securityPolicy = securityPolicy,
        )

        is OrcaBlock.ThematicBreak -> ThematicBreakNode(style = style)

        is OrcaBlock.Table -> TableBlockNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            onFootnoteReferenceClick = onFootnoteReferenceClick,
            sourceBlockKey = sourceBlockKey,
        )

        is OrcaBlock.Footnotes -> FootnotesNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            sourceBlockKey = sourceBlockKey,
            activeFootnoteLabel = activeFootnoteLabel,
            onFootnoteReferenceClick = onFootnoteReferenceClick,
            onFootnoteBackClick = onFootnoteBackClick,
        )

        is OrcaBlock.HtmlBlock -> HtmlBlockNode(
            block = block,
            style = style,
        )

        is OrcaBlock.Admonition -> AdmonitionNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            sourceBlockKey = sourceBlockKey,
            activeFootnoteLabel = activeFootnoteLabel,
            onFootnoteReferenceClick = onFootnoteReferenceClick,
            onFootnoteBackClick = onFootnoteBackClick,
        )
    }
}

@Composable
private fun HeadingNode(
    block: OrcaBlock.Heading,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
) {
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val currentOnFootnoteReferenceClick by rememberUpdatedState(onFootnoteReferenceClick)

    val headingText = remember(
        block.content,
        style,
        securityPolicy,
        footnoteNumbers,
        sourceBlockKey,
    ) {
        buildInlineAnnotatedString(
            inlines = block.content,
            style = style,
            onLinkClick = { url -> currentOnLinkClick(url) },
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            onFootnoteClick = { label -> currentOnFootnoteReferenceClick(label, sourceBlockKey) },
        )
    }
    val inlineImages = remember(block.content, style, securityPolicy) {
        buildInlineImageMap(
            inlines = block.content,
            style = style,
            securityPolicy = securityPolicy,
        )
    }
    InlineTextNode(
        text = headingText,
        textStyle = style.heading(block.level),
        inlineContent = inlineImages,
    )
}

@Composable
private fun ParagraphNode(
    block: OrcaBlock.Paragraph,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
) {
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val currentOnFootnoteReferenceClick by rememberUpdatedState(onFootnoteReferenceClick)

    val paragraphText = remember(
        block.content,
        style,
        securityPolicy,
        footnoteNumbers,
        sourceBlockKey,
    ) {
        buildInlineAnnotatedString(
            inlines = block.content,
            style = style,
            onLinkClick = { url -> currentOnLinkClick(url) },
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            onFootnoteClick = { label -> currentOnFootnoteReferenceClick(label, sourceBlockKey) },
        )
    }
    val inlineImages = remember(block.content, style, securityPolicy) {
        buildInlineImageMap(
            inlines = block.content,
            style = style,
            securityPolicy = securityPolicy,
        )
    }
    InlineTextNode(
        text = paragraphText,
        textStyle = style.typography.paragraph,
        inlineContent = inlineImages,
    )
}

@Composable
private fun ListBlockNode(
    block: OrcaBlock.ListBlock,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    activeFootnoteLabel: String?,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
    onFootnoteBackClick: (label: String) -> Unit,
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
                            securityPolicy = securityPolicy,
                            footnoteNumbers = footnoteNumbers,
                            sourceBlockKey = sourceBlockKey,
                            activeFootnoteLabel = activeFootnoteLabel,
                            onFootnoteReferenceClick = onFootnoteReferenceClick,
                            onFootnoteBackClick = onFootnoteBackClick,
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
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    activeFootnoteLabel: String?,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
    onFootnoteBackClick: (label: String) -> Unit,
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
                    securityPolicy = securityPolicy,
                    footnoteNumbers = footnoteNumbers,
                    sourceBlockKey = sourceBlockKey,
                    activeFootnoteLabel = activeFootnoteLabel,
                    onFootnoteReferenceClick = onFootnoteReferenceClick,
                    onFootnoteBackClick = onFootnoteBackClick,
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
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    activeFootnoteLabel: String?,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
    onFootnoteBackClick: (label: String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
    ) {
        block.definitions.forEach { definition ->
            val bringIntoViewRequester = remember(definition.label) { BringIntoViewRequester() }
            LaunchedEffect(activeFootnoteLabel, definition.label) {
                if (activeFootnoteLabel == definition.label) {
                    bringIntoViewRequester.bringIntoView()
                }
            }

            Row(
                modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester),
            ) {
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
                            securityPolicy = securityPolicy,
                            footnoteNumbers = footnoteNumbers,
                            sourceBlockKey = sourceBlockKey,
                            activeFootnoteLabel = activeFootnoteLabel,
                            onFootnoteReferenceClick = onFootnoteReferenceClick,
                            onFootnoteBackClick = onFootnoteBackClick,
                        )
                    }

                    if (activeFootnoteLabel == definition.label) {
                        Text(
                            text = "\u21A9",
                            style = style.inline.link.toTextStyle(style.typography.paragraph),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable {
                                    onFootnoteBackClick(definition.label)
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HtmlBlockNode(
    block: OrcaBlock.HtmlBlock,
    style: OrcaStyle,
) {
    val rendered = remember(block.html, style) {
        renderHtmlToAnnotatedString(
            html = block.html,
            style = style,
            onLinkClick = {},
            securityPolicy = OrcaSecurityPolicies.Default,
        )
    }
    Text(
        text = rendered,
        style = style.typography.paragraph,
    )
}

@Composable
private fun CodeBlockNode(
    block: OrcaBlock.CodeBlock,
    style: OrcaStyle,
) {
    val languageLabel = remember(block.language) { codeLanguageLabel(block.language) }
    val highlightedCode = remember(
        block.code,
        block.language,
        style.code.syntaxHighlightingEnabled,
        style.code.highlightKeyword,
        style.code.highlightString,
        style.code.highlightComment,
        style.code.highlightNumber,
    ) {
        buildCodeAnnotatedString(
            code = block.code,
            language = block.language,
            style = style,
        )
    }
    val lineNumbers = remember(block.code, style.code.showLineNumbers) {
        if (style.code.showLineNumbers) {
            codeLineNumbersText(block.code)
        } else {
            null
        }
    }

    val showHeader = languageLabel != null || style.code.showCopyButton

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(style.code.shape)
            .background(style.code.background, style.code.shape)
            .border(style.code.borderWidth, style.code.borderColor, style.code.shape)
            .padding(style.code.padding),
        verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
    ) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
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
                } else {
                    Spacer(modifier = Modifier.width(0.dp))
                }

                if (style.code.showCopyButton) {
                    CopyButton(code = block.code, style = style)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (lineNumbers != null) {
                Text(
                    text = lineNumbers,
                    style = style.code.lineNumber,
                    modifier = Modifier
                        .width(style.code.lineNumberMinWidth)
                        .padding(end = style.code.lineNumberEndPadding),
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
            ) {
                SelectionContainer {
                    Text(
                        text = highlightedCode,
                        style = style.code.text,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun CopyButton(
    code: String,
    style: OrcaStyle,
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    Text(
        text = if (copied) "Copied" else "Copy",
        style = style.code.languageLabel,
        maxLines = 1,
        modifier = Modifier
            .background(
                color = style.code.languageLabelBackground,
                shape = style.code.shape,
            )
            .clickable {
                @Suppress("DEPRECATION")
                clipboardManager.setText(AnnotatedString(code))
                copied = true
            }
            .padding(style.code.languageLabelPadding),
    )
}

@Composable
private fun AdmonitionNode(
    block: OrcaBlock.Admonition,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    activeFootnoteLabel: String?,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
    onFootnoteBackClick: (label: String) -> Unit,
) {
    val admonitionStyle = style.admonition
    val color = when (block.type) {
        OrcaAdmonitionType.NOTE -> admonitionStyle.noteColor
        OrcaAdmonitionType.TIP -> admonitionStyle.tipColor
        OrcaAdmonitionType.IMPORTANT -> admonitionStyle.importantColor
        OrcaAdmonitionType.WARNING -> admonitionStyle.warningColor
        OrcaAdmonitionType.CAUTION -> admonitionStyle.cautionColor
    }
    val background = when (block.type) {
        OrcaAdmonitionType.NOTE -> admonitionStyle.noteBackground
        OrcaAdmonitionType.TIP -> admonitionStyle.tipBackground
        OrcaAdmonitionType.IMPORTANT -> admonitionStyle.importantBackground
        OrcaAdmonitionType.WARNING -> admonitionStyle.warningBackground
        OrcaAdmonitionType.CAUTION -> admonitionStyle.cautionBackground
    }
    val icon = when (block.type) {
        OrcaAdmonitionType.NOTE -> "\u2139\uFE0F"
        OrcaAdmonitionType.TIP -> "\uD83D\uDCA1"
        OrcaAdmonitionType.IMPORTANT -> "\u2757"
        OrcaAdmonitionType.WARNING -> "\u26A0\uFE0F"
        OrcaAdmonitionType.CAUTION -> "\uD83D\uDED1"
    }
    val title = block.title ?: block.type.name.lowercase().replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .width(admonitionStyle.stripeWidth)
                .fillMaxHeight()
                .background(color),
        )
        Spacer(modifier = Modifier.width(admonitionStyle.spacing))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
        ) {
            Text(
                text = "$icon $title",
                style = admonitionStyle.titleStyle.copy(color = color),
            )
            block.blocks.forEach { childBlock ->
                OrcaBlockNode(
                    block = childBlock,
                    style = style,
                    onLinkClick = onLinkClick,
                    securityPolicy = securityPolicy,
                    footnoteNumbers = footnoteNumbers,
                    sourceBlockKey = sourceBlockKey,
                    activeFootnoteLabel = activeFootnoteLabel,
                    onFootnoteReferenceClick = onFootnoteReferenceClick,
                    onFootnoteBackClick = onFootnoteBackClick,
                )
            }
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
        OrcaTaskState.CHECKED -> "\u2611"
        OrcaTaskState.UNCHECKED -> "\u2610"
        null -> if (ordered) {
            "${startNumber + index}."
        } else {
            "\u2022"
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

internal fun codeLineNumbersText(code: String): String? {
    val lineCount = codeLineCount(code)
    if (lineCount <= 1) return null
    return (1..lineCount).joinToString(separator = "\n")
}

private fun codeLineCount(code: String): Int {
    if (code.isEmpty()) return 1
    return code.count { char -> char == '\n' } + 1
}

@Composable
private fun InlineTextNode(
    text: AnnotatedString,
    textStyle: TextStyle,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
) {
    if (inlineContent.isEmpty()) {
        Text(
            text = text,
            style = textStyle,
        )
    } else {
        Text(
            text = text,
            style = textStyle,
            inlineContent = inlineContent,
        )
    }
}

internal fun htmlBlockFallbackText(html: String): String {
    return decodeBasicHtmlEntities(
        html
            .replace(BLOCK_BREAK_TAG_REGEX, "\n")
            .replace(BR_TAG_REGEX, "\n")
            .replace(HTML_TAG_REGEX, ""),
    ).trim()
}

private fun androidx.compose.ui.text.SpanStyle.toTextStyle(base: TextStyle): TextStyle {
    return base.merge(TextStyle(color = color, textDecoration = textDecoration))
}
