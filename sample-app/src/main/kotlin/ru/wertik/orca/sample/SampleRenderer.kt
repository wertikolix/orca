package ru.wertik.orca.sample

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import ru.wertik.orca.compose.Orca
import ru.wertik.orca.compose.OrcaInlineRenderer
import ru.wertik.orca.compose.OrcaRootLayout
import ru.wertik.orca.compose.OrcaSecurityPolicies
import ru.wertik.orca.compose.OrcaStyle
import ru.wertik.orca.compose.OrcaTextHighlight
import ru.wertik.orca.core.OrcaInline
import ru.wertik.orca.core.OrcaParser
import ru.wertik.orca.images.coil.OrcaCoilImage
import ru.wertik.orca.images.coil.OrcaCoilInlineImage
import ru.wertik.orca.math.orcex.OrcaOrcexMath
import ru.wertik.orca.math.orcex.rememberOrcaOrcexInlineMathPlaceholder
import ru.wertik.orcex.font.stix2.StixTwoMath
import kotlin.reflect.KClass

/**
 * Single call site wiring every optional Orca integration used by the lab: Coil media, native
 * Orcex math, the URL policy, interactive tasks, inline overrides, and search highlighting.
 */
@Composable
internal fun MarkdownRenderer(
    markdown: String,
    parser: OrcaParser,
    cacheKey: Any,
    style: OrcaStyle,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    rootLayout: OrcaRootLayout = OrcaRootLayout.LAZY_COLUMN,
    onLinkClick: (String) -> Unit,
    onTaskToggle: ((Int, Boolean) -> Unit)? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
    highlight: OrcaTextHighlight? = null,
) {
    val context = LocalContext.current
    val mathTypeface = remember(context) { StixTwoMath.load(context) }
    val inlineMathFontSize = 19.sp
    val inlineMathPlaceholder = rememberOrcaOrcexInlineMathPlaceholder(mathTypeface, inlineMathFontSize)

    Orca(
        markdown = markdown,
        parser = parser,
        parseCacheKey = cacheKey,
        listState = listState,
        rootLayout = rootLayout,
        modifier = modifier,
        style = style,
        securityPolicy = OrcaSecurityPolicies.RemoteImages,
        imageContent = { url, description -> OrcaCoilImage(url, description, style) },
        inlineImageContent = { url, description -> OrcaCoilInlineImage(url, description, style) },
        blockMathContent = { source ->
            OrcaOrcexMath(
                source = source,
                typeface = mathTypeface,
                fontSize = 27.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        inlineMathContent = { source ->
            OrcaOrcexMath(
                source = source,
                typeface = mathTypeface,
                fontSize = inlineMathFontSize,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        inlineMathPlaceholder = inlineMathPlaceholder,
        onLinkClick = onLinkClick,
        onTaskToggle = onTaskToggle,
        inlineOverride = inlineOverride,
        highlight = highlight,
    )
}
