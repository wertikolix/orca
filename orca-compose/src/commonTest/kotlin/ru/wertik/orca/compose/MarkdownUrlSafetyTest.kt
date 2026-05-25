package ru.wertik.orca.compose

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkdownUrlSafetyTest {

    // --- hasAllowedScheme ---

    @Test
    fun `hasAllowedScheme returns true for http`() {
        assertTrue(hasAllowedScheme("http://example.com", setOf("http", "https")))
    }

    @Test
    fun `hasAllowedScheme returns true for https`() {
        assertTrue(hasAllowedScheme("https://example.com", setOf("http", "https")))
    }

    @Test
    fun `hasAllowedScheme returns true for mailto`() {
        assertTrue(hasAllowedScheme("mailto:user@example.com", setOf("http", "https", "mailto")))
    }

    @Test
    fun `hasAllowedScheme returns false for javascript`() {
        assertFalse(hasAllowedScheme("javascript:alert(1)", setOf("http", "https", "mailto")))
    }

    @Test
    fun `hasAllowedScheme returns false for data uri`() {
        assertFalse(hasAllowedScheme("data:text/html,<h1>hi</h1>", setOf("http", "https")))
    }

    @Test
    fun `hasAllowedScheme returns false for ftp`() {
        assertFalse(hasAllowedScheme("ftp://files.example.com", setOf("http", "https", "mailto")))
    }

    @Test
    fun `hasAllowedScheme returns false for empty string`() {
        assertFalse(hasAllowedScheme("", setOf("http", "https")))
    }

    @Test
    fun `hasAllowedScheme returns false when no colon present`() {
        assertFalse(hasAllowedScheme("relative/path", setOf("http", "https")))
    }

    @Test
    fun `hasAllowedScheme returns false when scheme contains digits`() {
        // scheme with digits is not a valid scheme per RFC — all chars must be letters
        assertFalse(hasAllowedScheme("h4x://evil.com", setOf("h4x")))
    }

    @Test
    fun `hasAllowedScheme normalizes uppercase scheme`() {
        assertTrue(hasAllowedScheme("HTTP://example.com", setOf("http", "https")))
        assertTrue(hasAllowedScheme("HTTPS://example.com", setOf("http", "https")))
        assertTrue(hasAllowedScheme("Mailto:user@example.com", setOf("http", "https", "mailto")))
    }

    @Test
    fun `hasAllowedScheme returns false when colon is at position zero`() {
        assertFalse(hasAllowedScheme(":nope", setOf("http", "https")))
    }

    @Test
    fun `hasAllowedScheme returns false for blank string`() {
        assertFalse(hasAllowedScheme("   ", setOf("http", "https")))
    }

    // --- isRelativeUrl ---

    @Test
    fun `isRelativeUrl returns true for relative path`() {
        assertTrue(isRelativeUrl("images/photo.png"))
    }

    @Test
    fun `isRelativeUrl returns true for root-relative path`() {
        assertTrue(isRelativeUrl("/images/photo.png"))
    }

    @Test
    fun `isRelativeUrl returns false for scheme-relative url`() {
        assertFalse(isRelativeUrl("//cdn.example.com/img.png"))
    }

    @Test
    fun `isRelativeUrl returns false for absolute http url`() {
        assertFalse(isRelativeUrl("http://example.com"))
    }

    @Test
    fun `isRelativeUrl returns false for absolute https url`() {
        assertFalse(isRelativeUrl("https://example.com/page"))
    }

    @Test
    fun `isRelativeUrl returns false for empty string`() {
        assertFalse(isRelativeUrl(""))
    }

    @Test
    fun `isRelativeUrl returns true for fragment-only url`() {
        assertTrue(isRelativeUrl("#anchor"))
    }

    @Test
    fun `isRelativeUrl returns true for query-only url`() {
        assertTrue(isRelativeUrl("?q=1"))
    }

    @Test
    fun `isRelativeUrl returns false for javascript scheme`() {
        assertFalse(isRelativeUrl("javascript:void(0)"))
    }

    @Test
    fun `isRelativeUrl returns false for mailto scheme`() {
        assertFalse(isRelativeUrl("mailto:user@example.com"))
    }

    // --- isSafeLinkDestination ---

    @Test
    fun `isSafeLinkDestination returns true for http`() {
        assertTrue(isSafeLinkDestination("http://example.com"))
    }

    @Test
    fun `isSafeLinkDestination returns true for https`() {
        assertTrue(isSafeLinkDestination("https://example.com/page"))
    }

    @Test
    fun `isSafeLinkDestination returns true for mailto`() {
        assertTrue(isSafeLinkDestination("mailto:user@example.com"))
    }

    @Test
    fun `isSafeLinkDestination returns false for javascript`() {
        assertFalse(isSafeLinkDestination("javascript:alert(1)"))
    }

    @Test
    fun `isSafeLinkDestination returns false for data uri`() {
        assertFalse(isSafeLinkDestination("data:text/html,<h1>xss</h1>"))
    }

    @Test
    fun `isSafeLinkDestination returns false for relative url by default policy`() {
        // Default policy only allows http/https/mailto — relative URLs have no scheme so they fail
        assertFalse(isSafeLinkDestination("relative/path"))
    }

    @Test
    fun `isSafeLinkDestination returns false for empty string`() {
        assertFalse(isSafeLinkDestination(""))
    }

    // --- isSafeImageSource ---

    @Test
    fun `isSafeImageSource blocks http by default`() {
        assertFalse(isSafeImageSource("http://example.com/img.png"))
    }

    @Test
    fun `isSafeImageSource blocks https by default`() {
        assertFalse(isSafeImageSource("https://example.com/img.png"))
    }

    @Test
    fun `isSafeImageSource returns false for data uri`() {
        assertFalse(isSafeImageSource("data:image/png;base64,abc123"))
    }

    @Test
    fun `isSafeImageSource returns false for relative path by default policy`() {
        assertFalse(isSafeImageSource("images/photo.png"))
    }

    @Test
    fun `isSafeImageSource returns false for mailto`() {
        // mailto is allowed for links but not images in default policy
        assertFalse(isSafeImageSource("mailto:user@example.com"))
    }

    @Test
    fun `isSafeImageSource returns false for javascript`() {
        assertFalse(isSafeImageSource("javascript:void(0)"))
    }

    @Test
    fun `isSafeImageSource returns false for empty string`() {
        assertFalse(isSafeImageSource(""))
    }
}

