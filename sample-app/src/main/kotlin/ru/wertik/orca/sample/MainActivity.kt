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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.wertik.orca.compose.Orca
import ru.wertik.orca.compose.OrcaDefaults
import ru.wertik.orca.compose.OrcaRootLayout
import ru.wertik.orca.compose.OrcaSecurityPolicies
import ru.wertik.orca.compose.OrcaStyle
import ru.wertik.orca.compose.orcaHeadingBlockIndex
import ru.wertik.orca.compose.rememberOrcaStreamingState
import ru.wertik.orca.core.OrcaIncrementalParserSession
import ru.wertik.orca.core.OrcaMarkdownParser
import ru.wertik.orca.core.tableOfContents
import ru.wertik.orca.images.coil.OrcaCoilImage
import ru.wertik.orca.images.coil.OrcaCoilInlineImage
import ru.wertik.orca.math.orcex.OrcaOrcexMath
import ru.wertik.orca.math.orcex.rememberOrcaOrcexInlineMathPlaceholder
import ru.wertik.orcex.font.stix2.StixTwoMath

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
            // Follow the system theme on first launch so the window background,
            // status bar icons and the in-app theme agree from the first frame.
            val systemDark = isSystemInDarkTheme()
            var isDark by rememberSaveable { mutableStateOf(systemDark) }

            // Keep system bar icon appearance in sync with the in-app theme toggle.
            // Without this, the status bar keeps the system appearance and turns
            // unreadable (light icons on light content or vice versa) after a toggle.
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

private const val SAMPLE_VERSION_LABEL = "0.15.0"

private val LightColors = lightColorScheme(
    background = Color(0xFFF7F6F3),
    surface = Color(0xFFF7F6F3),
    surfaceContainer = Color(0xFFF0EEE9),
    surfaceContainerHigh = Color(0xFFE9E6DF),
    onSurface = Color(0xFF201E1B),
    onSurfaceVariant = Color(0xFF625E57),
    primary = Color(0xFF3E5F67),
    onPrimary = Color(0xFFF7F6F3),
    outline = Color(0xFFC8C3BA),
    outlineVariant = Color(0xFFE0DCD4),
)

private val DarkColors = darkColorScheme(
    background = Color(0xFF121311),
    surface = Color(0xFF121311),
    surfaceContainer = Color(0xFF1A1B18),
    surfaceContainerHigh = Color(0xFF232420),
    onSurface = Color(0xFFE7E3DB),
    onSurfaceVariant = Color(0xFFAAA59C),
    primary = Color(0xFFAAC5CB),
    onPrimary = Color(0xFF172327),
    outline = Color(0xFF41423D),
    outlineVariant = Color(0xFF2B2C28),
)

@Composable
private fun OrcaSampleTheme(isDark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        content = content,
    )
}

