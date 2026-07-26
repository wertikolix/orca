package ru.wertik.orca.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.wertik.orca.compose.OrcaDensity
import ru.wertik.orca.compose.OrcaPalette
import ru.wertik.orca.compose.OrcaRootLayout
import ru.wertik.orca.compose.OrcaStyle
import ru.wertik.orca.core.OrcaMarkdownParser

/**
 * Live documentation of the 0.30 token system.
 *
 * Every swatch is the exact value the renderer uses, read back from the active [OrcaPalette],
 * so the page cannot drift from the library.
 */
@Composable
internal fun DesignScreen(
    screen: SampleScreen,
    parser: OrcaMarkdownParser,
    style: OrcaStyle,
    appearance: SampleAppearance,
    onLinkClick: (String) -> Unit,
) {
    val palette = appearance.palette()
    val swatches = remember(palette) { paletteSwatches(palette) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableWidth = maxWidth
        val horizontalPadding = if (availableWidth >= 720.dp) 32.dp else 18.dp
        val statColumns = if (availableWidth >= 620.dp) 4 else 2
        val columns = when {
            availableWidth >= 900.dp -> 4
            availableWidth >= 560.dp -> 3
            else -> 2
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 940.dp)
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "header") {
                    SectionHeader(screen = screen)
                }
                item(key = "summary") {
                    StatStrip(
                        stats = listOf(
                            SampleStat("Palette", appearance.styleSource.label),
                            SampleStat("Mode", if (palette.isDark) "Dark" else "Light"),
                            SampleStat("Density", appearance.density.label()),
                            SampleStat("Spacing", "${appearance.density.spacingScale}x"),
                        ),
                        columns = statColumns,
                    )
                }
                item(key = "swatch-label") {
                    MetaLabel(text = "COLOR TOKENS")
                }
                items(
                    count = (swatches.size + columns - 1) / columns,
                    key = { row -> "swatch-row-$row" },
                ) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val rowSwatches = swatches.drop(row * columns).take(columns)
                        rowSwatches.forEach { swatch ->
                            SwatchCell(swatch = swatch, modifier = Modifier.weight(1f))
                        }
                        repeat(columns - rowSwatches.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
                item(key = "density-label") {
                    MetaLabel(text = "DENSITY SCALE")
                }
                item(key = "density") {
                    FlatPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OrcaDensity.entries.forEach { density ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = density.label(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (density == appearance.density) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.widthIn(min = 76.dp),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height((15 * density.spacingScale).dp)
                                            .background(
                                                if (density == appearance.density) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                },
                                            ),
                                    )
                                    MetaLabel(text = "${density.spacingScale}x")
                                }
                            }
                        }
                    }
                }
                item(key = "document-label") {
                    MetaLabel(text = "RENDERED WITH THESE TOKENS")
                }
                item(key = "document") {
                    MarkdownRenderer(
                        markdown = DESIGN_MARKDOWN,
                        parser = parser,
                        cacheKey = "design-tokens",
                        style = style,
                        modifier = Modifier.fillMaxWidth(),
                        rootLayout = OrcaRootLayout.COLUMN,
                        onLinkClick = onLinkClick,
                    )
                }
            }
        }
    }
}

private data class Swatch(val name: String, val color: Color, val onColor: Color)

private fun paletteSwatches(palette: OrcaPalette): List<Swatch> = listOf(
    Swatch("background", palette.background, palette.text),
    Swatch("surface", palette.surface, palette.text),
    Swatch("surfaceMuted", palette.surfaceMuted, palette.text),
    Swatch("surfaceStrong", palette.surfaceStrong, palette.text),
    Swatch("outline", palette.outline, palette.background),
    Swatch("outlineMuted", palette.outlineMuted, palette.text),
    Swatch("text", palette.text, palette.background),
    Swatch("textMuted", palette.textMuted, palette.background),
    Swatch("accent", palette.accent, palette.onAccent),
    Swatch("code surface", palette.syntax.surface, palette.syntax.text),
    Swatch("keyword", palette.syntax.keyword, palette.background),
    Swatch("string", palette.syntax.string, palette.background),
    Swatch("note", palette.signal.note, palette.background),
    Swatch("tip", palette.signal.tip, palette.background),
    Swatch("warning", palette.signal.warning, palette.background),
    Swatch("caution", palette.signal.caution, palette.background),
)

@Composable
private fun SwatchCell(swatch: Swatch, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(swatch.color, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aa",
                style = MaterialTheme.typography.labelMedium,
                color = swatch.onColor,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = swatch.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            MetaLabel(text = swatch.color.toHex())
        }
    }
}

private fun Color.toHex(): String {
    fun channel(value: Float): String {
        val intValue = (value * 255f).toInt().coerceIn(0, 255)
        return intValue.toString(16).uppercase().padStart(2, '0')
    }
    return "#${channel(alpha)}${channel(red)}${channel(green)}${channel(blue)}"
}
