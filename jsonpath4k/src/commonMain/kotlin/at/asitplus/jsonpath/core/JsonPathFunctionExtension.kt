package at.asitplus.jsonpath.core

import kotlinx.serialization.json.JsonElement

/**
 * specification: https://datatracker.ietf.org/doc/rfc9535/
 * date: 2024-02
 * section: 2.4.  Function Extensions
 */
sealed class JsonPathFunctionExtension<ReturnType : JsonPathFilterExpressionValue>(
    vararg val argumentTypes: JsonPathFilterExpressionType,
) {
    abstract fun evaluate(arguments: List<JsonPathFilterExpressionValue>): ReturnType

    class ValueTypeFunctionExtension(
        vararg argumentTypes: JsonPathFilterExpressionType,
        private val evaluator: (arguments: List<JsonPathFilterExpressionValue>) -> JsonElement?
    ) : JsonPathFunctionExtension<JsonPathFilterExpressionValue.ValueTypeValue>(
        argumentTypes = argumentTypes,
    ) {
        override fun evaluate(arguments: List<JsonPathFilterExpressionValue>): JsonPathFilterExpressionValue.ValueTypeValue {
            return evaluator(arguments)?.let {
                JsonPathFilterExpressionValue.ValueTypeValue.JsonValue(it)
            } ?: JsonPathFilterExpressionValue.ValueTypeValue.Nothing
        }
    }

    class LogicalTypeFunctionExtension(
        vararg argumentTypes: JsonPathFilterExpressionType,
        private val evaluator: (arguments: List<JsonPathFilterExpressionValue>) -> Boolean
    ) : JsonPathFunctionExtension<JsonPathFilterExpressionValue.LogicalTypeValue>(
        argumentTypes = argumentTypes,
    ) {
        override fun evaluate(arguments: List<JsonPathFilterExpressionValue>): JsonPathFilterExpressionValue.LogicalTypeValue {
            return JsonPathFilterExpressionValue.LogicalTypeValue(evaluator(arguments))
        }
    }

    class NodesTypeFunctionExtension(
        vararg argumentTypes: JsonPathFilterExpressionType,
        private val evaluator: (arguments: List<JsonPathFilterExpressionValue>) -> List<JsonElement>
    ) : JsonPathFunctionExtension<JsonPathFilterExpressionValue.NodesTypeValue.FunctionExtensionResult>(
        argumentTypes = argumentTypes,
    ) {
        override fun evaluate(arguments: List<JsonPathFilterExpressionValue>): JsonPathFilterExpressionValue.NodesTypeValue.FunctionExtensionResult {
            return JsonPathFilterExpressionValue.NodesTypeValue.FunctionExtensionResult(
                evaluator(arguments)
            )
        }
    }
}