package at.asitplus.jsonpath

sealed interface NormalizedJsonPathSegment {
    class NameSegment(val memberName: String) : NormalizedJsonPathSegment {
        override fun toString(): String {
            return "[${Rfc9535Utils.escapeToSingleQuotedStringLiteral(memberName)}]"
        }
    }
    class IndexSegment(val index: UInt) : NormalizedJsonPathSegment {
        override fun toString(): String {
            return "[$index]"
        }
    }
}