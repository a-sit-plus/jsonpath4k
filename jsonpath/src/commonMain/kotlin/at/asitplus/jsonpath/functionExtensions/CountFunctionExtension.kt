package at.asitplus.wallet.lib.data.jsonpath.functionExtensions

import at.asitplus.jsonpath.JsonPathExpressionValue
import at.asitplus.jsonpath.JsonPathExpressionType
import at.asitplus.jsonpath.JsonPathFunctionExtension
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalSerializationApi::class)
data object CountFunctionExtension : JsonPathFunctionExtension.ValueTypeFunctionExtension(
    name = "count",
    argumentTypes = listOf(
        JsonPathExpressionType.NodesType,
    )
) {
    override fun invoke(arguments: List<JsonPathExpressionValue>): JsonPathExpressionValue.ValueTypeValue {
        super.validateArgumentTypes(arguments)
        return implementation(
            arguments[0] as JsonPathExpressionValue.NodesTypeValue
        )
    }

    private fun implementation(nodesTypeValue: JsonPathExpressionValue.NodesTypeValue): JsonPathExpressionValue.ValueTypeValue {
        return JsonPathExpressionValue.ValueTypeValue.JsonValue(
            JsonPrimitive(nodesTypeValue.nodeList.size.toUInt())
        )
    }
}