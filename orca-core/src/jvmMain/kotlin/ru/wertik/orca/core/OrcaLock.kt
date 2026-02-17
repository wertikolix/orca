package ru.wertik.orca.core

internal actual class OrcaLock {
    private val monitor = Any()

    actual inline fun <T> withLock(block: () -> T): T {
        return synchronized(monitor) { block() }
    }
}
