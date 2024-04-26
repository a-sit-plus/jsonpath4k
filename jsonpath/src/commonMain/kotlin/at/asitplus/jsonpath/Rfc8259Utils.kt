package at.asitplus.jsonpath

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

object Rfc8259Utils {
    fun unpackStringLiteral(string: String): String {
        return Json.decodeFromString<JsonPrimitive>(string).content
    }

    fun escapeToDoubleQuotedString(string: String): String {
        return Json.encodeToString(JsonPrimitive(string))
    }
}