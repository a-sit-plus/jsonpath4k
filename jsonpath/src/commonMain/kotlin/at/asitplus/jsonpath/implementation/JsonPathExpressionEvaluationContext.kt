package at.asitplus.jsonpath.implementation

import kotlinx.serialization.json.JsonElement

data class JsonPathExpressionEvaluationContext(
    val currentNode: JsonElement,
    val rootNode: JsonElement = currentNode,
)