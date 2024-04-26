package at.asitplus.jsonpath.functionExtensions

import at.asitplus.jsonpath.JsonPathFilterExpressionType
import at.asitplus.jsonpath.JsonPathFilterExpressionValue
import at.asitplus.jsonpath.JsonPathFunctionExtension
import kotlinx.serialization.json.JsonPrimitive

/*
specification: https://datatracker.ietf.org/doc/rfc9535/
section: 2.4.7.  search() Function Extension
 */
data object SearchFunctionExtension : JsonPathFunctionExtension.LogicalTypeFunctionExtension(
    name = "search",
    argumentTypes = listOf(
        JsonPathFilterExpressionType.ValueType,
        JsonPathFilterExpressionType.ValueType,
    )
) {
    override fun invoke(arguments: List<JsonPathFilterExpressionValue>): JsonPathFilterExpressionValue.LogicalTypeValue {
        super.validateArgumentTypes(arguments)
        return implementation(
            stringArgument = arguments[0] as JsonPathFilterExpressionValue.ValueTypeValue,
            regexArgument = arguments[1] as JsonPathFilterExpressionValue.ValueTypeValue,
        )
    }

    private fun implementation(
        stringArgument: JsonPathFilterExpressionValue.ValueTypeValue,
        regexArgument: JsonPathFilterExpressionValue.ValueTypeValue,
    ): JsonPathFilterExpressionValue.LogicalTypeValue {
        val stringValue = unpackArgumentToStringValue(stringArgument)
        val regexValue = unpackArgumentToStringValue(stringArgument)
        if(stringValue == null || regexValue == null) {
            return JsonPathFilterExpressionValue.LogicalTypeValue(false)
        }

        val isMatch = try {
            // TODO: check assumption that Regex supports RFC9485:
            //  https://www.rfc-editor.org/rfc/rfc9485.html
            Regex(regexValue).containsMatchIn(stringValue)
        } catch (throwable: Throwable) {
            false
        }

        return JsonPathFilterExpressionValue.LogicalTypeValue(isMatch)
    }
    private fun unpackArgumentToStringValue(
        valueArgument: JsonPathFilterExpressionValue.ValueTypeValue,
    ): String? {
        if (valueArgument !is JsonPathFilterExpressionValue.ValueTypeValue.JsonValue) {
            return null
        }

        val valueElement = valueArgument.jsonElement

        if (valueElement !is JsonPrimitive || !valueElement.isString) {
            return null
        }

        return valueElement.content
    }
}