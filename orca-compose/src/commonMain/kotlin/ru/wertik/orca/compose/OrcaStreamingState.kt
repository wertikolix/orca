package ru.wertik.orca.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Token-stream buffer for chat and LLM output.
 *
 * [append] accepts small deltas without publishing a new Compose value for every token. The
 * latest text is published at most once per [frameIntervalMs] while streaming, and [finish]
 * immediately publishes the final buffered value.
 *
 * The current parser still parses complete published snapshots; this state reduces upstream
 * recompositions and redundant parse requests while preserving exact Markdown output.
 */
@Stable
class OrcaStreamingState internal constructor(
    initialMarkdown: String,
    private val frameIntervalMs: Long,
    private val scope: CoroutineScope,
) {
    private val buffer = StringBuilder(initialMarkdown)
    private var flushJob: Job? = null

    /** Latest text snapshot published for rendering. */
    var markdown: String by mutableStateOf(initialMarkdown)
        private set

    /** Whether additional deltas are expected. */
    var isStreaming: Boolean by mutableStateOf(false)
        private set

    /** Begins a new streaming message, discarding buffered text. */
    fun clear() {
        scope.launch {
            flushJob?.cancel()
            flushJob = null
            buffer.clear()
            markdown = ""
            isStreaming = true
        }
    }

    /** Appends a token or chunk; empty deltas are ignored. */
    fun append(delta: String) {
        if (delta.isEmpty()) return
        scope.launch {
            buffer.append(delta)
            isStreaming = true
            if (markdown.isEmpty()) {
                publishNow()
            } else if (flushJob == null) {
                flushJob = scope.launch {
                    if (frameIntervalMs > 0) delay(frameIntervalMs)
                    publishNow()
                }
            }
        }
    }

    /** Replaces all content, useful when restoring or editing a message. */
    fun replace(value: String, streaming: Boolean = false) {
        scope.launch {
            flushJob?.cancel()
            flushJob = null
            buffer.clear()
            buffer.append(value)
            markdown = value
            isStreaming = streaming
        }
    }

    /** Publishes pending content immediately and marks the stream complete. */
    fun finish() {
        scope.launch {
            flushJob?.cancel()
            flushJob = null
            publishNow()
            isStreaming = false
        }
    }

    private fun publishNow() {
        flushJob = null
        val value = buffer.toString()
        if (markdown != value) {
            markdown = value
        }
    }
}

/** Remembers a token-stream buffer whose published snapshots are paced for rendering. */
@Composable
fun rememberOrcaStreamingState(
    initialMarkdown: String = "",
    frameIntervalMs: Long = 80L,
): OrcaStreamingState {
    require(frameIntervalMs >= 0) { "frameIntervalMs must be non-negative" }
    val scope = rememberCoroutineScope()
    return remember(scope, frameIntervalMs) {
        OrcaStreamingState(
            initialMarkdown = initialMarkdown,
            frameIntervalMs = frameIntervalMs,
            scope = scope,
        )
    }
}
