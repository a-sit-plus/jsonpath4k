package at.asitplus.jsonpath.core

import io.kotest.matchers.shouldBe
import kotlin.test.Test

@Suppress("unused")
class NormalizedJsonPathTest {
    @Test
    fun equivalence() {
        NormalizedJsonPath() shouldBe NormalizedJsonPath()
        NormalizedJsonPath("name") shouldBe NormalizedJsonPath() + "name"
        NormalizedJsonPath(1) shouldBe NormalizedJsonPath() + 1u
    }
}