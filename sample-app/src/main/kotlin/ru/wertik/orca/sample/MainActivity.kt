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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.wertik.orca.compose.Orca
import ru.wertik.orca.compose.OrcaInlineRenderer
import ru.wertik.orca.compose.OrcaRootLayout
import ru.wertik.orca.compose.OrcaSecurityPolicies
import ru.wertik.orca.compose.OrcaStyle
import ru.wertik.orca.compose.material3.rememberOrcaMaterialStyle
import ru.wertik.orca.compose.orcaHeadingBlockIndex
import ru.wertik.orca.compose.rememberOrcaStreamingState
import ru.wertik.orca.core.OrcaIncrementalParserSession
import ru.wertik.orca.core.OrcaInline
import ru.wertik.orca.core.OrcaMarkdownParser
import ru.wertik.orca.core.tableOfContents
import ru.wertik.orca.images.coil.OrcaCoilImage
import ru.wertik.orca.images.coil.OrcaCoilInlineImage
import ru.wertik.orca.math.orcex.OrcaOrcexMath
import ru.wertik.orca.math.orcex.rememberOrcaOrcexInlineMathPlaceholder
import ru.wertik.orcex.font.stix2.StixTwoMath
import kotlin.reflect.KClass

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
            var isDark by rememberSaveable { mutableStateOf(systemDark) }

            LaunchedEffect(isDark) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    },
                )
            }

            OrcaSampleTheme(isDark = isDark) {
                OrcaSampleApp(
                    isDark = isDark,
                    onToggleTheme = { isDark = !isDark },
                )
            }
        }
    }
}

private val LightColors = lightColorScheme(
    background = Color(0xFFF4F2EC),
    surface = Color(0xFFF8F6F1),
    surfaceContainer = Color(0xFFEDEAE2),
    surfaceContainerHigh = Color(0xFFE4E0D7),
    onSurface = Color(0xFF20211E),
    onSurfaceVariant = Color(0xFF5E615B),
    primary = Color(0xFF2F6263),
    onPrimary = Color(0xFFF2F8F6),
    secondary = Color(0xFF5D6456),
    onSecondary = Color(0xFFF3F5EF),
    outline = Color(0xFFAAA9A1),
    outlineVariant = Color(0xFFD4D0C7),
    error = Color(0xFF9C413D),
    onError = Color(0xFFFFF3F0),
)

private val DarkColors = darkColorScheme(
    background = Color(0xFF141512),
    surface = Color(0xFF191A17),
    surfaceContainer = Color(0xFF20211D),
    surfaceContainerHigh = Color(0xFF292A25),
    onSurface = Color(0xFFE8E5DD),
    onSurfaceVariant = Color(0xFFB0AEA6),
    primary = Color(0xFFA5CECC),
    onPrimary = Color(0xFF173536),
    secondary = Color(0xFFC4CBB8),
    onSecondary = Color(0xFF2D3328),
    outline = Color(0xFF74756E),
    outlineVariant = Color(0xFF3B3D37),
    error = Color(0xFFFFB4AC),
    onError = Color(0xFF5F1412),
)

private val SampleTypography = Typography(
    headlineSmall = TextStyle(
        fontSize = 25.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.25).sp,
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
private fun OrcaSampleTheme(isDark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        typography = SampleTypography,
        content = content,
    )
}

