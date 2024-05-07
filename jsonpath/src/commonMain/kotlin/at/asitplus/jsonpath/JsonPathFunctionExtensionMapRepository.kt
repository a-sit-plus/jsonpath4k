package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.JsonPathFunctionExtension

internal class JsonPathFunctionExtensionMapRepository(
    private val extensions: MutableMap<String, JsonPathFunctionExtension<*>> = mutableMapOf()
) : JsonPathFunctionExtensionRepository {
    override fun addExtension(
        name: String,
        extension: () -> JsonPathFunctionExtension<*>
    ): Boolean = if(extensions.containsKey(name)) {
        false
    } else true.also{
        extensions[name] = extension()
    }

    override fun getExtension(name: String): JsonPathFunctionExtension<*>? = extensions[name]
    override fun export(): Map<String, JsonPathFunctionExtension<*>> = extensions.toMap()
    override fun copy(): JsonPathFunctionExtensionRepository =
        JsonPathFunctionExtensionMapRepository(extensions.toMutableMap())
}