private enum class SampleScreen(
    val title: String,
    val shortLabel: String,
    val description: String,
) {
    OVERVIEW("Reader", "Read", "Long-form markdown rendering and links"),
    BLOCKS("Syntax", "Blocks", "Code, quotes, tasks and images"),
    TABLES("Tables", "Data", "Readable tabular content in both themes"),
    MATH("Math", "Math", "Native LaTeX rendering through optional Orcex"),
    ADVANCED("Extended", "More", "Footnotes, details and definitions"),
    STREAMING("Streaming", "Stream", "Token deltas with stable rendering"),
    PLAYGROUND("Playground", "Edit", "Edit markdown and inspect output"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrcaSampleApp(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
) {
    val context = LocalContext.current
    val parser = remember { OrcaMarkdownParser() }
    val style = if (isDark) OrcaDefaults.darkStyle() else OrcaDefaults.lightStyle()
    val screens = remember { SampleScreen.entries }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedScreen = screens[selectedIndex]
    val onLinkClick: (String) -> Unit = { link ->
        Toast.makeText(context, link, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        SampleHeader(isDark = isDark, onToggleTheme = onToggleTheme)
        ScreenTabs(
            screens = screens,
            selectedIndex = selectedIndex,
            onSelect = { selectedIndex = it },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Crossfade(
            targetState = selectedScreen,
            animationSpec = tween(durationMillis = 160),
            label = "screen",
            modifier = Modifier.fillMaxSize(),
        ) { screen ->
            when (screen) {
                SampleScreen.STREAMING -> StreamingScreen(parser, style, screen, onLinkClick)
                SampleScreen.PLAYGROUND -> PlaygroundScreen(parser, style, screen, onLinkClick)
                else -> DocumentScreen(screen, parser, style, onLinkClick)
            }
        }
    }
}

@Composable
private fun SampleHeader(isDark: Boolean, onToggleTheme: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "ORCA",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Markdown render lab",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = SAMPLE_VERSION_LABEL,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.size(6.dp))
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ScreenTabs(
    screens: List<SampleScreen>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        screens.forEachIndexed { index, screen ->
            val selected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
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
private fun SectionHeading(screen: SampleScreen, meta: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(screen.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(screen.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DocumentScreen(
    screen: SampleScreen,
    parser: OrcaMarkdownParser,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val mathTypeface = remember(context) { StixTwoMath.load(context) }
    val inlineMathFontSize = 19.sp
    val inlineMathPlaceholder = rememberOrcaOrcexInlineMathPlaceholder(mathTypeface, inlineMathFontSize)
    // Markdown is held in state so interactive task checkboxes can rewrite the source.
    var markdown by rememberSaveable(screen) { mutableStateOf(sampleMarkdown(screen)) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val document = remember(markdown) { parser.parseCached(key = "${screen.name}-toc", input = markdown) }
    val toc = remember(document) { document.tableOfContents(maxLevel = 2) }
    val anchorIndex = remember(document) { orcaHeadingBlockIndex(document) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            SectionHeading(screen = screen, meta = "Static")
            if (toc.size > 1) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    toc.forEach { entry ->
                        val target = entry.id?.let { anchorIndex[it] }
                        Surface(
                            onClick = {
                                if (target != null) scope.launch { listState.animateScrollToItem(target) }
                            },
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = entry.title,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Orca(
                markdown = markdown,
                parser = parser,
                parseCacheKey = screen.name,
                listState = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 18.dp),
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
                onTaskToggle = { taskIndex, checked ->
                    markdown = toggleMarkdownTask(markdown, taskIndex, checked)
                },
            )
        }
    }
}

@Composable
private fun StreamingScreen(
    parser: OrcaMarkdownParser,
    style: OrcaStyle,
    screen: SampleScreen,
    onLinkClick: (String) -> Unit,
) {
    val stream = rememberOrcaStreamingState(frameIntervalMs = 80L)
    val incrementalParser = remember(parser) { OrcaIncrementalParserSession(parser) }

    LaunchedEffect(Unit) {
        incrementalParser.reset()
        stream.clear()
        STREAMING_DEMO_MARKDOWN.chunked(9).forEach { chunk ->
            stream.append(chunk)
            delay(24L)
        }
        stream.finish()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            SectionHeading(screen = screen, meta = if (stream.isStreaming) "Live" else "Complete")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (stream.isStreaming) "Receiving tokens" else "Response complete",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${stream.markdown.length} / ${STREAMING_DEMO_MARKDOWN.length}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Orca(
                state = stream,
                parser = incrementalParser,
                parseCacheKey = "streaming-demo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 18.dp),
                style = style,
                securityPolicy = OrcaSecurityPolicies.RemoteImages,
                imageContent = { url, description -> OrcaCoilImage(url, description, style) },
                inlineImageContent = { url, description -> OrcaCoilInlineImage(url, description, style) },
                onLinkClick = onLinkClick,
                streamingCursor = "\u258D",
            )
        }
    }
}

@Composable
private fun PlaygroundScreen(
    parser: OrcaMarkdownParser,
    style: OrcaStyle,
    screen: SampleScreen,
    onLinkClick: (String) -> Unit,
) {
    var markdown by rememberSaveable { mutableStateOf(PLAYGROUND_DEFAULT_MARKDOWN) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            SectionHeading(screen = screen, meta = "${markdown.length} chars")
            OutlinedTextField(
                value = markdown,
                onValueChange = { markdown = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(158.dp),
                label = { Text("Markdown source") },
                supportingText = { Text("Preview updates while you type") },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 18.dp, bottom = 18.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Orca(
                markdown = markdown,
                parser = parser,
                parseCacheKey = "playground",
                modifier = Modifier.fillMaxSize(),
                style = style,
                rootLayout = OrcaRootLayout.COLUMN,
                securityPolicy = OrcaSecurityPolicies.RemoteImages,
                imageContent = { url, description -> OrcaCoilImage(url, description, style) },
                inlineImageContent = { url, description -> OrcaCoilInlineImage(url, description, style) },
                onLinkClick = onLinkClick,
            )
        }
    }
}

private fun sampleMarkdown(screen: SampleScreen): String {
    return when (screen) {
        SampleScreen.OVERVIEW -> OVERVIEW_MARKDOWN
        SampleScreen.BLOCKS -> BLOCKS_MARKDOWN
        SampleScreen.TABLES -> TABLES_MARKDOWN
        SampleScreen.MATH -> MATH_MARKDOWN
        SampleScreen.ADVANCED -> ADVANCED_MARKDOWN
        else -> ""
    }
}

private val TASK_MARKER_REGEX = Regex("""(?m)^(\s*(?:[-+*]|\d+[.)])\s+)\[( |x|X)]""")

/** Rewrites the [taskIndex]-th task checkbox in [markdown] to [checked]. */
private fun toggleMarkdownTask(markdown: String, taskIndex: Int, checked: Boolean): String {
    var current = -1
    return TASK_MARKER_REGEX.replace(markdown) { match ->
        current += 1
        if (current == taskIndex) {
            "${match.groupValues[1]}[${if (checked) "x" else " "}]"
        } else {
            match.value
        }
    }
}

// region Markdown content

private val OVERVIEW_MARKDOWN = """
# Orca Compose

Render Markdown in Compose with a small base artifact and opt-in integrations only where they are needed.

## Start with text

`orca-core` parses Markdown into an AST. `orca-compose` renders it. Images live in a separate optional module so chat and documentation screens do not inherit networking code by default.

The renderer supports **rich text**, *emphasis*, ~~strikethrough~~, `inline code`, and links with your own click policy.

## Current direction

Current release: 0.15.0

Base renderer: `orca-compose` + `orca-core`

Optional images: `orca-images-coil`

Optional math: `orca-math-orcex`

## Quick links

:white_check_mark: [Architecture](https://github.com/wertikolix/orca) — module split and usage
:wrench: [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) — rendering platform
:rocket: [CommonMark](https://commonmark.org) — syntax baseline

:warning: Remote images stay disabled unless the caller opts in. :bug: Malformed Markdown falls back to readable text.

---

## What to enable first

- Add `orca-compose` to render documents
- Supply an `OrcaMarkdownParser` instance
- Choose a security policy for remote content
- Add `orca-images-coil` only if your UI needs images

## Recommended order

1. Render plain text and links
2. Tune typography and table contrast
3. Enable streaming for chat messages
4. Opt into images or future math rendering

## Project status

Tap a checkbox — task lists are interactive since 0.14 and report the toggle back to the app.

- [x] Lightweight base renderer
- [x] Optional Coil image integration
- [x] Paced streaming state
- [x] Optional LaTeX/math integration
- [ ] Try tapping this checkbox

---

## Image loading

![Orca image sample](https://raw.githubusercontent.com/JetBrains/kotlin-web-site/master/static/images/kotlin-logo.png "The image title renders as a caption below the image.")
""".trimIndent()

private val BLOCKS_MARKDOWN = """
## Admonitions

> [!NOTE]
> Orca parses Markdown separately from rendering, so the same AST can power a preview and a full document view.

> [!TIP]
> Use a stable `parseCacheKey` when the same message is recomposed frequently.

> [!IMPORTANT]
> Keep remote image loading opt-in by passing content slots and an explicit security policy.

> [!WARNING]
> For chat streams, push deltas into `OrcaStreamingState` instead of rebuilding a full value for every token.

> [!CAUTION]
> Treat rendered input as untrusted content and keep link/image decisions in the host application.

---

## Blockquote

> The best code is the code you never write. Every line is a liability — it needs to be read, tested, maintained, and eventually deleted.
>
> — Someone who's debugged enough legacy code

## Nested blockquote

> From the Kotlin style guide:
> > Prefer `when` over chains of `if-else` when matching against multiple conditions. It's more readable and the compiler can optimize it better.
>
> This applies especially to sealed class hierarchies.

---

## Code blocks

```kotlin
suspend fun fetchUser(id: Long): Result<User> = runCatching {
    val response = httpClient.get("/api/users/${'$'}id")
    if (response.status != HttpStatusCode.OK) {
        error("Unexpected status: ${'$'}{response.status}")
    }
    response.body<UserDto>().toDomain()
}
```

```python
from pathlib import Path
import json

def load_config(path: str = "config.json") -> dict:
    config_file = Path(path)
    if not config_file.exists():
        raise FileNotFoundError(f"Config not found: {path}")
    return json.loads(config_file.read_text())
```

```sql
WITH monthly_revenue AS (
    SELECT
        DATE_TRUNC('month', created_at) AS month,
        SUM(amount) AS revenue
    FROM payments
    WHERE status = 'completed'
    GROUP BY 1
)
SELECT month, revenue,
       revenue - LAG(revenue) OVER (ORDER BY month) AS growth
FROM monthly_revenue
ORDER BY month DESC;
```

---

## Inline decorations

Orca renders ==highlighted== fragments, ++inserted text++, ~~removed text~~, x^2^ superscript and H~2~O subscript without extra modules.

Keyboard hints work through inline HTML: press <kbd>Ctrl</kbd> + <kbd>K</kbd>, and <mark>marked HTML</mark> follows the highlight style of the active theme.

---

## HTML block

<p>Most markdown renderers handle <b>basic HTML</b> inline — things like <i>emphasis</i>, <code>code</code>, and <a href="https://kotlinlang.org">links</a> work as expected.</p>

<blockquote>The tricky part is <mark>highlighted text</mark> and nested structures — not every renderer gets those right.</blockquote>

""".trimIndent()

private val TABLES_MARKDOWN = """
## Tables

### Feature surfaces

| Surface | Base module | Optional integration | Status |
|:-------|:------:|:--------:|-----:|
| **Text and links** | `orca-compose` | — | Ready |
| **Tables** | `orca-compose` | — | Ready |
| **Streaming prose** | `orca-compose` | — | Ready |
| **Remote images** | slots | `orca-images-coil` | Opt-in |
| **LaTeX math** | planned slots | planned module | Planned |

### Dependency strategy

| Module | Responsibility | Included by default | Weight goal |
|:--------|:---------|:---------:|:---------|
| `orca-core` | AST and parsing | Yes | Minimal |
| `orca-compose` | Compose renderer | Yes | Minimal |
| `orca-images-coil` | Network images | No | Consumer choice |
| future math module | Formula rendering | No | Measure first |

---

## Image

![Kotlin logo](https://raw.githubusercontent.com/JetBrains/kotlin-web-site/master/static/images/kotlin-logo.png "Captions come from the standard Markdown image title.")
""".trimIndent()

private val ADVANCED_MARKDOWN = """
## Footnotes

Kotlin was first announced in 2011[^1] and reached 1.0 in February 2016[^2]. Google declared it a first-class language for Android in 2017, and by 2019 it became the preferred language for Android development.

[^1]: JetBrains unveiled Project Kotlin at JVM Language Summit. The name comes from Kotlin Island near St. Petersburg.
[^2]: The 1.0 release focused on language stability and Java interop — no breaking changes since.

---

## Definition lists

Coroutine
:   A lightweight thread managed by the Kotlin runtime. Unlike OS threads, you can run thousands of coroutines without significant overhead.

Structured concurrency
:   A pattern where child coroutines are tied to a parent scope. If the parent cancels, all children cancel too — no orphaned tasks.

Recomposition
:   The process by which Compose re-executes composable functions when their inputs change. Skipping unchanged composables is what makes Compose fast.

---

## Abbreviations

*[KMP]: Kotlin Multiplatform
*[JVM]: Java Virtual Machine
*[AOT]: Ahead-of-Time

KMP compiles to JVM bytecode on Android and Desktop, and uses AOT compilation for native targets like iOS and Linux.

---

## Thematic breaks

Everything above this point covers language features.

---

***

___

Everything below gets into practical patterns.

---

## Highlight & anchors

This has ==highlighted text==, ++inserted text++ and normal text.

Jump to [Architecture patterns](#deep-nesting) section above.

---

## Deep nesting

<details>
<summary>Architecture patterns breakdown</summary>

- **MVVM** — standard for Compose apps
    - ViewModel holds `StateFlow`
    - UI collects and renders
- MVI — more structured, more boilerplate

</details>

<details open>
<summary>Testing strategies</summary>

- Unit tests for business logic
- `@Preview` for UI snapshots
- Integration tests with `ComposeTestRule`

</details>

- Architecture patterns
    - **MVVM** — standard for Compose apps
        - ViewModel holds `StateFlow`
        - UI collects and renders
    - MVI — more structured, more boilerplate
- Testing strategies
    - Unit tests for business logic
    - `@Preview` for UI snapshots

> From the Compose team's recommendations:
> - Keep composables small and focused
> - Hoist state to the caller
> - Avoid side effects in composition
>
> Example of state hoisting:
> ```kotlin
> @Composable
> fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
>     TextField(value = query, onValueChange = onQueryChange)
> }
> ```
""".trimIndent()

private val MATH_MARKDOWN = """
# Native LaTeX with Orcex

Orca keeps formula parsing in the document model while the optional `orca-math-orcex` adapter draws it natively on Android. Inline math stays part of readable prose: ${'$'}E = mc^2${'$'} and ${'$'}\\alpha + \\beta = \\gamma${'$'}.

## Display equation

${'$'}${'$'}
\\frac{1}{n} \\sum_{i=1}^{n} x_i = \\bar{x}
${'$'}${'$'}

## Matrix and integral

${'$'}${'$'}
\\int_0^1 x^2 \\, dx = \\frac{1}{3}
${'$'}${'$'}

## Colors, frames and stacked annotations

Orcex 0.5.0 adds LaTeX colors, boxed results and stacked annotations:

${'$'}${'$'}
\\boxed{E = mc^2}
${'$'}${'$'}

${'$'}${'$'}
\\textcolor{orange}{a^2} + \\textcolor{teal}{b^2} \\overset{!}{=} \\textcolor{#8000A0}{c^2}
${'$'}${'$'}

${'$'}${'$'}
\\underset{n \\to \\infty}{\\lim} \\left(1 + \\frac{1}{n}\\right)^n = e
${'$'}${'$'}

Unclosed formulas remain source text during streaming, then become rendered math only when the closing delimiter arrives.
""".trimIndent()

private val STREAMING_DEMO_MARKDOWN = """
## Streaming a response without jitter

Token deltas arrive frequently. Orca publishes a paced snapshot and incrementally reuses completed prose blocks whenever the syntax remains safe to reuse.

### The UI path

```
network chunks
  -> OrcaStreamingState.append(delta)
  -> paced rendered snapshot
  -> OrcaIncrementalParserSession
  -> stable document on screen
```

### Compose usage

Ktor is the standard choice for KMP networking. Here's a typical setup:

```kotlin
val stream = rememberOrcaStreamingState(frameIntervalMs = 80)
val parser = remember { OrcaIncrementalParserSession(OrcaMarkdownParser()) }

LaunchedEffect(messageId) {
    chunks.collect(stream::append)
    stream.finish()
}

Orca(state = stream, parser = parser, parseCacheKey = messageId)
```

> [!TIP]
> Plain prose receives the incremental fast path. Complex blocks always fall back to exact parsing.

### Things that trip people up

- **Unfinished tables** — fall back to complete parsing while delimiters arrive
- **Code fences** — remain exact rather than partially interpreted
- ~~Every-token recomposition~~ — avoided by paced snapshots
- **Remote images** — still require explicit policy and image slots

### A stable completed paragraph

```kotlin
Orca(
    state = stream,
    parser = parser,
    style = OrcaDefaults.darkStyle(),
)
```

The final document is exact, while live updates remain calm and readable.
""".trimIndent()

private val PLAYGROUND_DEFAULT_MARKDOWN = """
# Orca 0.15.0

This release focuses on **streaming performance** and reader UX.

## Changes

- Incremental streaming v2: headings, fences, lists and tables reuse stable blocks
- Text selection in the default lazy layout
- Table of contents API (`tableOfContents` + `orcaHeadingBlockIndex`)
- Streaming cursor for chat bubbles
- Admonition icons and Orcex 0.5.0 math colors

## Try it here

Edit this text — ==highlight==, ++underline++, x^2^, H~2~O.

- [x] Interactive in the Reader tab
- [ ] This one is static: playground has no `onTaskToggle`

> [!NOTE]
> The base artifact remains dependency-free: no networking, no Material, no gradients.
""".trimIndent()

// endregion
