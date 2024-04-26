package at.asitplus.jsonpath

import at.asitplus.jsonpath.functionExtensions.LengthFunctionExtension
import at.asitplus.jsonpath.functionExtensions.MatchFunctionExtension
import at.asitplus.jsonpath.functionExtensions.SearchFunctionExtension
import at.asitplus.jsonpath.functionExtensions.ValueFunctionExtension
import at.asitplus.wallet.lib.data.jsonpath.functionExtensions.CountFunctionExtension
import com.strumenta.antlrkotlin.runtime.BitSet
import io.github.aakira.napier.Napier
import org.antlr.v4.kotlinruntime.BaseErrorListener
import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer
import org.antlr.v4.kotlinruntime.atn.ATNConfigSet
import org.antlr.v4.kotlinruntime.dfa.DFA

object JsonPathDependencyManager {
    /**
     * Function extension repository that may be extended with custom functions by the user of this library.
     */
    val functionExtensionRepository: MutableReference<JsonPathFunctionExtensionRepository> by lazy {
        MutableReference(
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
    }

    /**
     * Implementations should support the case where the function extensions change before executing the resulting query.
     */
    val compiler: MutableReference<JsonPathCompiler> by lazy {
        val napierAntlrJsonPathCompilerErrorListener =
            object : AntlrJsonPathCompilerErrorListener, BaseErrorListener() {
                override fun unknownFunctionExtension(functionExtensionName: String) {
                    Napier.e("Unknown JSONPath function extension: \"$functionExtensionName\"")
                }

                override fun invalidFunctionExtensionForTestExpression(functionExtensionName: String) {
                    Napier.e("Invalid JSONPath function extension return type for test expression: \"$functionExtensionName\"")
                }

                override fun invalidFunctionExtensionForComparable(functionExtensionName: String) {
                    Napier.e("Invalid JSONPath function extension return type for test expression: \"$functionExtensionName\"")
                }

                override fun invalidArglistForFunctionExtension(
                    functionExtension: JsonPathFunctionExtension<*>,
                    coercedArgumentTypes: List<JsonPathExpression?>
                ) {
                    Napier.e(
                        "Invalid arguments for function extension \"${functionExtension.name}\": Expected: <${
                            functionExtension.argumentTypes.joinToString(
                                ", "
                            )
                        }>, but received <${coercedArgumentTypes.joinToString(", ")}>"
                    )
                }

                override fun invalidTestExpression(testContextString: String) {
                    Napier.e("Invalid test expression: $testContextString")
                }

                override fun syntaxError(
                    recognizer: Recognizer<*, *>,
                    offendingSymbol: Any?,
                    line: Int,
                    charPositionInLine: Int,
                    msg: String,
                    e: RecognitionException?
                ) {
                    Napier.e("Syntax error $line:$charPositionInLine $msg")
                }
            }

        MutableReference(
            AntlrJsonPathCompiler(
                errorListener = napierAntlrJsonPathCompilerErrorListener,
                functionExtensionRetriever = {
                    functionExtensionRepository.value.getExtension(it)
                }
            )
        )
    }
}