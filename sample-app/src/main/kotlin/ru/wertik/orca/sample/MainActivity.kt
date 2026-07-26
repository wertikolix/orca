package ru.wertik.orca.sample

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import ru.wertik.orca.compose.OrcaDensity
import ru.wertik.orca.compose.OrcaStyle
import ru.wertik.orca.core.OrcaMarkdownParser

class OrcaSampleApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val systemDark = isSystemInDarkTheme()
            var dark by rememberSaveable { mutableStateOf(systemDark) }
            var styleSource by rememberSaveable { mutableStateOf(SampleStyleSource.FLAT) }
            var density by rememberSaveable { mutableStateOf(OrcaDensity.COMFORTABLE) }
            val appearance = SampleAppearance(dark = dark, styleSource = styleSource, density = density)

            LaunchedEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (dark) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    },
                    navigationBarStyle = if (dark) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    },
                )
            }

            OrcaSampleTheme(appearance = appearance) {
                OrcaSampleApp(
                    appearance = appearance,
                    onToggleTheme = { dark = !dark },
                    onStyleSourceChange = { styleSource = it },
                    onDensityChange = { density = it },
                )
            }
        }
    }
}

internal enum class SampleScreen(
    val category: String,
    val title: String,
    val shortLabel: String,
    val description: String,
    val meta: String,
) {
    OVERVIEW("Foundation", "Reader", "Read", "Long-form Markdown with search, anchors, stats, and interactive tasks", "Static document"),
    DESIGN("System", "Design", "Tokens", "The 0.30 flat palette, density scale, and heading rules, read from the live style", "Token preview"),
    BLOCKS("Syntax", "Blocks", "Blocks", "Callouts, quotes, code, and the full inline vocabulary", "Rich blocks"),
    TABLES("Layout", "Tables", "Data", "Responsive columns, alignment, semantics, and overflow feedback", "Auto layout"),
    MEDIA("Security", "Media", "Media", "Markdown images plus strict HTML img and figure rendering", "Policy gated"),
    MATH("Optional", "Math", "Math", "Native block and inline LaTeX through the Orcex adapter", "Native layout"),
    ADVANCED("Documents", "Extended", "More", "Details, definitions, abbreviations, and footnote navigation", "Nested content"),
    RENDERERS("Extension", "Renderers", "API", "Exact-class inline overrides beside composable block renderers", "Custom output"),
    STREAMING("Realtime", "Streaming", "Stream", "Pause, replay, and inspect paced incremental rendering", "Live session"),
    PLAYGROUND("Workbench", "Playground", "Edit", "Edit source in a responsive split view and inspect the result", "Live preview"),
}

