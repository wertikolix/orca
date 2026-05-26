package ru.wertik.orca.compose

import androidx.compose.foundation.text.BasicText as Text
import androidx.compose.runtime.Composable
import ru.wertik.orca.core.OrcaBlock

/** Content slot used to render parsed LaTeX math without imposing an engine on the base renderer. */
typealias OrcaMathContent = @Composable (source: String) -> Unit

@Composable
internal fun MarkdownMathNode(
    block: OrcaBlock.Math,
    style: OrcaStyle,
    blockMathContent: OrcaMathContent? = null,
) {
    if (blockMathContent == null) {
        Text(text = "$$\n${block.source}\n$$", style = style.typography.paragraph)
    } else {
        blockMathContent(block.source)
    }
}
