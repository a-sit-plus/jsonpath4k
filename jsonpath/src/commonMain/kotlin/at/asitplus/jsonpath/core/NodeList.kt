package at.asitplus.jsonpath.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

typealias NodeList = List<NodeListEntry>

@Serializable
data class NodeListEntry(
    val normalizedJsonPath: NormalizedJsonPath,
    val value: JsonElement,
)
