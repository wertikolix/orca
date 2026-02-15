package ru.wertik.orca.compose

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaInline
import ru.wertik.orca.core.OrcaTaskState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OrcaInlineTextTest {

    @Test
    fun buildAnnotatedStringKeepsTextAndLinkCallback() {
        var clicked: String? = null
        val style = OrcaStyle()
        val inlines = listOf(
            OrcaInline.Text("open "),
            OrcaInline.Link(
                destination = "https://example.com",
                content = listOf(OrcaInline.Text("docs")),
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
    fun buildAnnotatedStringMapsBoldItalicStrikeAndInlineCodeStyles() {
        val style = OrcaStyle()
        val inlines = listOf(
            OrcaInline.Bold(content = listOf(OrcaInline.Text("bold"))),
            OrcaInline.Text(" "),
            OrcaInline.Italic(content = listOf(OrcaInline.Text("italic"))),
            OrcaInline.Text(" "),
            OrcaInline.Strikethrough(content = listOf(OrcaInline.Text("strike"))),
            OrcaInline.Text(" "),
            OrcaInline.InlineCode(code = "code"),
        )

        val text = buildInlineAnnotatedString(
            inlines = inlines,
            style = style,
            onLinkClick = {},
        )

        assertEquals("bold italic strike code", text.text)

        val hasBold = text.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        val hasItalic = text.spanStyles.any { it.item.fontStyle == FontStyle.Italic }
        val hasStrike = text.spanStyles.any { it.item.textDecoration == TextDecoration.LineThrough }
        val hasInlineCode = text.spanStyles.any { it.item.fontFamily == style.inline.inlineCode.fontFamily }

        assertTrue(hasBold)
        assertTrue(hasItalic)
        assertTrue(hasStrike)
        assertTrue(hasInlineCode)
    }

    @Test
    fun buildAnnotatedStringMapsFootnoteReferenceWithResolvedNumber() {
        val style = OrcaStyle()
        val text = buildInlineAnnotatedString(
            inlines = listOf(
                OrcaInline.Text("value"),
                OrcaInline.FootnoteReference(label = "note"),
            ),
            style = style,
            onLinkClick = {},
            footnoteNumbers = mapOf("note" to 2),
        )

        assertEquals("value[2]", text.text)
        assertTrue(
            text.spanStyles.any { span ->
                span.item.baselineShift == style.inline.footnoteReference.baselineShift
            },
        )
    }

    @Test
    fun footnoteReferenceCanEmitClickCallback() {
        var clicked: String? = null
        val text = buildInlineAnnotatedString(
            inlines = listOf(OrcaInline.FootnoteReference(label = "note")),
            style = OrcaStyle(),
            onLinkClick = {},
            footnoteNumbers = mapOf("note" to 1),
            onFootnoteClick = { label -> clicked = label },
        )

        val link = text.getLinkAnnotations(0, text.length).single().item as LinkAnnotation.Url
        assertEquals("orca-footnote://note", link.url)
        link.linkInteractionListener?.onClick(link)
        assertEquals("note", clicked)
    }

    @Test
    fun buildAnnotatedStringMapsFootnoteReferenceWithLabelFallback() {
        val text = buildInlineAnnotatedString(
            inlines = listOf(OrcaInline.FootnoteReference(label = "note")),
            style = OrcaStyle(),
            onLinkClick = {},
            footnoteNumbers = emptyMap(),
        )

        assertEquals("[note]", text.text)
    }

    @Test
    fun buildAnnotatedStringUsesDestinationWhenLinkContentIsEmpty() {
        val style = OrcaStyle()
        val inlines = listOf(
            OrcaInline.Link(
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
    fun buildAnnotatedStringKeepsNestedStylesInsideLink() {
        val style = OrcaStyle()
        val inlines = listOf(
            OrcaInline.Link(
                destination = "https://example.com",
                content = listOf(
                    OrcaInline.Bold(content = listOf(OrcaInline.Text("A"))),
                    OrcaInline.Text(" "),
                    OrcaInline.Italic(content = listOf(OrcaInline.Text("B"))),
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
    fun buildAnnotatedStringPreservesTwoDistinctLinksAndCallbacks() {
        val style = OrcaStyle()
        val clicked = mutableListOf<String>()
        val inlines = listOf(
            OrcaInline.Link("https://one.example", listOf(OrcaInline.Text("one"))),
            OrcaInline.Text(" "),
            OrcaInline.Link("https://two.example", listOf(OrcaInline.Text("two"))),
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
    fun linkStyleSpanIsPresent() {
        val style = OrcaStyle()
        val text = buildInlineAnnotatedString(
            inlines = listOf(
                OrcaInline.Link(
                    destination = "https://example.com",
                    content = listOf(OrcaInline.Text("click")),
                ),
            ),
            style = style,
            onLinkClick = {},
        )

        val link = text.getLinkAnnotations(0, text.length).single().item as LinkAnnotation.Url
        val linkStyle = link.styles?.style

        assertNotNull(linkStyle)
        assertEquals(style.inline.link.color, linkStyle.color)
        assertEquals(style.inline.link.textDecoration, linkStyle.textDecoration)
    }

    @Test
    fun unsafeLinkSchemeIsRenderedAsPlainTextWithoutClickAnnotation() {
        val style = OrcaStyle()
        val text = buildInlineAnnotatedString(
            inlines = listOf(
                OrcaInline.Link(
                    destination = "javascript:alert(1)",
                    content = listOf(OrcaInline.Text("run")),
                ),
            ),
            style = style,
            onLinkClick = {},
        )

        assertEquals("run", text.text)
        assertTrue(text.getLinkAnnotations(0, text.length).isEmpty())
    }

    @Test
    fun htmlInlineFallbackStripTagsAndDecodesEntities() {
        assertEquals(
            "bold & text",
            htmlInlineFallbackText("<b>bold</b> &amp; text"),
        )
        assertEquals(
            "a\nb",
            htmlInlineFallbackText("a<br/>b"),
        )
    }

    @Test
    fun inlineImageFallbackUsesAltTextOrSource() {
        assertEquals(
            "logo",
            imageInlineFallbackText(
                OrcaInline.Image(
                    source = "https://example.com/logo.png",
                    alt = "logo",
                    title = null,
                ),
            ),
        )
        assertEquals(
            "https://example.com/logo.png",
            imageInlineFallbackText(
                OrcaInline.Image(
                    source = "https://example.com/logo.png",
                    alt = null,
                    title = null,
                ),
            ),
        )
    }

    @Test
    fun orderedListMarkerRespectsStartNumber() {
        assertEquals("5.", listMarkerText(ordered = true, startNumber = 5, index = 0, taskState = null))
        assertEquals("6.", listMarkerText(ordered = true, startNumber = 5, index = 1, taskState = null))
        assertEquals("\u2022", listMarkerText(ordered = false, startNumber = 100, index = 7, taskState = null))
    }

    @Test
    fun taskListMarkerUsesCheckboxSymbols() {
        assertEquals("\u2611", listMarkerText(ordered = false, startNumber = 1, index = 0, taskState = OrcaTaskState.CHECKED))
        assertEquals("\u2610", listMarkerText(ordered = false, startNumber = 1, index = 0, taskState = OrcaTaskState.UNCHECKED))
    }

    @Test
    fun footnoteListMarkerUsesNumericMappingOrLabelFallback() {
        assertEquals("3.", footnoteListMarkerText(label = "a", footnoteNumbers = mapOf("a" to 3)))
        assertEquals("[b]", footnoteListMarkerText(label = "b", footnoteNumbers = emptyMap()))
    }

    @Test
    fun urlSafetyAllowsOnlyConfiguredSchemes() {
        assertTrue(isSafeLinkDestination("https://example.com"))
        assertTrue(isSafeLinkDestination("mailto:hello@example.com"))
        assertFalse(isSafeLinkDestination("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(isSafeLinkDestination("javascript:alert(1)"))

        assertTrue(isSafeImageSource("http://example.com/image.png"))
        assertTrue(isSafeImageSource("https://example.com/image.png"))
        assertFalse(isSafeImageSource("file:///sdcard/image.png"))
        assertFalse(isSafeImageSource("content://media/external/images/media/1"))
    }
}
