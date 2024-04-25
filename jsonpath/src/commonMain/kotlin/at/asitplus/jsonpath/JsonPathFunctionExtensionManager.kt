package at.asitplus.jsonpath

import at.asitplus.wallet.lib.data.jsonpath.functionExtensions.CountFunctionExtension
import at.asitplus.jsonpath.functionExtensions.LengthFunctionExtension
import at.asitplus.jsonpath.functionExtensions.MatchFunctionExtension
import at.asitplus.jsonpath.functionExtensions.SearchFunctionExtension
import at.asitplus.jsonpath.functionExtensions.ValueFunctionExtension

interface JsonPathFunctionExtensionManager {
    fun addExtension(functionExtension: JsonPathFunctionExtension<*>)
    fun putExtension(functionExtension: JsonPathFunctionExtension<*>)
    fun removeExtension(functionExtensionName: String)
    fun getExtension(name: String): JsonPathFunctionExtension<*>?
}

val defaultJsonPathFunctionExtensionManager by lazy {
    object : JsonPathFunctionExtensionManager {
        private val extensions: MutableMap<String, JsonPathFunctionExtension<*>> = mutableMapOf()

        override fun addExtension(functionExtension: JsonPathFunctionExtension<*>) {
            if(extensions.containsKey(functionExtension.name)) {
                throw FunctionExtensionCollisionException(functionExtension.name)
            }
            extensions[functionExtension.name] = functionExtension
        }

        override fun putExtension(functionExtension: JsonPathFunctionExtension<*>) {
            extensions[functionExtension.name] = functionExtension
        }

        override fun removeExtension(functionExtensionName: String) {
            extensions.remove(functionExtensionName)
        }

        override fun getExtension(name: String): JsonPathFunctionExtension<*>? {
            return extensions[name]
        }
    }.apply {
        addExtension(LengthFunctionExtension)
        addExtension(CountFunctionExtension)
        addExtension(MatchFunctionExtension)
        addExtension(SearchFunctionExtension)
        addExtension(ValueFunctionExtension)
    }
}

class FunctionExtensionCollisionException(val functionName: String) : Exception(
    "A function extension with the name \"$functionName\" has already been registered."
)