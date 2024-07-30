package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.JsonPathFunctionExtension

/**
 * This class is not specified in the rfc standard, it's but an implementation detail.
 * It's a way to provide users with a way to add custom function extensions.
 */
interface JsonPathFunctionExtensionRepository {
    /**
     * Implementations should throw FunctionExtensionCollisionException if an extension with that name already exists.
     */
    fun addExtension(name: String, extension: () -> JsonPathFunctionExtension<*>)
    fun getExtension(name: String): JsonPathFunctionExtension<*>?
    fun export(): Map<String, JsonPathFunctionExtension<*>>
}

open class FunctionExtensionCollisionException(message: String) : Exception(message)