<div align="center">

# ![JsonPath4K](jsonpath4k.png)

[![A-SIT Plus Official](https://raw.githubusercontent.com/a-sit-plus/a-sit-plus.github.io/709e802b3e00cb57916cbb254ca5e1a5756ad2a8/A-SIT%20Plus_%20official_opt.svg)](https://plus.a-sit.at/open-source.html)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-brightgreen.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-orange.svg?logo=kotlin)](http://kotlinlang.org)
[![Kotlin](https://img.shields.io/badge/kotlin-2.4.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Java](https://img.shields.io/badge/java-17+-blue.svg?logo=OPENJDK)](https://www.oracle.com/java/technologies/downloads/#java17)
[![Maven Central](https://img.shields.io/maven-central/v/at.asitplus/jsonpath4k)](https://mvnrepository.com/artifact/at.asitplus/jsonpath4k/)


</div>


This is a Kotlin Multiplatform Library for using Json Paths as specified in [RFC9535](https://datatracker.ietf.org/doc/rfc9535).

## Architecture

This library was built for [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html), supporting all KMP targets, except WASI.

Notable features for multiplatform are:

- Use of [Napier](https://github.com/AAkira/Napier) for default compiler error logging on JVM/Android, iOS, macOS, tvOS, JS, and WasmJS.
  - By default, no `Antilog` is registered.
  - On watchOS, Linux, Windows (mingw), and Android Native, the default listener does not log, due to Napier not supporting those platforms; provide a custom error listener if logging is required on those platforms.
- Use of [kotlinx-serialization](https://github.com/Kotlin/kotlinx.serialization) for serialization from/to JSON and to have JsonElement as evaluation target for JsonPathQuery
- Use of [TestBalloon](https://infix-de.github.io/testBalloon/latest/) with [TestBalloon Addons](https://github.com/a-sit-plus/testballoon-addons) for unit tests.

## Using the Library
1. Add JsonPath4K as a dependency in your project (`at.asitplus:jsonpath4k:$version`)
2. Use the `JsonPath` constructor for compiling JSONPath query expressions.
3. Invoke the method `JsonPath.query` to select nodes satisfying the JsonPath query expression from a `JsonElement`.
4. A `nodeList` containing both the selected values and their normalized paths is returned.

In general, things are called what they are. Most prominently, _JsonPath4K_ is not used anywhere in code – even though this
project is called JsonPath4K, it provides _JSONPath_ functionality for Kotlin Multiplatform, 
not _JsonPath4K_ functionality because there is no such thing.

```kotlin
val jsonElement = buildJsonArray { add(0) }

val jsonPathQueryExpression = "$[0]"
val jsonPath = JsonPath(jsonPathQueryExpression)

val nodeList = jsonPath.query(jsonElement)
val jsonValue = nodeList[0].value.jsonPrimitive
val normalizedPath = nodeList[0].normalizedJsonPath
```

## Function Extensions
This library supports the function extensions specified in [RFC9535](https://www.rfc-editor.org/rfc/rfc9535.html#name-function-extensions) by default. 

### Custom Function Extensions
Custom function extensions can be added using `JsonPath.defaultFunctionExtensionRepository.addExtension`:
```kotlin
// adding a logical type function extension with 1 parameter of type NodesType
JsonPath.defaultFunctionExtensionRepository.addExtension("foo") {
    JsonPathFunctionExtension.LogicalTypeFunctionExtension(
        JsonPathFilterExpressionType.ValueType,
        JsonPathFilterExpressionType.LogicalType,
        JsonPathFilterExpressionType.NodesType,
    ) {
        val value0 = it[0] as JsonPathFilterExpressionValue.ValueTypeValue
        val value1 = it[1] as JsonPathFilterExpressionValue.LogicalTypeValue
        val value2 = it[2] as JsonPathFilterExpressionValue.NodesTypeValue
        true
    }
}

// adding a value type function extension returning a JsonValue with 2 parameters of type ValueType
JsonPath.defaultFunctionExtensionRepository.addExtension("foo") {
    JsonPathFunctionExtension.ValueTypeFunctionExtension(
        JsonPathFilterExpressionType.ValueType,
        JsonPathFilterExpressionType.ValueType,
    ) {
        JsonPrimitive("")
    }
}

// adding a logical type function extension with 2 parameters of type ValueType returning false
JsonPath.defaultFunctionExtensionRepository.addExtension("foo") {
    JsonPathFunctionExtension.LogicalTypeFunctionExtension(
        JsonPathFilterExpressionType.ValueType,
        JsonPathFilterExpressionType.ValueType,
    ) {
        false
    }
}

// adding a value type function extension returning the special value `Nothing` with 2 parameters of type LogicalType
JsonPath.defaultFunctionExtensionRepository.addExtension("foo") {
    JsonPathFunctionExtension.ValueTypeFunctionExtension(
        JsonPathFilterExpressionType.LogicalType,
        JsonPathFilterExpressionType.LogicalType,
    ) {
        null
    }
}

// adding a nodes type function extension with 2 parameters of type ValueType
JsonPath.defaultFunctionExtensionRepository.addExtension("foo") {
    JsonPathFunctionExtension.NodesTypeFunctionExtension(
        JsonPathFilterExpressionType.ValueType,
        JsonPathFilterExpressionType.ValueType,
    ) {
        listOf()
    }
}

// reimplementing the count function as defined in [RFC9535](https://www.rfc-editor.org/rfc/rfc9535.html#name-function-extensions)
JsonPath.defaultFunctionExtensionRepository.addExtension("count") {
    JsonPathFunctionExtension.ValueTypeFunctionExtension(
        JsonPathFilterExpressionType.NodesType,
    ) {
        val nodesTypeValue = it[0] as JsonPathFilterExpressionValue.NodesTypeValue
        JsonPrimitive(nodesTypeValue.nodeList.size.toUInt())
    }
}
```

### Removing Function Extensions
Function extensions can be removed from the default repository by setting the value of `JsonPath.defaultFunctionExtensionRepository` to a new repository.

Existing functions can be preserved by exporting them using `JsonPath.defaultFunctionExtensionRepository.export()` and selectively importing them into the new repository.



### Testing Custom Function Extensions
In order to test custom function extensions without polluting the default function extension repository, it is recommended to make an export and use the resulting map to build a new function extension retriever.

```kotlin
val testRetriever = JsonPath.defaultFunctionExtensionRepository.export().plus(
    "foo" to JsonPathFunctionExtension.LogicalTypeFunctionExtension(
        JsonPathFilterExpressionType.ValueType,
        JsonPathFilterExpressionType.ValueType,
    ) {
        true
    }
)
val jsonPath = JsonPath(jsonPathStatement, functionExtensionRetriever = testRetriever::get)

// select from a json element
jsonPath.query(buildJsonElement {})
```

## Error Handling
Invalid JSONPath expressions fail during `JsonPath` construction with a `JsonPathCompilerException`. The compiler's error listener only receives diagnostic details; it does not suppress or replace that exception.

On JVM/Android, iOS, macOS, tvOS, JS, and WasmJS, the default listener sends diagnostics to Napier (provided the application has registered an `Antilog`). On watchOS, Linux, Windows (mingw), and Android Native, the default listener is a no-op. Compilation failures still throw on these platforms, but logging requires a custom implementation of `AntlrJsonPathCompilerErrorListener`.

Pass a compiler with that listener to one `JsonPath`, or assign it as the global default:
```kotlin
val compiler = AntlrJsonPathCompiler(errorListener = myErrorListener)
val jsonPath = JsonPath(jsonPathExpression, compiler = compiler)

// Optional: use it for subsequent JsonPath instances that do not specify a compiler.
JsonPath.defaultCompiler = compiler
```

Using a single, shared default works because operations only use an ANTLR cache as global state and this cache is guarded to enable concurrent access and mutation.

## Contributing
External contributions are greatly appreciated!
Just be sure to observe the contribution guidelines (see [CONTRIBUTING.md](CONTRIBUTING.md)).


<br>

---
<p align="center">
The Apache License does not apply to the logos, (including the A-SIT logo) and the project/module name(s), as these are the sole property of
A-SIT/A-SIT Plus GmbH and may not be used in derivative works without explicit permission!
</p>
