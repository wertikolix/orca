package ru.wertik.orca.core

fun interface OrcaParser {
    fun parse(input: String): OrcaDocument

    fun cacheKey(): Any = this::class
}
