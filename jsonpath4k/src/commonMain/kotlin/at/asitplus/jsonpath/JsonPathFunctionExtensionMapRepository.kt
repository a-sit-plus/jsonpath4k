package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.JsonPathFunctionExtension

internal class JsonPathFunctionExtensionMapRepository(
    private val extensions: MutableMap<String, JsonPathFunctionExtension<*>> = mutableMapOf()
) : JsonPathFunctionExtensionRepository {
    override fun addExtension(
        name: String,
        extension: () -> JsonPathFunctionExtension<*>
    ) {
        extensions[name]?.let {
            throw FunctionExtensionCollisionException(
                "A function extension with the name \"$name\" has already been added: $it"
            )
        }
        extensions[name] = extension()
    }

    override fun getExtension(name: String): JsonPathFunctionExtension<*>? = extensions[name]
    override fun export(): Map<String, JsonPathFunctionExtension<*>> = extensions.toMap()
}