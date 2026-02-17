package ru.wertik.orca.core

// wasmJs is single-threaded; no synchronization needed.
internal actual class OrcaLock {
    actual fun <T> withLock(block: () -> T): T = block()
}
