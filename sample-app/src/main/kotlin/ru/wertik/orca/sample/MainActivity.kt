package ru.wertik.orca.sample

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.wertik.orca.compose.Orca
import ru.wertik.orca.compose.OrcaDefaults
import ru.wertik.orca.compose.OrcaRootLayout
import ru.wertik.orca.core.OrcaMarkdownParser

class OrcaSampleApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            var isDark by rememberSaveable { mutableStateOf(false) }

            MaterialTheme(
                colorScheme = if (isDark) darkColorScheme() else lightColorScheme(),
            ) {
                OrcaSampleApp(
                    isDark = isDark,
                    onToggleTheme = { isDark = !isDark },
                )
            }
        }
    }
}

private enum class SampleScreen(
    val label: String,
    val icon: ImageVector,
) {
    OVERVIEW("Overview", Icons.Default.Article),
    BLOCKS("Blocks", Icons.Default.Code),
    TABLES("Tables", Icons.Default.TableChart),
    ADVANCED("Advanced", Icons.Default.Tune),
    STREAMING("Stream", Icons.Default.PlayArrow),
    PLAYGROUND("Edit", Icons.Default.Edit),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrcaSampleApp(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
) {
    val context = LocalContext.current
    val parser = remember { OrcaMarkdownParser() }
    val orcaStyle = if (isDark) OrcaDefaults.darkStyle() else OrcaDefaults.lightStyle()

    val screens = remember { SampleScreen.entries }
    var selectedScreen by rememberSaveable { mutableIntStateOf(0) }

    val backgroundColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF121212) else Color.White,
        animationSpec = tween(300),
        label = "bg",
    )

    val onLinkClick: (String) -> Unit = { link ->
        Toast.makeText(context, "Link: $link", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Orca",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Compose Multiplatform Markdown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = if (isDark) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = selectedScreen == index,
                        onClick = { selectedScreen = index },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label,
                            )
                        },
                        label = { Text(screen.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = if (isDark) Color(0xFF2D2D2D) else MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedScreen,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor),
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
            },
            label = "screen",
        ) { screenIndex ->
            when (screens[screenIndex]) {
                SampleScreen.STREAMING -> StreamingScreen(
                    parser = parser,
                    style = orcaStyle,
                    isDark = isDark,
                    onLinkClick = onLinkClick,
                )
                SampleScreen.PLAYGROUND -> PlaygroundScreen(
                    parser = parser,
                    style = orcaStyle,
                    isDark = isDark,
                    onLinkClick = onLinkClick,
                )
                else -> {
                    val markdown = sampleMarkdown(screens[screenIndex])
                    Orca(
                        markdown = markdown,
                        parser = parser,
                        parseCacheKey = screens[screenIndex].name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        style = orcaStyle,
                        onLinkClick = onLinkClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingScreen(
    parser: OrcaMarkdownParser,
    style: ru.wertik.orca.compose.OrcaStyle,
    isDark: Boolean,
    onLinkClick: (String) -> Unit,
) {
    val fullText = STREAMING_DEMO_MARKDOWN
    var displayedText by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        displayedText = ""
        isStreaming = true
        val random = kotlin.random.Random
        var i = 0
        while (i < fullText.length) {
            // simulate chunked token delivery — 1 to 6 chars at a time
            val chunkSize = random.nextInt(1, 7).coerceAtMost(fullText.length - i)
            i += chunkSize
            displayedText = fullText.substring(0, i)
            // variable delay: shorter for mid-word, longer at whitespace/newlines
            val lastChar = fullText[i - 1]
            val baseDelay = when {
                lastChar == '\n' -> random.nextLong(30, 80)
                lastChar == ' ' -> random.nextLong(10, 40)
                else -> random.nextLong(5, 20)
            }
            delay(baseDelay)
        }
        isStreaming = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (isStreaming) "Streaming..." else "Complete",
                style = MaterialTheme.typography.labelMedium,
                color = if (isStreaming) {
                    if (isDark) Color(0xFF82B1FF) else Color(0xFF1565C0)
                } else {
                    if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                },
            )
            Text(
                text = "${displayedText.length} / ${fullText.length} chars",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }

        Orca(
            markdown = displayedText,
            parser = parser,
            parseCacheKey = "streaming-demo",
            modifier = Modifier.fillMaxSize(),
            style = style,
            onLinkClick = onLinkClick,
        )
    }
}

@Composable
private fun PlaygroundScreen(
    parser: OrcaMarkdownParser,
    style: ru.wertik.orca.compose.OrcaStyle,
    isDark: Boolean,
    onLinkClick: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(PLAYGROUND_DEFAULT_MARKDOWN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(top = 12.dp),
            label = { Text("Markdown") },
            placeholder = { Text("Type markdown here...") },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isDark) Color(0xFF82B1FF) else Color(0xFF1565C0),
                unfocusedBorderColor = if (isDark) Color(0xFF424242) else Color(0xFFD0D7DE),
                focusedContainerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA),
                unfocusedContainerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA),
            ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(1.dp)
                .background(
                    if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0),
                ),
        )

        Orca(
            markdown = input,
            parser = parser,
            parseCacheKey = "playground",
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, bottom = 16.dp),
            style = style,
            rootLayout = OrcaRootLayout.COLUMN,
            onLinkClick = onLinkClick,
        )
    }
}

