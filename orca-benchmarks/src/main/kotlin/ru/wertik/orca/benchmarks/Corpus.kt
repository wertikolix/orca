package ru.wertik.orca.benchmarks

/**
 * Deterministic document generators.
 *
 * Every shape here exists because it once was, or still is, a place where the parser
 * can go quadratic: long paragraphs (the definition-list scanner), definition lists
 * themselves, unmatched link brackets (the inline scanner), and streamed code fences
 * (the incremental session).
 */
object Corpus {
    private val words = listOf(
        "orca", "markdown", "parser", "streaming", "compose", "render", "inline", "block",
        "kotlin", "multiplatform", "document", "heading", "table", "fence", "anchor",
    )

    private fun sentence(seed: Int, length: Int = 12): String {
        return (0 until length).joinToString(" ") { index -> words[(seed * 31 + index * 7) % words.size] }
    }

    /** Mixed real-world shape: headings, prose, lists, links, tables and code. */
    fun realisticDocument(sections: Int): String = buildString {
        append("# Orca benchmark document\n\n")
        repeat(sections) { section ->
            append("## Section $section\n\n")
            append(sentence(section))
            append(" with **bold**, _italic_, `code` and a [link](https://example.com/$section).\n\n")
            append(sentence(section + 1))
            append("\n\n")
            append("- first item ${sentence(section + 2, 6)}\n")
            append("- second item with `inline code`\n")
            append("- third item with a [reference](https://example.com/$section#anchor)\n\n")
            append("| column | value |\n|---|---|\n| a | $section |\n| b | ${section * 2} |\n\n")
            append("```kotlin\nfun section$section(): Int {\n    return $section\n}\n```\n\n")
            append("> ${sentence(section + 3, 8)}\n\n")
        }
    }

    /**
     * One paragraph with no blank lines at all. Line-oriented pre-passes that probe
     * every line for a structure they might start are quadratic exactly here.
     */
    fun singleParagraph(lines: Int): String =
        (0 until lines).joinToString("\n") { index -> sentence(index) }

    /** Definition lists, the construct the pre-pass is actually looking for. */
    fun definitionLists(entries: Int): String =
        (0 until entries).joinToString("\n\n") { index ->
            "Term $index\n: ${sentence(index, 8)}\n: ${sentence(index + 1, 8)}"
        }

    /** Wide tables, the other line-heavy construct. */
    fun tables(rows: Int): String = buildString {
        append("| column a | column b | column c |\n|---|---|---|\n")
        repeat(rows) { row ->
            append("| ${sentence(row, 3)} | ${sentence(row + 1, 3)} | $row |\n")
        }
    }

    /** Fenced code, the dominant shape of an assistant answer. */
    fun codeBlocks(lines: Int): String = buildString {
        append("Intro paragraph before the code.\n\n")
        append("```kotlin\n")
        repeat(lines) { line ->
            append("fun handler$line(request: Request): Response = Response(status = $line)\n")
        }
        append("```\n\nOutro paragraph after the code.\n")
    }

    /** Unmatched link openers: the inline scanner's worst case. */
    fun bracketBomb(openers: Int): String = "[".repeat(openers)

    /** Blockquote nesting: one level of block-parser recursion per marker. */
    fun nestedQuotes(levels: Int): String = "> ".repeat(levels) + "text"

    /** List nesting, which costs the block parser far more than quotes do. */
    fun nestedLists(levels: Int): String =
        (0 until levels).joinToString("\n") { level -> " ".repeat(level * 2) + "- item" }

    /** Same size, but balanced, so the scanner has nothing to backtrack over. */
    fun balancedBrackets(pairs: Int): String = "[]".repeat(pairs)

    /** A typical streamed answer: prose, a long fence, a list, a second fence. */
    fun assistantAnswer(): String = buildString {
        append("Sure — here is what that looks like.\n")
        append("```kotlin\n")
        repeat(60) { line ->
            append("fun handler$line(request: Request): Response = Response(status = $line)\n")
        }
        append("```\n")
        append("The snippet above wires the handler into the router.\n\n")
        append("## Notes\n\n")
        repeat(8) { note -> append("- ${sentence(note, 9)}\n") }
        append("\n```bash\n")
        repeat(20) { line -> append("./gradlew :module$line:assemble --no-daemon\n") }
        append("```\n\n")
        append("Let me know if you want the streaming variant as well.\n")
    }
}
