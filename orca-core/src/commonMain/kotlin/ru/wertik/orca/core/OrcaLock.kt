package ru.wertik.orca.core

internal expect class OrcaLock() {
    inline fun <T> withLock(block: () -> T): T
}
