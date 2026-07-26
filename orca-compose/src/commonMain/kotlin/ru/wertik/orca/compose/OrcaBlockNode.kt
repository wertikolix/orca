package ru.wertik.orca.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.BasicText as Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.wertik.orca.core.OrcaAdmonitionType
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaInline
import ru.wertik.orca.core.OrcaTaskState
import kotlin.reflect.KClass

private const val MAX_RENDER_DEPTH = 32

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
    imageContent: OrcaImageContent? = null,
    inlineImageContent: OrcaImageContent? = null,
    blockMathContent: OrcaMathContent? = null,
    inlineMathContent: OrcaMathContent? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
    depth: Int = 0,
) {
    // Guard against excessively nested markdown (e.g. 50-level deep quotes).
    // The parser caps at maxTreeDepth, but a custom parser might not.
    if (depth > MAX_RENDER_DEPTH) return
    when (block) {
        is OrcaBlock.Heading -> HeadingNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            onFootnoteReferenceClick = onFootnoteReferenceClick,
            sourceBlockKey = sourceBlockKey,
            inlineImageContent = inlineImageContent,
            inlineMathContent = inlineMathContent,
            inlineOverride = inlineOverride,
        )

        is OrcaBlock.Paragraph -> ParagraphNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            onFootnoteReferenceClick = onFootnoteReferenceClick,
            sourceBlockKey = sourceBlockKey,
            inlineImageContent = inlineImageContent,
            inlineMathContent = inlineMathContent,
            inlineOverride = inlineOverride,
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
            imageContent = imageContent,
            inlineImageContent = inlineImageContent,
            blockMathContent = blockMathContent,
            inlineMathContent = inlineMathContent,
            inlineOverride = inlineOverride,
            depth = depth,
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
            imageContent = imageContent,
            inlineImageContent = inlineImageContent,
            blockMathContent = blockMathContent,
            inlineMathContent = inlineMathContent,
            inlineOverride = inlineOverride,
            depth = depth,
        )

        is OrcaBlock.CodeBlock -> CodeBlockNode(block = block, style = style)

        is OrcaBlock.Math -> MarkdownMathNode(block = block, style = style, blockMathContent = blockMathContent)

        is OrcaBlock.Image -> MarkdownImageNode(
            block = block,
            style = style,
            securityPolicy = securityPolicy,
            imageContent = imageContent,
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
            inlineImageContent = inlineImageContent,
            inlineMathContent = inlineMathContent,
            inlineOverride = inlineOverride,
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
            imageContent = imageContent,
            inlineImageContent = inlineImageContent,
            blockMathContent = blockMathContent,
            inlineMathContent = inlineMathContent,
            inlineOverride = inlineOverride,
            depth = depth,
        )

        is OrcaBlock.HtmlBlock -> HtmlBlockNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            imageContent = imageContent,
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
            imageContent = imageContent,
            inlineImageContent = inlineImageContent,
            blockMathContent = blockMathContent,
            inlineMathContent = inlineMathContent,
            inlineOverride = inlineOverride,
            depth = depth,
        )

        is OrcaBlock.DefinitionList -> DefinitionListNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            sourceBlockKey = sourceBlockKey,
            activeFootnoteLabel = activeFootnoteLabel,
            onFootnoteReferenceClick = onFootnoteReferenceClick,
            onFootnoteBackClick = onFootnoteBackClick,
            imageContent = imageContent,
            inlineImageContent = inlineImageContent,
            blockMathContent = blockMathContent,
            inlineMathContent = inlineMathContent,
            inlineOverride = inlineOverride,
            depth = depth,
        )

        is OrcaBlock.Details -> DetailsNode(
            block = block,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            sourceBlockKey = sourceBlockKey,
            activeFootnoteLabel = activeFootnoteLabel,
            onFootnoteReferenceClick = onFootnoteReferenceClick,
            onFootnoteBackClick = onFootnoteBackClick,
            imageContent = imageContent,
            inlineImageContent = inlineImageContent,
            blockMathContent = blockMathContent,
            inlineMathContent = inlineMathContent,
            inlineOverride = inlineOverride,
            depth = depth,
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
    inlineImageContent: OrcaImageContent?,
    inlineMathContent: OrcaMathContent?,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer>,
) {
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val currentOnFootnoteReferenceClick by rememberUpdatedState(onFootnoteReferenceClick)
    val inlineMathPlaceholder = LocalOrcaInlineMathPlaceholder.current
    val highlight = LocalOrcaTextHighlight.current

    val headingText = remember(
        block.content,
        style,
        securityPolicy,
        footnoteNumbers,
        sourceBlockKey,
        inlineOverride,
        highlight,
    ) {
        buildInlineAnnotatedString(
            inlines = block.content,
            style = style,
            onLinkClick = { url -> currentOnLinkClick(url) },
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            onFootnoteClick = { label -> currentOnFootnoteReferenceClick(label, sourceBlockKey) },
            inlineOverride = inlineOverride,
            highlight = highlight,
        )
    }
    val inlineImages = remember(block.content, style, securityPolicy, inlineImageContent) {
        buildInlineImageMap(
            inlines = block.content,
            style = style,
            securityPolicy = securityPolicy,
            inlineImageContent = inlineImageContent,
        )
    }
    val inlineMath = remember(block.content, inlineMathContent, inlineMathPlaceholder) {
        buildInlineMathMap(block.content, inlineMathContent, inlineMathPlaceholder)
    }
    val rule = style.headingRule
    Column(modifier = Modifier.fillMaxWidth()) {
        InlineTextNode(
            text = headingText,
            textStyle = style.heading(block.level),
            inlineContent = inlineImages + inlineMath,
            modifier = Modifier.semantics { heading() },
        )
        if (rule.hasRule(block.level)) {
            Spacer(modifier = Modifier.height(rule.spacing))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rule.thickness)
                    .background(rule.color),
            )
        }
    }
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
    inlineImageContent: OrcaImageContent?,
    inlineMathContent: OrcaMathContent?,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer>,
) {
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val currentOnFootnoteReferenceClick by rememberUpdatedState(onFootnoteReferenceClick)
    val inlineMathPlaceholder = LocalOrcaInlineMathPlaceholder.current
    val highlight = LocalOrcaTextHighlight.current

    val paragraphText = remember(
        block.content,
        style,
        securityPolicy,
        footnoteNumbers,
        sourceBlockKey,
        inlineOverride,
        highlight,
    ) {
        buildInlineAnnotatedString(
            inlines = block.content,
            style = style,
            onLinkClick = { url -> currentOnLinkClick(url) },
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            onFootnoteClick = { label -> currentOnFootnoteReferenceClick(label, sourceBlockKey) },
            inlineOverride = inlineOverride,
            highlight = highlight,
        )
    }
    val inlineImages = remember(block.content, style, securityPolicy, inlineImageContent) {
        buildInlineImageMap(
            inlines = block.content,
            style = style,
            securityPolicy = securityPolicy,
            inlineImageContent = inlineImageContent,
        )
    }
    val inlineMath = remember(block.content, inlineMathContent, inlineMathPlaceholder) {
        buildInlineMathMap(block.content, inlineMathContent, inlineMathPlaceholder)
    }
    InlineTextNode(
        text = paragraphText,
        textStyle = style.typography.paragraph,
        inlineContent = inlineImages + inlineMath,
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
    imageContent: OrcaImageContent? = null,
    inlineImageContent: OrcaImageContent? = null,
    blockMathContent: OrcaMathContent? = null,
    inlineMathContent: OrcaMathContent? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
    depth: Int = 0,
) {
    val taskInteraction = LocalOrcaTaskInteraction.current
    val taskCheckboxContent = LocalOrcaTaskCheckboxContent.current
    Column(
        verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
    ) {
        block.items.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.Top) {
                if (item.taskState != null) {
                    val checked = item.taskState == OrcaTaskState.CHECKED
                    val taskHandler = taskInteraction?.let { interaction ->
                        val taskIndex = interaction.indexOf(item)
                        if (taskIndex == null) null else { newValue: Boolean ->
                            interaction.onTaskToggle(taskIndex, newValue)
                        }
                    }
                    val onCheckedChange: (Boolean) -> Unit = taskHandler ?: {}
                    Box(
                        modifier = Modifier.width(maxOf(style.layout.listMarkerWidth, style.task.touchTargetSize)),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        if (taskCheckboxContent != null) {
                            taskCheckboxContent(checked, taskHandler != null, onCheckedChange)
                        } else {
                            DefaultTaskCheckbox(
                                checked = checked,
                                enabled = taskHandler != null,
                                onCheckedChange = onCheckedChange,
                                style = style,
                            )
                        }
                    }
                } else {
                    Text(
                        text = listMarkerText(
                            ordered = block.ordered,
                            startNumber = block.startNumber,
                            index = index,
                            taskState = null,
                        ),
                        style = style.typography.paragraph,
                        modifier = Modifier.width(style.layout.listMarkerWidth),
                    )
                }
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
                            imageContent = imageContent,
                            inlineImageContent = inlineImageContent,
                            blockMathContent = blockMathContent,
                            inlineMathContent = inlineMathContent,
                            inlineOverride = inlineOverride,
                            depth = depth + 1,
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
    imageContent: OrcaImageContent? = null,
    inlineImageContent: OrcaImageContent? = null,
    blockMathContent: OrcaMathContent? = null,
    inlineMathContent: OrcaMathContent? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
    depth: Int = 0,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(style.quote.shape)
            .background(style.quote.background, style.quote.shape)
            .border(style.quote.borderWidth, style.quote.borderColor, style.quote.shape)
            .padding(style.quote.contentPadding),
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
                imageContent = imageContent,
                inlineImageContent = inlineImageContent,
                blockMathContent = blockMathContent,
                inlineMathContent = inlineMathContent,
                inlineOverride = inlineOverride,
                depth = depth + 1,
            )
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
    imageContent: OrcaImageContent? = null,
    inlineImageContent: OrcaImageContent? = null,
    blockMathContent: OrcaMathContent? = null,
    inlineMathContent: OrcaMathContent? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
    depth: Int = 0,
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
                            imageContent = imageContent,
                            inlineImageContent = inlineImageContent,
                            blockMathContent = blockMathContent,
                            inlineMathContent = inlineMathContent,
                            inlineOverride = inlineOverride,
                            depth = depth + 1,
                        )
                    }

                    if (activeFootnoteLabel == definition.label) {
                        Text(
                            text = "\u21A9",
                            style = style.inline.link.toTextStyle(style.typography.paragraph),
                            modifier = Modifier
                                .padding(top = 2.dp)
                            .clickable(role = Role.Button) {
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
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    imageContent: OrcaImageContent?,
) {
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val image = remember(block.html) { parseHtmlBlockImage(block.html) }
    if (image != null) {
        MarkdownImageNode(
            block = image,
            style = style,
            securityPolicy = securityPolicy,
            imageContent = imageContent,
        )
        return
    }

    val rendered = remember(block.html, style, securityPolicy) {
        renderHtmlToAnnotatedString(
            html = block.html,
            style = style,
            onLinkClick = { url -> currentOnLinkClick(url) },
            securityPolicy = securityPolicy,
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
                        style = style.code.languageLabel.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .background(
                                color = style.code.languageLabel.background,
                                shape = style.code.languageLabel.shape,
                            )
                            .padding(style.code.languageLabel.padding),
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
        style = style.code.copyButton.text,
        maxLines = 1,
        modifier = Modifier
            .background(
                color = style.code.copyButton.background,
                shape = style.code.copyButton.shape,
            )
            .clickable(role = Role.Button) {
                @Suppress("DEPRECATION")
                clipboardManager.setText(AnnotatedString(code))
                copied = true
            }
            .padding(style.code.copyButton.padding),
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
    imageContent: OrcaImageContent? = null,
    inlineImageContent: OrcaImageContent? = null,
    blockMathContent: OrcaMathContent? = null,
    inlineMathContent: OrcaMathContent? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
    depth: Int = 0,
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
    val title = block.title ?: block.type.name.lowercase().replaceFirstChar { it.uppercase() }
    val icon = when (block.type) {
        OrcaAdmonitionType.NOTE -> admonitionStyle.noteIcon
        OrcaAdmonitionType.TIP -> admonitionStyle.tipIcon
        OrcaAdmonitionType.IMPORTANT -> admonitionStyle.importantIcon
        OrcaAdmonitionType.WARNING -> admonitionStyle.warningIcon
        OrcaAdmonitionType.CAUTION -> admonitionStyle.cautionIcon
    }.takeIf { admonitionStyle.showIcons && it.isNotEmpty() }
    val collapsible = admonitionStyle.collapsible
    var expanded by remember { mutableStateOf(!admonitionStyle.collapsedByDefault) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(admonitionStyle.shape)
            .background(background, admonitionStyle.shape)
            .border(admonitionStyle.borderWidth, color, admonitionStyle.shape)
            .padding(admonitionStyle.contentPadding),
        verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
    ) {
        Row(
            modifier = if (collapsible) {
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded }
            } else {
                Modifier.fillMaxWidth()
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Text(
                    text = icon,
                    style = admonitionStyle.titleStyle.copy(color = color),
                    modifier = Modifier.padding(end = admonitionStyle.iconSpacing),
                )
            }
            Text(
                text = title,
                style = admonitionStyle.titleStyle.copy(color = color),
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (collapsible) {
                Text(
                    text = if (expanded) "\u25B2" else "\u25BC",
                    style = admonitionStyle.titleStyle.copy(color = color),
                )
            }
        }
        AnimatedVisibility(
            visible = expanded || !collapsible,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
            ) {
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
                        imageContent = imageContent,
                        inlineImageContent = inlineImageContent,
                        blockMathContent = blockMathContent,
                        inlineMathContent = inlineMathContent,
                        inlineOverride = inlineOverride,
                        depth = depth + 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun DefinitionListNode(
    block: OrcaBlock.DefinitionList,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    activeFootnoteLabel: String?,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
    onFootnoteBackClick: (label: String) -> Unit,
    imageContent: OrcaImageContent? = null,
    inlineImageContent: OrcaImageContent? = null,
    blockMathContent: OrcaMathContent? = null,
    inlineMathContent: OrcaMathContent? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
    depth: Int = 0,
) {
    val dlStyle = style.definitionList
    val inlineMathPlaceholder = LocalOrcaInlineMathPlaceholder.current
    val highlight = LocalOrcaTextHighlight.current
    Column(
        verticalArrangement = Arrangement.spacedBy(dlStyle.termSpacing),
    ) {
        block.items.forEach { item ->
            val currentOnLinkClick by rememberUpdatedState(onLinkClick)
            val currentOnFootnoteReferenceClick by rememberUpdatedState(onFootnoteReferenceClick)

            val termText = remember(item.term, style, securityPolicy, footnoteNumbers, sourceBlockKey, inlineOverride, highlight) {
                buildInlineAnnotatedString(
                    inlines = item.term,
                    style = style,
                    onLinkClick = { url -> currentOnLinkClick(url) },
                    securityPolicy = securityPolicy,
                    footnoteNumbers = footnoteNumbers,
                    onFootnoteClick = { label -> currentOnFootnoteReferenceClick(label, sourceBlockKey) },
                    inlineOverride = inlineOverride,
                    highlight = highlight,
                )
            }
            val termInlineImages = remember(item.term, style, securityPolicy, inlineImageContent) {
                buildInlineImageMap(
                    inlines = item.term,
                    style = style,
                    securityPolicy = securityPolicy,
                    inlineImageContent = inlineImageContent,
                )
            }
            val termInlineMath = remember(item.term, inlineMathContent, inlineMathPlaceholder) {
                buildInlineMathMap(item.term, inlineMathContent, inlineMathPlaceholder)
            }
            InlineTextNode(
                text = termText,
                textStyle = dlStyle.termStyle,
                inlineContent = termInlineImages + termInlineMath,
            )
            item.definitions.forEach { definitionBlocks ->
                Column(
                    modifier = Modifier.padding(start = dlStyle.definitionIndent),
                    verticalArrangement = Arrangement.spacedBy(dlStyle.definitionSpacing),
                ) {
                    definitionBlocks.forEach { childBlock ->
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
                            imageContent = imageContent,
                            inlineImageContent = inlineImageContent,
                            blockMathContent = blockMathContent,
                            inlineMathContent = inlineMathContent,
                            inlineOverride = inlineOverride,
                            depth = depth + 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsNode(
    block: OrcaBlock.Details,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    sourceBlockKey: String,
    activeFootnoteLabel: String?,
    onFootnoteReferenceClick: (label: String, sourceBlockKey: String) -> Unit,
    onFootnoteBackClick: (label: String) -> Unit,
    imageContent: OrcaImageContent? = null,
    inlineImageContent: OrcaImageContent? = null,
    blockMathContent: OrcaMathContent? = null,
    inlineMathContent: OrcaMathContent? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
    depth: Int = 0,
) {
    val detailsStyle = style.details
    var expanded by remember { mutableStateOf(block.startOpen) }
    val inlineMathPlaceholder = LocalOrcaInlineMathPlaceholder.current
    val highlight = LocalOrcaTextHighlight.current

    val summaryInlines = block.summary.ifEmpty { listOf(OrcaInline.Text("Details")) }
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val currentOnFootnoteReferenceClick by rememberUpdatedState(onFootnoteReferenceClick)

    val summaryText = remember(summaryInlines, style, securityPolicy, footnoteNumbers, sourceBlockKey, inlineOverride, highlight) {
        buildInlineAnnotatedString(
            inlines = summaryInlines,
            style = style,
            onLinkClick = { url -> currentOnLinkClick(url) },
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            onFootnoteClick = { label -> currentOnFootnoteReferenceClick(label, sourceBlockKey) },
            inlineOverride = inlineOverride,
            highlight = highlight,
        )
    }
    val summaryInlineImages = remember(summaryInlines, style, securityPolicy, inlineImageContent) {
        buildInlineImageMap(summaryInlines, style, securityPolicy, inlineImageContent)
    }
    val summaryInlineMath = remember(summaryInlines, inlineMathContent, inlineMathPlaceholder) {
        buildInlineMathMap(summaryInlines, inlineMathContent, inlineMathPlaceholder)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(detailsStyle.shape)
            .border(detailsStyle.borderWidth, detailsStyle.borderColor, detailsStyle.shape)
            .background(detailsStyle.background, detailsStyle.shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(detailsStyle.contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (expanded) "\u25BC" else "\u25B6",
                style = detailsStyle.summaryStyle,
                modifier = Modifier.padding(end = 8.dp),
            )
            InlineTextNode(
                text = summaryText,
                textStyle = detailsStyle.summaryStyle,
                inlineContent = summaryInlineImages + summaryInlineMath,
                modifier = Modifier.weight(1f),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(detailsStyle.contentPadding),
                verticalArrangement = Arrangement.spacedBy(style.layout.nestedBlockSpacing),
            ) {
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
                        imageContent = imageContent,
                        inlineImageContent = inlineImageContent,
                        blockMathContent = blockMathContent,
                        inlineMathContent = inlineMathContent,
                        inlineOverride = inlineOverride,
                        depth = depth + 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultTaskCheckbox(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    style: OrcaStyle,
) {
    val task = style.task
    Box(
        modifier = Modifier
            .size(task.touchTargetSize)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(task.size)
                .alpha(if (enabled) 1f else task.disabledAlpha)
                .clip(task.shape)
                .background(
                    if (checked) task.checkedBackground else task.uncheckedBackground,
                    task.shape,
                )
                .border(
                    width = task.borderWidth,
                    color = if (checked) task.checkedBorderColor else task.uncheckedBorderColor,
                    shape = task.shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                ) {
                    val strokeWidth = 1.8.dp.toPx()
                    drawLine(
                        color = task.checkColor,
                        start = Offset(size.width * 0.08f, size.height * 0.52f),
                        end = Offset(size.width * 0.38f, size.height * 0.82f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = task.checkColor,
                        start = Offset(size.width * 0.38f, size.height * 0.82f),
                        end = Offset(size.width * 0.94f, size.height * 0.18f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
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
    modifier: Modifier = Modifier,
) {
    if (inlineContent.isEmpty()) {
        Text(
            text = text,
            style = textStyle,
            modifier = modifier,
        )
    } else {
        Text(
            text = text,
            style = textStyle,
            inlineContent = inlineContent,
            modifier = modifier,
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
