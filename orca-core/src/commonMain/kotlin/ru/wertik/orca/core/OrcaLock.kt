package ru.wertik.orca.core

internal expect class OrcaLock() {
    fun <T> withLock(block: () -> T): T
}
