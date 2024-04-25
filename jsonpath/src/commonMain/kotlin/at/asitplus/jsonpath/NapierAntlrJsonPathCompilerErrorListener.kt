package at.asitplus.jsonpath

import io.github.aakira.napier.Napier
import org.antlr.v4.kotlinruntime.BaseErrorListener
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer

val napierAntlrJsonPathCompilerErrorListener : AntlrJsonPathCompilerErrorListener by lazy {
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
}