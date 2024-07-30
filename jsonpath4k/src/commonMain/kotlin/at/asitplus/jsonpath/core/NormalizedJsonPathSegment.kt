package at.asitplus.jsonpath.core

import kotlinx.serialization.Serializable

/**
 * specification: https://datatracker.ietf.org/doc/rfc9535/
 * date: 2024-02
 * section: 2.7.  Normalized Paths
 */
@Serializable
sealed interface NormalizedJsonPathSegment {
    @Serializable
    class NameSegment(val memberName: String) : NormalizedJsonPathSegment {
        override fun toString(): String {
            return "[${Rfc9535Utils.escapeToSingleQuotedStringLiteral(memberName)}]"
        }
    }
    @Serializable
    class IndexSegment(val index: UInt) : NormalizedJsonPathSegment {
        override fun toString(): String {
            return "[$index]"
        }
    }
}