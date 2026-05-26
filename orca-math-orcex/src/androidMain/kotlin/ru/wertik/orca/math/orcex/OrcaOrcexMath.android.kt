package ru.wertik.orca.math.orcex

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import ru.wertik.orca.compose.OrcaInlineMathPlaceholder

@Composable
fun rememberOrcaOrcexInlineMathPlaceholder(
    typeface: Typeface,
    fontSize: TextUnit = 18.sp,
): OrcaInlineMathPlaceholder = rememberOrcaOrcexInlineMathPlaceholder(
    fontFamily = FontFamily(typeface),
    fontSize = fontSize,
)

@Composable
fun OrcaOrcexMath(
    source: String,
    typeface: Typeface,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    color: Color = Color.Black,
) = OrcaOrcexMath(
    source = source,
    modifier = modifier,
    fontFamily = FontFamily(typeface),
    fontSize = fontSize,
    color = color,
)
