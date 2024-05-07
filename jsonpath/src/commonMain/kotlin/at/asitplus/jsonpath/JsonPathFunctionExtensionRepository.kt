package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.JsonPathFunctionExtension

/**
 * This class is not specified in the rfc standard, it's but an implementation detail.
 * It's a way to provide users with a way to add custom function extensions.
 */
interface JsonPathFunctionExtensionRepository {
    /**
     * Implementations should return false if an extension with that name already exists, and true if extension was added successfully
     */
    fun addExtension(name: String, extension: () -> JsonPathFunctionExtension<*>): Boolean
    fun getExtension(name: String): JsonPathFunctionExtension<*>?
    fun export(): Map<String, JsonPathFunctionExtension<*>>
    fun copy(): JsonPathFunctionExtensionRepository
}