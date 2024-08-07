package at.asitplus.jsonpath

interface Logger {
    fun e(throwable: Throwable? = null, tag: String? = null, message: () -> String)
}