private fun sampleMarkdown(screen: SampleScreen): String {
    return when (screen) {
        SampleScreen.OVERVIEW -> OVERVIEW_MARKDOWN
        SampleScreen.BLOCKS -> BLOCKS_MARKDOWN
        SampleScreen.TABLES -> TABLES_MARKDOWN
        SampleScreen.ADVANCED -> ADVANCED_MARKDOWN
        else -> ""
    }
}

// region Markdown content

private val OVERVIEW_MARKDOWN = """
# Setting up a Kotlin Multiplatform project

Getting KMP to work across all targets takes some effort, but the payoff is worth it. Here's a quick rundown of what you need to know.

## The basics

Your shared code lives in `commonMain` and platform-specific bits go into `androidMain`, `iosMain`, etc. The Gradle setup looks something like this — expect/actual declarations bridge the gap between platforms.

Key things: **dependency injection** works differently per target, *coroutines* are your best friend for async, and ~~don't bother with~~ `Dispatchers.IO` on native — use `Dispatchers.Default` instead.

## Versioning

Current release: v2.1.0^beta^

Minimum SDK: API 24 (Android), iOS 15+, JDK 17 (Desktop)

Chemical formula example: C~6~H~12~O~6~ (glucose)

## Quick links

:white_check_mark: [Kotlin docs](https://kotlinlang.org/docs/multiplatform.html) — official guide
:wrench: [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) — UI framework
:rocket: [KMP library template](https://github.com/AdrielCafe/lyricist) — good reference

:warning: Watch out for binary compatibility issues when publishing. :bug: iOS memory leaks with circular references are a classic trap.

---

## What to set up first

- Gradle version catalogs (`libs.versions.toml`)
- CI pipeline — **GitHub Actions** works well for KMP
- Detekt + ktlint for code style
- `expect`/`actual` for platform APIs like file I/O

## Recommended order

1. Get `commonMain` compiling with shared models
2. Wire up networking (Ktor is the standard choice)
3. Add platform-specific UI on top
4. Set up publishing to Maven Central

## Project status

- [x] Shared data layer
- [x] Ktor networking module
- [x] Compose UI for Android + Desktop
- [ ] iOS SwiftUI wrapper
- [ ] Wasm target support

---

## Image loading

![Kotlin logo](https://raw.githubusercontent.com/JetBrains/kotlin-web-site/master/static/images/kotlin-logo.png)
""".trimIndent()

private val BLOCKS_MARKDOWN = """
## Admonitions

> [!NOTE]
> Kotlin 2.1 introduced the new K2 compiler as the default. If your build breaks after updating, check for incompatible compiler plugins first.

> [!TIP]
> Run `./gradlew dependencies --scan` to get a full dependency tree — super useful for debugging version conflicts.

> [!IMPORTANT]
> The `android` block in `build.gradle.kts` must come *after* the `kotlin` block, otherwise the AGP plugin won't pick up your source sets.

> [!WARNING]
> Proguard rules for KMP libraries need to be in the consumer module, not the library itself. This catches people off guard regularly.

> [!CAUTION]
> Never store API keys in `BuildConfig` for open-source projects — even if the repo is private now, it might not be later.

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

## HTML block

<p>Most markdown renderers handle <b>basic HTML</b> inline — things like <i>emphasis</i>, <code>code</code>, and <a href="https://kotlinlang.org">links</a> work as expected.</p>

<blockquote>The tricky part is <mark>highlighted text</mark> and nested structures — not every renderer gets those right.</blockquote>

""".trimIndent()

