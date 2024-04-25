package at.asitplus.wallet.lib.data.jsonpath.functionExtensions

import at.asitplus.jsonpath.JsonPathFilterExpressionValue
import at.asitplus.jsonpath.JsonPathFilterExpressionType
import at.asitplus.jsonpath.JsonPathFunctionExtension
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalSerializationApi::class)
data object CountFunctionExtension : JsonPathFunctionExtension.ValueTypeFunctionExtension(
    name = "count",
    argumentTypes = listOf(
        JsonPathFilterExpressionType.NodesType,
    )
) {
    override fun invoke(arguments: List<JsonPathFilterExpressionValue>): JsonPathFilterExpressionValue.ValueTypeValue {
        super.validateArgumentTypes(arguments)
        return implementation(
            arguments[0] as JsonPathFilterExpressionValue.NodesTypeValue
        )
    }

    private fun implementation(nodesTypeValue: JsonPathFilterExpressionValue.NodesTypeValue): JsonPathFilterExpressionValue.ValueTypeValue {
        return JsonPathFilterExpressionValue.ValueTypeValue.JsonValue(
            JsonPrimitive(nodesTypeValue.nodeList.size.toUInt())
        )
    }
}