package ru.wertik.orca.compose.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import ru.wertik.orca.compose.OrcaAdmonitionStyle
import ru.wertik.orca.compose.OrcaCodeActionStyle
import ru.wertik.orca.compose.OrcaCodeBlockStyle
import ru.wertik.orca.compose.OrcaCodeLabelStyle
import ru.wertik.orca.compose.OrcaDefaults
import ru.wertik.orca.compose.OrcaDefinitionListStyle
import ru.wertik.orca.compose.OrcaDetailsStyle
import ru.wertik.orca.compose.OrcaImageStyle
import ru.wertik.orca.compose.OrcaInlineStyle
import ru.wertik.orca.compose.OrcaQuoteStyle
import ru.wertik.orca.compose.OrcaStyle
import ru.wertik.orca.compose.OrcaTableStyle
import ru.wertik.orca.compose.OrcaThematicBreakStyle
import ru.wertik.orca.compose.OrcaTypographyStyle

/**
 * Remembers an [OrcaStyle] derived from the active Material 3 theme.
 *
 * Use this in Material apps so Markdown surfaces automatically follow the app's light or dark
 * color scheme, typography, and shapes.
 */
@Composable
fun rememberOrcaMaterialStyle(): OrcaStyle {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    return remember(colorScheme, typography, shapes) {
        OrcaDefaults.materialStyle(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
        )
    }
}

/**
 * Builds an [OrcaStyle] from Material 3 tokens without requiring composition.
 */
fun OrcaDefaults.materialStyle(
    colorScheme: ColorScheme,
    typography: Typography = Typography(),
    shapes: Shapes = Shapes(),
): OrcaStyle = OrcaStyle(
    typography = OrcaTypographyStyle(
        heading1 = typography.headlineLarge.copy(color = colorScheme.onSurface),
        heading2 = typography.headlineMedium.copy(color = colorScheme.onSurface),
        heading3 = typography.headlineSmall.copy(color = colorScheme.onSurface),
        heading4 = typography.titleLarge.copy(color = colorScheme.onSurface),
        heading5 = typography.titleMedium.copy(color = colorScheme.onSurface),
        heading6 = typography.titleSmall.copy(color = colorScheme.onSurface),
        paragraph = typography.bodyLarge.copy(color = colorScheme.onSurface),
    ),
    inline = OrcaInlineStyle(
        inlineCode = SpanStyle(
            fontFamily = FontFamily.Monospace,
            background = colorScheme.surfaceVariant,
        ),
        link = SpanStyle(
            color = colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
        footnoteReference = SpanStyle(
            baselineShift = BaselineShift.Superscript,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant,
        ),
        abbreviation = SpanStyle(
            textDecoration = TextDecoration.Underline,
            background = colorScheme.surfaceVariant,
        ),
        highlight = SpanStyle(
            background = colorScheme.secondary.copy(alpha = 0.3f),
        ),
    ),
    quote = OrcaQuoteStyle(
        stripeColor = colorScheme.outline,
    ),
    code = OrcaCodeBlockStyle(
        text = typography.bodyMedium.copy(
            color = colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
        ),
        languageLabel = OrcaCodeLabelStyle(
            text = typography.labelSmall.copy(
                color = colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            ),
            background = colorScheme.surfaceVariant,
            shape = shapes.small,
        ),
        copyButton = OrcaCodeActionStyle(
            text = typography.labelSmall.copy(
                color = colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            ),
            background = colorScheme.surfaceVariant,
            shape = shapes.small,
        ),
        lineNumber = typography.labelSmall.copy(
            color = colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        ),
        background = colorScheme.surface,
        borderColor = colorScheme.outlineVariant,
        shape = shapes.medium,
        highlightKeyword = SpanStyle(color = colorScheme.primary, fontWeight = FontWeight.SemiBold),
        highlightString = SpanStyle(color = colorScheme.tertiary),
        highlightComment = SpanStyle(color = colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic),
        highlightNumber = SpanStyle(color = colorScheme.secondary),
    ),
    table = OrcaTableStyle(
        text = typography.bodyMedium.copy(color = colorScheme.onSurface),
        headerText = typography.labelLarge.copy(color = colorScheme.onSurface),
        borderColor = colorScheme.outlineVariant,
        headerBackground = colorScheme.surfaceVariant,
        rowBackground = colorScheme.surface,
        alternateRowBackground = colorScheme.surfaceVariant.copy(alpha = 0.35f),
        containerShape = shapes.medium,
        outerBorderColor = colorScheme.outlineVariant,
    ),
    thematicBreak = OrcaThematicBreakStyle(
        color = colorScheme.outlineVariant,
    ),
    image = OrcaImageStyle(
        shape = shapes.medium,
        background = colorScheme.surfaceVariant,
    ),
    admonition = OrcaAdmonitionStyle(
        noteColor = colorScheme.primary,
        tipColor = colorScheme.tertiary,
        importantColor = colorScheme.secondary,
        warningColor = colorScheme.tertiary,
        cautionColor = colorScheme.error,
        noteBackground = colorScheme.primary.copy(alpha = 0.12f),
        tipBackground = colorScheme.tertiary.copy(alpha = 0.12f),
        importantBackground = colorScheme.secondary.copy(alpha = 0.12f),
        warningBackground = colorScheme.tertiary.copy(alpha = 0.12f),
        cautionBackground = colorScheme.error.copy(alpha = 0.12f),
    ),
    definitionList = OrcaDefinitionListStyle(
        termStyle = typography.bodyLarge.copy(
            color = colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        ),
    ),
    details = OrcaDetailsStyle(
        summaryStyle = typography.titleMedium.copy(color = colorScheme.onSurface),
        borderColor = colorScheme.outlineVariant,
        background = colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ),
)
