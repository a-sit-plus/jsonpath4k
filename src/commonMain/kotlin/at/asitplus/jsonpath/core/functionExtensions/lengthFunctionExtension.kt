package at.asitplus.jsonpath.core.functionExtensions

import at.asitplus.jsonpath.core.JsonPathFilterExpressionType
import at.asitplus.jsonpath.core.JsonPathFilterExpressionValue
import at.asitplus.jsonpath.core.JsonPathFunctionExtension
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * specification: https://datatracker.ietf.org/doc/rfc9535/
 * date: 2024-02
 * section: 2.4.4.  length() Function Extension
 */
@OptIn(ExperimentalSerializationApi::class)
internal val lengthFunctionExtension by lazy {
    "length" to JsonPathFunctionExtension.ValueTypeFunctionExtension(
        JsonPathFilterExpressionType.ValueType,
    ) {
        val argument = it[0] as JsonPathFilterExpressionValue.ValueTypeValue

        if (argument !is JsonPathFilterExpressionValue.ValueTypeValue.JsonValue) {
            null
        } else when (argument.jsonElement) {
            is JsonArray -> JsonPrimitive(argument.jsonElement.size.toUInt())

            is JsonObject -> JsonPrimitive(argument.jsonElement.size.toUInt())

            is JsonPrimitive -> if (argument.jsonElement.isString) {
                JsonPrimitive(
                    run {
                        val codePoints =
                            argument.jsonElement.content.count() + argument.jsonElement.content.count {
                                it.code > 0xffff
                            }
                        codePoints.toUInt()
                    }
                )
            } else null
        }
    }
}