package ru.wertik.orca.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
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

    val fallbackText = remember(block) { imageBlockFallbackText(block) }

    SubcomposeAsyncImage(
        model = safeSource,
        contentDescription = block.alt,
        contentScale = style.image.contentScale,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = style.image.maxHeight)
            .clip(style.image.shape)
            .background(style.image.background),
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Error -> {
                Text(
                    text = fallbackText,
                    style = style.typography.paragraph,
                )
            }
            is AsyncImagePainter.State.Loading,
            is AsyncImagePainter.State.Empty -> {
                Text(
                    text = fallbackText,
                    style = style.typography.paragraph,
                )
            }
            is AsyncImagePainter.State.Success -> {
                SubcomposeAsyncImageContent()
            }
        }
    }
}

internal fun imageBlockFallbackText(block: OrcaBlock.Image): String {
    return block.alt?.takeIf { it.isNotBlank() } ?: block.source
}
