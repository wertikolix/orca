package ru.wertik.orca.core

/**
 * Result of parsing markdown, bundling the [document] with any [diagnostics].
 *
 * @property document The parsed [OrcaDocument].
 * @property diagnostics Warnings and errors collected during parsing. Empty by default.
 */
data class OrcaParseResult(
    val document: OrcaDocument,
    val diagnostics: OrcaParseDiagnostics = OrcaParseDiagnostics(),
)

/**
 * Diagnostics collected during a parse pass.
 *
 * @property warnings Non-fatal issues (e.g. depth limit exceeded). Defaults to empty.
 * @property errors Fatal issues that prevented full parsing. Defaults to empty.
 */
data class OrcaParseDiagnostics(
    val warnings: List<OrcaParseWarning> = emptyList(),
    val errors: List<OrcaParseError> = emptyList(),
) {
    /** `true` when at least one warning was recorded. */
    val hasWarnings: Boolean
        get() = warnings.isNotEmpty()

    /** `true` when at least one error was recorded. */
    val hasErrors: Boolean
        get() = errors.isNotEmpty()
}

/** Non-fatal warning produced during parsing. */
sealed interface OrcaParseWarning {
    /**
     * AST nesting exceeded the configured limit.
     *
     * @property maxTreeDepth The configured maximum depth.
     * @property exceededDepth The actual depth that was reached.
     */
    data class DepthLimitExceeded(
        val maxTreeDepth: Int,
        val exceededDepth: Int,
    ) : OrcaParseWarning

    /**
     * One or more blocks carried more unmatched `[` than the inline scanner is allowed
     * to backtrack over, and were kept as plain text instead of being parsed.
     *
     * @property maxInlineBracketDepth The configured maximum run of unmatched openers.
     * @property exceededDepth The deepest run that was found.
     * @property guardedBlocks How many blocks were kept as plain text.
     */
    data class InlineBracketLimitExceeded(
        val maxInlineBracketDepth: Int,
        val exceededDepth: Int,
        val guardedBlocks: Int,
    ) : OrcaParseWarning
}

/** Fatal error that prevented the parser from producing a complete document. */
sealed interface OrcaParseError {
    /**
     * The underlying parser threw an exception.
     *
     * @property message Error description from the caught exception.
     */
    data class ParserFailure(
        val message: String,
    ) : OrcaParseError
}
