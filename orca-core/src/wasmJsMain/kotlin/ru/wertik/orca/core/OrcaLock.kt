package ru.wertik.orca.core

// wasmJs is single-threaded; no synchronization needed.
internal actual class OrcaLock {
    actual inline fun <T> withLock(block: () -> T): T = block()
}
