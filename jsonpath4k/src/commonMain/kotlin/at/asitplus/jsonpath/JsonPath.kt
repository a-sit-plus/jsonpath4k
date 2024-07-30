package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.JsonPathCompiler
import at.asitplus.jsonpath.core.JsonPathFunctionExtension
import at.asitplus.jsonpath.core.NodeList
import kotlinx.serialization.json.JsonElement

class JsonPath(
    jsonPathExpression: String,
    compiler: JsonPathCompiler = JsonPathDependencyManager.compiler,
    functionExtensionRetriever: (String) -> JsonPathFunctionExtension<*>? = JsonPathDependencyManager.functionExtensionRepository::getExtension
) {
    private val query = compiler.compile(
        jsonPath = jsonPathExpression,
        functionExtensionRetriever = functionExtensionRetriever,
    )

    fun query(jsonElement: JsonElement): NodeList {
        return query.invoke(jsonElement)
    }
}