package at.asitplus.jsonpath

interface JsonPathCompiler {
    fun compile(jsonPath: String): JsonPathQuery
}

val defaultJsonPathCompiler: JsonPathCompiler by lazy {
    AntlrJsonPathCompiler(
        errorListener = napierAntlrJsonPathCompilerErrorListener,
        functionExtensionRetriever = defaultJsonPathFunctionExtensionManager::getExtension,
    )
}