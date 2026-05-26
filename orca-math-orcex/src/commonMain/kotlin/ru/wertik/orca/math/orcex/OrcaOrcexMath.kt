package ru.wertik.orca.math.orcex

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import ru.wertik.orca.compose.OrcaInlineMathPlaceholder
import ru.wertik.orcex.core.LatexParser
import ru.wertik.orcex.layout.MathLayout
import ru.wertik.orcex.layout.MathLayoutEngine
import ru.wertik.orcex.layout.MathStyle
import ru.wertik.orcex.render.compose.OrcexMath
import ru.wertik.orcex.render.compose.rememberComposeMathRenderer

/** Builds exact inline bounds so tall formulas are visible without source-length gaps. */
@Composable
fun rememberOrcaOrcexInlineMathPlaceholder(
    fontFamily: FontFamily = FontFamily.Default,
    fontSize: TextUnit = 18.sp,
): OrcaInlineMathPlaceholder {
    val renderer = rememberComposeMathRenderer(fontFamily)
    val density = LocalDensity.current
    val engine = remember(renderer) { MathLayoutEngine(renderer) }
    val parser = remember { LatexParser() }
    return remember(engine, parser, density, fontSize) {
        { source ->
            layoutOrNull(source, parser, engine, with(density) { fontSize.toPx() })?.let { layout ->
                Placeholder(
                    width = with(density) { layout.width.toSp() },
                    height = with(density) { layout.height.toSp() },
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                )
            }
        }
    }
}

/** Renders an Orca math slot using Orcex's Compose Multiplatform backend. */
@Composable
fun OrcaOrcexMath(
    source: String,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Default,
    fontSize: TextUnit = 18.sp,
    color: Color = Color.Black,
) {
    val renderer = rememberComposeMathRenderer(fontFamily)
    val density = LocalDensity.current
    val engine = remember(renderer) { MathLayoutEngine(renderer) }
    val parser = remember { LatexParser() }
    val layout = remember(source, engine, parser, density, fontSize) {
        layoutOrNull(source, parser, engine, with(density) { fontSize.toPx() })
    }
    if (layout == null) {
        BasicText(text = "\$${source}\$", modifier = modifier)
        return
    }
    OrcexMath(
        layout = layout,
        renderer = renderer,
        modifier = modifier,
        color = color,
        contentDescription = source,
    )
}

private fun layoutOrNull(
    source: String,
    parser: LatexParser,
    engine: MathLayoutEngine,
    fontSizePx: Float,
): MathLayout? = runCatching {
    engine.layout(parser.parse(source), MathStyle(fontSize = fontSizePx))
}.getOrNull()
