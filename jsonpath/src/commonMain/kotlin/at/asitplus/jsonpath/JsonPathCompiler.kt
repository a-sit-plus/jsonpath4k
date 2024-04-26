package at.asitplus.jsonpath

interface JsonPathCompiler {
    fun compile(jsonPath: String): JsonPathQuery
}