internal enum class SampleScreen(
    val category: String,
    val title: String,
    val shortLabel: String,
    val description: String,
    val meta: String,
) {
    OVERVIEW("Foundation", "Reader", "Read", "Long-form Markdown, anchors, code actions, and interactive tasks", "Static document"),
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
    isDark: Boolean,
    onToggleTheme: () -> Unit,
) {
    val parser = remember { OrcaMarkdownParser() }
    val baseStyle = rememberOrcaMaterialStyle()
    val colors = MaterialTheme.colorScheme
    val style = remember(baseStyle, colors) {
        baseStyle.copy(
            layout = baseStyle.layout.copy(blockSpacing = 15.dp, nestedBlockSpacing = 9.dp),
            quote = baseStyle.quote.copy(
                background = colors.surfaceContainer,
                borderColor = colors.outlineVariant,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ),
            code = baseStyle.code.copy(
                background = colors.surfaceContainer,
                borderColor = colors.outlineVariant,
                shape = RoundedCornerShape(10.dp),
            ),
            table = baseStyle.table.copy(
                headerBackground = colors.surfaceContainerHigh,
                rowBackground = colors.surface,
                alternateRowBackground = colors.surfaceContainer.copy(alpha = 0.62f),
                outerBorderColor = colors.outlineVariant,
                scrollTrackColor = colors.surfaceContainerHigh,
                scrollIndicatorColor = colors.primary,
            ),
            details = baseStyle.details.copy(
                background = colors.surfaceContainer.copy(alpha = 0.72f),
                borderColor = colors.outlineVariant,
            ),
            task = baseStyle.task.copy(touchTargetSize = 40.dp),
        )
    }
    val screens = remember { SampleScreen.entries }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val wideLayout = maxWidth >= 900.dp
        Column(modifier = Modifier.fillMaxSize()) {
            SampleHeader(isDark = isDark, onToggleTheme = onToggleTheme)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (wideLayout) {
                Row(modifier = Modifier.fillMaxSize()) {
                    WideNavigation(
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
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    CompactNavigation(
                        screens = screens,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ScreenContent(
                        screen = screens[selectedIndex],
                        parser = parser,
                        style = style,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleHeader(isDark: Boolean, onToggleTheme: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 480.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "O",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "ORCA LAB",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!compact) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp)),
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDark) "Use light theme" else "Use dark theme",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WideNavigation(
    screens: List<SampleScreen>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(242.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Text(
            text = "RENDER SUITES",
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 11.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            itemsIndexed(screens, key = { _, screen -> screen.name }) { index, screen ->
                NavigationItem(
                    index = index,
                    screen = screen,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp)),
            )
            Text(
                text = "${screens.size} suites, flat UI",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NavigationItem(
    index: Int,
    screen: SampleScreen,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            ),
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.outline else Color.Transparent,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = (index + 1).toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = screen.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = screen.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CompactNavigation(
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        itemsIndexed(screens, key = { _, screen -> screen.name }) { index, screen ->
            val selected = index == selectedIndex
            Surface(
                modifier = Modifier.selectable(
                    selected = selected,
                    role = Role.Tab,
                    onClick = { onSelect(index) },
                ),
                color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Text(
                    text = screen.shortLabel,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ScreenContent(
    screen: SampleScreen,
    parser: OrcaMarkdownParser,
    style: OrcaStyle,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

@Composable
private fun DocumentScreen(
    screen: SampleScreen,
    parser: OrcaMarkdownParser,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
) {
    var markdown by rememberSaveable(screen) { mutableStateOf(sampleMarkdown(screen)) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val document = remember(screen, markdown) {
        parser.parseCached(key = "sample-${screen.name}", input = markdown)
    }
    val toc = remember(document) { document.tableOfContents(maxLevel = 2) }
    val anchorIndex = remember(document) { orcaHeadingBlockIndex(document) }
    val colors = MaterialTheme.colorScheme
    val inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = remember(screen, colors) {
        if (screen == SampleScreen.RENDERERS) {
            mapOf(
                OrcaInline.Abbreviation::class to { inline ->
                    val abbreviation = inline as OrcaInline.Abbreviation
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.SemiBold)) {
                            append(abbreviation.text)
                        }
                        withStyle(SpanStyle(color = colors.onSurfaceVariant)) {
                            append(" [${abbreviation.title}]")
                        }
                    }
                },
            )
        } else {
            emptyMap()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth >= 720.dp) 32.dp else 18.dp
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 920.dp)
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 22.dp),
            ) {
                SectionHeader(screen = screen)
                if (screen == SampleScreen.RENDERERS) {
                    RendererStatus()
                }
                if (toc.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        itemsIndexed(toc, key = { index, entry -> entry.id ?: "toc-$index" }) { _, entry ->
                            val target = entry.id?.let(anchorIndex::get)
                            Surface(
                                onClick = {
                                    if (target != null) scope.launch { listState.animateScrollToItem(target) }
                                },
                                enabled = target != null,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Text(
                                    text = entry.title,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                MarkdownRenderer(
                    markdown = markdown,
                    parser = parser,
                    cacheKey = screen.name,
                    style = style,
                    listState = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 20.dp),
                    onLinkClick = onLinkClick,
                    onTaskToggle = { taskIndex, checked ->
                        markdown = toggleMarkdownTask(markdown, taskIndex, checked)
                    },
                    inlineOverride = inlineOverride,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(screen: SampleScreen) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
    ) {
        val wide = maxWidth >= 620.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                SectionIdentity(screen = screen, modifier = Modifier.weight(1f))
                MetaLabel(text = screen.meta)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionIdentity(screen = screen)
                MetaLabel(text = screen.meta)
            }
        }
    }
}

@Composable
private fun SectionIdentity(screen: SampleScreen, modifier: Modifier = Modifier) {
    val index = SampleScreen.entries.indexOf(screen) + 1
    Column(
        modifier = modifier.widthIn(max = 650.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = "${index.toString().padStart(2, '0')}  /  ${screen.category.uppercase()}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = screen.title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = screen.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetaLabel(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RendererStatus() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "INLINE OVERRIDE ACTIVE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Abbreviation",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MarkdownRenderer(
    markdown: String,
    parser: OrcaMarkdownParser,
    cacheKey: Any,
    style: OrcaStyle,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    rootLayout: OrcaRootLayout = OrcaRootLayout.LAZY_COLUMN,
    onLinkClick: (String) -> Unit,
    onTaskToggle: ((Int, Boolean) -> Unit)? = null,
    inlineOverride: Map<KClass<out OrcaInline>, OrcaInlineRenderer> = emptyMap(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mathTypeface = remember(context) { StixTwoMath.load(context) }
    val inlineMathFontSize = 19.sp
    val inlineMathPlaceholder = rememberOrcaOrcexInlineMathPlaceholder(mathTypeface, inlineMathFontSize)

    Orca(
        markdown = markdown,
        parser = parser,
        parseCacheKey = cacheKey,
        listState = listState,
        rootLayout = rootLayout,
        modifier = modifier,
        style = style,
        securityPolicy = OrcaSecurityPolicies.RemoteImages,
        imageContent = { url, description -> OrcaCoilImage(url, description, style) },
        inlineImageContent = { url, description -> OrcaCoilInlineImage(url, description, style) },
        blockMathContent = { source ->
            OrcaOrcexMath(
                source = source,
                typeface = mathTypeface,
                fontSize = 27.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        inlineMathContent = { source ->
            OrcaOrcexMath(
                source = source,
                typeface = mathTypeface,
                fontSize = inlineMathFontSize,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        inlineMathPlaceholder = inlineMathPlaceholder,
        onLinkClick = onLinkClick,
        onTaskToggle = onTaskToggle,
        inlineOverride = inlineOverride,
    )
}

@Composable
private fun StreamingScreen(
    parser: OrcaMarkdownParser,
    style: OrcaStyle,
    screen: SampleScreen,
    onLinkClick: (String) -> Unit,
) {
    val stream = rememberOrcaStreamingState(frameIntervalMs = 64L)
    val incrementalParser = remember(parser) { OrcaIncrementalParserSession(parser) }
    var runId by rememberSaveable { mutableIntStateOf(0) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var speed by rememberSaveable { mutableIntStateOf(1) }

    LaunchedEffect(runId) {
        incrementalParser.reset()
        stream.clear()
        paused = false
        STREAMING_DEMO_MARKDOWN.chunked(10).forEach { chunk ->
            while (paused) delay(50L)
            stream.append(chunk)
            delay((30L / speed.coerceAtLeast(1)).coerceAtLeast(7L))
        }
        stream.finish()
    }

    val progress = if (STREAMING_DEMO_MARKDOWN.isEmpty()) {
        0f
    } else {
        stream.markdown.length.toFloat() / STREAMING_DEMO_MARKDOWN.length.toFloat()
    }.coerceIn(0f, 1f)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth >= 720.dp) 32.dp else 18.dp
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 920.dp)
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 22.dp),
            ) {
                SectionHeader(screen = screen)
                StreamingControls(
                    isStreaming = stream.isStreaming,
                    paused = paused,
                    speed = speed,
                    progress = progress,
                    characterCount = stream.markdown.length,
                    totalCount = STREAMING_DEMO_MARKDOWN.length,
                    onPauseToggle = { paused = !paused },
                    onReplay = { runId += 1 },
                    onSpeedChange = { speed = it },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 18.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Orca(
                    state = stream,
                    parser = incrementalParser,
                    parseCacheKey = "stream-$runId",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 20.dp),
                    style = style,
                    onLinkClick = onLinkClick,
                    streamingCursor = "\u258D",
                )
            }
        }
    }
}

@Composable
private fun StreamingControls(
    isStreaming: Boolean,
    paused: Boolean,
    speed: Int,
    progress: Float,
    characterCount: Int,
    totalCount: Int,
    onPauseToggle: () -> Unit,
    onReplay: () -> Unit,
    onSpeedChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (paused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(999.dp),
                        ),
                )
                Text(
                    text = when {
                        paused -> "Paused"
                        isStreaming -> "Receiving tokens"
                        else -> "Response complete"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "$characterCount / $totalCount",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 520.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlatActionButton(
                            label = "Replay",
                            icon = Icons.Default.Replay,
                            onClick = onReplay,
                        )
                        FlatActionButton(
                            label = if (paused) "Resume" else "Pause",
                            icon = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            enabled = isStreaming,
                            onClick = onPauseToggle,
                        )
                    }
                    SpeedSelector(speed = speed, onSpeedChange = onSpeedChange)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlatActionButton(
                            label = "Replay",
                            icon = Icons.Default.Replay,
                            onClick = onReplay,
                        )
                        FlatActionButton(
                            label = if (paused) "Resume" else "Pause",
                            icon = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            enabled = isStreaming,
                            onClick = onPauseToggle,
                        )
                    }
                    SpeedSelector(speed = speed, onSpeedChange = onSpeedChange)
                }
            }
        }
    }
}

@Composable
private fun FlatActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SpeedSelector(speed: Int, onSpeedChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "PACE",
            modifier = Modifier.padding(end = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        listOf(1, 2, 4).forEach { value ->
            val selected = value == speed
            Surface(
                modifier = Modifier.selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = { onSpeedChange(value) },
                ),
                color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Text(
                    text = "${value}x",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private enum class PlaygroundPane { SOURCE, PREVIEW }

@Composable
private fun PlaygroundScreen(
    parser: OrcaMarkdownParser,
    style: OrcaStyle,
    screen: SampleScreen,
    onLinkClick: (String) -> Unit,
) {
    var markdown by rememberSaveable { mutableStateOf(PLAYGROUND_DEFAULT_MARKDOWN) }
    var pane by rememberSaveable { mutableStateOf(PlaygroundPane.SOURCE) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 760.dp
        val horizontalPadding = if (maxWidth >= 900.dp) 32.dp else 18.dp
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1080.dp)
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 22.dp),
            ) {
                SectionHeader(screen = screen)
                PlaygroundToolbar(
                    wideLayout = wideLayout,
                    pane = pane,
                    onPaneChange = { pane = it },
                    onReset = { markdown = PLAYGROUND_DEFAULT_MARKDOWN },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (wideLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 14.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                    ) {
                        EditorPane(
                            markdown = markdown,
                            onMarkdownChange = { markdown = it },
                            modifier = Modifier.weight(0.44f),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                        PreviewPane(
                            markdown = markdown,
                            parser = parser,
                            style = style,
                            onLinkClick = onLinkClick,
                            onTaskToggle = { taskIndex, checked ->
                                markdown = toggleMarkdownTask(markdown, taskIndex, checked)
                            },
                            modifier = Modifier.weight(0.56f),
                        )
                    }
                } else {
                    Crossfade(
                        targetState = pane,
                        animationSpec = tween(140),
                        label = "playground-pane",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 14.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                    ) { selectedPane ->
                        when (selectedPane) {
                            PlaygroundPane.SOURCE -> EditorPane(
                                markdown = markdown,
                                onMarkdownChange = { markdown = it },
                            )
                            PlaygroundPane.PREVIEW -> PreviewPane(
                                markdown = markdown,
                                parser = parser,
                                style = style,
                                onLinkClick = onLinkClick,
                                onTaskToggle = { taskIndex, checked ->
                                    markdown = toggleMarkdownTask(markdown, taskIndex, checked)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaygroundToolbar(
    wideLayout: Boolean,
    pane: PlaygroundPane,
    onPaneChange: (PlaygroundPane) -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (wideLayout) {
            Text(
                text = "SOURCE + PREVIEW",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                PlaygroundPane.entries.forEach { value ->
                    val selected = pane == value
                    val contentColor = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        modifier = Modifier.selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onPaneChange(value) },
                        ),
                        color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (value == PlaygroundPane.SOURCE) Icons.Default.Edit else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = contentColor,
                            )
                            Text(
                                text = if (value == PlaygroundPane.SOURCE) "Source" else "Preview",
                                style = MaterialTheme.typography.labelMedium,
                                color = contentColor,
                            )
                        }
                    }
                }
            }
        }
        Surface(
            onClick = onReset,
            color = Color.Transparent,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                text = "Reset",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EditorPane(
    markdown: String,
    onMarkdownChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        PaneHeader(
            label = "MARKDOWN SOURCE",
            meta = "${markdown.lineSequence().count()} lines",
        )
        OutlinedTextField(
            value = markdown,
            onValueChange = onMarkdownChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .semantics { contentDescription = "Markdown source editor" },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun PreviewPane(
    markdown: String,
    parser: OrcaMarkdownParser,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    onTaskToggle: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        PaneHeader(label = "RENDERED OUTPUT", meta = "${markdown.length} chars")
        MarkdownRenderer(
            markdown = markdown,
            parser = parser,
            cacheKey = "playground",
            style = style,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            onLinkClick = onLinkClick,
            onTaskToggle = onTaskToggle,
        )
    }
}

@Composable
private fun PaneHeader(label: String, meta: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.7.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = meta,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
