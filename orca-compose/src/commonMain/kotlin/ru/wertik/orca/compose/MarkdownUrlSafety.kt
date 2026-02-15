package ru.wertik.orca.compose

private val safeLinkSchemes = setOf("http", "https", "mailto")
private val safeImageSchemes = setOf("http", "https")

internal fun isSafeLinkDestination(destination: String): Boolean {
    return hasAllowedScheme(
        value = destination,
        allowedSchemes = safeLinkSchemes,
    )
}

internal fun isSafeImageSource(source: String): Boolean {
    return hasAllowedScheme(
        value = source,
        allowedSchemes = safeImageSchemes,
    )
}

private fun hasAllowedScheme(
    value: String,
    allowedSchemes: Set<String>,
): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return false
    val colonIndex = trimmed.indexOf(':')
    if (colonIndex <= 0) return false
    val scheme = trimmed.substring(0, colonIndex).lowercase()
    if (!scheme.all { it.isLetter() }) return false
    return scheme in allowedSchemes
}
