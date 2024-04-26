package at.asitplus.jsonpath

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject

@Suppress("unused")
class DependencyManagementTest : FreeSpec({
    // making sure that the dependencies are reset to their default for the next test
    val defaultCompilerBuilderBackup = JsonPathDependencyManager.compiler.value
    var defaultTestFunctionExtensionRepository =
        JsonPathDependencyManager.functionExtensionRepository.value
    val defaultFunctionExtensionRepositoryBackup =
        JsonPathDependencyManager.functionExtensionRepository.value
    beforeEach {
        // prepare a dummy repository to be modified by the tests
        JsonPathDependencyManager.functionExtensionRepository.value =
            JsonPathFunctionExtensionMapRepository(
                JsonPathDependencyManager.functionExtensionRepository.value.export().toMutableMap()
            )
        defaultTestFunctionExtensionRepository = JsonPathDependencyManager.functionExtensionRepository.value
    }
    afterEach {
        JsonPathDependencyManager.apply {
            compiler.value = defaultCompilerBuilderBackup
            functionExtensionRepository.value = defaultFunctionExtensionRepositoryBackup
        }
    }

    "dependency manager compiler should support the functions in the repo at the time of compilation, and query should be executable afterwards too" - {
        "compiler that was built when the repository supported a function extension before it was removed should succeed compilation before and query afterwards" {
            val jsonPathStatement = "$[?foo()]"

            JsonPathDependencyManager.functionExtensionRepository.value.apply {
                addExtension(
                    object : JsonPathFunctionExtension.LogicalTypeFunctionExtension(
                        name = "foo",
                        argumentTypes = listOf(),
                    ) {
                        override fun invoke(arguments: List<JsonPathFilterExpressionValue>): JsonPathFilterExpressionValue.LogicalTypeValue {
                            return JsonPathFilterExpressionValue.LogicalTypeValue(true)
                        }
                    }
                )
            }
            val jsonPath = shouldNotThrowAny {
                JsonPath(jsonPathStatement)
            }

            // this basically removes the foo-extension by changing to the default extension repository
            JsonPathDependencyManager.functionExtensionRepository.value =
                defaultTestFunctionExtensionRepository

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
})
