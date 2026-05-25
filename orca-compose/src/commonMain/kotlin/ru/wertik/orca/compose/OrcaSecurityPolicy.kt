package ru.wertik.orca.compose

/** Category of URL being checked by [OrcaSecurityPolicy]. */
enum class OrcaUrlType {
    /** Clickable link destination (e.g. `<a href="...">`). */
    LINK,
    /** Image source URL (e.g. `<img src="...">`). */
    IMAGE,
}

/**
 * Determines whether a URL is safe to render as a clickable link or loadable image.
 *
 * Unsafe URLs are rendered as plain text fallback instead of active elements.
 * @see OrcaSecurityPolicies
 */
fun interface OrcaSecurityPolicy {
    /**
     * Returns `true` if the given [value] is allowed for the specified URL [type].
     */
    fun isAllowed(
        type: OrcaUrlType,
        value: String,
    ): Boolean
}

/**
 * Built-in security policy implementations.
 *
 * [Default] allows `http`, `https`, `mailto`, and fragment targets for links, while
 * blocking all image loading. This prevents untrusted Markdown from triggering network
 * requests merely by being displayed.
 *
 * Schemes not in the allow-list are **blocked by default**. This includes potentially
 * dangerous schemes such as `javascript:` and `data:`. If you need `data:` URLs (e.g.
 * for inline base64 images), explicitly opt in via [byAllowedSchemes]:
 *
 * ```kotlin
 * OrcaSecurityPolicies.byAllowedSchemes(
 *     imageSchemes = setOf("http", "https", "data"),
 * )
 * ```
 *
 * **Warning:** allowing `data:` for links enables injection of arbitrary content
 * (e.g. `data:text/html,...`). Only allow it for image sources when you trust the input.
 */
object OrcaSecurityPolicies {
    /** Default policy: allows safe links and blocks images until explicitly enabled. */
    val Default: OrcaSecurityPolicy = OrcaSecurityPolicy { type, value ->
        when (type) {
            OrcaUrlType.LINK -> isFragmentLink(value) || hasAllowedScheme(value, DEFAULT_SAFE_LINK_SCHEMES)

            OrcaUrlType.IMAGE -> hasAllowedScheme(
                value = value,
                allowedSchemes = DEFAULT_SAFE_IMAGE_SCHEMES,
            )
        }
    }

    /** Convenient opt-in policy enabling remote HTTP(S) images in trusted content. */
    val RemoteImages: OrcaSecurityPolicy = byAllowedSchemes(
        imageSchemes = setOf("http", "https"),
    )

    /**
     * Creates a policy that allows URLs matching the given scheme sets.
     *
     * @param linkSchemes allowed schemes for links (default: http, https, mailto)
     * @param imageSchemes allowed schemes for images (default: none; opt in explicitly)
     * @param allowRelativeLinks if `true`, relative URLs are allowed for links
     * @param allowRelativeImages if `true`, relative URLs are allowed for images
     */
    fun byAllowedSchemes(
        linkSchemes: Set<String> = DEFAULT_SAFE_LINK_SCHEMES,
        imageSchemes: Set<String> = DEFAULT_SAFE_IMAGE_SCHEMES,
        allowRelativeLinks: Boolean = false,
        allowRelativeImages: Boolean = false,
    ): OrcaSecurityPolicy {
        val normalizedLinkSchemes = linkSchemes.mapTo(linkedSetOf()) { it.lowercase() }
        val normalizedImageSchemes = imageSchemes.mapTo(linkedSetOf()) { it.lowercase() }
        return OrcaSecurityPolicy { type, value ->
            when (type) {
                OrcaUrlType.LINK -> isFragmentLink(value) || hasAllowedScheme(value, normalizedLinkSchemes)
                    || (allowRelativeLinks && isRelativeUrl(value))
                OrcaUrlType.IMAGE -> hasAllowedScheme(value, normalizedImageSchemes)
                    || (allowRelativeImages && isRelativeUrl(value))
            }
        }
    }
}