class OrcaSecurityPoliciesByAllowedSchemesTest {

    @Test
    fun remoteImagesOptInAllowsHttpImages() {
        assertTrue(OrcaSecurityPolicies.RemoteImages.isAllowed(OrcaUrlType.IMAGE, "https://example.com/img.png"))
        assertTrue(OrcaSecurityPolicies.RemoteImages.isAllowed(OrcaUrlType.LINK, "#section"))
    }

    @Test
    fun byAllowedSchemesAllowsCustomLinkScheme() {
        val policy = OrcaSecurityPolicies.byAllowedSchemes(linkSchemes = setOf("ftp", "https"))
        assertTrue(policy.isAllowed(OrcaUrlType.LINK, "ftp://files.example.com"))
        assertFalse(policy.isAllowed(OrcaUrlType.LINK, "http://example.com"))
    }

    @Test
    fun byAllowedSchemesAllowsCustomImageScheme() {
        val policy = OrcaSecurityPolicies.byAllowedSchemes(imageSchemes = setOf("data", "https"))
        assertTrue(policy.isAllowed(OrcaUrlType.IMAGE, "data:image/png;base64,abc"))
        assertFalse(policy.isAllowed(OrcaUrlType.IMAGE, "http://example.com/img.png"))
    }

    @Test
    fun byAllowedSchemesAllowsRelativeLinksWhenEnabled() {
        val policy = OrcaSecurityPolicies.byAllowedSchemes(allowRelativeLinks = true)
        assertTrue(policy.isAllowed(OrcaUrlType.LINK, "relative/path"))
        assertTrue(policy.isAllowed(OrcaUrlType.LINK, "/root/page"))
        assertTrue(policy.isAllowed(OrcaUrlType.LINK, "#anchor"))
    }

    @Test
    fun byAllowedSchemesBlocksRelativeLinksWhenDisabled() {
        val policy = OrcaSecurityPolicies.byAllowedSchemes(allowRelativeLinks = false)
        assertFalse(policy.isAllowed(OrcaUrlType.LINK, "relative/path"))
        assertFalse(policy.isAllowed(OrcaUrlType.LINK, "/root/page"))
    }

    @Test
    fun byAllowedSchemesAllowsRelativeImagesWhenEnabled() {
        val policy = OrcaSecurityPolicies.byAllowedSchemes(allowRelativeImages = true)
        assertTrue(policy.isAllowed(OrcaUrlType.IMAGE, "images/photo.png"))
        assertTrue(policy.isAllowed(OrcaUrlType.IMAGE, "/static/img.jpg"))
    }

    @Test
    fun byAllowedSchemesBlocksRelativeImagesWhenDisabled() {
        val policy = OrcaSecurityPolicies.byAllowedSchemes(allowRelativeImages = false)
        assertFalse(policy.isAllowed(OrcaUrlType.IMAGE, "images/photo.png"))
    }

    @Test
    fun byAllowedSchemesNormalizesSchemeInputToLowercase() {
        val policy = OrcaSecurityPolicies.byAllowedSchemes(linkSchemes = setOf("HTTPS", "HTTP"))
        assertTrue(policy.isAllowed(OrcaUrlType.LINK, "https://example.com"))
        assertTrue(policy.isAllowed(OrcaUrlType.LINK, "http://example.com"))
    }

    @Test
    fun byAllowedSchemesDoesNotAllowSchemeRelativeUrlAsRelative() {
        val policy = OrcaSecurityPolicies.byAllowedSchemes(allowRelativeLinks = true)
        // //host/path is scheme-relative, not relative — should NOT be allowed unless https/http is in schemes
        assertFalse(policy.isAllowed(OrcaUrlType.LINK, "//cdn.example.com/img.png"))
    }

    @Test
    fun byAllowedSchemesBlocksJavascriptEvenWithRelativeLinksEnabled() {
        val policy = OrcaSecurityPolicies.byAllowedSchemes(allowRelativeLinks = true)
        // javascript: has a scheme so it's not relative — blocked unless explicitly in linkSchemes
        assertFalse(policy.isAllowed(OrcaUrlType.LINK, "javascript:alert(1)"))
    }
}