private val TABLES_MARKDOWN = """
## Tables

### Kotlin targets — build time comparison

| Target | Clean build | Incremental | Binary size |
|:-------|:------:|:--------:|-----:|
| **Android** | 24s | 3s | 4.2 MB |
| **Desktop (JVM)** | 31s | 5s | 18.7 MB |
| **iOS arm64** | 48s | 12s | 9.1 MB |
| **Wasm** | 52s | 8s | 2.8 MB |

### Common libraries for KMP

| Library | Category | Platforms | Maturity |
|:--------|:---------|:---------:|:---------|
| [Ktor](https://ktor.io) | Networking | All | :white_check_mark: Stable |
| [SQLDelight](https://github.com/cashapp/sqldelight) | Database | All | :white_check_mark: Stable |
| [Koin](https://insert-koin.io) | DI | All | :white_check_mark: Stable |
| [Napier](https://github.com/AdrielCafe/napier) | Logging | All | :wrench: Maintained |
| [Multiplatform Settings](https://github.com/russhwolf/multiplatform-settings) | Key-value | All | :white_check_mark: Stable |
| [KStore](https://github.com/AdrielCafe/kstore) | File storage | All | :wrench: Maintained |
| [Compose ImageLoader](https://github.com/AdrielCafe/compose-imageloader) | Images | Android, iOS, Desktop | :warning: Alpha |

---

## Image

![Kotlin logo](https://raw.githubusercontent.com/JetBrains/kotlin-web-site/master/static/images/kotlin-logo.png)
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

private val STREAMING_DEMO_MARKDOWN = """
## How to structure a Kotlin Multiplatform project

The key decision is how much code to share. Most teams start with shared data models and networking, then gradually move UI logic into `commonMain` as they get comfortable.

### Recommended project layout

```
shared/
  commonMain/    -- models, use cases, repositories
  androidMain/   -- Android-specific implementations
  iosMain/       -- iOS-specific implementations
app-android/     -- Android Compose UI
app-ios/         -- SwiftUI wrapper
app-desktop/     -- Desktop Compose UI
```

### Networking with Ktor

Ktor is the standard choice for KMP networking. Here's a typical setup:

```kotlin
val client = HttpClient {
    install(ContentNegotiation) { json() }
    install(Logging) { level = LogLevel.HEADERS }
    defaultRequest {
        url("https://api.example.com/v2/")
        header("Accept", "application/json")
    }
}
```

> [!TIP]
> Use `expect`/`actual` for the HTTP engine — `OkHttp` on Android, `Darwin` on iOS, `CIO` for desktop.

### Things that trip people up

- **Serialization** — `@Serializable` classes must be in `commonMain`, not platform modules
- **Coroutine scopes** — iOS doesn't have `viewModelScope`, you'll need a custom scope
- ~~Freezing~~ — no longer needed since the new Kotlin/Native memory model
- **Resources** — each platform handles strings, images, and assets differently

### Database layer

```kotlin
// SQLDelight generates type-safe Kotlin from SQL
val players: Flow<List<Player>> =
    playerQueries.selectAll()
        .asFlow()
        .mapToList(Dispatchers.Default)
```

That covers the basics — the rest is just connecting the pieces and writing tests.
""".trimIndent()

private val PLAYGROUND_DEFAULT_MARKDOWN = """
# Release notes — v2.1.0

This release focuses on **performance** and *developer experience*.

## Changes

- Reduced recomposition count by **40%** in large documents
- Added `streamingDebounceMs` parameter for real-time use cases
- Fixed ~~incorrect~~ table column alignment on RTL layouts
- New `OrcaRootLayout.COLUMN` option for non-scrollable containers

## Migration

```kotlin
// Before
Orca(markdown = text, parser = parser)

// After — explicit cache key recommended
Orca(markdown = text, parser = parser, parseCacheKey = "my-key")
```

> [!NOTE]
> The `parseCacheKey` parameter is optional but improves performance when the same content is rendered in multiple places.

- [x] Update dependencies
- [x] Run full test suite
- [ ] Update documentation site
""".trimIndent()

// endregion
