package at.asitplus.jsonpath.functionExtensions

import at.asitplus.jsonpath.JsonPathFilterExpressionType
import at.asitplus.jsonpath.JsonPathFilterExpressionValue
import at.asitplus.jsonpath.JsonPathFunctionExtension


data object ValueFunctionExtension : JsonPathFunctionExtension.ValueTypeFunctionExtension(
    name = "value",
    argumentTypes = listOf(
        JsonPathFilterExpressionType.NodesType,
    )
) {
    override fun invoke(arguments: List<JsonPathFilterExpressionValue>): JsonPathFilterExpressionValue.ValueTypeValue {
        super.validateArgumentTypes(arguments)
        return implementation(
            nodesTypeValue = arguments[0] as JsonPathFilterExpressionValue.NodesTypeValue
        )
    }

    private fun implementation(nodesTypeValue: JsonPathFilterExpressionValue.NodesTypeValue): JsonPathFilterExpressionValue.ValueTypeValue {
        return if (nodesTypeValue.nodeList.size == 1) {
            JsonPathFilterExpressionValue.ValueTypeValue.JsonValue(nodesTypeValue.nodeList[0])
        } else JsonPathFilterExpressionValue.ValueTypeValue.Nothing
    }
}