package ru.wertik.orca.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import ru.wertik.orca.core.OrcaBlock

@Composable
internal fun MarkdownImageNode(
    block: OrcaBlock.Image,
    style: OrcaStyle,
    securityPolicy: OrcaSecurityPolicy,
    imageContent: (@Composable (url: String, contentDescription: String?) -> Unit)? = null,
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

    val description = block.alt ?: "Image"

    if (imageContent != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = style.image.maxHeight)
                .clip(style.image.shape)
                .semantics { contentDescription = description },
        ) {
            imageContent(safeSource, description)
        }
        return
    }

    SubcomposeAsyncImage(
        model = safeSource,
        contentDescription = description,
        contentScale = style.image.contentScale,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = style.image.maxHeight)
            .clip(style.image.shape)
            .background(style.image.background)
            .semantics { contentDescription = description },
        loading = { ShimmerPlaceholder(style = style) },
        error = { ImageErrorPlaceholder(style = style) },
        success = { SubcomposeAsyncImageContent() },
    )
}

@Composable
internal fun ShimmerPlaceholder(
    style: OrcaStyle,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-translate",
    )
    val shimmerColors = listOf(
        style.image.background,
        style.image.background.copy(alpha = 0.4f),
        style.image.background,
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(style.image.shape)
            .background(brush),
    )
}

@Composable
internal fun ImageErrorPlaceholder(
    style: OrcaStyle,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(style.image.shape)
            .background(style.image.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "\u26A0 Failed to load image",
            style = style.typography.paragraph,
            color = Color(0xFF9E9E9E),
        )
    }
}

internal fun imageBlockFallbackText(block: OrcaBlock.Image): String {
    return block.alt?.takeIf { it.isNotBlank() } ?: block.source
}
