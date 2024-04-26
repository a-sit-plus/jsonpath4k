package at.asitplus.jsonpath.implementation

import at.asitplus.jsonpath.core.JsonPathFilterExpressionValue
import at.asitplus.jsonpath.core.Rfc9535Utils
import kotlinx.serialization.json.JsonObject
import org.antlr.v4.kotlinruntime.ParserRuleContext

/**
 * specification: https://datatracker.ietf.org/doc/rfc9535/
 * date: 2024-02
 */
open class JsonPathCompilerException(message: String) : Exception(message)

class JsonPathLexerException : JsonPathCompilerException(
    "Lexer errors have occured. See the output of the error listener for more details"
)

class JsonPathParserException : JsonPathCompilerException(
    "Parser errors have occured. See the output of the error listener for more details"
)

open class JsonPathTypeCheckerException(message: String) : JsonPathCompilerException(message)

open class JsonPathRuntimeException(message: String) : Exception(message)

class UnexpectedTokenException(ctx: ParserRuleContext) : JsonPathRuntimeException(
    "Unexpected text at position ${ctx.position?.let { "${it.start.line}:${it.start.column}" }}: ${
        Rfc9535Utils.escapeToDoubleQuoted(
            ctx.text
        )
    }"
)

class InvalidTestExpressionValueException(expression: ParserRuleContext, value: JsonPathFilterExpressionValue?) : JsonPathRuntimeException(
    "Invalid test expression value at position ${expression.position?.let { "${it.start.line}:${it.start.column}" }}: ${
        Rfc9535Utils.escapeToDoubleQuoted(
            expression.toString()
        )
    } results in: ${Rfc9535Utils.escapeToDoubleQuoted(value.toString())}"
)

class InvalidComparableValueException(expression: ParserRuleContext, value: JsonPathFilterExpressionValue) : JsonPathRuntimeException(
    "Invalid expression value at position ${expression.position?.let { "${it.start.line}:${it.start.column}" }}: ${
        Rfc9535Utils.escapeToDoubleQuoted(
            expression.toString()
        )
    } results in: ${Rfc9535Utils.escapeToDoubleQuoted(value.toString())}"
)

class MissingKeyException(jsonObject: JsonObject, key: String) : JsonPathRuntimeException(
    "Missing key ${Rfc9535Utils.escapeToDoubleQuoted(key)} at object ${
        Rfc9535Utils.escapeToDoubleQuoted(
            jsonObject.toString()
        )
    }"
)

class UnknownFunctionExtensionException(functionExtensionName: String) : JsonPathRuntimeException(
    "Unknown function extension: $functionExtensionName"
)