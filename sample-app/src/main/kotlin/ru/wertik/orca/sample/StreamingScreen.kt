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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext
import ru.wertik.orca.compose.Orca
import ru.wertik.orca.compose.OrcaStyle
import ru.wertik.orca.compose.rememberOrcaStreamingState
import ru.wertik.orca.core.OrcaDocumentStats
import ru.wertik.orca.core.OrcaIncrementalParserSession
import ru.wertik.orca.core.OrcaMarkdownParser
import ru.wertik.orca.core.stats

@Composable
internal fun StreamingScreen(
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

    // Sampling keeps the measurement strip live without parsing the transcript on the UI thread
    // for every token batch.
    var liveStats by remember { mutableStateOf(OrcaDocumentStats()) }
    LaunchedEffect(runId, parser) {
        snapshotFlow { stream.markdown }
            .conflate()
            .collect { text ->
                liveStats = withContext(Dispatchers.Default) { parser.parse(text).stats() }
                delay(220L)
            }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth >= 720.dp) 32.dp else 18.dp
        val statColumns = if (maxWidth >= 620.dp) 4 else 2
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 940.dp)
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
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
                StatStrip(
                    stats = listOf(
                        SampleStat("Blocks", liveStats.blocks.toString()),
                        SampleStat("Words", liveStats.words.toString()),
                        SampleStat("Code", liveStats.codeBlocks.toString()),
                        SampleStat("Progress", "${(progress * 100).toInt()}%"),
                    ),
                    columns = statColumns,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Orca(
                    state = stream,
                    parser = incrementalParser,
                    parseCacheKey = "stream-$runId",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 6.dp),
                    style = style,
                    onLinkClick = onLinkClick,
                    streamingCursor = "▍",
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
    FlatPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
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
                                if (paused) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
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
            FlatProgress(
                progress = progress,
                modifier = Modifier.semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                },
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 520.dp
                val actions: @Composable () -> Unit = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlatButton(
                            label = "Replay",
                            icon = Icons.Default.Replay,
                            onClick = onReplay,
                        )
                        FlatButton(
                            label = if (paused) "Resume" else "Pause",
                            icon = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            enabled = isStreaming,
                            onClick = onPauseToggle,
                        )
                    }
                }
                val pace: @Composable () -> Unit = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MetaLabel(text = "PACE", modifier = Modifier.padding(end = 2.dp))
                        FlatChipGroup(
                            options = listOf(1, 2, 4),
                            selected = speed,
                            label = { value -> "${value}x" },
                            onSelect = onSpeedChange,
                        )
                    }
                }
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        actions()
                        pace()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        actions()
                        pace()
                    }
                }
            }
        }
    }
}
