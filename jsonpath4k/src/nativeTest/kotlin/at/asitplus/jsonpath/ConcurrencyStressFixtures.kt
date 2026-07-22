package at.asitplus.jsonpath

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Shared fixtures for the ANTLR concurrency stress suites ([AntlrConcurrencyNativeTest] uses a dedicated thread
 * pool; [AntlrCoroutineConcurrencyNativeTest] launches many coroutines). Extracted so the two suites don't
 * duplicate the data — and because private top-level *classes* (unlike vals/funs) share the package namespace,
 * so declaring `WrongResult` in both files would collide.
 */

/** Real (non-virtual-time) execution: real concurrency cannot run under TestBalloon's virtual-time `TestScope`. */
internal val realConcurrency: TestConfig = TestConfig.testScope(isEnabled = false)

// Non-trivial, valid expressions. Filters, function extensions, logical operators, slices and descendant
// segments all force the parser through lots of ATN prediction / DFA construction — the racy hot path.
internal val bookstoreExpressions = listOf(
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
internal val compileOnlyExpressions = bookstoreExpressions + listOf(
    "$[?length(@) < 3]",
    "$[?count(@.*) == 1]",
    "$[?value(@..color) == 'red']",
    "$[?match(@.timezone, 'Europe/.*')]",
    "$[?@.a || @.b && @.c]",
    "$[1:5:2]",
    "$.o[?@<3, ?@<3]",
)

internal val bookStore: JsonElement = Json.decodeFromString(
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

/** Thrown when a concurrent compile+query produced a different result than the single-threaded baseline. */
internal class WrongResult(message: String) : Exception(message)

/** Formats collected concurrent failures into an [AssertionError] message, grouped by kind. */
internal fun concurrencyFailure(label: String, total: Int, failures: List<Throwable>): AssertionError {
    val summary = failures
        .groupingBy { "${it::class.simpleName}: ${it.message}" }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .joinToString("\n") { (kind, count) -> "  x$count  $kind" }
    return AssertionError(
        "[$label] ${failures.size} of $total concurrent ANTLR operations failed " +
            "(antlr-kotlin is not thread-safe). Distinct failures:\n$summary"
    )
}
