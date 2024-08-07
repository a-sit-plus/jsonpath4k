package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.JsonPathCompiler
import at.asitplus.jsonpath.core.JsonPathFunctionExtension
import at.asitplus.jsonpath.core.NodeList
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.JsonElement

class JsonPath(
    jsonPathExpression: String,
    compiler: JsonPathCompiler = JsonPathDependencyManager.compiler,
    functionExtensionRetriever: (String) -> JsonPathFunctionExtension<*>? = JsonPathDependencyManager.functionExtensionRepository::getExtension
) {
    init {
        JsonPathDependencyManager.logger = object : Logger {
            override fun e(throwable: Throwable?, tag: String?, message: () -> String) =
                Napier.e(throwable, tag, message)
        }
    }

    private val query = compiler.compile(
        jsonPath = jsonPathExpression,
        functionExtensionRetriever = functionExtensionRetriever,
    )

    fun query(jsonElement: JsonElement): NodeList {
        return query.invoke(jsonElement)
    }
}