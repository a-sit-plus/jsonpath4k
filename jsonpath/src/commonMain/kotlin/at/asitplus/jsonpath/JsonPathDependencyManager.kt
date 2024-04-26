package at.asitplus.jsonpath

import at.asitplus.jsonpath.functionExtensions.LengthFunctionExtension
import at.asitplus.jsonpath.functionExtensions.MatchFunctionExtension
import at.asitplus.jsonpath.functionExtensions.SearchFunctionExtension
import at.asitplus.jsonpath.functionExtensions.ValueFunctionExtension
import at.asitplus.wallet.lib.data.jsonpath.functionExtensions.CountFunctionExtension

object JsonPathDependencyManager {
    /**
     * Function extension repository that may be extended with custom functions by the user of this library.
     */
    val functionExtensionRepository: MutableReference<JsonPathFunctionExtensionRepository> = MutableReference(
        JsonPathFunctionExtensionMapRepository(
            listOf(
                LengthFunctionExtension,
                CountFunctionExtension,
                MatchFunctionExtension,
                SearchFunctionExtension,
                ValueFunctionExtension,
            ).associateBy {
                it.name
            }.toMutableMap()
        )
    )

    /**
     * Implementations should support the case where the function extensions change before executing the resulting query.
     */
    val compiler: MutableReference<JsonPathCompiler> = MutableReference(
        AntlrJsonPathCompiler(
            errorListener = napierAntlrJsonPathCompilerErrorListener,
            functionExtensionRetriever = {
                functionExtensionRepository.value.getExtension(it)
            }
        )
    )
}