package at.asitplus.jsonpath.core

import at.asitplus.testballoon.matrix.matrixSuite
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

val NormalizedJsonPathTest by matrixSuite {
    "equality" {
        NormalizedJsonPath() shouldBe NormalizedJsonPath()
        NormalizedJsonPath("name") shouldBe NormalizedJsonPath() + "name"
        NormalizedJsonPath(1) shouldBe NormalizedJsonPath() + 1u

        mapOf(
            NormalizedJsonPath() to "1",
            NormalizedJsonPath() to "2",
        ) shouldHaveSize 1
    }
    "inequality" {
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
