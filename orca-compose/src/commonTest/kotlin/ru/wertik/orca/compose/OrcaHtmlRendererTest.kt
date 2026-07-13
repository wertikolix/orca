package ru.wertik.orca.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrcaHtmlRendererTest {

    @Test
    fun plainTextExtractedFromHtml() {
        val result = extractHtmlPlainText("<p>Hello <b>world</b></p>")
        assertEquals("Hello world", result)
    }

    @Test
    fun brTagConvertedToNewline() {
        val result = extractHtmlPlainText("line1<br>line2")
        assertEquals("line1\nline2", result)
    }

    @Test
    fun entitiesDecoded() {
        val result = extractHtmlPlainText("&lt;tag&gt; &amp; &quot;quotes&quot;")
        assertEquals("<tag> & \"quotes\"", result)
    }

    @Test
    fun nestedTagsStripped() {
        val result = extractHtmlPlainText("<div><p><b><i>text</i></b></p></div>")
        assertTrue(result.contains("text"))
    }

    @Test
    fun emptyHtmlProducesEmptyText() {
        val result = extractHtmlPlainText("")
        assertEquals("", result)
    }

    @Test
    fun standaloneHtmlImageParsesQuotedAndUnquotedAttributes() {
        val image = parseHtmlBlockImage(
            "<img src=https://example.com/orca.png alt='Orca &amp; Compose' title=Preview>",
        )

        assertEquals("https://example.com/orca.png", image?.source)
        assertEquals("Orca & Compose", image?.alt)
        assertEquals("Preview", image?.title)
    }

    @Test
    fun figureCaptionBecomesImageCaption() {
        val image = parseHtmlBlockImage(
            """
            <figure>
              <img src="https://example.com/orca.png" alt="Orca">
              <figcaption><b>Flat</b> media renderer</figcaption>
            </figure>
            """.trimIndent(),
        )

        assertEquals("https://example.com/orca.png", image?.source)
        assertEquals("Flat media renderer", image?.title)
    }

    @Test
    fun mixedHtmlContentIsNotCollapsedIntoAnImage() {
        val html = "<p>Keep this paragraph</p><img src=\"https://example.com/orca.png\" alt=\"Orca image\">"
        assertNull(
            parseHtmlBlockImage(html),
        )
        val rendered = renderHtmlToAnnotatedString(
            html = html,
            style = OrcaStyle(),
            onLinkClick = {},
            securityPolicy = OrcaSecurityPolicies.Default,
        )
        assertTrue(rendered.text.contains("Keep this paragraph"))
        assertTrue(rendered.text.contains("Orca image"))
    }
}
