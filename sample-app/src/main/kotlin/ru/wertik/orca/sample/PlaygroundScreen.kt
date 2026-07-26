package ru.wertik.orca.sample

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ru.wertik.orca.compose.OrcaStyle
import ru.wertik.orca.core.OrcaMarkdownParser
import ru.wertik.orca.core.stats

private enum class PlaygroundPane { SOURCE, PREVIEW }

/** Markdown snippet that the toolbar can append to the editor. */
private data class PlaygroundSnippet(val label: String, val markdown: String)

private val PLAYGROUND_SNIPPETS = listOf(
    PlaygroundSnippet("Heading", "\n## New section\n"),
    PlaygroundSnippet("Callout", "\n> [!TIP]\n> Flat callouts use an outline and a solid tint.\n"),
    PlaygroundSnippet("Code", "\n```kotlin\nval document = parser.parse(source)\n```\n"),
    PlaygroundSnippet("Table", "\n| Token | Role |\n|:--|:--|\n| outline | structure |\n| accent | emphasis |\n"),
    PlaygroundSnippet("Tasks", "\n- [x] Written\n- [ ] Reviewed\n"),
    PlaygroundSnippet("Math", "\n\$\$\n\\int_0^1 x^2 \\, dx = \\frac{1}{3}\n\$\$\n"),
    PlaygroundSnippet("Details", "\n<details>\n<summary>More</summary>\n\nHidden body.\n\n</details>\n"),
)

@Composable
internal fun PlaygroundScreen(
    parser: OrcaMarkdownParser,
    style: OrcaStyle,
    screen: SampleScreen,
    onLinkClick: (String) -> Unit,
) {
    var markdown by rememberSaveable { mutableStateOf(PLAYGROUND_DEFAULT_MARKDOWN) }
    var pane by rememberSaveable { mutableStateOf(PlaygroundPane.SOURCE) }
    val stats = remember(markdown) { parser.parseCached("playground-stats", markdown).stats() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableWidth = maxWidth
        val wideLayout = availableWidth >= 760.dp
        val horizontalPadding = if (availableWidth >= 900.dp) 32.dp else 18.dp
        val statColumns = if (availableWidth >= 620.dp) 4 else 2
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1120.dp)
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionHeader(screen = screen)
                StatStrip(
                    stats = listOf(
                        SampleStat("Words", stats.words.toString()),
                        SampleStat("Blocks", stats.blocks.toString()),
                        SampleStat("Lines", markdown.lineSequence().count().toString()),
                        SampleStat("Tasks", "${stats.completedTasks}/${stats.tasks}"),
                    ),
                    columns = statColumns,
                )
                PlaygroundToolbar(
                    wideLayout = wideLayout,
                    pane = pane,
                    onPaneChange = { pane = it },
                    onReset = { markdown = PLAYGROUND_DEFAULT_MARKDOWN },
                    onClear = { markdown = "" },
                    onInsert = { snippet ->
                        markdown = if (markdown.isEmpty()) snippet.trimStart() else markdown.trimEnd() + "\n" + snippet
                        pane = PlaygroundPane.SOURCE
                    },
                )
                val paneShape = RoundedCornerShape(10.dp)
                if (wideLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, paneShape),
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
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, paneShape),
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
    onClear: () -> Unit,
    onInsert: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (wideLayout) {
                MetaLabel(text = "SOURCE + PREVIEW")
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlaygroundPane.entries.forEach { value ->
                        FlatChip(
                            label = if (value == PlaygroundPane.SOURCE) "Source" else "Preview",
                            icon = if (value == PlaygroundPane.SOURCE) Icons.Default.Edit else Icons.Default.Visibility,
                            selected = pane == value,
                            role = Role.Tab,
                            onClick = { onPaneChange(value) },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlatButton(label = "Clear", onClick = onClear)
                FlatButton(label = "Reset", onClick = onReset)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetaLabel(text = "INSERT")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PLAYGROUND_SNIPPETS, key = { snippet -> snippet.label }) { snippet ->
                    FlatChip(
                        label = snippet.label,
                        selected = false,
                        role = Role.Button,
                        onClick = { onInsert(snippet.markdown) },
                    )
                }
            }
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetaLabel(text = label, color = MaterialTheme.colorScheme.onSurface)
            MetaLabel(text = meta)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}
