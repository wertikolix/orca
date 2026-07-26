package ru.wertik.orca.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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

/**
 * Code surface colors used by the flat style builder.
 *
 * @see OrcaPalette
 */
@Immutable
data class OrcaSyntaxPalette(
    val surface: Color,
    val text: Color,
    val lineNumber: Color,
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
)

/**
 * Admonition accent colors, one per [ru.wertik.orca.core.OrcaAdmonitionType].
 *
 * @property surfaceAlpha alpha applied to each accent to derive its solid container tint.
 * Flat surfaces are a single translucent fill — never a gradient.
 */
@Immutable
data class OrcaSignalPalette(
    val note: Color,
    val tip: Color,
    val important: Color,
    val warning: Color,
    val caution: Color,
    val surfaceAlpha: Float = 0.10f,
)

/**
 * Color tokens for the flat Orca design system introduced in 0.30.
 *
 * The system deliberately has no elevation, gradient, or shadow tokens: structure comes from
 * solid fills, one-pixel outlines, and typography. Pass a palette to [orcaFlatStyle] to obtain a
 * complete [OrcaStyle], or start from a preset in [OrcaPalettes].
 *
 * @property background page background behind the document.
 * @property surface quiet container fill (quotes, details, zebra rows).
 * @property surfaceMuted secondary fill used for table headers and captions.
 * @property surfaceStrong strongest flat fill, used for chips such as inline code and labels.
 * @property outline primary one-pixel outline color.
 * @property outlineMuted subtle separators and container outlines.
 * @property text primary text color.
 * @property textMuted secondary text color for captions, line numbers, and footnote markers.
 * @property accent single accent used for links, task checkboxes, and interactive affordances.
 * @property onAccent content color drawn on top of [accent].
 * @property accentSurface flat accent tint used behind accent chips.
 * @property highlight background for `==marked==` text.
 * @property searchMatch background applied to [OrcaTextHighlight] query matches.
 */
@Immutable
data class OrcaPalette(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceStrong: Color,
    val outline: Color,
    val outlineMuted: Color,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSurface: Color,
    val highlight: Color,
    val searchMatch: Color,
    val syntax: OrcaSyntaxPalette,
    val signal: OrcaSignalPalette,
    val isDark: Boolean,
)

/**
 * Spacing scale applied by [orcaFlatStyle].
 *
 * Density only changes spacing and padding; text metrics stay identical so line lengths and
 * measured heights remain predictable across the three modes.
 */
enum class OrcaDensity(val spacingScale: Float) {
    /** Dense reading surface for chat transcripts and side panels. */
    COMPACT(0.78f),

    /** Balanced default for documentation and long-form reading. */
    COMFORTABLE(1f),

    /** Generous spacing for large screens and presentation surfaces. */
    SPACIOUS(1.25f),
}

/** Built-in flat palettes. All four are gradient-free and shadow-free by construction. */
object OrcaPalettes {

    /** Warm neutral light palette — the 0.30 default. */
    val FlatLight: OrcaPalette = OrcaPalette(
        background = Color(0xFFFBFAF7),
        surface = Color(0xFFF3F2ED),
        surfaceMuted = Color(0xFFEDECE5),
        surfaceStrong = Color(0xFFE4E2DA),
        outline = Color(0xFFB6B4AA),
        outlineMuted = Color(0xFFDCDAD1),
        text = Color(0xFF1B1C19),
        textMuted = Color(0xFF5D6058),
        accent = Color(0xFF2F6263),
        onAccent = Color(0xFFF4F9F7),
        accentSurface = Color(0x142F6263),
        highlight = Color(0x3FE0B341),
        searchMatch = Color(0x452F6263),
        syntax = OrcaSyntaxPalette(
            surface = Color(0xFFF3F2ED),
            text = Color(0xFF23251F),
            lineNumber = Color(0xFF8E9188),
            keyword = Color(0xFF1F5F60),
            string = Color(0xFF4B6B2C),
            comment = Color(0xFF7B7E74),
            number = Color(0xFF7A4A8C),
        ),
        signal = OrcaSignalPalette(
            note = Color(0xFF2F6263),
            tip = Color(0xFF3F6B39),
            important = Color(0xFF5B4B8A),
            warning = Color(0xFF8A5A18),
            caution = Color(0xFF9C413D),
        ),
        isDark = false,
    )

