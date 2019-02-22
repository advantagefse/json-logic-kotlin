# json-logic-kotlin
[ ![Download](https://api.bintray.com/packages/advantagefse/json-logic-kotlin/eu.afse.jsonlogic/images/download.svg?version=0.9) ](https://bintray.com/advantagefse/json-logic-kotlin/eu.afse.jsonlogic/0.9/link)

This is a pure Kotlin implementation of JsonLogic http://jsonlogic.com rule engine. JsonLogic is documented extensively at [JsonLogic.com](http://jsonlogic.com), including examples of every [supported operation](http://jsonlogic.com/operations.html).

## Installation

Gradle

```groovy
implementation 'eu.afse:eu.afse.jsonlogic:0.9'
```

Maven

```xml
<dependency>
  <groupId>eu.afse</groupId>
  <artifactId>eu.afse.jsonlogic</artifactId>
  <version>0.9</version>
  <type>pom</type>
</dependency>
```

## Examples

Typically jsonLogic will be called with a rule object and optionally a data object. Both rules and data input are JSON formatted strings.

### Simple

This is a simple test, equivalent to 1 == 1

```kotlin
JsonLogic().apply("{\"==\":[1,1]}")
//true
```

### Compound

An example with nested rules
```kotlin
val jsonLogic = JsonLogic()
jsonLogic.apply(
    "{\"and\" : [" +
    "    { \">\" : [3,1] }," +
    "    { \"<\" : [1,3] }" +
    "] }"
)
//true
```

### Data-Driven

You can use the var operator to get attributes of the data object

```kotlin
val jsonLogic = JsonLogic()
val result = jsonLogic.apply(
    "{ \"var\" : [\"a\"] }", // Rule
    "{ a : 1, b : 2 }" // Data
)
//1
```

You can also use the var operator to access an array by numeric index

```kotlin
JsonLogic().apply(
    "{\"var\" : 1 }", // Rule
    "[ \"apple\", \"banana\", \"carrot\" ]" // Data
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
jsonLogic.apply("{\"sqrt\":\"9\"}")
//3
```

## Compatibility

This implementation is as close as it gets to the [JS implementation](https://github.com/jwadhams/json-logic-js/) and passes all the official [Unit Tests](http://jsonlogic.com/tests.json).
