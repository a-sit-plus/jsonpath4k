package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.JsonPathCompiler
import at.asitplus.jsonpath.core.JsonPathFunctionExtension
import at.asitplus.jsonpath.core.JsonPathQuery
import at.asitplus.testballoon.matrix.ExecutionMode
import at.asitplus.testballoon.matrix.matrixConfig
import at.asitplus.testballoon.matrix.matrixSuite
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEachTest
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject

// Captured once, before any test mutates the process-global JsonPath dependencies.
private val defaultCompilerBuilderBackup = JsonPath.defaultCompiler
private val defaultFunctionExtensionRepositoryBackup =
    JsonPath.defaultFunctionExtensionRepository.export()

val DependencyManagementTest by matrixSuite(
    matrixConfig {
        execution = ExecutionMode.Sequential
        // Reset the global dependencies to their defaults around every test (was beforeEach/afterEach).
        testConfig = TestConfig.aroundEachTest { action ->
            // prepare a dummy repository to be modified by the tests
            JsonPath.defaultFunctionExtensionRepository = JsonPathFunctionExtensionMapRepository(
                defaultFunctionExtensionRepositoryBackup.toMutableMap()
            )
            try {
                action()
            } finally {
                JsonPath.defaultCompiler = defaultCompilerBuilderBackup
                JsonPath.defaultFunctionExtensionRepository = JsonPathFunctionExtensionMapRepository(
                    defaultFunctionExtensionRepositoryBackup.toMutableMap()
                )
            }
        }
    }
) {
    "dependency manager compiler should support the functions in the repo at the time of compilation, and query should be executable afterwards too" - {
        "compiler that was built when the repository supported a function extension before it was removed should succeed compilation before and query afterwards" {
            val jsonPathStatement = "$[?foo()]"

            val jsonPath = shouldNotThrowAny {
                val testRepo = JsonPath.defaultFunctionExtensionRepository.export().plus(
                    "foo" to JsonPathFunctionExtension.LogicalTypeFunctionExtension {
                        true
                    }
                )
                JsonPath(jsonPathStatement, functionExtensionRetriever = testRepo::get)
            }

            shouldNotThrowAny {
                val jsonElement = buildJsonObject {
                    put("a", JsonNull)
                }
                val nodeList = jsonPath.query(jsonElement)

                nodeList shouldHaveSize 1
                jsonElement["a"].shouldNotBeNull().shouldBeIn(nodeList.map { it.value })
            }
        }
    }

    "changing the compiler also changes the compiler used in the next JsonPath" {
        val incorrectEmptyQueryCompiler = object : JsonPathCompiler {
            override fun compile(
                jsonPath: String,
                functionExtensionRetriever: (String) -> JsonPathFunctionExtension?,
            ) = JsonPathQuery(
                selectors = listOf()
            )
        }
        JsonPath.defaultCompiler = incorrectEmptyQueryCompiler
        val emptyQueryResult = JsonPath("$").query(buildJsonObject {})
        // this checks, whether the new compiler has indeed been used
        emptyQueryResult.shouldHaveSize(0)
    }
}
