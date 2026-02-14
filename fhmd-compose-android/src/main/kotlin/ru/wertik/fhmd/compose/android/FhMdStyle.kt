package ru.wertik.fhmd.compose.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FhMdStyle(
    val heading1: TextStyle = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    val heading2: TextStyle = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    val heading3: TextStyle = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    val heading4: TextStyle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    val heading5: TextStyle = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    val heading6: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    val paragraph: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    val codeBlock: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily.Monospace,
    ),
    val inlineCode: SpanStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = Color(0x16000000),
    ),
    val strikethroughStyle: SpanStyle = SpanStyle(
        textDecoration = TextDecoration.LineThrough,
    ),
    val linkStyle: SpanStyle = SpanStyle(
        color = Color(0xFF1565C0),
        textDecoration = TextDecoration.Underline,
    ),
    val quoteStripeColor: Color = Color(0xFFB0BEC5),
    val quoteStripeWidth: Dp = 3.dp,
    val quoteSpacing: Dp = 10.dp,
    val blockSpacing: Dp = 12.dp,
    val nestedBlockSpacing: Dp = 8.dp,
    val listMarkerWidth: Dp = 22.dp,
    val codeBlockBackground: Color = Color(0xFFF3F3F3),
    val codeBlockShape: Shape = RoundedCornerShape(8.dp),
    val codeBlockPadding: PaddingValues = PaddingValues(12.dp),
    val tableText: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    val tableHeaderText: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    val tableColumnWidth: Dp = 160.dp,
    val tableCellPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    val tableBorderColor: Color = Color(0xFFD0D7DE),
    val tableBorderWidth: Dp = 1.dp,
    val tableHeaderBackground: Color = Color(0xFFF7F9FB),
    val thematicBreakColor: Color = Color(0xFFD0D7DE),
    val thematicBreakThickness: Dp = 1.dp,
    val imageShape: Shape = RoundedCornerShape(8.dp),
    val imageBackground: Color = Color(0xFFF7F9FB),
    val imageMaxHeight: Dp = 360.dp,
    val imageContentScale: ContentScale = ContentScale.Fit,
) {
    fun heading(level: Int): TextStyle {
        return when (level.coerceIn(1, 6)) {
            1 -> heading1
            2 -> heading2
            3 -> heading3
            4 -> heading4
            5 -> heading5
            else -> heading6
        }
    }
}

internal val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
internal val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
