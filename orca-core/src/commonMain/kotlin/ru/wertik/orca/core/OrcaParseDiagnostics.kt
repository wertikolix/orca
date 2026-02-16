package ru.wertik.orca.core

data class OrcaParseResult(
    val document: OrcaDocument,
    val diagnostics: OrcaParseDiagnostics = OrcaParseDiagnostics(),
)

data class OrcaParseDiagnostics(
    val warnings: List<OrcaParseWarning> = emptyList(),
    val errors: List<OrcaParseError> = emptyList(),
) {
    val hasWarnings: Boolean
        get() = warnings.isNotEmpty()

    val hasErrors: Boolean
        get() = errors.isNotEmpty()
}

sealed interface OrcaParseWarning {
    data class DepthLimitExceeded(
        val maxTreeDepth: Int,
        val exceededDepth: Int,
    ) : OrcaParseWarning
}

sealed interface OrcaParseError {
    data class ParserFailure(
        val message: String,
    ) : OrcaParseError
}
