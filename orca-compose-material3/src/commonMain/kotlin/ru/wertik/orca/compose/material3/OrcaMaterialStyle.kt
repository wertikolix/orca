package ru.wertik.orca.compose.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ru.wertik.orca.compose.OrcaDefaults
import ru.wertik.orca.compose.OrcaDensity
import ru.wertik.orca.compose.OrcaPalette
import ru.wertik.orca.compose.OrcaSignalPalette
import ru.wertik.orca.compose.OrcaStyle
import ru.wertik.orca.compose.OrcaSyntaxPalette
import ru.wertik.orca.compose.OrcaTypographyStyle
import ru.wertik.orca.compose.orcaFlatStyle

/**
 * Remembers an [OrcaStyle] derived from the active Material 3 theme.
 *
 * Use this in Material apps so Markdown surfaces automatically follow the app's light or dark
 * color scheme, typography, and shapes. Since 0.30 the adapter maps the scheme into an
 * [OrcaPalette] and builds the style through [orcaFlatStyle], so Material hosts get the same
 * flat, shadow-free render system as the built-in palettes.
 *
 * @param density spacing scale applied to block gaps and container padding.
 * @param headingRules whether H1 and H2 render a one-pixel rule underneath.
 */
@Composable
fun rememberOrcaMaterialStyle(
    density: OrcaDensity = OrcaDensity.COMFORTABLE,
    headingRules: Boolean = true,
): OrcaStyle {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    return remember(colorScheme, typography, shapes, density, headingRules) {
        OrcaDefaults.materialStyle(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            density = density,
            headingRules = headingRules,
        )
    }
}

/**
 * Maps a Material 3 [ColorScheme] into flat Orca color tokens.
 *
 * @param isDark whether the scheme is dark. Inferred from background luminance by default.
 */
fun OrcaDefaults.materialPalette(
    colorScheme: ColorScheme,
    isDark: Boolean = colorScheme.background.luminance() < 0.5f,
): OrcaPalette = OrcaPalette(
    background = colorScheme.background,
    surface = colorScheme.surfaceContainer,
    surfaceMuted = colorScheme.surfaceContainerHigh,
    surfaceStrong = colorScheme.surfaceContainerHighest,
    outline = colorScheme.outline,
    outlineMuted = colorScheme.outlineVariant,
    text = colorScheme.onSurface,
    textMuted = colorScheme.onSurfaceVariant,
    accent = colorScheme.primary,
    onAccent = colorScheme.onPrimary,
    accentSurface = colorScheme.primary.copy(alpha = 0.12f),
    highlight = colorScheme.tertiary.copy(alpha = if (isDark) 0.34f else 0.28f),
    searchMatch = colorScheme.primary.copy(alpha = if (isDark) 0.34f else 0.26f),
    syntax = OrcaSyntaxPalette(
        surface = colorScheme.surfaceContainerLow,
        text = colorScheme.onSurface,
        lineNumber = colorScheme.onSurfaceVariant,
        keyword = colorScheme.primary,
        string = colorScheme.tertiary,
        comment = colorScheme.onSurfaceVariant,
        number = colorScheme.secondary,
    ),
    signal = OrcaSignalPalette(
        note = colorScheme.primary,
        tip = colorScheme.tertiary,
        important = colorScheme.secondary,
        warning = colorScheme.tertiary,
        caution = colorScheme.error,
        surfaceAlpha = if (isDark) 0.16f else 0.10f,
    ),
    isDark = isDark,
)

/**
 * Builds an [OrcaStyle] from Material 3 tokens without requiring composition.
 *
 * @param density spacing scale applied to block gaps and container padding.
 * @param headingRules whether H1 and H2 render a one-pixel rule underneath.
 * @param isDark whether the scheme is dark. Inferred from background luminance by default.
 */
fun OrcaDefaults.materialStyle(
    colorScheme: ColorScheme,
    typography: Typography = Typography(),
    shapes: Shapes = Shapes(),
    density: OrcaDensity = OrcaDensity.COMFORTABLE,
    headingRules: Boolean = true,
    isDark: Boolean = colorScheme.background.luminance() < 0.5f,
): OrcaStyle {
    val palette = materialPalette(colorScheme = colorScheme, isDark = isDark)
    val style = orcaFlatStyle(
        palette = palette,
        density = density,
        containerShape = shapes.medium,
        chipShape = shapes.small,
        typography = OrcaTypographyStyle(
            heading1 = typography.headlineLarge.copy(color = colorScheme.onSurface),
            heading2 = typography.headlineMedium.copy(color = colorScheme.onSurface),
            heading3 = typography.headlineSmall.copy(color = colorScheme.onSurface),
            heading4 = typography.titleLarge.copy(color = colorScheme.onSurface),
            heading5 = typography.titleMedium.copy(color = colorScheme.onSurface),
            heading6 = typography.titleSmall.copy(color = colorScheme.onSurface),
            paragraph = typography.bodyLarge.copy(color = colorScheme.onSurface),
        ),
        headingRuleLevels = if (headingRules) setOf(1, 2) else emptySet(),
    )
    return style.copy(
        code = style.code.copy(
            text = typography.bodyMedium.copy(
                color = colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
            ),
            languageLabel = style.code.languageLabel.copy(
                text = typography.labelSmall.copy(
                    color = colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                ),
            ),
            copyButton = style.code.copyButton.copy(
                text = typography.labelSmall.copy(
                    color = colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                ),
            ),
            lineNumber = typography.labelSmall.copy(
                color = colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            ),
        ),
        table = style.table.copy(
            text = typography.bodyMedium.copy(color = colorScheme.onSurface),
            headerText = typography.labelLarge.copy(color = colorScheme.onSurface),
        ),
        definitionList = style.definitionList.copy(
            termStyle = typography.bodyLarge.copy(
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
        ),
        details = style.details.copy(
            summaryStyle = typography.titleMedium.copy(color = colorScheme.onSurface),
        ),
        image = style.image.copy(
            captionText = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
        ),
        task = style.task.copy(shape = shapes.extraSmall),
    )
}
