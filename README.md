# json-logic-kotlin
[ ![Download](https://api.bintray.com/packages/advantagefse/json-logic-kotlin/eu.afse.jsonlogic/images/download.svg) ](https://bintray.com/advantagefse/json-logic-kotlin/eu.afse.jsonlogic/_latestVersion)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.3.21-blue.svg)](https://kotlinlang.org) 
[![Build Status](https://travis-ci.com/advantagefse/json-logic-kotlin.svg?branch=master)](https://travis-ci.com/advantagefse/json-logic-kotlin)
[![Codecov](https://codecov.io/github/advantagefse/json-logic-kotlin/coverage.svg?branch=master)](https://codecov.io/gh/advantagefse/json-logic-kotlin)

This is a pure Kotlin implementation of JsonLogic http://jsonlogic.com rule engine. JsonLogic is documented extensively at [JsonLogic.com](http://jsonlogic.com), including examples of every [supported operation](http://jsonlogic.com/operations.html).

## Installation

Gradle

```groovy
implementation 'eu.afse:eu.afse.jsonlogic:0.9.7'
```

Maven

```xml
<dependency>
  <groupId>eu.afse</groupId>
  <artifactId>eu.afse.jsonlogic</artifactId>
  <version>0.9.7</version>
  <type>pom</type>
</dependency>
```

## Examples

Typically jsonLogic will be called with a rule object and optionally a data object. Both parameters can either be JSON formatted strings or Any Kotlin objects.

### Simple

This is a simple test, equivalent to 1 == 1

```kotlin
JsonLogic().apply("""{"==":[1,1]}""")
//true
```

### Compound

An example with nested rules as strings
```kotlin
val jsonLogic = JsonLogic()
jsonLogic.apply("""
    { "and" : [
        { ">" : [3,1] },
        { "<" : [1,3] }
    ] }
""")
//true
```

An example with nested rules as objects
```kotlin
val jsonLogic = JsonLogic()
val logic = mapOf(
    "and" to listOf(
        mapOf(">" to listOf(3, 1)),
        mapOf("<" to listOf(1, 3))
    )
)
jsonLogic.apply(logic)
//true
```

### Data-Driven

You can use the var operator to get attributes of the data object

```kotlin
val jsonLogic = JsonLogic()
val rule = mapOf("var" to listOf("a"))
val data = mapOf("a" to 1, "b" to 2)
jsonLogic.apply(rule, data)
//1
```

or with json string parameters

```kotlin
val jsonLogic = JsonLogic()
jsonLogic.apply(
    """{ "var" : ["a"] }""", // Rule
    "{ a : 1, b : 2 }" // Data
)
//1
```

You can also use the var operator to access an array by numeric index

```kotlin
JsonLogic().apply(
    """{"var" : 1 }""", // Rule
    """[ "apple", "banana", "carrot" ]""" // Data
)
//banana
```

### Customization

[Adding custom operations](http://jsonlogic.com/add_operation.html) is also supported.

```kotlin
val jsonLogic = JsonLogic()
jsonLogic.addOperation("sqrt") { l, _ ->
    try {
        if (l != null && l.size > 0) Math.sqrt(l[0].toString().toDouble())
        else null
    } catch (e: Exception) {
        null
    }
}
jsonLogic.apply("""{"sqrt":"9"}""")
//3
```

An example passing object parameters

```kotlin
val jsonLogic = JsonLogic()
jsonLogic.addOperation("pow") { l, _ ->
    try {
        if (l != null && l.size > 1) Math.pow(l[0].toString().toDouble(), l[1].toString().toDouble())
        else null
    } catch (e: Exception) {
        null
    }
}
jsonLogic.apply(mapOf("pow" to listOf(3,2)))
//9
```
## Compatibility

This implementation is as close as it gets to the [JS implementation](https://github.com/jwadhams/json-logic-js/) and passes all the official [Unit Tests](http://jsonlogic.com/tests.json).
