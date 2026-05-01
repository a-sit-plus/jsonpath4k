package at.asitplus.jsonpath.core

import at.asitplus.jsonpath.generated.JsonPathLexer
import at.asitplus.jsonpath.generated.JsonPathParser
import kotlinx.serialization.Serializable
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.ListTokenSource

/**
 * specification: https://datatracker.ietf.org/doc/rfc9535/
 * date: 2024-02
 * section: 2.7.  Normalized Paths
 */
@Serializable
sealed interface NormalizedJsonPathSegment {
    fun toNormalizedJsonPathSegmentString(): String

    @Serializable
    data class NameSegment(val memberName: String) : NormalizedJsonPathSegment {
        override fun toNormalizedJsonPathSegmentString() = toString()

        override fun toString(): String {
            return "[${Rfc9535Utils.escapeToSingleQuotedStringLiteral(memberName)}]"
        }

        @Throws(Throwable::class)
        fun toShorthandNotation(): String {
            val tokens = JsonPathLexer(CharStreams.fromString(".$memberName")).allTokens

            val commonTokenStream = CommonTokenStream(ListTokenSource(tokens))
            val shorthandSegmentContext = JsonPathParser(commonTokenStream).shorthand_segment()

            shorthandSegmentContext.memberNameShorthand()?.let {
                if (it.MEMBER_NAME_SHORTHAND().text != memberName) {
                    null
                } else {
                    memberName
                }
            } ?: throw IllegalStateException(
                "Cannot represent member name ${
                    Rfc9535Utils.escapeToDoubleQuoted(memberName)
                } in shorthand notation."
            )

            return ".$memberName"
        }
    }

    @Serializable
    data class IndexSegment(val index: UInt) : NormalizedJsonPathSegment {
        companion object {
            operator fun invoke(int: Int) = IndexSegment(int.also {
                require(int >= 0)
            }.toUInt())
        }

        override fun toNormalizedJsonPathSegmentString() = toString()

        override fun toString(): String {
            return "[$index]"
        }
    }
}