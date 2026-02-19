package ru.wertik.orca.compose

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
