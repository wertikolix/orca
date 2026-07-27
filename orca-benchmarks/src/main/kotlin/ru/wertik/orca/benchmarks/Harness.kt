package ru.wertik.orca.benchmarks

/** One measured benchmark case. */
data class Measurement(
    val name: String,
    /** Best observed time per operation. The minimum is the most stable statistic here. */
    val bestNanos: Long,
    val medianNanos: Long,
    val iterations: Int,
    /** Bytes of markdown processed by one operation, for throughput reporting. */
    val bytesPerOp: Int,
    /** Free-form counters a case wants to report (reused blocks, fast paths, …). */
    val notes: Map<String, String> = emptyMap(),
) {
    val megabytesPerSecond: Double
        get() = if (bestNanos == 0L) 0.0 else bytesPerOp * 1_000.0 / bestNanos

    val bestMillis: Double
        get() = bestNanos / 1_000_000.0
}

/**
 * Small adaptive harness.
 *
 * Every case is warmed up until the JIT has settled and then measured until a time
 * budget is spent, reporting the minimum. CI runners are noisy in the upward direction
 * only: a run can be delayed by a neighbour, never made faster than the machine allows,
 * which is why the minimum (not the mean) is what regressions are compared against.
 */
class Harness(
    private val warmupNanos: Long = 400_000_000,
    private val measureNanos: Long = 1_000_000_000,
    private val minIterations: Int = 5,
    private val maxIterations: Int = 200,
    private val quick: Boolean = false,
) {
    fun measure(
        name: String,
        bytesPerOp: Int,
        notes: () -> Map<String, String> = ::emptyMap,
        body: () -> Any?,
    ): Measurement {
        val warmupBudget = if (quick) warmupNanos / 4 else warmupNanos
        val measureBudget = if (quick) measureNanos / 4 else measureNanos

        var elapsed = 0L
        var warmupRuns = 0
        while (elapsed < warmupBudget || warmupRuns < 2) {
            elapsed += timed(body)
            warmupRuns += 1
            if (warmupRuns >= maxIterations) break
        }

        val samples = ArrayList<Long>(minIterations)
        elapsed = 0L
        while ((elapsed < measureBudget || samples.size < minIterations) && samples.size < maxIterations) {
            val sample = timed(body)
            samples += sample
            elapsed += sample
        }
        samples.sort()

        return Measurement(
            name = name,
            bestNanos = samples.first(),
            medianNanos = samples[samples.size / 2],
            iterations = samples.size,
            bytesPerOp = bytesPerOp,
            notes = notes(),
        )
    }

    private fun timed(body: () -> Any?): Long {
        val start = System.nanoTime()
        val result = body()
        val end = System.nanoTime()
        sink = result
        return end - start
    }

    companion object {
        /** Keeps the JIT from optimising away work whose result is unused. */
        @Volatile
        @JvmStatic
        var sink: Any? = null
    }
}

/** A pass/fail assertion evaluated over measurements, reported by `--check`. */
data class Check(
    val name: String,
    val detail: String,
    val passed: Boolean,
)

/**
 * Compares how a case scales when its input grows by [sizeFactor].
 *
 * Ratios are used instead of absolute budgets on purpose: they say the same thing on a
 * laptop and on a shared CI runner. Linear work lands near [sizeFactor]; quadratic work
 * lands near `sizeFactor * sizeFactor`, which is what the limit is set to catch.
 */
fun scalingCheck(
    name: String,
    small: Measurement,
    large: Measurement,
    sizeFactor: Int,
    maxRatio: Double,
): Check {
    val ratio = large.bestNanos.toDouble() / small.bestNanos.toDouble()
    return Check(
        name = name,
        detail = "x$sizeFactor input took x${format(ratio)} time (limit x${format(maxRatio)}, " +
            "${format(small.bestMillis)}ms -> ${format(large.bestMillis)}ms)",
        passed = ratio <= maxRatio,
    )
}

fun ratioCheck(
    name: String,
    detail: String,
    value: Double,
    limit: Double,
    lowerIsBetter: Boolean = true,
): Check {
    return Check(
        name = name,
        detail = detail,
        passed = if (lowerIsBetter) value <= limit else value >= limit,
    )
}

fun format(value: Double): String {
    val rounded = kotlin.math.round(value * 100) / 100
    return rounded.toString()
}
