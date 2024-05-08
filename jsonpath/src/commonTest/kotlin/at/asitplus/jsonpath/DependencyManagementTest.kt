package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.JsonPathCompiler
import at.asitplus.jsonpath.core.JsonPathFunctionExtension
import at.asitplus.jsonpath.core.JsonPathQuery
import at.asitplus.jsonpath.core.NodeList
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject

@Suppress("unused")
class DependencyManagementTest : FreeSpec({
    // making sure that the dependencies are reset to their default for the next test
    val defaultCompilerBuilderBackup = JsonPathDependencyManager.compiler
    val defaultFunctionExtensionRepositoryBackup =
        JsonPathDependencyManager.functionExtensionRepository.export()
    beforeEach {
        // prepare a dummy repository to be modified by the tests
        JsonPathDependencyManager.functionExtensionRepository =
            JsonPathFunctionExtensionMapRepository(
                defaultFunctionExtensionRepositoryBackup.toMutableMap()
            )
    }
    afterEach {
        JsonPathDependencyManager.apply {
            compiler = defaultCompilerBuilderBackup
            functionExtensionRepository = JsonPathFunctionExtensionMapRepository(
                defaultFunctionExtensionRepositoryBackup.toMutableMap()
            )
        }
    }

    "dependency manager compiler should support the functions in the repo at the time of compilation, and query should be executable afterwards too" - {
        "compiler that was built when the repository supported a function extension before it was removed should succeed compilation before and query afterwards" {
            val jsonPathStatement = "$[?foo()]"

            val jsonPath = shouldNotThrowAny {
                val testRepo = JsonPathDependencyManager.functionExtensionRepository.export().plus(
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
                functionExtensionRetriever: (String) -> JsonPathFunctionExtension<*>?,
            ): JsonPathQuery {
                return object : JsonPathQuery {
                    override fun invoke(currentNode: JsonElement, rootNode: JsonElement): NodeList {
                        return listOf()
                    }
                }
            }
        }
        JsonPathDependencyManager.compiler = incorrectEmptyQueryCompiler
        val emptyQueryResult = JsonPath("$").query(buildJsonObject {})
        // this checks, whether the new compiler has indeed been used
        emptyQueryResult.shouldHaveSize(0)
    }
})
