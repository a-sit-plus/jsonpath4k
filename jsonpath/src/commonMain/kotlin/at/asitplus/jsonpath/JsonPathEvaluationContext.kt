package at.asitplus.jsonpath

import kotlinx.serialization.json.JsonElement

data class JsonPathEvaluationContext(
    val currentNode: JsonElement,
    val rootNode: JsonElement = currentNode,
)