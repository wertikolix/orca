package ru.wertik.orca.compose

enum class OrcaUrlType {
    LINK,
    IMAGE,
}

fun interface OrcaSecurityPolicy {
    fun isAllowed(
        type: OrcaUrlType,
        value: String,
    ): Boolean
}

object OrcaSecurityPolicies {
    val Default: OrcaSecurityPolicy = OrcaSecurityPolicy { type, value ->
        when (type) {
            OrcaUrlType.LINK -> hasAllowedScheme(
                value = value,
                allowedSchemes = DEFAULT_SAFE_LINK_SCHEMES,
            )

            OrcaUrlType.IMAGE -> hasAllowedScheme(
                value = value,
                allowedSchemes = DEFAULT_SAFE_IMAGE_SCHEMES,
            )
        }
    }

    fun byAllowedSchemes(
        linkSchemes: Set<String> = DEFAULT_SAFE_LINK_SCHEMES,
        imageSchemes: Set<String> = DEFAULT_SAFE_IMAGE_SCHEMES,
    ): OrcaSecurityPolicy {
        val normalizedLinkSchemes = linkSchemes.mapTo(linkedSetOf()) { it.lowercase() }
        val normalizedImageSchemes = imageSchemes.mapTo(linkedSetOf()) { it.lowercase() }
        return OrcaSecurityPolicy { type, value ->
            when (type) {
                OrcaUrlType.LINK -> hasAllowedScheme(value, normalizedLinkSchemes)
                OrcaUrlType.IMAGE -> hasAllowedScheme(value, normalizedImageSchemes)
            }
        }
    }
}
