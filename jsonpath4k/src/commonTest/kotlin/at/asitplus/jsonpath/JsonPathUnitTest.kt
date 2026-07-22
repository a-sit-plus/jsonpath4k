package at.asitplus.jsonpath

import at.asitplus.jsonpath.core.JsonPathFilterExpressionType
import at.asitplus.jsonpath.core.JsonPathFunctionExtension
import at.asitplus.jsonpath.implementation.JsonPathParserException
import at.asitplus.jsonpath.implementation.JsonPathTypeCheckerException
import at.asitplus.testballoon.matrix.MatrixSuiteScope
import at.asitplus.testballoon.matrix.matrixSuite
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEachTest
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Registers a FreeSpec-style test whose name IS the JSONPath expression, compiles that exact expression,
 * queries [json] and hands the resulting value list to [assertions]. Mirrors the original Kotest idiom that
 * recovered the expression from `testCase.name.originalName`.
 */
private fun MatrixSuiteScope.query(
    path: String,
    json: JsonElement,
    assertions: (nodeList: List<JsonElement>) -> Unit,
) = path { assertions(JsonPath(path).query(json).map { it.value }) }

/** Test named after [path] asserting that compiling it succeeds. */
private fun MatrixSuiteScope.compiles(path: String) =
    path { shouldNotThrowAny { JsonPath(path) } }

/** Test named after [path] asserting that compiling it fails type checking. */
private fun MatrixSuiteScope.failsTypeCheck(path: String) =
    path { shouldThrow<JsonPathTypeCheckerException> { JsonPath(path) } }

/** Test named after [path] asserting that compiling it fails parsing. */
private fun MatrixSuiteScope.failsParsing(path: String) =
    path { shouldThrow<JsonPathParserException> { JsonPath(path) } }

// Captured once, before any test in section 2.4 mutates the process-global JsonPath dependencies.
private val defaultCompilerBuilderBackup = JsonPath.defaultCompiler
private val defaultFunctionExtensionRepositoryBackup =
    JsonPath.defaultFunctionExtensionRepository.export()

