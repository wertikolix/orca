package ru.wertik.orca.compose

internal val DEFAULT_SAFE_LINK_SCHEMES = setOf("http", "https", "mailto")
internal val DEFAULT_SAFE_IMAGE_SCHEMES = setOf("http", "https")

internal fun isSafeLinkDestination(destination: String): Boolean {
    return OrcaSecurityPolicies.Default.isAllowed(
        type = OrcaUrlType.LINK,
        value = destination,
    )
}

internal fun isSafeImageSource(source: String): Boolean {
    return OrcaSecurityPolicies.Default.isAllowed(
        type = OrcaUrlType.IMAGE,
        value = source,
    )
}

internal fun hasAllowedScheme(
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