    /** Warm neutral dark palette. */
    val FlatDark: OrcaPalette = OrcaPalette(
        background = Color(0xFF131412),
        surface = Color(0xFF1C1D19),
        surfaceMuted = Color(0xFF232420),
        surfaceStrong = Color(0xFF2B2C26),
        outline = Color(0xFF6F7069),
        outlineMuted = Color(0xFF383A33),
        text = Color(0xFFE9E6DE),
        textMuted = Color(0xFFACAAA2),
        accent = Color(0xFFA5CECC),
        onAccent = Color(0xFF16302F),
        accentSurface = Color(0x1FA5CECC),
        highlight = Color(0x40E0B341),
        searchMatch = Color(0x4DA5CECC),
        syntax = OrcaSyntaxPalette(
            surface = Color(0xFF191A17),
            text = Color(0xFFD9D6CE),
            lineNumber = Color(0xFF6A6C64),
            keyword = Color(0xFFA5CECC),
            string = Color(0xFFC9B08A),
            comment = Color(0xFF808478),
            number = Color(0xFFC4A7D8),
        ),
        signal = OrcaSignalPalette(
            note = Color(0xFF8FC2C3),
            tip = Color(0xFF9CC08F),
            important = Color(0xFFB8A8E0),
            warning = Color(0xFFD9A86A),
            caution = Color(0xFFE0A09A),
            surfaceAlpha = 0.14f,
        ),
        isDark = true,
    )

    /** High-contrast light palette for accessibility and bright environments. */
    val ContrastLight: OrcaPalette = FlatLight.copy(
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFF2F2F2),
        surfaceMuted = Color(0xFFE9E9E9),
        surfaceStrong = Color(0xFFDCDCDC),
        outline = Color(0xFF4A4A4A),
        outlineMuted = Color(0xFF8C8C8C),
        text = Color(0xFF000000),
        textMuted = Color(0xFF3D3D3D),
        accent = Color(0xFF0F4C4D),
        accentSurface = Color(0x1F0F4C4D),
        searchMatch = Color(0x660F4C4D),
        syntax = FlatLight.syntax.copy(
            surface = Color(0xFFF2F2F2),
            text = Color(0xFF000000),
            lineNumber = Color(0xFF565656),
            comment = Color(0xFF565656),
        ),
    )

    /** High-contrast dark palette. */
    val ContrastDark: OrcaPalette = FlatDark.copy(
        background = Color(0xFF000000),
        surface = Color(0xFF141414),
        surfaceMuted = Color(0xFF1C1C1C),
        surfaceStrong = Color(0xFF262626),
        outline = Color(0xFFBFBFBF),
        outlineMuted = Color(0xFF6E6E6E),
        text = Color(0xFFFFFFFF),
        textMuted = Color(0xFFD2D2D2),
        accent = Color(0xFFBFE3E1),
        onAccent = Color(0xFF06201F),
        accentSurface = Color(0x2ABFE3E1),
        searchMatch = Color(0x66BFE3E1),
        syntax = FlatDark.syntax.copy(
            surface = Color(0xFF101010),
            text = Color(0xFFF2F2F2),
            lineNumber = Color(0xFF9A9A9A),
            comment = Color(0xFF9A9A9A),
        ),
    )
}

/**
 * Builds the typography block of a flat style from [palette].
 *
 * Heading sizes follow a 1.18 modular scale and share the palette's primary text color.
 */
fun orcaFlatTypography(
    palette: OrcaPalette,
    fontFamily: FontFamily? = null,
): OrcaTypographyStyle {
    fun heading(size: Int, lineHeight: Int, weight: FontWeight, tracking: Float) = TextStyle(
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
        letterSpacing = tracking.sp,
        color = palette.text,
        fontFamily = fontFamily,
    )
    return OrcaTypographyStyle(
        heading1 = heading(30, 38, FontWeight.SemiBold, -0.4f),
        heading2 = heading(25, 32, FontWeight.SemiBold, -0.3f),
        heading3 = heading(21, 28, FontWeight.SemiBold, -0.2f),
        heading4 = heading(18, 25, FontWeight.SemiBold, -0.1f),
        heading5 = heading(16, 23, FontWeight.Medium, 0f),
        heading6 = heading(15, 22, FontWeight.Medium, 0.1f),
        paragraph = TextStyle(
            fontSize = 16.sp,
            lineHeight = 25.sp,
            color = palette.text,
            fontFamily = fontFamily,
        ),
    )
}

