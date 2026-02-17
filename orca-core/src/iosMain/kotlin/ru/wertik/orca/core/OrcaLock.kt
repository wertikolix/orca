package ru.wertik.orca.core

import kotlin.concurrent.AtomicInt

internal actual class OrcaLock {
    private val locked = AtomicInt(0)

    actual inline fun <T> withLock(block: () -> T): T {
        while (!locked.compareAndSet(0, 1)) {
            // spin
        }
        try {
            return block()
        } finally {
            locked.compareAndSet(1, 0)
        }
    }
}
