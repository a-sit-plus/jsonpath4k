package at.asitplus.jsonpath

import at.asitplus.jsonpath.JsonPathDependencyManager.logger
import at.asitplus.jsonpath.core.JsonPathCompiler
import at.asitplus.jsonpath.core.JsonPathFilterExpressionType
import at.asitplus.jsonpath.core.JsonPathFunctionExtension
import at.asitplus.jsonpath.core.functionExtensions.lengthFunctionExtension
import at.asitplus.jsonpath.core.functionExtensions.matchFunctionExtension
import at.asitplus.jsonpath.core.functionExtensions.searchFunctionExtension
import at.asitplus.jsonpath.core.functionExtensions.valueFunctionExtension
import at.asitplus.jsonpath.core.functionExtensions.countFunctionExtension
import at.asitplus.jsonpath.implementation.AntlrJsonPathCompiler
import at.asitplus.jsonpath.implementation.AntlrJsonPathCompilerErrorListener
import org.antlr.v4.kotlinruntime.BaseErrorListener
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer

object JsonPathDependencyManager {

    internal lateinit var logger:Logger

    /**
     * Function extension repository that may be extended with custom functions by the user of this library.
     */
    var functionExtensionRepository: JsonPathFunctionExtensionRepository =
        JsonPathFunctionExtensionMapRepository(
            listOf(
                lengthFunctionExtension,
                countFunctionExtension,
                matchFunctionExtension,
                searchFunctionExtension,
                valueFunctionExtension,
            ).toMap().toMutableMap()
        )

    var compiler: JsonPathCompiler = AntlrJsonPathCompiler(
        errorListener = napierAntlrJsonPathCompilerErrorListener,
    )
}

private val napierAntlrJsonPathCompilerErrorListener by lazy {
    object : AntlrJsonPathCompilerErrorListener, BaseErrorListener() {
        override fun unknownFunctionExtension(functionExtensionName: String) {
            logger.e {
                "Unknown JSONPath function extension: \"$functionExtensionName\""
            }
        }

        override fun invalidFunctionExtensionForTestExpression(functionExtensionName: String) {
            logger.e {
                "Invalid JSONPath function extension return type for test expression: \"$functionExtensionName\""
            }
        }

        override fun invalidFunctionExtensionForComparable(functionExtensionName: String) {
            logger.e {
                "Invalid JSONPath function extension return type for comparable expression: \"$functionExtensionName\""
            }
        }

        override fun invalidArglistForFunctionExtension(
            functionExtensionName: String,
            functionExtensionImplementation: JsonPathFunctionExtension<*>,
            coercedArgumentTypes: List<Pair<JsonPathFilterExpressionType?, String>>
        ) {
            logger.e {
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
            logger.e {
                "Invalid test expression: $testContextString"
            }
        }

        override fun syntaxError(
            recognizer: Recognizer<*, *>,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ) {
            logger.e {
                "Syntax error $line:$charPositionInLine $msg"
            }
        }
    }
}