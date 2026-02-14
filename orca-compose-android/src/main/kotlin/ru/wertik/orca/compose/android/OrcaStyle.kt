package ru.wertik.orca.compose.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OrcaTypographyStyle(
    val heading1: TextStyle = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    val heading2: TextStyle = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    val heading3: TextStyle = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    val heading4: TextStyle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    val heading5: TextStyle = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    val heading6: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    val paragraph: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
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

data class OrcaInlineStyle(
    val inlineCode: SpanStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = Color(0x16000000),
    ),
    val strikethrough: SpanStyle = SpanStyle(
        textDecoration = TextDecoration.LineThrough,
    ),
    val link: SpanStyle = SpanStyle(
        color = Color(0xFF1565C0),
        textDecoration = TextDecoration.Underline,
    ),
    val footnoteReference: SpanStyle = SpanStyle(
        baselineShift = BaselineShift.Superscript,
        fontSize = 12.sp,
        color = Color(0xFF455A64),
    ),
)

data class OrcaLayoutStyle(
    val blockSpacing: Dp = 12.dp,
    val nestedBlockSpacing: Dp = 8.dp,
    val listMarkerWidth: Dp = 22.dp,
)

data class OrcaQuoteStyle(
    val stripeColor: Color = Color(0xFFB0BEC5),
    val stripeWidth: Dp = 3.dp,
    val spacing: Dp = 10.dp,
)

data class OrcaCodeBlockStyle(
    val text: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily.Monospace,
    ),
    val languageLabel: TextStyle = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
    ),
    val lineNumber: TextStyle = TextStyle(
        fontSize = 12.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFF7A7A7A),
    ),
    val background: Color = Color(0xFFF3F3F3),
    val languageLabelBackground: Color = Color(0x12000000),
    val borderColor: Color = Color(0xFFD0D7DE),
    val borderWidth: Dp = 1.dp,
    val shape: Shape = RoundedCornerShape(8.dp),
    val padding: PaddingValues = PaddingValues(12.dp),
    val showLineNumbers: Boolean = true,
    val lineNumberMinWidth: Dp = 28.dp,
    val lineNumberEndPadding: Dp = 12.dp,
    val languageLabelPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
)

data class OrcaTableStyle(
    val text: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    val headerText: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    val columnWidth: Dp = 160.dp,
    val cellPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    val borderColor: Color = Color(0xFFD0D7DE),
    val borderWidth: Dp = 1.dp,
    val headerBackground: Color = Color(0xFFF7F9FB),
)

data class OrcaThematicBreakStyle(
    val color: Color = Color(0xFFD0D7DE),
    val thickness: Dp = 1.dp,
)

data class OrcaImageStyle(
    val shape: Shape = RoundedCornerShape(8.dp),
    val background: Color = Color(0xFFF7F9FB),
    val maxHeight: Dp = 360.dp,
    val contentScale: ContentScale = ContentScale.Fit,
)

data class OrcaStyle(
    val typography: OrcaTypographyStyle = OrcaTypographyStyle(),
    val inline: OrcaInlineStyle = OrcaInlineStyle(),
    val layout: OrcaLayoutStyle = OrcaLayoutStyle(),
    val quote: OrcaQuoteStyle = OrcaQuoteStyle(),
    val code: OrcaCodeBlockStyle = OrcaCodeBlockStyle(),
    val table: OrcaTableStyle = OrcaTableStyle(),
    val thematicBreak: OrcaThematicBreakStyle = OrcaThematicBreakStyle(),
    val image: OrcaImageStyle = OrcaImageStyle(),
) {
    fun heading(level: Int): TextStyle = typography.heading(level)

    // compatibility accessors for v0.1 clients using flat style fields
    val heading1: TextStyle get() = typography.heading1
    val heading2: TextStyle get() = typography.heading2
    val heading3: TextStyle get() = typography.heading3
    val heading4: TextStyle get() = typography.heading4
    val heading5: TextStyle get() = typography.heading5
    val heading6: TextStyle get() = typography.heading6
    val paragraph: TextStyle get() = typography.paragraph

    val inlineCode: SpanStyle get() = inline.inlineCode
    val strikethroughStyle: SpanStyle get() = inline.strikethrough
    val linkStyle: SpanStyle get() = inline.link
    val footnoteReferenceStyle: SpanStyle get() = inline.footnoteReference

    val quoteStripeColor: Color get() = quote.stripeColor
    val quoteStripeWidth: Dp get() = quote.stripeWidth
    val quoteSpacing: Dp get() = quote.spacing

    val blockSpacing: Dp get() = layout.blockSpacing
    val nestedBlockSpacing: Dp get() = layout.nestedBlockSpacing
    val listMarkerWidth: Dp get() = layout.listMarkerWidth

    val codeBlock: TextStyle get() = code.text
    val codeBlockLanguageLabel: TextStyle get() = code.languageLabel
    val codeBlockLineNumber: TextStyle get() = code.lineNumber
    val codeBlockBackground: Color get() = code.background
    val codeBlockLanguageLabelBackground: Color get() = code.languageLabelBackground
    val codeBlockBorderColor: Color get() = code.borderColor
    val codeBlockBorderWidth: Dp get() = code.borderWidth
    val codeBlockShape: Shape get() = code.shape
    val codeBlockPadding: PaddingValues get() = code.padding
    val codeBlockShowLineNumbers: Boolean get() = code.showLineNumbers
    val codeBlockLineNumberMinWidth: Dp get() = code.lineNumberMinWidth
    val codeBlockLineNumberEndPadding: Dp get() = code.lineNumberEndPadding
    val codeBlockLanguageLabelPadding: PaddingValues get() = code.languageLabelPadding

    val tableText: TextStyle get() = table.text
    val tableHeaderText: TextStyle get() = table.headerText
    val tableColumnWidth: Dp get() = table.columnWidth
    val tableCellPadding: PaddingValues get() = table.cellPadding
    val tableBorderColor: Color get() = table.borderColor
    val tableBorderWidth: Dp get() = table.borderWidth
    val tableHeaderBackground: Color get() = table.headerBackground

    val thematicBreakColor: Color get() = thematicBreak.color
    val thematicBreakThickness: Dp get() = thematicBreak.thickness

    val imageShape: Shape get() = image.shape
    val imageBackground: Color get() = image.background
    val imageMaxHeight: Dp get() = image.maxHeight
    val imageContentScale: ContentScale get() = image.contentScale
}

internal val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
internal val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
