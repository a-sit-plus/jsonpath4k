package at.asitplus.jsonpath.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * specification: https://datatracker.ietf.org/doc/rfc9535/
 * date: 2024-02
 * section: 2.7.  Normalized Paths
 */
@Serializable
@JvmInline
value class NormalizedJsonPath(
    private val segments: List<NormalizedJsonPathSegment> = listOf(),
) : List<NormalizedJsonPathSegment> by segments {
    constructor(vararg segments: NormalizedJsonPathSegment) : this(segments = segments.asList())

    @ExperimentalUnsignedTypes
    constructor(vararg segments: UInt) : this(
        segments.map {
            NormalizedJsonPathSegment.IndexSegment(it)
        }
    )

    constructor(vararg segments: String) : this(
        segments.map {
            NormalizedJsonPathSegment.NameSegment(it)
        }
    )

    companion object {
        operator fun invoke(vararg segments: Int) = NormalizedJsonPath(segments.map {
            require(it >= 0) {
                "Expected index segments to be non-negative, but got $it"
            }
            NormalizedJsonPathSegment.IndexSegment(it)
        })
    }

    operator fun plus(
        other: List<NormalizedJsonPathSegment>
    ) = NormalizedJsonPath(this.segments + other)

    operator fun plus(segment: NormalizedJsonPathSegment) = this + listOf(segment)

    operator fun plus(memberName: String) = this + NormalizedJsonPathSegment.NameSegment(memberName)

    operator fun plus(index: UInt) = this + NormalizedJsonPathSegment.IndexSegment(index)

    override fun toString(): String {
        return "$${joinToString("")}"
    }

    fun toNormalizedJsonPathString() = toString()

    @Throws(Throwable::class)
    fun toShorthandNameSegmentNotation(): String {
        return "$${
            joinToString("") {
                when (it) {
                    is NormalizedJsonPathSegment.IndexSegment -> it.toString()
                    is NormalizedJsonPathSegment.NameSegment -> it.toShorthandNotation()
                }
            }
        }"
    }

    fun toShorthandNameSegmentNotationWherePossible(): String {
        return "$${
            joinToString("") {
                when (it) {
                    is NormalizedJsonPathSegment.IndexSegment -> it.toNormalizedJsonPathSegmentString()
                    is NormalizedJsonPathSegment.NameSegment -> try {
                        it.toShorthandNotation()
                    } catch (_: Throwable) {
                        it.toNormalizedJsonPathSegmentString()
                    }
                }
            }
        }"
    }
}