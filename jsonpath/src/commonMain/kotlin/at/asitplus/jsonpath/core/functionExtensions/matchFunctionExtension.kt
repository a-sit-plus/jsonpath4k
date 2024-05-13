package at.asitplus.jsonpath.core.functionExtensions

import at.asitplus.jsonpath.core.JsonPathFilterExpressionType
import at.asitplus.jsonpath.core.JsonPathFilterExpressionValue
import at.asitplus.jsonpath.core.JsonPathFunctionExtension
import kotlinx.serialization.json.JsonPrimitive

/**
 * specification: https://datatracker.ietf.org/doc/rfc9535/
 * date: 2024-02
 * section: 2.4.6.  match() Function Extension
 */
internal val matchFunctionExtension by lazy {
    "match" to JsonPathFunctionExtension.LogicalTypeFunctionExtension(
        JsonPathFilterExpressionType.ValueType,
        JsonPathFilterExpressionType.ValueType,
    ) {
        val stringArgument = it[0] as JsonPathFilterExpressionValue.ValueTypeValue
        val regexArgument = it[1] as JsonPathFilterExpressionValue.ValueTypeValue

        val stringValue = unpackArgumentToStringValue(stringArgument)
        val regexValue = unpackArgumentToStringValue(regexArgument)

        if(stringValue == null || regexValue == null) {
            false
        } else {
            try {
                // TODO: check assumption that Regex supports RFC9485:
                //  https://www.rfc-editor.org/rfc/rfc9485.html
                Regex(regexValue).matches(stringValue)
            } catch (throwable: Throwable) {
                false
            }
        }
    }
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