/**
 * Builds a complete [OrcaStyle] from flat design tokens.
 *
 * The result contains solid fills, one-pixel outlines, and typography only — no shadows, no
 * gradients, and no elevation overlays anywhere in the render tree.
 *
 * @param palette color tokens; see [OrcaPalettes] for presets.
 * @param density spacing scale applied to block gaps and container padding.
 * @param cornerRadius radius shared by containers (code, quotes, tables, admonitions, details).
 * @param containerShape shape of block containers; defaults to a rounded rect of [cornerRadius].
 * @param chipShape shape of small chips such as code labels and copy actions.
 * @param typography text styles; defaults to [orcaFlatTypography] for [palette].
 * @param headingRuleLevels heading levels that render a one-pixel rule underneath. Pass an empty
 * set to disable the rules.
 * @param showLineNumbers whether code blocks render a line-number gutter.
 * @param zebraTables whether table rows alternate between [OrcaPalette.background] and
 * [OrcaPalette.surface].
 */
fun orcaFlatStyle(
    palette: OrcaPalette,
    density: OrcaDensity = OrcaDensity.COMFORTABLE,
    cornerRadius: Dp = 10.dp,
    containerShape: Shape = RoundedCornerShape(cornerRadius),
    chipShape: Shape = RoundedCornerShape((cornerRadius.value * 0.6f).dp),
    typography: OrcaTypographyStyle = orcaFlatTypography(palette),
    headingRuleLevels: Set<Int> = setOf(1, 2),
    showLineNumbers: Boolean = true,
    zebraTables: Boolean = true,
): OrcaStyle {
    val scale = density.spacingScale
    val monospace = FontFamily.Monospace

    fun scaled(value: Float): Dp = (value * scale).dp

    val labelText = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = monospace,
        fontWeight = FontWeight.Medium,
        color = palette.textMuted,
    )

    return OrcaStyle(
        typography = typography,
        inline = OrcaInlineStyle(
            inlineCode = SpanStyle(
                fontFamily = monospace,
                background = palette.surfaceStrong,
                color = palette.text,
            ),
            strikethrough = SpanStyle(
                textDecoration = TextDecoration.LineThrough,
                color = palette.textMuted,
            ),
            link = SpanStyle(
                color = palette.accent,
                textDecoration = TextDecoration.Underline,
            ),
            footnoteReference = SpanStyle(
                baselineShift = BaselineShift.Superscript,
                fontSize = 12.sp,
                color = palette.accent,
            ),
            superscript = SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 12.sp),
            subscript = SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 12.sp),
            abbreviation = SpanStyle(
                textDecoration = TextDecoration.Underline,
                background = palette.surfaceStrong,
            ),
            highlight = SpanStyle(background = palette.highlight, color = palette.text),
            underline = SpanStyle(textDecoration = TextDecoration.Underline),
            kbd = SpanStyle(
                fontFamily = monospace,
                fontSize = 13.sp,
                background = palette.surfaceStrong,
                color = palette.text,
            ),
            searchMatch = SpanStyle(
                background = palette.searchMatch,
                color = palette.text,
                fontWeight = FontWeight.Medium,
            ),
        ),
        layout = OrcaLayoutStyle(
            blockSpacing = scaled(15f),
            nestedBlockSpacing = scaled(9f),
            listMarkerWidth = scaled(24f),
        ),
        headingRule = OrcaHeadingRuleStyle(
            levels = headingRuleLevels,
            color = palette.outlineMuted,
            thickness = 1.dp,
            spacing = scaled(8f),
        ),
        quote = OrcaQuoteStyle(
            stripeColor = palette.outline,
            stripeWidth = 1.dp,
            spacing = scaled(10f),
            background = palette.surface,
            borderColor = palette.outlineMuted,
            borderWidth = 1.dp,
            shape = containerShape,
            contentPadding = PaddingValues(horizontal = scaled(16f), vertical = scaled(13f)),
        ),
        code = OrcaCodeBlockStyle(
            text = TextStyle(
                fontSize = 14.sp,
                lineHeight = 21.sp,
                fontFamily = monospace,
                color = palette.syntax.text,
            ),
            languageLabel = OrcaCodeLabelStyle(
                text = labelText,
                background = Color.Transparent,
                shape = chipShape,
                padding = PaddingValues(horizontal = scaled(8f), vertical = scaled(3f)),
            ),
            copyButton = OrcaCodeActionStyle(
                text = labelText.copy(color = palette.accent, fontWeight = FontWeight.SemiBold),
                background = Color.Transparent,
                shape = chipShape,
                padding = PaddingValues(horizontal = scaled(8f), vertical = scaled(3f)),
            ),
            lineNumber = TextStyle(
                fontSize = 12.sp,
                lineHeight = 21.sp,
                fontFamily = monospace,
                color = palette.syntax.lineNumber,
            ),
            background = palette.syntax.surface,
            borderColor = palette.outlineMuted,
            borderWidth = 1.dp,
            shape = containerShape,
            padding = PaddingValues(scaled(13f)),
            showLineNumbers = showLineNumbers,
            lineNumberMinWidth = scaled(28f),
            lineNumberEndPadding = scaled(12f),
            showCopyButton = true,
            syntaxHighlightingEnabled = true,
            highlightKeyword = SpanStyle(color = palette.syntax.keyword, fontWeight = FontWeight.SemiBold),
            highlightString = SpanStyle(color = palette.syntax.string),
            highlightComment = SpanStyle(color = palette.syntax.comment, fontStyle = FontStyle.Italic),
            highlightNumber = SpanStyle(color = palette.syntax.number),
        ),
        table = OrcaTableStyle(
            text = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, color = palette.text),
            headerText = TextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
                color = palette.text,
            ),
            cellPadding = PaddingValues(horizontal = scaled(12f), vertical = scaled(9f)),
            borderColor = palette.outlineMuted,
            borderWidth = 1.dp,
            headerBackground = palette.surfaceMuted,
            rowBackground = palette.background,
            alternateRowBackground = if (zebraTables) palette.surface else palette.background,
            containerShape = containerShape,
            outerBorderColor = palette.outlineMuted,
            showScrollIndicator = true,
            scrollTrackColor = palette.surfaceMuted,
            scrollIndicatorColor = palette.accent,
            scrollIndicatorSpacing = scaled(6f),
        ),
        thematicBreak = OrcaThematicBreakStyle(
            color = palette.outlineMuted,
            thickness = 1.dp,
        ),
        image = OrcaImageStyle(
            shape = containerShape,
            background = palette.surfaceMuted,
            captionText = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, color = palette.textMuted),
            captionSpacing = scaled(6f),
        ),
        admonition = OrcaAdmonitionStyle(
            stripeWidth = 1.dp,
            spacing = scaled(10f),
            titleStyle = TextStyle(
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            ),
            iconSpacing = scaled(7f),
            noteColor = palette.signal.note,
            tipColor = palette.signal.tip,
            importantColor = palette.signal.important,
            warningColor = palette.signal.warning,
            cautionColor = palette.signal.caution,
            noteBackground = palette.signal.note.copy(alpha = palette.signal.surfaceAlpha),
            tipBackground = palette.signal.tip.copy(alpha = palette.signal.surfaceAlpha),
            importantBackground = palette.signal.important.copy(alpha = palette.signal.surfaceAlpha),
            warningBackground = palette.signal.warning.copy(alpha = palette.signal.surfaceAlpha),
            cautionBackground = palette.signal.caution.copy(alpha = palette.signal.surfaceAlpha),
            borderWidth = 1.dp,
            shape = containerShape,
            contentPadding = PaddingValues(scaled(13f)),
        ),
        inlineImage = OrcaInlineImageStyle(
            shape = RoundedCornerShape(3.dp),
        ),
        definitionList = OrcaDefinitionListStyle(
            termStyle = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
            ),
            definitionIndent = scaled(16f),
            termSpacing = scaled(8f),
            definitionSpacing = scaled(4f),
        ),
        details = OrcaDetailsStyle(
            summaryStyle = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                color = palette.text,
            ),
            borderColor = palette.outlineMuted,
            borderWidth = 1.dp,
            shape = containerShape,
            background = palette.surface,
            contentPadding = PaddingValues(scaled(13f)),
        ),
        task = OrcaTaskStyle(
            size = 18.dp,
            touchTargetSize = 40.dp,
            shape = RoundedCornerShape(4.dp),
            borderWidth = 1.dp,
            checkedBackground = palette.accent,
            uncheckedBackground = Color.Transparent,
            checkedBorderColor = palette.accent,
            uncheckedBorderColor = palette.outline,
            checkColor = palette.onAccent,
        ),
    )
}
