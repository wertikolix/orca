package ru.wertik.orca.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OrcaMathParserTest {
    private val parser = OrcaMarkdownParser()

    @Test
    fun parsesInlineMathWithoutTouchingSurroundingText() {
        val paragraph = assertIs<OrcaBlock.Paragraph>(parser.parse("Area is ${'$'}x^2 + y^2${'$'} now.").blocks.single())

        assertEquals(OrcaInline.Math("x^2 + y^2"), paragraph.content[1])
    }

    @Test
    fun keepsIncompleteOrCurrencyLikeDollarsAsText() {
        val paragraph = assertIs<OrcaBlock.Paragraph>(parser.parse("It costs $5 and $6 today").blocks.single())

        assertFalse(paragraph.content.any { it is OrcaInline.Math })
    }

    @Test
    fun parsesNumericInlineMathWithCommands() {
        val paragraph = assertIs<OrcaBlock.Paragraph>(
            parser.parse("Матрица $2 \\times 2$:").blocks.single(),
        )

        assertEquals(OrcaInline.Math("2 \\times 2"), paragraph.content[1])
    }

    @Test
    fun parsesDisplayMathBlock() {
        val document = parser.parse("""
            Before.

            $$
            \frac{a}{b} = c
            $$

            After.
        """.trimIndent())

        assertTrue(document.blocks.any { it == OrcaBlock.Math("\\frac{a}{b} = c") })
    }

    @Test
    fun leavesUnclosedDisplayMathReadableDuringStreaming() {
        val document = parser.parse("$$\n\\sum_{i=1}^{n} i")

        assertFalse(document.blocks.any { it is OrcaBlock.Math })
    }

    @Test
    fun doesNotParseDisplayMathInsideCodeFence() {
        val document = parser.parse("""
            ```text
            $$
            x^2
            $$
            ```
        """.trimIndent())

        assertIs<OrcaBlock.CodeBlock>(document.blocks.single())
        assertFalse(document.blocks.any { it is OrcaBlock.Math })
    }
}
