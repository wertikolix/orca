package ru.wertik.fhmd.compose.android

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import ru.wertik.fhmd.core.CommonmarkFhMdParser
import ru.wertik.fhmd.core.FhMdBlock
import ru.wertik.fhmd.core.FhMdInline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FhMdInlineTextTest {

    @Test
    fun `build annotated string keeps text and link callback`() {
        var clicked: String? = null
        val style = FhMdStyle()
        val inlines = listOf(
            FhMdInline.Text("open "),
            FhMdInline.Link(
                destination = "https://example.com",
                content = listOf(FhMdInline.Text("docs")),
            ),
        )

        val text = buildInlineAnnotatedString(
            inlines = inlines,
            style = style,
            onLinkClick = { clicked = it },
        )

        assertEquals("open docs", text.text)
        val links = text.getLinkAnnotations(0, text.length)
        assertEquals(1, links.size)
        val link = links.single().item as LinkAnnotation.Url
        assertEquals("https://example.com", link.url)

        link.linkInteractionListener?.onClick(link)
        assertEquals("https://example.com", clicked)
    }

    @Test
    fun `build annotated string maps bold italic and inline code styles`() {
        val style = FhMdStyle()
        val inlines = listOf(
            FhMdInline.Bold(content = listOf(FhMdInline.Text("bold"))),
            FhMdInline.Text(" "),
            FhMdInline.Italic(content = listOf(FhMdInline.Text("italic"))),
            FhMdInline.Text(" "),
            FhMdInline.InlineCode(code = "code"),
        )

        val text = buildInlineAnnotatedString(
            inlines = inlines,
            style = style,
            onLinkClick = {},
        )

        assertEquals("bold italic code", text.text)

        val hasBold = text.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        val hasItalic = text.spanStyles.any { it.item.fontStyle == FontStyle.Italic }
        val hasInlineCode = text.spanStyles.any { it.item.fontFamily == style.inlineCode.fontFamily }

        assertTrue(hasBold)
        assertTrue(hasItalic)
        assertTrue(hasInlineCode)
    }

    @Test
    fun `build annotated string uses destination when link content is empty`() {
        val style = FhMdStyle()
        val inlines = listOf(
            FhMdInline.Link(
                destination = "https://fallback.example",
                content = emptyList(),
            ),
        )

        val text = buildInlineAnnotatedString(
            inlines = inlines,
            style = style,
            onLinkClick = {},
        )

        assertEquals("https://fallback.example", text.text)
        val link = text.getLinkAnnotations(0, text.length).single().item as LinkAnnotation.Url
        assertEquals("https://fallback.example", link.url)
    }

    @Test
    fun `build annotated string keeps nested styles inside link`() {
        val style = FhMdStyle()
        val inlines = listOf(
            FhMdInline.Link(
                destination = "https://example.com",
                content = listOf(
                    FhMdInline.Bold(content = listOf(FhMdInline.Text("A"))),
                    FhMdInline.Text(" "),
                    FhMdInline.Italic(content = listOf(FhMdInline.Text("B"))),
                ),
            ),
        )

        val text = buildInlineAnnotatedString(
            inlines = inlines,
            style = style,
            onLinkClick = {},
        )

        assertEquals("A B", text.text)
        assertEquals(1, text.getLinkAnnotations(0, text.length).size)
        assertTrue(text.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(text.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun `build annotated string preserves two distinct links and callbacks`() {
        val style = FhMdStyle()
        val clicked = mutableListOf<String>()
        val inlines = listOf(
            FhMdInline.Link("https://one.example", listOf(FhMdInline.Text("one"))),
            FhMdInline.Text(" "),
            FhMdInline.Link("https://two.example", listOf(FhMdInline.Text("two"))),
        )

        val text = buildInlineAnnotatedString(
            inlines = inlines,
            style = style,
            onLinkClick = clicked::add,
        )

        assertEquals("one two", text.text)
        val links = text.getLinkAnnotations(0, text.length).map { it.item as LinkAnnotation.Url }
        assertEquals(2, links.size)
        assertEquals("https://one.example", links[0].url)
        assertEquals("https://two.example", links[1].url)

        links.forEach { it.linkInteractionListener?.onClick(it) }
        assertEquals(listOf("https://one.example", "https://two.example"), clicked)
    }

    @Test
    fun `link style span is present`() {
        val style = FhMdStyle()
        val text = buildInlineAnnotatedString(
            inlines = listOf(
                FhMdInline.Link(
                    destination = "https://example.com",
                    content = listOf(FhMdInline.Text("click")),
                ),
            ),
            style = style,
            onLinkClick = {},
        )

        val link = text.getLinkAnnotations(0, text.length).single().item as LinkAnnotation.Url
        val linkStyle = link.styles?.style

        assertNotNull(linkStyle)
        assertEquals(style.linkStyle.color, linkStyle?.color)
        assertEquals(style.linkStyle.textDecoration, linkStyle?.textDecoration)
    }

    @Test
    fun `unsafe link scheme is rendered as plain text without click annotation`() {
        val style = FhMdStyle()
        val text = buildInlineAnnotatedString(
            inlines = listOf(
                FhMdInline.Link(
                    destination = "javascript:alert(1)",
                    content = listOf(FhMdInline.Text("run")),
                ),
            ),
            style = style,
            onLinkClick = {},
        )

        assertEquals("run", text.text)
        assertTrue(text.getLinkAnnotations(0, text.length).isEmpty())
    }

    @Test
    fun `parser to inline render integration keeps links and inline code`() {
        val parser = CommonmarkFhMdParser()
        val document = parser.parse("See [docs](https://example.com) and `code`.")
        val paragraph = document.blocks.single() as FhMdBlock.Paragraph

        val rendered = buildInlineAnnotatedString(
            inlines = paragraph.content,
            style = FhMdStyle(),
            onLinkClick = {},
        )

        assertEquals("See docs and code.", rendered.text)
        assertEquals(1, rendered.getLinkAnnotations(0, rendered.length).size)
        assertNotNull(
            rendered.spanStyles.firstOrNull { it.item.fontFamily == FhMdStyle().inlineCode.fontFamily },
        )
    }

    @Test
    fun `inline image fallback uses alt text or source`() {
        assertEquals(
            "logo",
            imageInlineFallbackText(
                FhMdInline.Image(
                    source = "https://example.com/logo.png",
                    alt = "logo",
                    title = null,
                ),
            ),
        )
        assertEquals(
            "https://example.com/logo.png",
            imageInlineFallbackText(
                FhMdInline.Image(
                    source = "https://example.com/logo.png",
                    alt = null,
                    title = null,
                ),
            ),
        )
    }

    @Test
    fun `parser to inline render integration maps inline image to fallback text`() {
        val parser = CommonmarkFhMdParser()
        val document = parser.parse("before ![logo](https://example.com/logo.png) after")
        val paragraph = document.blocks.single() as FhMdBlock.Paragraph

        val rendered = buildInlineAnnotatedString(
            inlines = paragraph.content,
            style = FhMdStyle(),
            onLinkClick = {},
        )

        assertEquals("before logo after", rendered.text)
    }

    @Test
    fun `ordered list marker respects start number`() {
        assertEquals("5.", listMarkerText(ordered = true, startNumber = 5, index = 0))
        assertEquals("6.", listMarkerText(ordered = true, startNumber = 5, index = 1))
        assertEquals("•", listMarkerText(ordered = false, startNumber = 100, index = 7))
    }

    @Test
    fun `url safety allows only configured schemes`() {
        assertTrue(isSafeLinkDestination("https://example.com"))
        assertTrue(isSafeLinkDestination("mailto:hello@example.com"))
        assertFalse(isSafeLinkDestination("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(isSafeLinkDestination("javascript:alert(1)"))

        assertTrue(isSafeImageSource("http://example.com/image.png"))
        assertTrue(isSafeImageSource("https://example.com/image.png"))
        assertFalse(isSafeImageSource("file:///sdcard/image.png"))
        assertFalse(isSafeImageSource("content://media/external/images/media/1"))
    }

    @Test
    fun `parser to table render integration keeps table cell inline formatting`() {
        val parser = CommonmarkFhMdParser()
        val document = parser.parse(
            """
            | feature |
            |:--------|
            | **bold** [docs](https://example.com) |
            """.trimIndent(),
        )
        val table = document.blocks.single() as FhMdBlock.Table

        val rendered = buildInlineAnnotatedString(
            inlines = table.rows[0][0].content,
            style = FhMdStyle(),
            onLinkClick = {},
        )

        assertEquals("bold docs", rendered.text)
        assertEquals(1, rendered.getLinkAnnotations(0, rendered.length).size)
        assertTrue(rendered.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
    }
}
