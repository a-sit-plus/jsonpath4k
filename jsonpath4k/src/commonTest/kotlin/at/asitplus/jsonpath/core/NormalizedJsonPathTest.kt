package at.asitplus.jsonpath.core

import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

@Suppress("unused")
class NormalizedJsonPathTest {
    @Test
    fun equality() {
        NormalizedJsonPath() shouldBe NormalizedJsonPath()
        NormalizedJsonPath("name") shouldBe NormalizedJsonPath() + "name"
        NormalizedJsonPath(1) shouldBe NormalizedJsonPath() + 1u

        mapOf(
            NormalizedJsonPath() to "1",
            NormalizedJsonPath() to "2",
        ) shouldHaveSize 1
    }
    @Test
    fun inequality() {
        NormalizedJsonPath(1) shouldNotBe NormalizedJsonPath()
        NormalizedJsonPath(1) shouldNotBe NormalizedJsonPath() + "1"
        NormalizedJsonPath(1) shouldNotBe NormalizedJsonPath() + 2
        NormalizedJsonPath("1") shouldNotBe NormalizedJsonPath()
        NormalizedJsonPath("1") shouldNotBe NormalizedJsonPath() + "2"
        NormalizedJsonPath("1") shouldNotBe NormalizedJsonPath() + 1
        NormalizedJsonPath() shouldNotBe NormalizedJsonPath() + "1"
        NormalizedJsonPath() shouldNotBe NormalizedJsonPath() + 1
    }
}