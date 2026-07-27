package ru.wertik.orca.benchmarks

import java.io.File
import kotlin.system.exitProcess

/**
 * Entry point for `./gradlew :orca-benchmarks:run`.
 *
 * ```
 * --check            fail the run when a scaling or streaming check regresses
 * --quick            shorter warmup/measure budgets, for a local sanity run
 * --markdown <file>  write the report as a markdown table (CI step summary)
 * --json <file>      write the raw measurements
 * ```
 */
fun main(args: Array<String>) {
    val options = args.toList()
    val check = "--check" in options
    val quick = "--quick" in options
    val markdownPath = options.valueOf("--markdown")
    val jsonPath = options.valueOf("--json")

    val report = runBenchmarks(quick = quick)

    println(renderTable(report))
    println()
    println(renderChecks(report))

    markdownPath?.let { path -> File(path).writeText(renderMarkdown(report)) }
    jsonPath?.let { path -> File(path).writeText(renderJson(report)) }

    if (check && report.failed.isNotEmpty()) {
        println()
        println("${report.failed.size} check(s) failed.")
        exitProcess(1)
    }
}

private fun List<String>.valueOf(flag: String): String? {
    val index = indexOf(flag)
    return if (index >= 0 && index + 1 < size) this[index + 1] else null
}

private fun renderTable(report: BenchmarkReport): String {
    val nameWidth = report.measurements.maxOf { measurement -> measurement.name.length }
    return buildString {
        append("case".padEnd(nameWidth))
        append("   best ms   median ms      MB/s   iters\n")
        append("-".repeat(nameWidth + 40))
        append('\n')
        report.measurements.forEach { measurement ->
            append(measurement.name.padEnd(nameWidth))
            append(format(measurement.bestMillis).padStart(10))
            append(format(measurement.medianNanos / 1_000_000.0).padStart(12))
            append(format(measurement.megabytesPerSecond).padStart(10))
            append(measurement.iterations.toString().padStart(8))
            append('\n')
            measurement.notes.forEach { (key, value) ->
                append("  ".padEnd(nameWidth))
                append("  $key: $value\n")
            }
        }
    }
}

private fun renderChecks(report: BenchmarkReport): String = buildString {
    report.checks.forEach { check ->
        append(if (check.passed) "PASS  " else "FAIL  ")
        append(check.name)
        append(" — ")
        append(check.detail)
        append('\n')
    }
}

private fun renderMarkdown(report: BenchmarkReport): String = buildString {
    append("## Orca benchmarks\n\n")
    append("| case | best ms | median ms | MB/s | iterations |\n")
    append("|---|---:|---:|---:|---:|\n")
    report.measurements.forEach { measurement ->
        append("| `${measurement.name}` | ${format(measurement.bestMillis)} | ")
        append("${format(measurement.medianNanos / 1_000_000.0)} | ")
        append("${format(measurement.megabytesPerSecond)} | ${measurement.iterations} |\n")
    }
    append("\n### Checks\n\n")
    report.checks.forEach { check ->
        append("- ${if (check.passed) "✅" else "❌"} **${check.name}** — ${check.detail}\n")
    }
    val notes = report.measurements.filter { measurement -> measurement.notes.isNotEmpty() }
    if (notes.isNotEmpty()) {
        append("\n### Counters\n\n")
        notes.forEach { measurement ->
            append("- `${measurement.name}`: ")
            append(measurement.notes.entries.joinToString(", ") { (key, value) -> "$key=$value" })
            append('\n')
        }
    }
}

private fun renderJson(report: BenchmarkReport): String = buildString {
    append("{\n  \"measurements\": [\n")
    report.measurements.forEachIndexed { index, measurement ->
        append("    {")
        append("\"name\": \"${measurement.name}\", ")
        append("\"bestNanos\": ${measurement.bestNanos}, ")
        append("\"medianNanos\": ${measurement.medianNanos}, ")
        append("\"bytes\": ${measurement.bytesPerOp}, ")
        append("\"iterations\": ${measurement.iterations}")
        append("}")
        if (index != report.measurements.lastIndex) append(",")
        append('\n')
    }
    append("  ],\n  \"checks\": [\n")
    report.checks.forEachIndexed { index, check ->
        append("    {")
        append("\"name\": \"${check.name}\", ")
        append("\"passed\": ${check.passed}, ")
        append("\"detail\": \"${check.detail.replace("\"", "'")}\"")
        append("}")
        if (index != report.checks.lastIndex) append(",")
        append('\n')
    }
    append("  ]\n}\n")
}
