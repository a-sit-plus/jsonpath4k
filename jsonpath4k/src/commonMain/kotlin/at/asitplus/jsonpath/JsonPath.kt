package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.JsonPathCompiler
import at.asitplus.jsonpath.core.JsonPathFilterExpressionType
import at.asitplus.jsonpath.core.JsonPathFunctionExtension
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.functionExtensions.countFunctionExtension
import at.asitplus.jsonpath.core.functionExtensions.lengthFunctionExtension
import at.asitplus.jsonpath.core.functionExtensions.matchFunctionExtension
import at.asitplus.jsonpath.core.functionExtensions.searchFunctionExtension
import at.asitplus.jsonpath.core.functionExtensions.valueFunctionExtension
import at.asitplus.jsonpath.implementation.AntlrJsonPathCompiler
import at.asitplus.jsonpath.implementation.AntlrJsonPathCompilerErrorListener
import com.strumenta.antlrkotlin.runtime.BitSet
//import io.github.aakira.napier.Napier
import kotlinx.serialization.json.JsonElement
import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer
import org.antlr.v4.kotlinruntime.atn.ATNConfigSet
import org.antlr.v4.kotlinruntime.dfa.DFA

class JsonPath(
    jsonPathExpression: String,
    compiler: JsonPathCompiler = defaultCompiler,
    functionExtensionRetriever: (String) -> JsonPathFunctionExtension? = defaultFunctionExtensionRepository::getExtension
) {
    private val query = compiler.compile(
        jsonPath = jsonPathExpression,
        functionExtensionRetriever = functionExtensionRetriever,
    )

    fun query(jsonElement: JsonElement): NodeList {
        return query.invoke(jsonElement)
    }

    companion object {
        /**
         * Default json path compiler used when no compiler is explicitly chosen
         */
        var defaultCompiler: JsonPathCompiler = AntlrJsonPathCompiler(
            errorListener = object : AntlrJsonPathCompilerErrorListener {
                override fun unknownFunctionExtension(functionExtensionName: String) {
               //     TODO("Not yet implemented")
                }

                override fun invalidFunctionExtensionForTestExpression(functionExtensionName: String) {
               //     TODO("Not yet implemented")
                }

                override fun invalidFunctionExtensionForComparable(functionExtensionName: String) {
                    //TODO("Not yet implemented")
                }

                override fun invalidArglistForFunctionExtension(
                    functionExtensionName: String,
                    functionExtensionImplementation: JsonPathFunctionExtension,
                    coercedArgumentTypes: List<Pair<JsonPathFilterExpressionType?, String>>
                ) {
                   // TODO("Not yet implemented")
                }

                override fun invalidTestExpression(testContextString: String) {
                  //  TODO("Not yet implemented")
                }

                override fun syntaxError(
                    recognizer: Recognizer<*, *>,
                    offendingSymbol: Any?,
                    line: Int,
                    charPositionInLine: Int,
                    msg: String,
                    e: RecognitionException?
                ) {
                    //TODO("Not yet implemented")
                }

                override fun reportAmbiguity(
                    recognizer: Parser,
                    dfa: DFA,
                    startIndex: Int,
                    stopIndex: Int,
                    exact: Boolean,
                    ambigAlts: BitSet,
                    configs: ATNConfigSet
                ) {
                 //   TODO("Not yet implemented")
                }

                override fun reportAttemptingFullContext(
                    recognizer: Parser,
                    dfa: DFA,
                    startIndex: Int,
                    stopIndex: Int,
                    conflictingAlts: BitSet,
                    configs: ATNConfigSet
                ) {
                   // TODO("Not yet implemented")
                }

                override fun reportContextSensitivity(
                    recognizer: Parser,
                    dfa: DFA,
                    startIndex: Int,
                    stopIndex: Int,
                    prediction: Int,
                    configs: ATNConfigSet
                ) {
                   // TODO("Not yet implemented")
                }
            },
        )

        /**
         * Function extension repository that may be extended with custom functions by the user of this library.
         */
        var defaultFunctionExtensionRepository: JsonPathFunctionExtensionRepository = JsonPathFunctionExtensionMapRepository(
            listOf(
                lengthFunctionExtension,
                countFunctionExtension,
                matchFunctionExtension,
                searchFunctionExtension,
                valueFunctionExtension,
            ).toMap().toMutableMap()
        )
    }
}
/*
private val napierAntlrJsonPathCompilerErrorListener by lazy {
    object : AntlrJsonPathCompilerErrorListener {
        override fun unknownFunctionExtension(functionExtensionName: String) {
            Napier.e {
                "Unknown JSONPath function extension: \"$functionExtensionName\""
            }
        }

        override fun invalidFunctionExtensionForTestExpression(functionExtensionName: String) {
            Napier.e {
                "Invalid JSONPath function extension return type for test expression: \"$functionExtensionName\""
            }
        }

        override fun invalidFunctionExtensionForComparable(functionExtensionName: String) {
            Napier.e {
                "Invalid JSONPath function extension return type for comparable expression: \"$functionExtensionName\""
            }
        }

        override fun invalidArglistForFunctionExtension(
            functionExtensionName: String,
            functionExtensionImplementation: JsonPathFunctionExtension,
            coercedArgumentTypes: List<Pair<JsonPathFilterExpressionType?, String>>
        ) {
            Napier.e {
                "Invalid arguments for function extension \"$functionExtensionName\": Expected: <${
                    functionExtensionImplementation.argumentTypes.joinToString(
                        ", "
                    )
                }>, but received <${
                    coercedArgumentTypes.map { it.first }.joinToString(", ")
                }>: <${coercedArgumentTypes.map { it.second }.joinToString(", ")}>"
            }
        }

        override fun invalidTestExpression(testContextString: String) {
            Napier.e {
                "Invalid test expression: $testContextString"
            }
        }

        override fun reportAmbiguity(
            recognizer: Parser,
            dfa: DFA,
            startIndex: Int,
            stopIndex: Int,
            exact: Boolean,
            ambigAlts: BitSet,
            configs: ATNConfigSet
        ) {
            // noop
        }

        override fun reportAttemptingFullContext(
            recognizer: Parser,
            dfa: DFA,
            startIndex: Int,
            stopIndex: Int,
            conflictingAlts: BitSet,
            configs: ATNConfigSet
        ) {
            // noop
        }

        override fun reportContextSensitivity(
            recognizer: Parser,
            dfa: DFA,
            startIndex: Int,
            stopIndex: Int,
            prediction: Int,
            configs: ATNConfigSet
        ) {
            // noop
        }

        override fun syntaxError(
            recognizer: Recognizer<*, *>,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ) {
            Napier.e {
                "Syntax error $line:$charPositionInLine $msg"
            }
        }
    }
}
*/