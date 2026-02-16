package ru.wertik.orca.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import coil3.compose.AsyncImage
import ru.wertik.orca.core.OrcaBlock

@Composable
internal fun MarkdownImageNode(
    block: OrcaBlock.Image,
    style: OrcaStyle,
    securityPolicy: OrcaSecurityPolicy,
) {
    val safeSource = remember(block.source, securityPolicy) {
        block.source.takeIf { source ->
            securityPolicy.isAllowed(
                type = OrcaUrlType.IMAGE,
                value = source,
            )
        }
    }
    if (safeSource == null) {
        Text(
            text = imageBlockFallbackText(block),
            style = style.typography.paragraph,
        )
        return
    }

    AsyncImage(
        model = safeSource,
        contentDescription = block.alt,
        contentScale = style.image.contentScale,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = style.image.maxHeight)
            .clip(style.image.shape)
            .background(style.image.background),
    )
}

internal fun imageBlockFallbackText(block: OrcaBlock.Image): String {
    return block.alt?.takeIf { it.isNotBlank() } ?: block.source
}
