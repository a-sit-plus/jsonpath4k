package at.asitplus.jsonpath.core.functionExtensions

import at.asitplus.jsonpath.core.JsonPathFilterExpressionType
import at.asitplus.jsonpath.core.JsonPathFilterExpressionValue
import at.asitplus.jsonpath.core.JsonPathFunctionExtension

/**
 * specification: https://datatracker.ietf.org/doc/rfc9535/
 * date: 2024-02
 * section: 2.4.8.  value() Function Extension
 */
internal val valueFunctionExtension by lazy {
    "value" to JsonPathFunctionExtension.ValueTypeFunctionExtension(
        JsonPathFilterExpressionType.NodesType,
    ) {
        val argument = it[0] as JsonPathFilterExpressionValue.NodesTypeValue
        if (argument.nodeList.size == 1) {
            argument.nodeList[0]
        } else null
    }
}