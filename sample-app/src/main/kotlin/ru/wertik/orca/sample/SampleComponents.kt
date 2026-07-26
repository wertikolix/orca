package ru.wertik.orca.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Flat container: a solid fill plus a one-pixel outline.
 *
 * The sample intentionally has no elevation, shadow, or gradient helper — structure comes from
 * outlines, fills, and spacing only.
 */
@Composable
internal fun FlatPanel(
    modifier: Modifier = Modifier,
    fill: Color = MaterialTheme.colorScheme.surfaceContainer,
    outline: Color = MaterialTheme.colorScheme.outlineVariant,
    cornerRadius: Int = 10,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .background(fill, shape)
            .border(1.dp, outline, shape),
    ) {
        content()
    }
}

/** Uppercase monospace section label used above groups of controls. */
@Composable
internal fun MetaLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.9.sp,
        ),
        color = color,
    )
}

/** Outlined pill used for tabs, filters, and single-choice options. */
@Composable
internal fun FlatChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    role: Role = Role.Tab,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val contentColor = when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = 0.5f)
        selected -> colors.onSurface
        else -> colors.onSurfaceVariant
    }
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .background(if (selected) colors.surfaceContainerHigh else Color.Transparent, shape)
            .border(1.dp, if (selected) colors.outline else colors.outlineVariant, shape)
            .selectable(selected = selected, enabled = enabled, role = role, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = contentColor,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** Outlined action button. */
@Composable
internal fun FlatButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    emphasis: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    val fill = when {
        !enabled -> Color.Transparent
        emphasis -> colors.primaryContainer
        else -> Color.Transparent
    }
    val contentColor = when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = 0.5f)
        emphasis -> colors.primary
        else -> colors.onSurface
    }
    Row(
        modifier = modifier
            .background(fill, shape)
            .border(1.dp, if (enabled) colors.outlineVariant else colors.outlineVariant.copy(alpha = 0.6f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = if (enabled) colors.primary else contentColor,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** Square outlined icon button, sized for a comfortable touch target. */
@Composable
internal fun FlatIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(40.dp)
            .border(1.dp, colors.outlineVariant, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = if (enabled) colors.onSurfaceVariant else colors.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

/** Single-choice row of chips. */
@Composable
internal fun <T> FlatChipGroup(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            FlatChip(
                label = label(option),
                selected = option == selected,
                role = Role.RadioButton,
                onClick = { onSelect(option) },
            )
        }
    }
}

/** One measurement inside a [StatStrip]. */
internal data class SampleStat(val label: String, val value: String)

/**
 * Row of measurements separated by one-pixel dividers.
 *
 * Wraps into a second line on narrow screens instead of scrolling.
 */
@Composable
internal fun StatStrip(
    stats: List<SampleStat>,
    modifier: Modifier = Modifier,
    columns: Int = stats.size.coerceAtMost(4),
) {
    if (stats.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val safeColumns = columns.coerceAtLeast(1)
    FlatPanel(modifier = modifier.fillMaxWidth()) {
        Column {
            stats.chunked(safeColumns).forEachIndexed { rowIndex, rowStats ->
                if (rowIndex > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.outlineVariant),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                ) {
                    rowStats.forEachIndexed { index, stat ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(colors.outlineVariant),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stat.value,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            MetaLabel(text = stat.label.uppercase())
                        }
                    }
                    repeat(safeColumns - rowStats.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Flat determinate progress track: two solid rectangles, no gradient. */
@Composable
internal fun FlatProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(indicatorColor),
        )
    }
}
