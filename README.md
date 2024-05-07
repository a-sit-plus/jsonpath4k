# JsonPath

[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-brightgreen.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-orange.svg?logo=kotlin)](http://kotlinlang.org)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.23-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Java](https://img.shields.io/badge/java-17+-blue.svg?logo=OPENJDK)](https://www.oracle.com/java/technologies/downloads/#java11)
[![Maven Central](https://img.shields.io/maven-central/v/at.asitplus/jsonpath)](https://mvnrepository.com/artifact/at.asitplus/jsonpath/)

This is a Kotlin Multiplatform Library for using Json Paths as defined in [RFC9535](https://datatracker.ietf.org/doc/rfc9535).

## Architecture

This library was built with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) and [Multiplatform Mobile](https://kotlinlang.org/lp/mobile/) in mind. Its primary targets are JVM, Android and iOS. 

Notable features for multiplatform are:

- Use of [Napier](https://github.com/AAkira/Napier) as the logging framework for the default compiler instance
- Use of [Kotest](https://kotest.io/) for unit tests
- Use of [kotlinx-serialization](https://github.com/Kotlin/kotlinx.serialization) for serialization from/to JSON.

## Using the library
1. Use the JsonPath constructor for compiling JSONPath query expressions.
2. Invoke the method JsonPath.query to select nodes satisfying the JsonPath query expression from a JsonElement.
3. A nodeList containing both the selected values and their normalized paths is returned.

```kotlin
val jsonElement = buildJsonArray { add(0) }

val jsonPathQueryExpression = "$[0]"
val jsonPath = JsonPath(jsonPathQueryExpression)

val nodeList = jsonPath.query(jsonElement)
val jsonValue = nodeList[0].value.jsonPrimitive
val normalizedPath = nodeList[0].normalizedJsonPath
```

## Function extensions
This library supports the function extensions specified in [RFC9535](https://www.rfc-editor.org/rfc/rfc9535.html#name-function-extensions) by default. 

### Custom function extensions
Custom function extensions can be added using `JsonPathDependencyManager.functionExtensionRepository.addExtension`:
```kotlin
// adding a logical type function extension with 1 parameter of type NodesType
JsonPathDependencyManager.functionExtensionRepository.addExtension("foo") {
    JsonPathFunctionExtension.LogicalTypeFunctionExtension(
        JsonPathFilterExpressionType.NodesType
    ) {
        true
    }
}

// adding a value type function extension returning a JsonValue with 2 parameters of type ValueType
JsonPathDependencyManager.functionExtensionRepository.addExtension("foo") {
    JsonPathFunctionExtension.ValueTypeFunctionExtension(
        JsonPathFilterExpressionType.ValueType,
        JsonPathFilterExpressionType.ValueType,
    ) {
        JsonPrimitive("")
    }
}

// adding a value type function extension returning the JsonValue with 2 parameters of type ValueType
JsonPathDependencyManager.functionExtensionRepository.addExtension("foo") {
    JsonPathFunctionExtension.ValueTypeFunctionExtension(
        JsonPathFilterExpressionType.ValueType,
        JsonPathFilterExpressionType.ValueType,
    ) {
        JsonNull
    }
}

// adding a value type function extension returning the special value `Nothing` with 2 parameters of type LogicalType
JsonPathDependencyManager.functionExtensionRepository.addExtension("foo") {
    JsonPathFunctionExtension.ValueTypeFunctionExtension(
        JsonPathFilterExpressionType.LogicalType,
        JsonPathFilterExpressionType.LogicalType,
    ) {
        null
    }
}

// adding a nodes type function extension with 2 parameters of type ValueType
JsonPathDependencyManager.functionExtensionRepository.addExtension("foo") {
    JsonPathFunctionExtension.NodesTypeFunctionExtension(
        JsonPathFilterExpressionType.ValueType,
        JsonPathFilterExpressionType.ValueType,
    ) {
        listOf()
    }
}

// adding a nodes type function extension with 2 parameters of type ValueType
JsonPathDependencyManager.functionExtensionRepository.addExtension("foo") {
    JsonPathFunctionExtension.NodesTypeFunctionExtension(
        JsonPathFilterExpressionType.ValueType,
        JsonPathFilterExpressionType.ValueType,
    ) {
        listOf()
    }
}
```

### Removing Function extensions
Function extensions can be removed by setting the value of `JsonPathDependencyManager.functionExtensionRepository` to a new repository.

Existing functions can be preserved by exporting them using `JsonPathDependencyManager.functionExtensionRepository.export()` and selectively importing them into the new repository.

### Testing custom function extensions
In order to test custom function extensions without polluting the default function extension repository, it is advised to copy the function extension repository before adding functions to be tested, and reset it to its original state afterwards.

```kotlin
// make backup of extension repository 
val repositoryBackup = JsonPathDependencyManager.functionExtensionRepository.copy()

// add extension
JsonPathDependencyManager.functionExtensionRepository.addExtension("foo") {
    JsonPathFunctionExtension.LogicalTypeFunctionExtension {
        true
    }
}

// compile json path
val jsonPath = try {
    JsonPath(jsonPathStatement)
} finally {
    // reset extension repository to its original state
    JsonPathDependencyManager.functionExtensionRepository = repositoryBackup
}

// evaluate against a json element
jsonPath.evaluate(buildJsonElement {})
```

## Error handeling
The default compiler uses Napier for reporting errors. 
It is possible to implement a custom error listener by extending `AntlrJsonPathCompilerErrorListener` and setting a new default compiler:
```kotlin
JsonPathDependencyManager.compiler = AntlrJsonPathCompiler(
    errorListener = object : AntlrJsonPathCompilerErrorListener {
        //TODO: IMPLEMENT MEMBERS                                                            
    },
    functionExtensionRetriever = {
        JsonPathDependencyManager.functionExtensionRepository.getExtension(it)
    }
)
```