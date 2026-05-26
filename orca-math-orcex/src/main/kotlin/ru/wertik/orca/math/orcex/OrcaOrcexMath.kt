package ru.wertik.orca.math.orcex

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import ru.wertik.orcex.render.android.AndroidLatexEngine
import ru.wertik.orcex.render.android.CanvasMathRenderer

/** Renders a parsed Orca math slot with Orcex's native Android Canvas backend. */
@Composable
fun OrcaOrcexMath(
    source: String,
    typeface: Typeface,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    color: Color = Color.Black,
) {
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }
    val layout = remember(source, typeface, fontSizePx) {
        runCatching { AndroidLatexEngine(typeface).layout(source, fontSizePx) }.getOrNull()
    }
    if (layout == null) {
        BasicText(text = "\$${source}\$", modifier = modifier)
        return
    }
    val width = with(density) { layout.width.toDp() }
    val height = with(density) { layout.height.toDp() }
    Canvas(modifier = modifier.size(width, height)) {
        drawIntoCanvas { canvas ->
            CanvasMathRenderer(typeface = typeface, color = color.toArgb())
                .draw(canvas = canvas.nativeCanvas, layout = layout, x = 0f, y = 0f)
        }
    }
}
