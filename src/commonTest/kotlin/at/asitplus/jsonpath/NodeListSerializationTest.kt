package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.NodeListEntry
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

@Suppress("unused")
class NodeListSerializationTest : FreeSpec({
    "NormalizedJsonPathSegment" - {
        "NameSegment" {
            val segment: NormalizedJsonPathSegment = NormalizedJsonPathSegment.NameSegment("test")

            val stringified = Json.encodeToString(segment)
            val reconstructed = Json.decodeFromString<NormalizedJsonPathSegment>(stringified)

            reconstructed.toString() shouldBe segment.toString()
        }
        "IndexSegment" {
            val segment: NormalizedJsonPathSegment = NormalizedJsonPathSegment.IndexSegment(42u)

            val stringified = Json.encodeToString(segment)
            val reconstructed = Json.decodeFromString<NormalizedJsonPathSegment>(stringified)

            reconstructed.toString() shouldBe segment.toString()
        }
    }
    "NodeListEntry" - {
        "1 NameSegment" {
            val jsonObject = buildJsonObject {
                put("test", JsonPrimitive(42))
            }
            val entry = NodeListEntry(
                normalizedJsonPath = NormalizedJsonPath(NormalizedJsonPathSegment.NameSegment("test")),
                value = jsonObject
            )
            val stringified = Json.encodeToString(entry)
            val reconstructed = Json.decodeFromString<NodeListEntry>(stringified)
            reconstructed.normalizedJsonPath.toString() shouldBe entry.normalizedJsonPath.toString()
        }
        "1 IndexSegment" {
            val jsonObject = buildJsonArray {
                add(JsonPrimitive(42))
            }
            val entry = NodeListEntry(
                normalizedJsonPath = NormalizedJsonPath(NormalizedJsonPathSegment.IndexSegment(0u)),
                value = jsonObject
            )
            val stringified = Json.encodeToString(entry)
            val reconstructed = Json.decodeFromString<NodeListEntry>(stringified)
            reconstructed.normalizedJsonPath.toString() shouldBe entry.normalizedJsonPath.toString()
        }
        "1 IndexSegment, 1 NameSegment" {
            val key = "key"
            val jsonObject = buildJsonArray {
                add(buildJsonObject {
                    put(key, JsonPrimitive(42))
                })
            }
            val entry = NodeListEntry(
                normalizedJsonPath = NormalizedJsonPath(
                    NormalizedJsonPathSegment.IndexSegment(0u),
                    NormalizedJsonPathSegment.NameSegment(key),
                ),
                value = jsonObject
            )
            val stringified = Json.encodeToString(entry)
            val reconstructed = Json.decodeFromString<NodeListEntry>(stringified)
            reconstructed.normalizedJsonPath.toString() shouldBe entry.normalizedJsonPath.toString()
        }
        "1 NameSegment, 1 IndexSegment" {
            val key = "key"
            val jsonObject = buildJsonObject {
                put(key, buildJsonArray {
                    add(JsonPrimitive(42))
                })
            }
            val entry = NodeListEntry(
                normalizedJsonPath = NormalizedJsonPath(
                    NormalizedJsonPathSegment.IndexSegment(0u),
                    NormalizedJsonPathSegment.NameSegment(key),
                ),
                value = jsonObject
            )
            val stringified = Json.encodeToString(entry)
            val reconstructed = Json.decodeFromString<NodeListEntry>(stringified)
            reconstructed.normalizedJsonPath.toString() shouldBe entry.normalizedJsonPath.toString()
        }
    }
    "NodeList" - {
        // trusting the default serializer for list for this one
    }
})