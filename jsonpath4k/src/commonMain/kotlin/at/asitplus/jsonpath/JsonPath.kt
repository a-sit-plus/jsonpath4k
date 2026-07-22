package at.asitplus.jsonpath

//import io.github.aakira.napier.Napier
import at.asitplus.jsonpath.core.JsonPathCompiler
import at.asitplus.jsonpath.core.JsonPathFilterExpressionType
import at.asitplus.jsonpath.core.JsonPathFunctionExtension
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.functionExtensions.*
import at.asitplus.jsonpath.implementation.AntlrJsonPathCompiler
import at.asitplus.jsonpath.implementation.AntlrJsonPathCompilerErrorListener
import com.strumenta.antlrkotlin.runtime.BitSet
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
        var defaultCompiler: JsonPathCompiler = AntlrJsonPathCompiler(defaultErrorListener)

        /**
         * Function extension repository that may be extended with custom functions by the user of this library.
         */
        var defaultFunctionExtensionRepository: JsonPathFunctionExtensionRepository =
            JsonPathFunctionExtensionMapRepository(
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

internal expect val defaultErrorListener: AntlrJsonPathCompilerErrorListener

internal val noopErrorListener = object : AntlrJsonPathCompilerErrorListener {
    override fun unknownFunctionExtension(functionExtensionName: String) {
    }

    override fun invalidFunctionExtensionForTestExpression(functionExtensionName: String) {
    }

    override fun invalidFunctionExtensionForComparable(functionExtensionName: String) {
    }

    override fun invalidArglistForFunctionExtension(
        functionExtensionName: String,
        functionExtensionImplementation: JsonPathFunctionExtension,
        coercedArgumentTypes: List<Pair<JsonPathFilterExpressionType?, String>>
    ) {
    }

    override fun invalidTestExpression(testContextString: String) {
    }

    override fun syntaxError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?
    ) {
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
    }

    override fun reportAttemptingFullContext(
        recognizer: Parser,
        dfa: DFA,
        startIndex: Int,
        stopIndex: Int,
        conflictingAlts: BitSet,
        configs: ATNConfigSet
    ) {
    }

    override fun reportContextSensitivity(
        recognizer: Parser,
        dfa: DFA,
        startIndex: Int,
        stopIndex: Int,
        prediction: Int,
        configs: ATNConfigSet
    ) {
    }
}