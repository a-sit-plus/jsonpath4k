package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.JsonPathCompiler
import at.asitplus.jsonpath.core.JsonPathFilterExpressionType
import at.asitplus.jsonpath.core.JsonPathFunctionExtension
import at.asitplus.jsonpath.implementation.AntlrJsonPathCompiler
import at.asitplus.jsonpath.implementation.AntlrJsonPathCompilerErrorListener
import com.strumenta.antlrkotlin.runtime.BitSet
import io.github.aakira.napier.Napier
import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer
import org.antlr.v4.kotlinruntime.atn.ATNConfigSet
import org.antlr.v4.kotlinruntime.dfa.DFA

internal actual val defaultErrorListener=  object : AntlrJsonPathCompilerErrorListener {
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