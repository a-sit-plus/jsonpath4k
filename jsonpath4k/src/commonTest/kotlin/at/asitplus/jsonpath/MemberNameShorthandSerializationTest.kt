package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.testballoon.matrix.matrixSuite
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe

val MemberNameShorthandSerializationTest by matrixSuite {
    data(
        "should be serializable as member name shorthand",
        listOf(
            "test",
            "test_123",
            "t1",
        )
    ) test {
        shouldNotThrowAny {
            NormalizedJsonPathSegment.NameSegment(it).toShorthandNotation()
        } shouldBe ".$it"
    }

    data(
        "should not be serializable as member name shorthand",
        listOf(
            "1",
            "*",
            "'",
            "\"",
            "test-data",
        )
    ) test {
        shouldThrowAny {
            NormalizedJsonPathSegment.NameSegment(it).toShorthandNotation() shouldBe ".$it"
        }
    }
}

