package ru.wertik.orca.sample

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.wertik.orca.compose.OrcaInlineRenderer
import ru.wertik.orca.compose.OrcaStyle
import ru.wertik.orca.compose.OrcaTextHighlight
import ru.wertik.orca.compose.orcaHeadingBlockIndex
import ru.wertik.orca.core.OrcaInline
import ru.wertik.orca.core.OrcaMarkdownParser
import ru.wertik.orca.core.findMatches
import ru.wertik.orca.core.stats
import ru.wertik.orca.core.tableOfContents
import kotlin.reflect.KClass

/**
 * Reading surface for the static suites.
 *
 * Combines the 0.30 document APIs: `stats()` drives the measurement strip, `findMatches()` drives
 * search navigation, and `OrcaTextHighlight` shades the hits inside the rendered document.
 */
@Composable
internal fun DocumentScreen(
    screen: SampleScreen,
    parser: OrcaMarkdownParser,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
) {
    var markdown by rememberSaveable(screen) { mutableStateOf(sampleMarkdown(screen)) }
    var query by rememberSaveable(screen) { mutableStateOf("") }
    var showStats by rememberSaveable { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val document = remember(screen, markdown) {
        parser.parseCached(key = "sample-${screen.name}", input = markdown)
    }
    val stats = remember(document) { document.stats() }
    val toc = remember(document) { document.tableOfContents(maxLevel = 2) }
    val anchorIndex = remember(document) { orcaHeadingBlockIndex(document) }
    val matches = remember(document, query) {
        if (query.isBlank()) emptyList() else document.findMatches(query)
    }
    var matchCursor by rememberSaveable(screen, query) { mutableIntStateOf(0) }

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

    fun jumpToMatch(index: Int) {
        if (matches.isEmpty()) return
        val wrapped = ((index % matches.size) + matches.size) % matches.size
        matchCursor = wrapped
        scope.launch { listState.animateScrollToItem(matches[wrapped].blockIndex) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableWidth = maxWidth
        val horizontalPadding = if (availableWidth >= 720.dp) 32.dp else 18.dp
        val statColumns = if (availableWidth >= 620.dp) 4 else 2
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 940.dp)
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionHeader(
                    screen = screen,
                    trailing = {
                        FlatChip(
                            label = if (showStats) "Hide stats" else "Stats",
                            selected = showStats,
                            onClick = { showStats = !showStats },
                        )
                    },
                )

                SearchBar(
                    query = query,
                    matchCount = matches.size,
                    matchCursor = matchCursor,
                    onQueryChange = { query = it },
                    onPrevious = { jumpToMatch(matchCursor - 1) },
                    onNext = { jumpToMatch(matchCursor + 1) },
                )

                if (showStats) {
                    StatStrip(
                        stats = listOfNotNull(
                            SampleStat("Words", stats.words.toString()),
                            SampleStat("Read", "${stats.readingMinutes} min"),
                            SampleStat("Blocks", stats.blocks.toString()),
                            SampleStat("Code", stats.codeBlocks.toString()),
                            SampleStat("Links", stats.links.toString()),
                            SampleStat("Tables", stats.tables.toString()),
                            if (stats.tasks > 0) {
                                SampleStat("Tasks", "${stats.completedTasks}/${stats.tasks}")
                            } else {
                                null
                            },
                            if (matches.isNotEmpty()) SampleStat("Hits", matches.size.toString()) else null,
                        ),
                        columns = statColumns,
                    )
                }

                if (screen == SampleScreen.RENDERERS) {
                    RendererStatus()
                }

                if (toc.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        itemsIndexed(toc, key = { index, entry -> entry.id ?: "toc-$index" }) { _, entry ->
                            val target = entry.id?.let(anchorIndex::get)
                            FlatChip(
                                label = entry.title,
                                selected = false,
                                enabled = target != null,
                                onClick = {
                                    if (target != null) scope.launch { listState.animateScrollToItem(target) }
                                },
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.outlineVariant),
                )

                MarkdownRenderer(
                    markdown = markdown,
                    parser = parser,
                    cacheKey = screen.name,
                    style = style,
                    listState = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 6.dp),
                    onLinkClick = onLinkClick,
                    onTaskToggle = { taskIndex, checked ->
                        markdown = toggleMarkdownTask(markdown, taskIndex, checked)
                    },
                    inlineOverride = inlineOverride,
                    highlight = query.takeIf { it.isNotBlank() }?.let { OrcaTextHighlight(it) },
                )
            }
        }
    }
}

/** Query field plus match counter and previous/next navigation. */
@Composable
internal fun SearchBar(
    query: String,
    matchCount: Int,
    matchCursor: Int,
    onQueryChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Search the document" },
            singleLine = true,
            placeholder = {
                Text(
                    text = "Search this document",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                )
            },
            trailingIcon = if (query.isEmpty()) {
                null
            } else {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (matchCount == 0) "0" else "${matchCursor + 1}/$matchCount",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            color = colors.onSurfaceVariant,
                        )
                        Box(modifier = Modifier.padding(start = 8.dp, end = 4.dp)) {
                            FlatIconButton(
                                icon = Icons.Default.Close,
                                contentDescription = "Clear search",
                                onClick = { onQueryChange("") },
                            )
                        }
                    }
                }
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.outlineVariant,
                focusedContainerColor = colors.surfaceContainer,
                unfocusedContainerColor = colors.surfaceContainer,
                cursorColor = colors.primary,
            ),
        )
        FlatIconButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "Previous match",
            onClick = onPrevious,
            enabled = matchCount > 0,
        )
        FlatIconButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "Next match",
            onClick = onNext,
            enabled = matchCount > 0,
        )
    }
}

/** Suite identity: index, category, title, description, and a meta chip. */
@Composable
internal fun SectionHeader(
    screen: SampleScreen,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val index = SampleScreen.entries.indexOf(screen) + 1
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val wide = maxWidth >= 620.dp
        val identity: @Composable (Modifier) -> Unit = { identityModifier ->
            Column(
                modifier = identityModifier.widthIn(max = 660.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                MetaLabel(
                    text = "${index.toString().padStart(2, '0')}  /  ${screen.category.uppercase()}",
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
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                identity(Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetaChip(text = screen.meta)
                    trailing?.invoke()
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                identity(Modifier)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetaChip(text = screen.meta)
                    trailing?.invoke()
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    FlatPanel(cornerRadius = 999) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RendererStatus() {
    FlatPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetaLabel(
                text = "INLINE OVERRIDE ACTIVE",
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Abbreviation",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.2.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
