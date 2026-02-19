package ru.wertik.orca.compose

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
import androidx.compose.ui.unit.TextUnit
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
    val superscript: SpanStyle = SpanStyle(
        baselineShift = BaselineShift.Superscript,
        fontSize = 12.sp,
    ),
    val subscript: SpanStyle = SpanStyle(
        baselineShift = BaselineShift.Subscript,
        fontSize = 12.sp,
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
    val showCopyButton: Boolean = true,
    val syntaxHighlightingEnabled: Boolean = true,
    val highlightKeyword: SpanStyle = SpanStyle(color = Color(0xFF0B57D0), fontWeight = FontWeight.SemiBold),
    val highlightString: SpanStyle = SpanStyle(color = Color(0xFF2E7D32)),
    val highlightComment: SpanStyle = SpanStyle(color = Color(0xFF6D6D6D), fontStyle = FontStyle.Italic),
    val highlightNumber: SpanStyle = SpanStyle(color = Color(0xFF8E24AA)),
)

enum class OrcaTableLayoutMode {
    FIXED,
    AUTO,
}

data class OrcaTableStyle(
    val text: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    val headerText: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    val columnWidth: Dp = 160.dp,
    val layoutMode: OrcaTableLayoutMode = OrcaTableLayoutMode.AUTO,
    val minColumnWidth: Dp = 120.dp,
    val maxColumnWidth: Dp = 320.dp,
    val autoColumnCharacterWidth: Dp = 7.dp,
    val fillAvailableWidth: Boolean = true,
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

data class OrcaAdmonitionStyle(
    val stripeWidth: Dp = 3.dp,
    val spacing: Dp = 10.dp,
    val titleStyle: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    val noteColor: Color = Color(0xFF1565C0),
    val tipColor: Color = Color(0xFF2E7D32),
    val importantColor: Color = Color(0xFF7B1FA2),
    val warningColor: Color = Color(0xFFEF6C00),
    val cautionColor: Color = Color(0xFFC62828),
    val noteBackground: Color = Color(0x0D1565C0),
    val tipBackground: Color = Color(0x0D2E7D32),
    val importantBackground: Color = Color(0x0D7B1FA2),
    val warningBackground: Color = Color(0x0DEF6C00),
    val cautionBackground: Color = Color(0x0DC62828),
)

data class OrcaInlineImageStyle(
    val size: Dp = 20.dp,
    val shape: Shape = RoundedCornerShape(2.dp),
    val widthSp: TextUnit = 18.sp,
    val heightSp: TextUnit = 18.sp,
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
    val admonition: OrcaAdmonitionStyle = OrcaAdmonitionStyle(),
    val inlineImage: OrcaInlineImageStyle = OrcaInlineImageStyle(),
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
    val codeBlockSyntaxHighlightingEnabled: Boolean get() = code.syntaxHighlightingEnabled
    val codeBlockHighlightKeyword: SpanStyle get() = code.highlightKeyword
    val codeBlockHighlightString: SpanStyle get() = code.highlightString
    val codeBlockHighlightComment: SpanStyle get() = code.highlightComment
    val codeBlockHighlightNumber: SpanStyle get() = code.highlightNumber

    val tableText: TextStyle get() = table.text
    val tableHeaderText: TextStyle get() = table.headerText
    val tableColumnWidth: Dp get() = table.columnWidth
    val tableLayoutMode: OrcaTableLayoutMode get() = table.layoutMode
    val tableMinColumnWidth: Dp get() = table.minColumnWidth
    val tableMaxColumnWidth: Dp get() = table.maxColumnWidth
    val tableAutoColumnCharacterWidth: Dp get() = table.autoColumnCharacterWidth
    val tableFillAvailableWidth: Boolean get() = table.fillAvailableWidth
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

object OrcaDefaults {

    fun lightStyle(): OrcaStyle = OrcaStyle()

    fun darkStyle(): OrcaStyle = OrcaStyle(
        typography = OrcaTypographyStyle(
            heading1 = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE0E0E0)),
            heading2 = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE0E0E0)),
            heading3 = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE0E0E0)),
            heading4 = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE0E0E0)),
            heading5 = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE0E0E0)),
            heading6 = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE0E0E0)),
            paragraph = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = Color(0xFFD0D0D0)),
        ),
        inline = OrcaInlineStyle(
            inlineCode = SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Color(0x1AFFFFFF),
            ),
            link = SpanStyle(
                color = Color(0xFF82B1FF),
                textDecoration = TextDecoration.Underline,
            ),
            footnoteReference = SpanStyle(
                baselineShift = BaselineShift.Superscript,
                fontSize = 12.sp,
                color = Color(0xFF90A4AE),
            ),
        ),
        quote = OrcaQuoteStyle(
            stripeColor = Color(0xFF546E7A),
        ),
        code = OrcaCodeBlockStyle(
            text = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFD4D4D4),
            ),
            languageLabel = TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF9E9E9E),
            ),
            lineNumber = TextStyle(
                fontSize = 12.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF616161),
            ),
            background = Color(0xFF1E1E1E),
            languageLabelBackground = Color(0x1AFFFFFF),
            borderColor = Color(0xFF333333),
            highlightKeyword = SpanStyle(color = Color(0xFF569CD6), fontWeight = FontWeight.SemiBold),
            highlightString = SpanStyle(color = Color(0xFFCE9178)),
            highlightComment = SpanStyle(color = Color(0xFF6A9955), fontStyle = FontStyle.Italic),
            highlightNumber = SpanStyle(color = Color(0xFFB5CEA8)),
        ),
        table = OrcaTableStyle(
            borderColor = Color(0xFF333333),
            headerBackground = Color(0xFF252525),
        ),
        thematicBreak = OrcaThematicBreakStyle(
            color = Color(0xFF424242),
        ),
        image = OrcaImageStyle(
            background = Color(0xFF252525),
        ),
        admonition = OrcaAdmonitionStyle(
            noteColor = Color(0xFF64B5F6),
            tipColor = Color(0xFF81C784),
            importantColor = Color(0xFFCE93D8),
            warningColor = Color(0xFFFFB74D),
            cautionColor = Color(0xFFEF9A9A),
            noteBackground = Color(0x1A64B5F6),
            tipBackground = Color(0x1A81C784),
            importantBackground = Color(0x1ACE93D8),
            warningBackground = Color(0x1AFFB74D),
            cautionBackground = Color(0x1AEF9A9A),
        ),
    )
}

internal val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
internal val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
