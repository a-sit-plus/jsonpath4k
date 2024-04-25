package at.asitplus.jsonpath

import kotlinx.serialization.json.JsonElement

class JsonPath(
    jsonPath: String,
    compiler: JsonPathCompiler = defaultJsonPathCompiler,
) {
    private val query = compiler.compile(jsonPath)

    fun query(jsonElement: JsonElement): NodeList {
        return query.invoke(jsonElement)
    }
}