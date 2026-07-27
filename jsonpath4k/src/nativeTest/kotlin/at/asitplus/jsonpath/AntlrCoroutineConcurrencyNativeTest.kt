package at.asitplus.jsonpath

import at.asitplus.testballoon.matrix.matrixSuite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Coroutine-fan-out sibling of [AntlrConcurrencyNativeTest].
 *
 * Instead of a bounded, dedicated thread pool, this fires **many coroutines at once** (see
 * [coroutineStressProfile]) with `launch` onto [Dispatchers.Default] (multi-threaded on Kotlin/Native), gated so
 * they start together. The enclosing `coroutineScope` suspends
 * until all of them finish (structured concurrency joins the children for us — no manual gate/`joinAll`), so
 * they slam the ANTLR-generated parser's process-global, mutable prediction state (decoded `ATN`,
 * `decisionToDFA`, `PredictionContextCache`) concurrently.
 *
 * antlr-kotlin does no locking around that state, so concurrent `JsonPath(...)` compilation races on it. Like
 * the thread-pool suite, these tests are **expected to FAIL** against an unpatched antlr-kotlin (thrown
 * exceptions and/or a wrong parse vs. a single-threaded baseline) and serve to measure a thread-safety patch.
 *
 * `TestScope` (virtual time) is disabled per test: real concurrency cannot run under virtual time.
 */

// Sized per target (see coroutineStressProfile). Coroutines are cheap, but true parallelism is capped at
// Dispatchers.Default's size (~CPU cores), so this is a strong reproducer only on many-core hosts and a light
// smoke test on constrained simulators — the thread-based suite is the reliable gate.
private val COROUTINES = coroutineStressProfile.fanOut
private val ITERATIONS = coroutineStressProfile.iterations
private val ROUNDS = coroutineStressProfile.rounds

/**
 * Launches [COROUTINES] coroutines on [Dispatchers.Default], each running [block] [ITERATIONS] times, and lets
 * the surrounding `coroutineScope` join them all. Failures are funneled through an unlimited [Channel]
 * (thread-safe `trySend`), then drained after the scope completes — no shared mutable collection.
 */
private suspend fun coroutineStress(
    label: String,
    block: (worker: Int, iteration: Int) -> Unit,
) {
    val failures = Channel<Throwable>(Channel.UNLIMITED)
    coroutineScope {
        // Start barrier: every coroutine parks here (cheap, no parser work) until all are launched, then all
        // are released at once so they collide on the *cold* shared ANTLR state simultaneously. Without it the
        // first coroutines fully build the DFA before the rest start, closing the race window (test goes green).
        val gate = CompletableDeferred<Unit>()
        repeat(COROUTINES) { worker ->
            launch(Dispatchers.Default) {
                gate.await()
                repeat(ITERATIONS) { iteration ->
                    try {
                        block(worker, iteration)
                    } catch (t: Throwable) {
                        failures.trySend(t)
                    }
                }
            }
        }
        gate.complete(Unit) // release all COROUTINES at once
    } // coroutineScope suspends here until all coroutines complete
    failures.close()

    val collected = ArrayList<Throwable>()
    while (true) {
        collected += failures.tryReceive().getOrNull() ?: break
    }
    if (collected.isNotEmpty()) throw concurrencyFailure(label, COROUTINES * ITERATIONS, collected)
}

val AntlrCoroutineConcurrencyNativeTest by matrixSuite {

    // 1) COLD DFA, SAME EXPRESSION: many coroutines compile the identical complex expression with no warmup.
    test("$COROUTINES coroutines compiling the same expression on a cold parser", testConfig = realConcurrency) {
        withTimeout(coroutineStressProfile.timeout) {
            repeat(ROUNDS) {
                val expr = "$.store.book[?@.price < 10 && @.category == 'fiction'].title"
                coroutineStress("same-expr") { _, _ ->
                    JsonPath(expr)
                }
            }
        }
    }

    // 2) MANY DISTINCT EXPRESSIONS: coroutines spread across many expressions -> churn across many decisions.
    test("$COROUTINES coroutines compiling many distinct expressions", testConfig = realConcurrency) {
        withTimeout(coroutineStressProfile.timeout) {
            repeat(ROUNDS) {
                coroutineStress("distinct-expr") { worker, iteration ->
                    val expr = compileOnlyExpressions[(worker + iteration) % compileOnlyExpressions.size]
                    JsonPath(expr)
                }
            }
        }
    }

    // 3) CORRECTNESS UNDER CONCURRENCY: compile + query in parallel, compare against a single-threaded baseline
    //    to catch silent corruption (no exception, but a wrong/incomplete parse -> wrong node list).
    test("$COROUTINES coroutines produce results consistent with a single-threaded baseline", testConfig = realConcurrency) {
        val baseline: Map<String, String> = bookstoreExpressions.associateWith { expr ->
            JsonPath(expr).query(bookStore).map { it.value }.toString()
        }
        withTimeout(coroutineStressProfile.timeout) {
            repeat(ROUNDS) {
                coroutineStress("correctness") { worker, iteration ->
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
}
