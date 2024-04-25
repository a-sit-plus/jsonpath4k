package at.asitplus.jsonpath

import kotlinx.serialization.json.JsonElement

typealias NodeList = List<NodeListEntry>

data class NodeListEntry(
    val normalizedJsonPath: NormalizedJsonPath,
    val value: JsonElement,
)
