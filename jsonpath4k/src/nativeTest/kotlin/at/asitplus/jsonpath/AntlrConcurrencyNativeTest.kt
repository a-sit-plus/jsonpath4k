@file:OptIn(DelicateCoroutinesApi::class)

package at.asitplus.jsonpath

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

/**
 * Highly-concurrent stress tests that hammer the ANTLR-generated lexer/parser (via [JsonPath]) from many
 * threads at once.
 *
 * antlr-kotlin is **not** thread-safe: a generated `Lexer`/`Parser` shares process-global, mutable prediction
 * state through the companion object — the decoded `ATN`, the `decisionToDFA` array, and the
 * `PredictionContextCache`. `adaptivePredict` mutates those shared `DFA` structures (adding states/edges) with
 * no locking, so compiling JSONPath expressions concurrently races on that shared state. On Kotlin/Native
 * (strict, no forgiving JIT) this surfaces as thrown exceptions (NPE, index-out-of-bounds, illegal state) or,
 * worse, silently wrong parses — and can even hard-crash the worker.
 *
 * These tests are therefore **expected to FAIL** against an unpatched antlr-kotlin: they assert that no worker
 * threw and that every concurrent compile produced the same result as a single-threaded baseline. They exist to
 * (a) demonstrate the defect and (b) measure how far a thread-safety patch to antlr-kotlin gets us.
 *
 * `TestScope` (virtual time) is disabled per test: matrix/coroutine real concurrency cannot run under virtual
 * time, and we deliberately want real threads.
 */

private const val WORKERS = 64
private const val ITERATIONS = 250
private const val ROUNDS = 8

private val realConcurrency = TestConfig.testScope(isEnabled = false)

// Non-trivial, valid expressions. Filters, function extensions, logical operators, slices and descendant
// segments all force the parser through lots of ATN prediction / DFA construction — the racy hot path.
private val bookstoreExpressions = listOf(
    "$.store.book[*].author",
    "$..author",
    "$.store.*",
    "$.store..price",
    "$..book[2]",
    "$..book[-1]",
    "$..book[0,1]",
    "$..book[:2]",
    "$..book[?@.isbn]",
    "$..book[?@.price<10]",
    "$..*",
    "$.store.book[?@.price < 10 && @.category == 'fiction'].title",
    "$.store.book[?match(@.category, 'fic.*')].author",
    "$.store.book[?search(@.title, 'the')].title",
)

// Expressions that only need to *compile* (type-checkable standalone) — used for the pure-compile race.
private val compileOnlyExpressions = bookstoreExpressions + listOf(
    "$[?length(@) < 3]",
    "$[?count(@.*) == 1]",
    "$[?value(@..color) == 'red']",
    "$[?match(@.timezone, 'Europe/.*')]",
    "$[?@.a || @.b && @.c]",
    "$[1:5:2]",
    "$.o[?@<3, ?@<3]",
)

private val bookStore: JsonElement = Json.decodeFromString(
    """
    { "store": {
        "book": [
          { "category": "reference", "author": "Nigel Rees", "title": "Sayings of the Century", "price": 8.95 },
          { "category": "fiction", "author": "Evelyn Waugh", "title": "Sword of Honour", "price": 12.99 },
          { "category": "fiction", "author": "Herman Melville", "title": "Moby Dick", "isbn": "0-553-21311-3", "price": 8.99 },
          { "category": "fiction", "author": "J. R. R. Tolkien", "title": "The Lord of the Rings", "isbn": "0-395-19395-8", "price": 22.99 }
        ],
        "bicycle": { "color": "red", "price": 399 }
      }
    }
    """.trimIndent()
)

private class WrongResult(message: String) : Exception(message)

/**
 * Runs [block] on [WORKERS] coroutines dispatched to a real, multi-threaded pool. All workers wait on a start
 * gate so they hit the hot path simultaneously (maximizing the race window), then each runs [ITERATIONS] times.
 * Each worker collects its own failures locally (no shared mutable state), aggregated after `awaitAll`.
 */
