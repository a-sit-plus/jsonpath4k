package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.Rfc9535Utils
import at.asitplus.testballoon.matrix.matrixSuite
import io.kotest.matchers.shouldBe

val Rfc9535UtilsUnitTest by matrixSuite {
    "Rfc9535Utils.unpackStringLiteral Unit Tests" - {
        "rfc8259 conformance" - {
            "special escape characters" - {
                data(
                    "double quoted",
                    listOf(
                        "\"\\\\\"" to "\\",
                        "\"\\/\"" to "/",
                        "\"\\\"\"" to "\"",
                        "\"\\b\"" to Char(0x0008).toString(),
                        "\"\\f\"" to Char(0x000C).toString(),
                        "\"\\n\"" to "\n",
                        "\"\\r\"" to "\r",
                        "\"\\t\"" to "\t",
                    ),
                    nameFn = { (input, _) -> input },
                ) test { (input, expected) ->
                    Rfc9535Utils.unpackStringLiteral(input) shouldBe expected
                }

                data(
                    "single quoted",
                    listOf(
                        "'\\\\'" to "\\",
                        "'\\/'" to "/",
                        "'\\''" to "'",
                        "'\\b'" to Char(0x0008).toString(),
                        "'\\f'" to Char(0x000C).toString(),
                        "'\\n'" to "\n",
                        "'\\r'" to "\r",
                        "'\\t'" to "\t",
                    ),
                    nameFn = { (input, _) -> input },
                ) test { (input, expected) ->
                    Rfc9535Utils.unpackStringLiteral(input) shouldBe expected
                }
            }
        }
    }

    "Rfc9535Utils.switchToSingleQuotedString Unit Tests" - {
        "rfc8259 conformance" - {
            data(
                "special escape characters",
                listOf(
                    "\"\\\\\"" to "'\\\\'",
                    "\"\\/\"" to "'\\/'",
                    "\"\\\"\"" to "'\"'",
                    "\"'\"" to "'\\''",
                    "\"\\b\"" to "'\\b'",
                    "\"\\f\"" to "'\\f'",
                    "\"\\n\"" to "'\\n'",
                    "\"\\r\"" to "'\\r'",
                    "\"\\t\"" to "'\\t'",
                ),
                nameFn = { (input, _) -> input },
            ) test { (input, expected) ->
                Rfc9535Utils.switchToSingleQuotedString(input) shouldBe expected
                // switching an already single-quoted string is idempotent
                Rfc9535Utils.switchToSingleQuotedString(expected) shouldBe expected
            }
        }
    }

    "Rfc9535Utils.switchToDoubleQuotedString Unit Tests" - {
        "rfc8259 conformance" - {
            data(
                "special escape characters",
                listOf(
                    "'\\\\'" to "\"\\\\\"",
                    "'\\/'" to "\"\\/\"",
                    "'\"'" to "\"\\\"\"",
                    "'\\''" to "\"'\"",
                    "'\\b'" to "\"\\b\"",
                    "'\\f'" to "\"\\f\"",
                    "'\\n'" to "\"\\n\"",
                    "'\\r'" to "\"\\r\"",
                    "'\\t'" to "\"\\t\"",
                ),
                nameFn = { (input, _) -> input },
            ) test { (input, expected) ->
                Rfc9535Utils.switchToDoubleQuotedString(input) shouldBe expected
                // switching an already double-quoted string is idempotent
                Rfc9535Utils.switchToDoubleQuotedString(expected) shouldBe expected
            }
        }
    }
}
