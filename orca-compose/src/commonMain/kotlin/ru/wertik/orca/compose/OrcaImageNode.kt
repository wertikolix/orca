package ru.wertik.orca.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ru.wertik.orca.core.OrcaBlock

/** Content slot used to render an allowed Markdown image URL. */
typealias OrcaImageContent = @Composable (url: String, contentDescription: String?) -> Unit

@Composable
internal fun MarkdownImageNode(
    block: OrcaBlock.Image,
    style: OrcaStyle,
    securityPolicy: OrcaSecurityPolicy,
    imageContent: OrcaImageContent? = null,
) {
    val safeSource = remember(block.source, securityPolicy) {
        block.source.takeIf { source ->
            securityPolicy.isAllowed(
                type = OrcaUrlType.IMAGE,
                value = source,
            )
        }
    }
    if (safeSource == null || imageContent == null) {
        Text(
            text = imageBlockFallbackText(block),
            style = style.typography.paragraph,
        )
        return
    }

    val description = block.alt ?: "Image"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = style.image.maxHeight)
            .clip(style.image.shape)
            .semantics { contentDescription = description },
    ) {
        imageContent(safeSource, description)
    }
}

internal fun imageBlockFallbackText(block: OrcaBlock.Image): String {
    return block.alt?.takeIf { it.isNotBlank() } ?: block.source
}
