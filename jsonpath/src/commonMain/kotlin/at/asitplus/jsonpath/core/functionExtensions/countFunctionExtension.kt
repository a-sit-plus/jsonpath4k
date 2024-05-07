package at.asitplus.jsonpath.core.functionExtensions

import at.asitplus.jsonpath.core.JsonPathFilterExpressionValue
import at.asitplus.jsonpath.core.JsonPathFilterExpressionType
import at.asitplus.jsonpath.core.JsonPathFunctionExtension
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonPrimitive

/**
 * specification: https://datatracker.ietf.org/doc/rfc9535/
 * date: 2024-02
 * section: 2.4.5.  count() Function Extension
 */
@OptIn(ExperimentalSerializationApi::class)
internal val countFunctionExtension by lazy {
    "count" to JsonPathFunctionExtension.ValueTypeFunctionExtension(
        JsonPathFilterExpressionType.NodesType,
    ) {
        val nodesTypeValue = it[0] as JsonPathFilterExpressionValue.NodesTypeValue
        JsonPrimitive(nodesTypeValue.nodeList.size.toUInt())
    }
}