@OptIn(DelicateCoroutinesApi::class)
private suspend fun stress(
    label: String,
    dispatcher: CoroutineContext,
    block: (worker: Int, iteration: Int) -> Unit,
): List<Throwable> = coroutineScope {
    val gate = CompletableDeferred<Unit>()
    val deferred = (0 until WORKERS).map { worker ->
        async(dispatcher) {
            gate.await()
            val local = ArrayList<Throwable>()
            repeat(ITERATIONS) { iteration ->
                try {
                    block(worker, iteration)
                } catch (t: Throwable) {
                    local += t
                }
            }
            local
        }
    }
    gate.complete(Unit) // release all workers at once
    deferred.awaitAll().flatten().also { failures ->
        if (failures.isNotEmpty()) {
            val summary = failures
                .groupingBy { "${it::class.simpleName}: ${it.message}" }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .joinToString("\n") { (kind, count) -> "  x$count  $kind" }
            throw AssertionError(
                "[$label] ${failures.size} of ${WORKERS * ITERATIONS} concurrent ANTLR operations failed " +
                    "(antlr-kotlin is not thread-safe). Distinct failures:\n$summary"
            )
        }
    }
}

val AntlrConcurrencyNativeTest by testSuite {

    // 1) COLD DFA, SAME EXPRESSION: every worker compiles the identical complex expression with no warmup, so
    //    they all race to build the very same shared DFA decision states at once — the tightest contention.
    test("concurrent compilation of the same expression on a cold parser", testConfig = realConcurrency) {
        val pool = newFixedThreadPoolContext(WORKERS, "antlr-same")
        try {
            withTimeout(120.seconds) {
                repeat(ROUNDS) {
                    val expr = "$.store.book[?@.price < 10 && @.category == 'fiction'].title"
                    stress("same-expr", pool) { _, _ ->
                        JsonPath(expr)
                    }
                }
            }
        } finally {
            pool.close()
        }
    }

    // 2) MANY DISTINCT EXPRESSIONS: workers pick different expressions, maximizing churn across many decisions
    //    and the shared PredictionContextCache simultaneously.
    test("concurrent compilation of many distinct expressions", testConfig = realConcurrency) {
        val pool = newFixedThreadPoolContext(WORKERS, "antlr-distinct")
        try {
            withTimeout(120.seconds) {
                repeat(ROUNDS) {
                    stress("distinct-expr", pool) { worker, iteration ->
                        val expr = compileOnlyExpressions[(worker + iteration) % compileOnlyExpressions.size]
                        JsonPath(expr)
                    }
                }
            }
        } finally {
            pool.close()
        }
    }

    // 3) CORRECTNESS UNDER CONCURRENCY: compile + query in parallel and compare against a single-threaded
    //    baseline. Catches silent corruption (no exception, but a wrong/incomplete parse -> wrong node list).
    test("concurrent compilation yields results consistent with a single-threaded baseline", testConfig = realConcurrency) {
        // Baseline computed sequentially, before any concurrency touches the shared ANTLR state.
        val baseline: Map<String, String> = bookstoreExpressions.associateWith { expr ->
            JsonPath(expr).query(bookStore).map { it.value }.toString()
        }
        val pool = newFixedThreadPoolContext(WORKERS, "antlr-correct")
        try {
            withTimeout(120.seconds) {
                repeat(ROUNDS) {
                    stress("correctness", pool) { worker, iteration ->
                        val expr = bookstoreExpressions[(worker + iteration) % bookstoreExpressions.size]
                        val actual = JsonPath(expr).query(bookStore).map { it.value }.toString()
                        val expected = baseline.getValue(expr)
                        if (actual != expected) {
                            throw WrongResult("expr <$expr>: expected $expected but got $actual")
                        }
                    }
                }
            }
        } finally {
            pool.close()
        }
    }
}
