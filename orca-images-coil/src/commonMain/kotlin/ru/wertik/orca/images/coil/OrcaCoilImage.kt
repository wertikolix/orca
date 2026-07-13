package ru.wertik.orca.images.coil

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import ru.wertik.orca.compose.OrcaStyle

/** Coil-backed renderer for a block image passed through Orca's `imageContent` slot. */
@Composable
fun OrcaCoilImage(
    url: String,
    contentDescription: String?,
    style: OrcaStyle,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = style.image.contentScale,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = style.image.maxHeight)
            .clip(style.image.shape)
            .background(style.image.background)
            .semantics { this.contentDescription = contentDescription ?: "Image" },
        loading = { OrcaCoilLoadingPlaceholder(style = style) },
        error = { OrcaCoilImageErrorPlaceholder(style = style) },
        success = { SubcomposeAsyncImageContent() },
    )
}

/** Coil-backed renderer for an inline image passed through Orca's `inlineImageContent` slot. */
@Composable
fun OrcaCoilInlineImage(
    url: String,
    contentDescription: String?,
    style: OrcaStyle,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier
            .fillMaxSize()
            .clip(style.inlineImage.shape),
    )
}

@Composable
private fun OrcaCoilLoadingPlaceholder(
    style: OrcaStyle,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "orca-coil-loading")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orca-coil-loading-alpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(style.image.shape)
            .alpha(alpha)
            .background(style.image.background),
    )
}

@Composable
private fun OrcaCoilImageErrorPlaceholder(
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
        BasicText(
            text = "Image unavailable",
            style = style.image.captionText,
        )
    }
}
