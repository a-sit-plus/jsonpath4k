package at.asitplus.jsonpath

import at.asitplus.wallet.lib.data.jsonpath.JsonPathSelector
import kotlinx.serialization.json.JsonElement

interface JsonPathQuery {
    fun invoke(currentNode: JsonElement, rootNode: JsonElement = currentNode): NodeList
}

class JsonPathSelectorQuery(
    val selectors: List<JsonPathSelector>,
) : JsonPathQuery {
    override fun invoke(currentNode: JsonElement, rootNode: JsonElement): NodeList {
        var matches = listOf(
            NodeListEntry(
                normalizedJsonPath = NormalizedJsonPath(),
                value = currentNode,
            )
        )
        selectors.forEach { selector ->
            matches = matches.flatMap { match ->
                selector.invoke(
                    currentNode = match.value,
                    rootNode = rootNode,
                ).map { newMatch ->
                    NodeListEntry(
                        normalizedJsonPath = match.normalizedJsonPath + newMatch.normalizedJsonPath,
                        value = newMatch.value
                    )
                }
            }
        }
        return matches
    }

    val isSingularQuery: Boolean
        get() = selectors.all {
            when(it) {
                JsonPathSelector.RootSelector,
                JsonPathSelector.CurrentNodeSelector,
                is JsonPathSelector.MemberSelector,
                is JsonPathSelector.IndexSelector -> true
                else -> false
            }
        }
}