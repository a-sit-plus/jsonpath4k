package at.asitplus.jsonpath.functionExtensions

import at.asitplus.jsonpath.JsonPathFilterExpressionType
import at.asitplus.jsonpath.JsonPathFilterExpressionValue
import at.asitplus.jsonpath.JsonPathFunctionExtension
import kotlinx.serialization.json.JsonPrimitive


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
        if(stringArgument !is JsonPathFilterExpressionValue.ValueTypeValue.JsonValue) {
            return JsonPathFilterExpressionValue.LogicalTypeValue(false)
        }
        if(regexArgument !is JsonPathFilterExpressionValue.ValueTypeValue.JsonValue) {
            return JsonPathFilterExpressionValue.LogicalTypeValue(false)
        }

        val stringElement = stringArgument.jsonElement
        val regexElement = regexArgument.jsonElement

        if (stringElement !is JsonPrimitive) {
            return JsonPathFilterExpressionValue.LogicalTypeValue(false)
        }
        if (regexElement !is JsonPrimitive) {
            return JsonPathFilterExpressionValue.LogicalTypeValue(false)
        }

        if (stringElement.isString != true) {
            return JsonPathFilterExpressionValue.LogicalTypeValue(false)
        }
        if (regexElement.isString != true) {
            return JsonPathFilterExpressionValue.LogicalTypeValue(false)
        }

        val isMatch = try {
            // TODO: check assumption that Regex supports RFC9485:
            //  https://www.rfc-editor.org/rfc/rfc9485.html
            Regex(regexElement.content).containsMatchIn(stringElement.content)
        } catch (throwable: Throwable) {
            false
        }

        return JsonPathFilterExpressionValue.LogicalTypeValue(isMatch)
    }
}