val JsonPathUnitTest by matrixSuite {
    "Examples from https://datatracker.ietf.org/doc/rfc9535/" - {
        "1.5.  JSONPath Examples" - {
            val bookStore = Json.decodeFromString<JsonElement>(
                "   { \"store\": {\n" +
                        "       \"book\": [\n" +
                        "         { \"category\": \"reference\",\n" +
                        "           \"author\": \"Nigel Rees\",\n" +
                        "           \"title\": \"Sayings of the Century\",\n" +
                        "           \"price\": 8.95\n" +
                        "         },\n" +
                        "         { \"category\": \"fiction\",\n" +
                        "           \"author\": \"Evelyn Waugh\",\n" +
                        "           \"title\": \"Sword of Honour\",\n" +
                        "           \"price\": 12.99\n" +
                        "         },\n" +
                        "         { \"category\": \"fiction\",\n" +
                        "           \"author\": \"Herman Melville\",\n" +
                        "           \"title\": \"Moby Dick\",\n" +
                        "           \"isbn\": \"0-553-21311-3\",\n" +
                        "           \"price\": 8.99\n" +
                        "         },\n" +
                        "         { \"category\": \"fiction\",\n" +
                        "           \"author\": \"J. R. R. Tolkien\",\n" +
                        "           \"title\": \"The Lord of the Rings\",\n" +
                        "           \"isbn\": \"0-395-19395-8\",\n" +
                        "           \"price\": 22.99\n" +
                        "         }\n" +
                        "       ],\n" +
                        "       \"bicycle\": {\n" +
                        "         \"color\": \"red\",\n" +
                        "         \"price\": 399\n" +
                        "       }\n" +
                        "     }\n" +
                        "   }"
            )

            query("$.store.book[*].author", bookStore) { nodeList ->
                nodeList shouldHaveSize 4
                val bookArray =
                    bookStore.jsonObject["store"].shouldNotBeNull().jsonObject["book"].shouldNotBeNull().jsonArray
                bookArray.forEach {
                    val author = it.jsonObject["author"].shouldNotBeNull()
                    author.shouldBeIn(nodeList)
                }
            }

            query("$..author", bookStore) { nodeList ->
                nodeList shouldHaveSize 4
                val bookArray =
                    bookStore.jsonObject["store"].shouldNotBeNull().jsonObject["book"].shouldNotBeNull().jsonArray
                bookArray.forEach {
                    val author = it.jsonObject["author"].shouldNotBeNull()
                    author.shouldBeIn(nodeList)
                }
            }

            query("\$.store.*", bookStore) { nodeList ->
                nodeList shouldHaveSize 2
                val store =
                    bookStore.jsonObject["store"].shouldNotBeNull().jsonObject
                store.get("book").shouldNotBeNull().shouldBeIn(nodeList)
                store.get("bicycle").shouldNotBeNull().shouldBeIn(nodeList)
            }

            query("\$.store..price", bookStore) { nodeList ->
                nodeList shouldHaveSize 5
                val store =
                    bookStore.jsonObject["store"].shouldNotBeNull().jsonObject
                store.get("book").shouldNotBeNull().jsonArray.forEach {
                    it.jsonObject["price"].shouldBeIn(nodeList)
                }
                store.get("bicycle").shouldNotBeNull().jsonObject["price"].shouldNotBeNull()
                    .shouldBeIn(nodeList)
            }

            query("\$..book[2]", bookStore) { nodeList ->
                nodeList shouldHaveSize 1
                bookStore.jsonObject["store"]
                    .shouldNotBeNull().jsonObject["book"]
                    .shouldNotBeNull().jsonArray[2]
                    .shouldNotBeNull().shouldBeIn(nodeList)
            }

            query("\$..book[2].author", bookStore) { nodeList ->
                nodeList shouldHaveSize 1
                bookStore.jsonObject["store"]
                    .shouldNotBeNull().jsonObject["book"]
                    .shouldNotBeNull().jsonArray[2]
                    .shouldNotBeNull().jsonObject["author"]
                    .shouldNotBeNull().shouldBeIn(nodeList)
            }

            query("\$..book[2].publisher", bookStore) { nodeList ->
                nodeList shouldHaveSize 0
            }

            query("\$..book[-1]", bookStore) { nodeList ->
                nodeList shouldHaveSize 1
                bookStore.jsonObject["store"]
                    .shouldNotBeNull().jsonObject["book"]
                    .shouldNotBeNull().jsonArray.let { it[it.size - 1] }
                    .shouldNotBeNull().shouldBeIn(nodeList)
            }

            query("\$..book[0,1]", bookStore) { nodeList ->
                nodeList shouldHaveSize 2
                bookStore.jsonObject["store"]
                    .shouldNotBeNull().jsonObject["book"]
                    .shouldNotBeNull().jsonArray.filterIndexed { index, jsonElement ->
                        index < 2
                    }.forEach {
                        it.shouldBeIn(nodeList)
                    }
            }

            query("\$..book[:2]", bookStore) { nodeList ->
                nodeList shouldHaveSize 2
                bookStore.jsonObject["store"]
                    .shouldNotBeNull().jsonObject["book"]
                    .shouldNotBeNull().jsonArray.filterIndexed { index, jsonElement ->
                        index < 2
                    }.forEach {
                        it.shouldBeIn(nodeList)
                    }
            }

            query("\$..book[?@.isbn]", bookStore) { nodeList ->
                nodeList shouldHaveSize 2
                bookStore.jsonObject["store"]
                    .shouldNotBeNull().jsonObject["book"]
                    .shouldNotBeNull().jsonArray.filter {
                        it.jsonObject.containsKey("isbn")
                    }.forEach {
                        it.shouldBeIn(nodeList)
                    }
            }

            query("\$..book[?@.price<10]", bookStore) { nodeList ->
                nodeList shouldHaveSize 2
                bookStore.jsonObject["store"]
                    .shouldNotBeNull().jsonObject["book"]
                    .shouldNotBeNull().jsonArray.filter {
                        it.jsonObject.get("price").shouldNotBeNull().jsonPrimitive.double < 10
                    }.forEach {
                        it.shouldBeIn(nodeList)
                    }
            }

            query("\$..*", bookStore) { nodeList ->
                bookStore.jsonObject["store"]
                    .shouldNotBeNull().shouldBeIn(nodeList).jsonObject.let { store ->
                        store["book"].shouldNotBeNull()
                            .shouldBeIn(nodeList).jsonArray.forEach { book ->
                                book.shouldBeIn(nodeList).jsonObject.forEach { entry ->
                                    entry.value.shouldBeIn(nodeList)
                                }
                            }
                        store["bicycle"].shouldNotBeNull()
                            .shouldBeIn(nodeList).jsonObject.forEach { entry ->
                                entry.value.shouldBeIn(nodeList)
                            }
                    }
            }
        }

        "2.1. Overview" - {
            val jsonElement =
                Json.decodeFromString<JsonElement>("{\"a\":[{\"b\":0},{\"b\":1},{\"c\":2}]}")
            query("\$", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement.shouldBeIn(nodeList)
            }
            query("\$.a", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement.jsonObject["a"].shouldNotBeNull().shouldBeIn(nodeList)
            }
            query("\$.a[*]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 3
                jsonElement.jsonObject["a"].shouldNotBeNull().jsonArray.forEach {
                    it.shouldBeIn(nodeList)
                }
            }
            query("\$.a[*].b", jsonElement) { nodeList ->
                nodeList shouldHaveSize 2
                jsonElement.jsonObject["a"].shouldNotBeNull().jsonArray.forEach {
                    it.shouldBeInstanceOf<JsonObject>()["b"]?.jsonPrimitive?.shouldBeIn(nodeList)
                }
            }
        }

        "2.2. Root Identifier" - {
            val jsonElement = Json.decodeFromString<JsonElement>("{\"k\": \"v\"}")
            query("$", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement.shouldBeIn(nodeList)
            }
        }

        "2.3.1. Name Selector" - {
            val jsonElement = Json.decodeFromString<JsonElement>(
                "{\n" +
                        "                \"o\": {\"j j\": {\"k.k\": 3}},\n" +
                        "                \"'\": {\"@\": 2}\n" +
                        "            }"
            )
            query("\$.o['j j']", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement.jsonObject.get("o")
                    .shouldNotBeNull().jsonObject["j j"]
                    .shouldNotBeNull().shouldBeIn(nodeList)
            }
            query("\$.o['j j']['k.k']", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement.jsonObject.get("o")
                    .shouldNotBeNull().jsonObject["j j"]
                    .shouldNotBeNull().jsonObject["k.k"]
                    .shouldNotBeNull().shouldBeIn(nodeList)
            }
            query("\$.o[\"j j\"][\"k.k\"]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement.jsonObject.get("o")
                    .shouldNotBeNull().jsonObject["j j"]
                    .shouldNotBeNull().jsonObject["k.k"]
                    .shouldNotBeNull().shouldBeIn(nodeList)
            }
            query("\$[\"'\"][\"@\"]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement.jsonObject.get("'")
                    .shouldNotBeNull().jsonObject["@"]
                    .shouldNotBeNull().shouldBeIn(nodeList)
            }
        }

        "2.3.2. Wildcard Selector" - {
            val jsonElement = Json.decodeFromString<JsonElement>(
                "   {\n" +
                        "     \"o\": {\"j\": 1, \"k\": 2},\n" +
                        "     \"a\": [5, 3]\n" +
                        "   }"
            )
            query("\$[*]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 2
                jsonElement.jsonObject.get("o")
                    .shouldNotBeNull().shouldBeIn(nodeList)
                jsonElement.jsonObject.get("a")
                    .shouldNotBeNull().shouldBeIn(nodeList)
            }
            query("\$.o[*]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 2
                jsonElement.jsonObject["o"].shouldNotBeNull().let {
                    it.jsonObject["j"]
                        .shouldNotBeNull().shouldBeIn(nodeList)
                    it.jsonObject["k"]
                        .shouldNotBeNull().shouldBeIn(nodeList)
                }
            }
            query("\$.o[*, *]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 4
                jsonElement.jsonObject["o"].shouldNotBeNull().let {
                    it.jsonObject["j"]
                        .shouldNotBeNull().shouldBeIn(nodeList).let { j ->
                            nodeList.count {
                                it == j
                            }.shouldBe(2)
                        }
                    it.jsonObject["k"]
                        .shouldNotBeNull().shouldBeIn(nodeList).let { k ->
                            nodeList.count {
                                it == k
                            }.shouldBe(2)
                        }
                }
            }
            query("\$.a[*]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 2
                jsonElement.jsonObject["a"].shouldNotBeNull().let {
                    it.jsonArray[0]
                        .shouldNotBeNull().shouldBeIn(nodeList).let { zero ->
                            nodeList.count {
                                it == zero
                            }.shouldBe(1)
                        }
                    it.jsonArray[1]
                        .shouldNotBeNull().shouldBeIn(nodeList).let { one ->
                            nodeList.count {
                                it == one
                            }.shouldBe(1)
                        }
                }
            }
        }

        "2.3.3.  Index Selector" - {
            val jsonElement = Json.decodeFromString<JsonElement>("[\"a\",\"b\"]")

            query("\$[1]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement.jsonArray[1].shouldNotBeNull().shouldBeIn(nodeList)
            }
            query("\$[-2]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement.jsonArray[0].shouldNotBeNull().shouldBeIn(nodeList)
            }
        }

        "2.3.4.  Array Slice Selector" - {
            val jsonElement =
                Json.decodeFromString<JsonElement>("[\"a\", \"b\", \"c\", \"d\", \"e\", \"f\", \"g\"]")

            query("\$[1:3]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 2
                jsonElement.jsonArray[1].shouldNotBeNull().shouldBeIn(nodeList).let {
                    nodeList[0].shouldBe(it)
                }
                jsonElement.jsonArray[2].shouldNotBeNull().shouldBeIn(nodeList).let {
                    nodeList[1].shouldBe(it)
                }
            }
            query("\$[5:]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 2
                jsonElement.jsonArray[5].shouldNotBeNull().shouldBeIn(nodeList).let {
                    nodeList[0].shouldBe(it)
                }
                jsonElement.jsonArray[6].shouldNotBeNull().shouldBeIn(nodeList).let {
                    nodeList[1].shouldBe(it)
                }
            }
            query("\$[1:5:2]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 2
                jsonElement.jsonArray[1].shouldNotBeNull().shouldBeIn(nodeList).let {
                    nodeList[0].shouldBe(it)
                }
                jsonElement.jsonArray[3].shouldNotBeNull().shouldBeIn(nodeList).let {
                    nodeList[1].shouldBe(it)
                }
            }
            query("\$[5:1:-2]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 2
                jsonElement.jsonArray[5].shouldNotBeNull().shouldBeIn(nodeList).let {
                    nodeList[0].shouldBe(it)
                }
                jsonElement.jsonArray[3].shouldNotBeNull().shouldBeIn(nodeList).let {
                    nodeList[1].shouldBe(it)
                }
            }
            query("\$[::-1]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 7
                for (i in 6.downTo(0)) {
                    jsonElement.jsonArray[i].shouldNotBeNull().shouldBeIn(nodeList).let {
                        nodeList[6 - i].shouldBe(it)
                    }
                }
            }
        }

        "2.3.5.  Filter Selector" - {
            "Comparisons" - {
                // since this should be compiler agnostic, check whether the evaluation is correct by using
                // the return list as an indicator for true/false:
                // - if the result should be true, all children should be returned, otherwise zero
                val jsonElement = Json.decodeFromString<JsonElement>(
                    "{\n" +
                            "     \"obj\": {\"x\": \"y\"},\n" +
                            "     \"arr\": [2, 3]\n" +
                            "   }"
                ).jsonObject

                query("\$[?\$.absent1 == \$.absent2]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize jsonElement.size
                }
                query("\$[?\$.absent1 <= \$.absent2 ]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize jsonElement.size
                }
                query("\$[?\$.absent == 'g']", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?\$.absent1 != \$.absent2]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?\$.absent != 'g']", jsonElement) { nodeList ->
                    nodeList shouldHaveSize jsonElement.size
                }
                query("\$[?1 <= 2]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize jsonElement.size
                }
                query("\$[?1 > 2]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?13 == '13']", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?'a' <= 'b']", jsonElement) { nodeList ->
                    nodeList shouldHaveSize jsonElement.size
                }
                query("\$[?'a' > 'b']", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?\$.obj == \$.arr]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?\$.obj != \$.arr]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize jsonElement.size
                }
                query("\$[?\$.obj == \$.obj]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize jsonElement.size
                }
                query("\$[?\$.obj != \$.obj]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?\$.arr == \$.arr]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize jsonElement.size
                }
                query("\$[?\$.arr != \$.arr]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?\$.obj == 17]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?\$.obj != 17]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize jsonElement.size
                }
                query("\$[?\$.obj <= \$.arr]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?\$.obj < \$.arr]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?\$.obj <= \$.obj]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize jsonElement.size
                }
                query("\$[?\$.arr <= \$.arr]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize jsonElement.size
                }
                query("\$[?1 <= \$.arr]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?1 >= \$.arr]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?1 > \$.arr]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?1 < \$.arr]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
                query("\$[?true <= true]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize jsonElement.size
                }
                query("\$[?true > true]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
            }
            "Queries" - {
                val jsonElement = Json.decodeFromString<JsonElement>(
                    "{\n" +
                            "     \"a\": [3, 5, 1, 2, 4, 6,\n" +
                            "           {\"b\": \"j\"},\n" +
                            "           {\"b\": \"k\"},\n" +
                            "           {\"b\": {}},\n" +
                            "           {\"b\": \"kilo\"}\n" +
                            "          ],\n" +
                            "     \"o\": {\"p\": 1, \"q\": 2, \"r\": 3, \"s\": 5, \"t\": {\"u\": 6}},\n" +
                            "     \"e\": \"f\"\n" +
                            "   }"
                ).jsonObject

                query("\$.a[?@.b == 'kilo']", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 1
                    jsonElement.jsonObject["a"].shouldNotBeNull()
                        .jsonArray.get(9).shouldNotBeNull().shouldBeIn(nodeList)
                }
                query("\$.a[?(@.b == 'kilo')]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 1
                    jsonElement.jsonObject["a"].shouldNotBeNull()
                        .jsonArray.get(9).shouldNotBeNull().shouldBeIn(nodeList)
                }
                query("\$.a[?@>3.5]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 3
                    jsonElement.jsonObject["a"].shouldNotBeNull().let { a ->
                        a.jsonArray[1].shouldNotBeNull().shouldBeIn(nodeList)
                        a.jsonArray[4].shouldNotBeNull().shouldBeIn(nodeList)
                        a.jsonArray[5].shouldNotBeNull().shouldBeIn(nodeList)
                    }
                }
                query("\$.a[?@.b]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 4
                    jsonElement.jsonObject["a"].shouldNotBeNull().let { a ->
                        a.jsonArray[6].shouldNotBeNull().shouldBeIn(nodeList)
                        a.jsonArray[7].shouldNotBeNull().shouldBeIn(nodeList)
                        a.jsonArray[8].shouldNotBeNull().shouldBeIn(nodeList)
                        a.jsonArray[9].shouldNotBeNull().shouldBeIn(nodeList)
                    }
                }
                query("\$[?@.*]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 2
                    jsonElement.jsonObject["a"].shouldNotBeNull().shouldBeIn(nodeList)
                    jsonElement.jsonObject["o"].shouldNotBeNull().shouldBeIn(nodeList)
                }
                query("\$[?@[?@.b]]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 1
                    jsonElement.jsonObject["a"].shouldNotBeNull().shouldBeIn(nodeList)
                }
                query("\$.o[?@<3, ?@<3]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 4
                    jsonElement.jsonObject["o"].shouldNotBeNull().jsonObject.let { o ->
                        o["p"].shouldNotBeNull().shouldBeIn(nodeList).let { p ->
                            nodeList.count {
                                it == p
                            }.shouldBe(2)
                        }
                        o["q"].shouldNotBeNull().shouldBeIn(nodeList).let { q ->
                            nodeList.count {
                                it == q
                            }.shouldBe(2)
                        }
                    }
                }
                query("\$.a[?@<2 || @.b == \"k\"]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 2
                    jsonElement.jsonObject["a"].shouldNotBeNull().let { a ->
                        a.jsonArray.get(2).shouldNotBeNull().shouldBeIn(nodeList)
                        a.jsonArray.get(7).shouldNotBeNull().shouldBeIn(nodeList)
                    }
                }
                query("\$.a[?match(@.b, \"[jk]\")]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 2
                    jsonElement.jsonObject["a"].shouldNotBeNull().let { a ->
                        a.jsonArray.get(6).shouldNotBeNull().shouldBeIn(nodeList)
                        a.jsonArray.get(7).shouldNotBeNull().shouldBeIn(nodeList)
                    }
                }
                query("\$.a[?search(@.b, \"[jk]\")]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 3
                    jsonElement.jsonObject["a"].shouldNotBeNull().let { a ->
                        a.jsonArray.get(6).shouldNotBeNull().shouldBeIn(nodeList)
                        a.jsonArray.get(7).shouldNotBeNull().shouldBeIn(nodeList)
                        a.jsonArray.get(9).shouldNotBeNull().shouldBeIn(nodeList)
                    }
                }
                query("\$.o[?@>1 && @<4]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 2
                    jsonElement.jsonObject["o"].shouldNotBeNull().jsonObject.let { o ->
                        o["q"].shouldNotBeNull().shouldBeIn(nodeList)
                        o["r"].shouldNotBeNull().shouldBeIn(nodeList)
                    }
                }
                query("\$.o[?@.u || @.x]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 1
                    jsonElement.jsonObject["o"].shouldNotBeNull().jsonObject
                        .get("t").shouldNotBeNull().shouldBeIn(nodeList)
                }
                query("\$.a[?@.b == $.x]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 6
                    jsonElement.jsonObject["a"].shouldNotBeNull().jsonArray.let { a ->
                        nodeList[0].shouldBe(a[0])
                        nodeList[1].shouldBe(a[1])
                        nodeList[2].shouldBe(a[2])
                        nodeList[3].shouldBe(a[3])
                        nodeList[4].shouldBe(a[4])
                        nodeList[5].shouldBe(a[5])
                    }
                }
                query("\$.a[?@ || @]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 10
                    jsonElement.jsonObject["a"].shouldNotBeNull().jsonArray.let { a ->
                        for (index in 0..9) {
                            nodeList[index].shouldBe(a[index])
                        }
                    }
                }
            }
        }

        "2.4. Function Extensions"(
            TestConfig.aroundEachTest { action ->
                // prepare a dummy repository to be modified by the tests
                JsonPath.defaultFunctionExtensionRepository = JsonPathFunctionExtensionMapRepository(
                    defaultFunctionExtensionRepositoryBackup.toMutableMap()
                )
                try {
                    action()
                } finally {
                    JsonPath.defaultCompiler = defaultCompilerBuilderBackup
                    JsonPath.defaultFunctionExtensionRepository = JsonPathFunctionExtensionMapRepository(
                        defaultFunctionExtensionRepositoryBackup.toMutableMap()
                    )
                }
            }
        ) - {
            compiles("\$[?length(@) < 3]")
            failsTypeCheck("\$[?length(@.*) < 3]")
            compiles("\$[?count(@.*) == 1]")
            failsTypeCheck("\$[?count(1) == 1]")
            "\$[?count(foo(@.*)) == 1]".let { path ->
                path {
                    JsonPath.defaultFunctionExtensionRepository.addExtension("foo") {
                        JsonPathFunctionExtension.NodesTypeFunctionExtension(
                            JsonPathFilterExpressionType.NodesType,
                        ) {
                            listOf()
                        }
                    }

                    shouldNotThrowAny {
                        JsonPath(path)
                    }
                }
            }
            failsTypeCheck("\$[?match(@.timezone, 'Europe/.*') == true]")
            compiles("\$[?value(@..color) == 'red']")
            failsTypeCheck("\$[?value(@..color)]")
            "\$[?bar(@.a)]".let { path ->
                path - {
                    "logical type argument" {
                        JsonPath.defaultFunctionExtensionRepository.addExtension("bar") {
                            JsonPathFunctionExtension.LogicalTypeFunctionExtension(
                                JsonPathFilterExpressionType.LogicalType,
                            ) {
                                true
                            }
                        }

                        shouldNotThrowAny {
                            JsonPath(path)
                        }
                    }
                    "value type argument" {
                        JsonPath.defaultFunctionExtensionRepository.addExtension("bar") {
                            JsonPathFunctionExtension.LogicalTypeFunctionExtension(
                                JsonPathFilterExpressionType.ValueType,
                            ) {
                                true
                            }
                        }

                        shouldNotThrowAny {
                            JsonPath(path)
                        }
                    }
                    "nodes type argument" {
                        JsonPath.defaultFunctionExtensionRepository.addExtension("bar") {
                            JsonPathFunctionExtension.LogicalTypeFunctionExtension(
                                JsonPathFilterExpressionType.NodesType,
                            ) {
                                true
                            }
                        }

                        shouldNotThrowAny {
                            JsonPath(path)
                        }
                    }
                }
            }
            "\$[?bnl(@.*)]".let { path ->
                path - {
                    "logical type argument" {
                        JsonPath.defaultFunctionExtensionRepository.addExtension("bnl") {
                            JsonPathFunctionExtension.LogicalTypeFunctionExtension(
                                JsonPathFilterExpressionType.LogicalType,
                            ) {
                                true
                            }
                        }

                        shouldNotThrowAny {
                            JsonPath(path)
                        }
                    }
                    "value type argument" {
                        JsonPath.defaultFunctionExtensionRepository.addExtension("bnl") {
                            JsonPathFunctionExtension.LogicalTypeFunctionExtension(
                                JsonPathFilterExpressionType.ValueType,
                            ) {
                                true
                            }
                        }

                        shouldThrow<JsonPathTypeCheckerException> {
                            JsonPath(path)
                        }
                    }
                    "nodes type argument" {
                        JsonPath.defaultFunctionExtensionRepository.addExtension("bnl") {
                            JsonPathFunctionExtension.LogicalTypeFunctionExtension(
                                JsonPathFilterExpressionType.NodesType
                            ) {
                                true
                            }
                        }

                        shouldNotThrowAny {
                            JsonPath(path)
                        }
                    }
                }
            }
            "\$[?blt(1==1)]".let { path ->
                path {
                    JsonPath.defaultFunctionExtensionRepository.addExtension("blt") {
                        JsonPathFunctionExtension.LogicalTypeFunctionExtension(
                            JsonPathFilterExpressionType.LogicalType,
                        ) {
                            true
                        }
                    }

                    shouldNotThrowAny {
                        JsonPath(path)
                    }
                }
            }
            "\$[?blt(1)]".let { path ->
                path {
                    JsonPath.defaultFunctionExtensionRepository.addExtension("blt") {
                        JsonPathFunctionExtension.LogicalTypeFunctionExtension(
                            JsonPathFilterExpressionType.LogicalType,
                        ) {
                            true
                        }
                    }

                    shouldThrow<JsonPathTypeCheckerException> {
                        JsonPath(path)
                    }
                }
            }
            "\$[?bal(1)]".let { path ->
                path {
                    JsonPath.defaultFunctionExtensionRepository.addExtension("bal") {
                        JsonPathFunctionExtension.LogicalTypeFunctionExtension(
                            JsonPathFilterExpressionType.ValueType,
                        ) {
                            true
                        }
                    }

                    shouldNotThrowAny {
                        JsonPath(path)
                    }
                }
            }
        }

        "2.5.1.  Child Segment" - {
            val jsonElement = Json.decodeFromString<JsonElement>(
                "[\"a\", \"b\", \"c\", \"d\", \"e\", \"f\", \"g\"]"
            ).jsonArray
            query("\$[0, 3]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 2
                nodeList[0].shouldBe(jsonElement[0])
                nodeList[1].shouldBe(jsonElement[3])
            }
            query("\$[0:2,5]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 3
                nodeList[0].shouldBe(jsonElement[0])
                nodeList[1].shouldBe(jsonElement[1])
                nodeList[2].shouldBe(jsonElement[5])
            }
            query("\$[0,0]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 2
                nodeList[0].shouldBe(jsonElement[0])
                nodeList[1].shouldBe(jsonElement[0])
            }
        }

        "2.5.2.  Descendant Segment" - {
            val jsonElement = Json.decodeFromString<JsonElement>(
                "{\n" +
                        "     \"o\": {\"j\": 1, \"k\": 2},\n" +
                        "     \"a\": [5, 3, [{\"j\": 4}, {\"k\": 6}]]\n" +
                        "   }"
            ).jsonObject
            query("\$..j", jsonElement) { nodeList ->
                nodeList shouldHaveSize 2
                jsonElement["o"].shouldNotBeNull().jsonObject["j"].shouldBeIn(nodeList)
                jsonElement["a"].shouldNotBeNull()
                    .jsonArray[2].shouldNotBeNull()
                    .jsonArray[0].shouldNotBeNull()
                    .jsonObject["j"].shouldNotBeNull().shouldBeIn(nodeList)
            }
            query("\$..[0]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 2
                jsonElement["a"].shouldNotBeNull().jsonArray.let { a ->
                    a.jsonArray[0].shouldNotBeNull().shouldBe(nodeList[0])
                    a.jsonArray[2].shouldNotBeNull().jsonArray[0].shouldBe(nodeList[1])
                }
            }
            query("\$..[*]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 11
                jsonElement["a"].shouldNotBeNull().shouldBeIn(nodeList).jsonArray.let { a ->
                    a.forEach { item ->
                        item.shouldBeIn(nodeList)
                    }
                    a[2].shouldNotBeNull().jsonArray.let { a2 ->
                        a2.forEach { a2children ->
                            a2children.shouldBeIn(nodeList)
                            a2children.jsonObject.forEach {
                                it.value.shouldBeIn(nodeList)
                            }
                        }
                    }
                }
                jsonElement["o"].shouldNotBeNull().shouldBeIn(nodeList).jsonObject.let { o ->
                    o.forEach { item ->
                        item.value.shouldBeIn(nodeList)
                    }
                }
            }
            query("\$..*", jsonElement) { nodeList -> // same as the one before this
                nodeList shouldHaveSize 11
                jsonElement["a"].shouldNotBeNull().shouldBeIn(nodeList).jsonArray.let { a ->
                    a.forEach { item ->
                        item.shouldBeIn(nodeList)
                    }
                    a[2].shouldNotBeNull().jsonArray.let { a2 ->
                        a2.forEach { a2children ->
                            a2children.shouldBeIn(nodeList)
                            a2children.jsonObject.forEach {
                                it.value.shouldBeIn(nodeList)
                            }
                        }
                    }
                }
                jsonElement["o"].shouldNotBeNull().shouldBeIn(nodeList).jsonObject.let { o ->
                    o.forEach { item ->
                        item.value.shouldBeIn(nodeList)
                    }
                }
            }
            query("\$..o", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement["o"].shouldNotBeNull().shouldBeIn(nodeList)
            }
            query("\$.o..[*, *]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 4
                jsonElement["o"].shouldNotBeNull().jsonObject.let { o ->
                    o.forEach { oDescendant ->
                        oDescendant.value.shouldBeIn(nodeList)
                        nodeList.count {
                            it == oDescendant.value
                        } shouldBe 2
                    }
                }
            }
            query("\$.a..[0, 1]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 4
                jsonElement["a"].shouldNotBeNull().jsonArray.let { a ->
                    listOf(a, a[2]).forEach { descendant ->
                        descendant.jsonArray[0].shouldBeIn(nodeList)
                        descendant.jsonArray[1].shouldBeIn(nodeList)
                    }
                }
            }
        }

        "2.6.  Semantics of null" - {
            val jsonElement = Json.decodeFromString<JsonElement>(
                "   {\"a\": null, \"b\": [null], \"c\": [{}], \"null\": 1}"
            ).jsonObject
            query("\$.a", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement["a"].shouldNotBeNull().shouldBeIn(nodeList)
            }
            query("\$.a[0]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 0
            }
            query("\$.a.d", jsonElement) { nodeList ->
                nodeList shouldHaveSize 0
            }
            query("\$.b[0]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement["b"].shouldNotBeNull().jsonArray[0].shouldBeIn(nodeList)
            }
            query("\$.b[*]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement["b"].shouldNotBeNull().jsonArray.mapIndexed { index, value ->
                    nodeList[index].shouldBe(value)
                }
            }
            query("\$.b[?@]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement["b"].shouldNotBeNull().jsonArray.mapIndexed { index, value ->
                    nodeList[index].shouldBe(value)
                }
            }
            query("\$.b[?@==null]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement["b"].shouldNotBeNull().jsonArray.mapIndexed { index, value ->
                    nodeList[index].shouldBe(value)
                }
            }
            query("\$.c[?@.d==null]", jsonElement) { nodeList ->
                nodeList shouldHaveSize 0
            }
            query("\$.null", jsonElement) { nodeList ->
                nodeList shouldHaveSize 1
                jsonElement["null"].shouldNotBeNull().shouldBeIn(nodeList)
            }
        }
    }

    "Examples from issues" - {
        "https://github.com/a-sit-plus/jsonpath4k/issues/20" - {
            val jsonElement = Json.decodeFromString<JsonElement>(
                "{\n" +
                        " \"id\": \"ic2:te\",\n" +
                        " \"meta\": 1,\n" +
                        " \"pos\": {\n" +
                        "  \"x\": 23,\n" +
                        "  \"y\": 64,\n" +
                        "  \"z\": 179\n" +
                        " },\n" +
                        " \"dimension\": 0,\n" +
                        " \"tag\": {\n" +
                        "  \"components\": {\n" +
                        "   \"energy\": {\n" +
                        "    \"storage\": 0.0\n" +
                        "   }\n" +
                        "  },\n" +
                        "  \"fuel\": 0,\n" +
                        "  \"InvSlots\": {\n" +
                        "   \"charge\": {\n" +
                        "    \"Contents\": []\n" +
                        "   },\n" +
                        "   \"fuel\": {\n" +
                        "    \"Contents\": []\n" +
                        "   }\n" +
                        "  },\n" +
                        "  \"x\": 23,\n" +
                        "  \"y\": 64,\n" +
                        "  \"facing\": 2,\n" +
                        "  \"active\": 0,\n" +
                        "  \"z\": 179,\n" +
                        "  \"id\": \"ic2:generator\",\n" +
                        "  \"totalFuel\": 0\n" +
                        " }\n" +
                        "}"
            )

            "working" - {
                query("$[?$.tag.id == 'ic2:geo_generator' || $.tag.id == 'ic2:generator']", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 5
                }

                query("\$[?(\$.tag.id == 'ic2:geo_generator' || \$.tag.id == 'ic2:generator')]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 5
                }

                query("\$.tag[?(\$.tag.id == 'ic2:geo_generator' || \$.tag.id == 'ic2:generator')]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 10
                }
            }

            "not working" - {
                query("\$.tag.id[?(\$.tag.id == 'ic2:geo_generator' || \$.tag.id == 'ic2:generator')]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }

                query("\$[?(@.tag.id == 'ic2:geo_generator' || @.tag.id == 'ic2:generator')]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }

                query("\$.tag[?(@.id == 'ic2:geo_generator' || @.id == 'ic2:generator')]", jsonElement) { nodeList ->
                    nodeList shouldHaveSize 0
                }
            }
        }

        "RMLTC0002g-JSON" - {
            failsParsing("$.students[*]]")
        }
    }
}
