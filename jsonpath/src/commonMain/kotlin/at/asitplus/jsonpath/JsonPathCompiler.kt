package at.asitplus.jsonpath

import at.asitplus.wallet.lib.data.jsonpath.AntlrJsonPathCompiler

interface JsonPathCompiler {
    fun compile(jsonPath: String): JsonPathQuery
}

val defaultJsonPathCompiler: JsonPathCompiler by lazy {
    AntlrJsonPathCompiler(
        errorListener = napierAntlrJsonPathCompilerErrorListener,
        functionExtensionRetriever = defaultJsonPathFunctionExtensionManager::getExtension,
    )
}