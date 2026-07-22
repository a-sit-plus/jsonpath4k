package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.testballoon.matrix.matrixSuite
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe

val NormalizedJsonPathSerializationTest by matrixSuite {
    "normalized json path string" - {
        data(
            listOf(
                "test" to "$['test']",
                "test_123" to "$['test_123']",
                "t1" to "$['t1']",
            )
        ) test { (input, expected) ->
            shouldNotThrowAny {
                val path = NormalizedJsonPath() + NormalizedJsonPathSegment.NameSegment(input)
                path.toNormalizedJsonPathString() shouldBe expected
            }
        }
    }
    "shorthand serialization" - {
        "should be serializable as member name shorthand" - {
            data(
                listOf(
                    "test",
                    "test_123",
                    "t1",
                )
            ) test {
                shouldNotThrowAny {
                    val path = NormalizedJsonPath() + NormalizedJsonPathSegment.NameSegment(it)
                    path.toShorthandNameSegmentNotation() shouldBe "$.$it"
                    path.toShorthandNameSegmentNotationWherePossible() shouldBe "$.$it"
                }
            }
        }
        "should not be serializable as member name shorthand" - {
            data(
                listOf(
                    "1",
                    "*",
                    "'",
                    "\"",
                    "test-data",
                )
            ) test {
                val path = NormalizedJsonPath() + NormalizedJsonPathSegment.NameSegment(it)
                shouldThrowAny {
                    path.toShorthandNameSegmentNotation()
                }
                path.toShorthandNameSegmentNotationWherePossible() shouldBe path.toNormalizedJsonPathString()
            }
        }
    }
}
