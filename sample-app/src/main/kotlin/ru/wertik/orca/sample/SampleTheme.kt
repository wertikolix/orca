package ru.wertik.orca.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.wertik.orca.compose.OrcaDensity
import ru.wertik.orca.compose.OrcaPalette
import ru.wertik.orca.compose.OrcaPalettes
import ru.wertik.orca.compose.OrcaStyle
import ru.wertik.orca.compose.orcaFlatStyle
import ru.wertik.orca.compose.material3.rememberOrcaMaterialStyle

/** Which style pipeline renders the Markdown document. */
internal enum class SampleStyleSource(val label: String, val description: String) {
    FLAT("Flat", "orcaFlatStyle with the built-in warm neutral palette"),
    CONTRAST("Contrast", "orcaFlatStyle with the high-contrast palette"),
    MATERIAL("Material", "rememberOrcaMaterialStyle mapped from the app color scheme"),
}

/** Everything the render lab lets the user switch at runtime. */
internal data class SampleAppearance(
    val dark: Boolean,
    val styleSource: SampleStyleSource = SampleStyleSource.FLAT,
    val density: OrcaDensity = OrcaDensity.COMFORTABLE,
)

/**
 * The palette that drives both the app chrome and the rendered document.
 *
 * Keeping one token source for the shell and the renderer is the point of the 0.30 system: the
 * lab never invents colors that the library could not produce.
 */
internal fun SampleAppearance.palette(): OrcaPalette = when (styleSource) {
    SampleStyleSource.CONTRAST -> if (dark) OrcaPalettes.ContrastDark else OrcaPalettes.ContrastLight
    else -> if (dark) OrcaPalettes.FlatDark else OrcaPalettes.FlatLight
}

private val SampleTypography = Typography(
    headlineSmall = TextStyle(
        fontSize = 25.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
internal fun OrcaSampleTheme(
    appearance: SampleAppearance,
    content: @Composable () -> Unit,
) {
    val palette = appearance.palette()
    val colorScheme = remember(palette) {
        val base = if (palette.isDark) darkColorScheme() else lightColorScheme()
        base.copy(
            background = palette.background,
            onBackground = palette.text,
            surface = palette.background,
            onSurface = palette.text,
            surfaceContainerLowest = palette.background,
            surfaceContainerLow = palette.surface,
            surfaceContainer = palette.surface,
            surfaceContainerHigh = palette.surfaceMuted,
            surfaceContainerHighest = palette.surfaceStrong,
            surfaceVariant = palette.surfaceMuted,
            onSurfaceVariant = palette.textMuted,
            primary = palette.accent,
            onPrimary = palette.onAccent,
            primaryContainer = palette.accentSurface,
            onPrimaryContainer = palette.text,
            secondary = palette.signal.tip,
            onSecondary = palette.onAccent,
            tertiary = palette.signal.important,
            outline = palette.outline,
            outlineVariant = palette.outlineMuted,
            error = palette.signal.caution,
            onError = palette.onAccent,
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SampleTypography,
        content = content,
    )
}

/** Builds the [OrcaStyle] for the current appearance, exercising both style pipelines. */
@Composable
internal fun rememberSampleOrcaStyle(appearance: SampleAppearance): OrcaStyle {
    val materialStyle = rememberOrcaMaterialStyle(density = appearance.density)
    val palette = appearance.palette()
    return remember(appearance, palette, materialStyle) {
        when (appearance.styleSource) {
            SampleStyleSource.MATERIAL -> materialStyle
            else -> orcaFlatStyle(palette = palette, density = appearance.density)
        }
    }
}

internal fun OrcaDensity.label(): String = when (this) {
    OrcaDensity.COMPACT -> "Compact"
    OrcaDensity.COMFORTABLE -> "Cozy"
    OrcaDensity.SPACIOUS -> "Roomy"
}

internal fun OrcaDensity.next(): OrcaDensity = when (this) {
    OrcaDensity.COMPACT -> OrcaDensity.COMFORTABLE
    OrcaDensity.COMFORTABLE -> OrcaDensity.SPACIOUS
    OrcaDensity.SPACIOUS -> OrcaDensity.COMPACT
}
