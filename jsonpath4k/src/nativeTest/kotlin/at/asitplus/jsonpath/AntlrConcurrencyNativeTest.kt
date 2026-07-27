package at.asitplus.jsonpath

import at.asitplus.testballoon.matrix.matrixSuite
import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

/**
 * Highly-concurrent stress tests that hammer the ANTLR-generated lexer/parser (via [JsonPath]) from many
 * **real OS threads** at once, using Kotlin/Native [Worker]s (no coroutines — see
 * [AntlrCoroutineConcurrencyNativeTest] for the coroutine-fan-out flavour).
 *
 * antlr-kotlin is **not** thread-safe: a generated `Lexer`/`Parser` shares process-global, mutable prediction
 * state through the companion object — the decoded `ATN`, the `decisionToDFA` array, and the
 * `PredictionContextCache`. `adaptivePredict` mutates those shared `DFA` structures (adding states/edges) with
 * no locking, so compiling JSONPath expressions concurrently races on that shared state. On Kotlin/Native
 * (strict, no forgiving JIT) this surfaces as thrown exceptions (index-out-of-bounds, NPE, illegal state) or,
 * worse, silently wrong parses — and can even hard-crash the worker.
 *
 * These tests are therefore **expected to FAIL** against an unpatched antlr-kotlin. They exist to demonstrate
 * the defect and to measure how far a thread-safety patch gets us.
 */

// Sized per target (see threadStress). The race triggers by *oversubscription* — many more threads than cores
// forces heavy interleaving — so it reproduces regardless of a runner's core count; the ~8 MB stack per Worker
// is the memory ceiling that caps the fan-out on constrained runners.
private val WORKERS = threadStress.fanOut
private val ITERATIONS = threadStress.iterations
private val ROUNDS = threadStress.rounds

/** Everything a worker thread needs, handed over via the [Worker.execute] producer (no captured state). */
private class WorkerInput(
    val worker: Int,
    val gate: AtomicInt,
    val ready: AtomicInt,
    val block: (worker: Int, iteration: Int) -> Unit,
)

/**
 * Spawns [WORKERS] real threads, each parked on a spin-gate, then releases them all at once so they collide on
 * the *cold* shared ANTLR state simultaneously (the tightest race window). Each thread runs [block]
 * [ITERATIONS] times and returns the throwables it caught; those are aggregated after every thread joins.
 */
private fun stress(label: String, block: (worker: Int, iteration: Int) -> Unit) {
    val gate = AtomicInt(0)   // 0 = closed, 1 = released
    val ready = AtomicInt(0)  // number of threads parked at the gate
    val workers = List(WORKERS) { Worker.start(name = "antlr-$label-$it") }

    val futures = workers.mapIndexed { index, worker ->
        worker.execute(TransferMode.SAFE, { WorkerInput(index, gate, ready, block) }) { input ->
            input.ready.incrementAndGet()
            while (input.gate.value == 0) { /* spin until every thread is parked and the gate opens */ }
            val local = ArrayList<Throwable>()
            repeat(ITERATIONS) { iteration ->
                try {
                    input.block(input.worker, iteration)
                } catch (t: Throwable) {
                    local += t
                }
            }
            local
        }
    }

    while (ready.value < WORKERS) { /* wait until all threads are parked */ }
    gate.value = 1 // release all WORKERS at the same instant

    val failures = futures.flatMap { it.result } // joins each worker
    workers.forEach { it.requestTermination().result }

    if (failures.isNotEmpty()) throw concurrencyFailure(label, WORKERS * ITERATIONS, failures)
}

val AntlrConcurrencyNativeTest by matrixSuite {

    // 1) COLD DFA, SAME EXPRESSION: every thread compiles the identical complex expression with no warmup, so
    //    they all race to build the very same shared DFA decision states at once — the tightest contention.
    test("concurrent compilation of the same expression on a cold parser", testConfig = realConcurrency) {
        repeat(ROUNDS) {
            val expr = "$.store.book[?@.price < 10 && @.category == 'fiction'].title"
            stress("same-expr") { _, _ ->
                JsonPath(expr)
            }
        }
    }

    // 2) MANY DISTINCT EXPRESSIONS: threads pick different expressions, maximizing churn across many decisions
    //    and the shared PredictionContextCache simultaneously.
    test("concurrent compilation of many distinct expressions", testConfig = realConcurrency) {
        repeat(ROUNDS) {
            stress("distinct-expr") { worker, iteration ->
                val expr = compileOnlyExpressions[(worker + iteration) % compileOnlyExpressions.size]
                JsonPath(expr)
            }
        }
    }

    // 3) CORRECTNESS UNDER CONCURRENCY: compile + query in parallel and compare against a single-threaded
    //    baseline. Catches silent corruption (no exception, but a wrong/incomplete parse -> wrong node list).
    test("concurrent compilation yields results consistent with a single-threaded baseline", testConfig = realConcurrency) {
        // Baseline computed before any thread touches the shared ANTLR state.
        val baseline: Map<String, String> = bookstoreExpressions.associateWith { expr ->
            JsonPath(expr).query(bookStore).map { it.value }.toString()
        }
        repeat(ROUNDS) {
            stress("correctness") { worker, iteration ->
                val expr = bookstoreExpressions[(worker + iteration) % bookstoreExpressions.size]
                val actual = JsonPath(expr).query(bookStore).map { it.value }.toString()
                val expected = baseline.getValue(expr)
                if (actual != expected) {
                    throw WrongResult("expr <$expr>: expected $expected but got $actual")
                }
            }
        }
    }
}
