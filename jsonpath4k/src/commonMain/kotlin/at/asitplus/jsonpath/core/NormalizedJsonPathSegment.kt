package at.asitplus.jsonpath.core

import at.asitplus.jsonpath.generated.JsonPathLexer
import at.asitplus.jsonpath.generated.JsonPathParser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.ListTokenSource
import kotlin.jvm.JvmInline

/**
 * specification: https://datatracker.ietf.org/doc/rfc9535/
 * date: 2024-02
 * section: 2.7.  Normalized Paths
 */
@Serializable(with = NormalizedJsonPathSegment.JsonDistinguishableSerializer::class)
sealed interface NormalizedJsonPathSegment {
    fun toNormalizedJsonPathSegmentString(): String

    @Serializable
    @JvmInline
    value class NameSegment(val memberName: String) : NormalizedJsonPathSegment {
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
    @JvmInline
    value class IndexSegment(val index: UInt) : NormalizedJsonPathSegment {
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

    class JsonDistinguishableSerializer : KSerializer<NormalizedJsonPathSegment> {
        override val descriptor: SerialDescriptor
            get() = SerialDescriptor(
                original = JsonElement.serializer().descriptor,
                serialName = JsonDistinguishableSerializer::class.simpleName!!,
            )

        override fun serialize(
            encoder: Encoder,
            value: NormalizedJsonPathSegment
        ) {
            when (value) {
                is IndexSegment -> encoder.encodeLong(value.index.toLong())
                is NameSegment -> encoder.encodeString(value.memberName)
            }
        }

        override fun deserialize(decoder: Decoder): NormalizedJsonPathSegment {
            require(decoder is JsonDecoder) {
                "Expected decoder to be ${JsonDecoder::class.simpleName!!}, but was `$decoder`."
            }
            val jsonElement = decoder.decodeJsonElement().jsonPrimitive
            return if (jsonElement.isString) {
                NameSegment(jsonElement.content)
            } else {
                IndexSegment(jsonElement.long.also {
                    require(it >= 0) {
                        "Expected index segment to be non-negative, but got `$it`."
                    }
                }.toUInt())
            }
        }
    }
}