@Composable
private fun OrcaSampleApp(
    appearance: SampleAppearance,
    onToggleTheme: () -> Unit,
    onStyleSourceChange: (SampleStyleSource) -> Unit,
    onDensityChange: (OrcaDensity) -> Unit,
) {
    val parser = remember { OrcaMarkdownParser() }
    val style = rememberSampleOrcaStyle(appearance)
    val screens = remember { SampleScreen.entries }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var controlsExpanded by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val wideLayout = maxWidth >= 900.dp
        val inlineControls = maxWidth >= 680.dp
        Column(modifier = Modifier.fillMaxSize()) {
            CommandBar(
                appearance = appearance,
                inlineControls = inlineControls,
                controlsExpanded = controlsExpanded,
                onToggleControls = { controlsExpanded = !controlsExpanded },
                onToggleTheme = onToggleTheme,
                onStyleSourceChange = onStyleSourceChange,
                onDensityChange = onDensityChange,
            )
            HairLine()
            if (!inlineControls && controlsExpanded) {
                AppearanceControls(
                    appearance = appearance,
                    onStyleSourceChange = onStyleSourceChange,
                    onDensityChange = onDensityChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                HairLine()
            }
            if (wideLayout) {
                Row(modifier = Modifier.fillMaxSize()) {
                    SuiteRail(
                        screens = screens,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    ScreenContent(
                        screen = screens[selectedIndex],
                        parser = parser,
                        style = style,
                        appearance = appearance,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    SuiteTabs(
                        screens = screens,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it },
                    )
                    HairLine()
                    ScreenContent(
                        screen = screens[selectedIndex],
                        parser = parser,
                        style = style,
                        appearance = appearance,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HairLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun CommandBar(
    appearance: SampleAppearance,
    inlineControls: Boolean,
    controlsExpanded: Boolean,
    onToggleControls: () -> Unit,
    onToggleTheme: () -> Unit,
    onStyleSourceChange: (SampleStyleSource) -> Unit,
    onDensityChange: (OrcaDensity) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 480.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "O",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    MetaLabel(
                        text = "ORCA LAB  ·  v${BuildConfig.VERSION_NAME}",
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (compact) "Render workbench" else "Markdown render workbench",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (inlineControls) {
                AppearanceControls(
                    appearance = appearance,
                    onStyleSourceChange = onStyleSourceChange,
                    onDensityChange = onDensityChange,
                )
            } else {
                FlatIconButton(
                    icon = Icons.Default.Tune,
                    contentDescription = if (controlsExpanded) "Hide appearance controls" else "Show appearance controls",
                    onClick = onToggleControls,
                )
            }
            FlatIconButton(
                icon = if (appearance.dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (appearance.dark) "Use light theme" else "Use dark theme",
                onClick = onToggleTheme,
            )
        }
    }
}

@Composable
private fun AppearanceControls(
    appearance: SampleAppearance,
    onStyleSourceChange: (SampleStyleSource) -> Unit,
    onDensityChange: (OrcaDensity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlatChipGroup(
            options = SampleStyleSource.entries,
            selected = appearance.styleSource,
            label = { source -> source.label },
            onSelect = onStyleSourceChange,
        )
        FlatChip(
            label = appearance.density.label(),
            selected = appearance.density != OrcaDensity.COMFORTABLE,
            role = Role.Button,
            onClick = { onDensityChange(appearance.density.next()) },
        )
    }
}

@Composable
private fun SuiteRail(
    screens: List<SampleScreen>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(248.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        MetaLabel(
            text = "RENDER SUITES",
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 11.dp),
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(screens, key = { _, screen -> screen.name }) { index, screen ->
                RailItem(
                    index = index,
                    screen = screen,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                )
            }
        }
        HairLine()
        Row(
            modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp)),
            )
            Text(
                text = "${screens.size} suites · flat UI",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RailItem(
    index: Int,
    screen: SampleScreen,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.surfaceContainerHigh else Color.Transparent, shape)
            .border(1.dp, if (selected) colors.outline else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (index + 1).toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = if (selected) colors.primary else colors.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = screen.title,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface,
            )
            Text(
                text = screen.category,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SuiteTabs(
    screens: List<SampleScreen>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val state = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        state.animateScrollToItem(selectedIndex)
    }
    LazyRow(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        itemsIndexed(screens, key = { _, screen -> screen.name }) { index, screen ->
            FlatChip(
                label = screen.shortLabel,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun ScreenContent(
    screen: SampleScreen,
    parser: OrcaMarkdownParser,
    style: OrcaStyle,
    appearance: SampleAppearance,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val onLinkClick: (String) -> Unit = remember(context) {
        { link -> Toast.makeText(context, link, Toast.LENGTH_SHORT).show() }
    }
    Crossfade(
        targetState = screen,
        animationSpec = tween(durationMillis = 160),
        label = "render-suite",
        modifier = modifier.fillMaxSize(),
    ) { target ->
        when (target) {
            SampleScreen.DESIGN -> DesignScreen(
                screen = target,
                parser = parser,
                style = style,
                appearance = appearance,
                onLinkClick = onLinkClick,
            )

            SampleScreen.STREAMING -> StreamingScreen(
                parser = parser,
                style = style,
                screen = target,
                onLinkClick = onLinkClick,
            )

            SampleScreen.PLAYGROUND -> PlaygroundScreen(
                parser = parser,
                style = style,
                screen = target,
                onLinkClick = onLinkClick,
            )

            else -> DocumentScreen(
                screen = target,
                parser = parser,
                style = style,
                onLinkClick = onLinkClick,
            )
        }
    }
}
