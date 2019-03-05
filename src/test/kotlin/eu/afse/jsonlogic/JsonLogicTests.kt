package eu.afse.jsonlogic

import com.google.gson.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.net.URL

/**
 * Runs official tests from jsonlogic.com
 */

private const val testUrl = "http://jsonlogic.com/tests.json"

class JsonLogicTests {

    private val gson = GsonBuilder().create()

    private data class JsonLogicTest(val title: String, val rule: Any?, val data: Any?, val expected: Any?)

    private fun officialTests() = mutableListOf<JsonLogicTest>().apply {
        val gson = GsonBuilder().create()
        var category = ""
        gson.fromJson(URL(testUrl).readText(), List::class.java).forEach {
            when (it) {
                is String -> category = it
                is List<*> -> {
                    add(JsonLogicTest("${it[0]}, ${it[1]} $category", it[0], it[1], it[2]))
                }
            }
        }
    }

    @TestFactory
    fun dynamicStringTests(): Collection<DynamicTest> = officialTests().map {
        DynamicTest.dynamicTest(it.title) {
            assertEquals(
                gson.toJson(it.expected),
                JsonLogic().apply(gson.toJson(it.rule), gson.toJson(it.data))
            )
        }
    }

    @TestFactory
    fun dynamicTests(): Collection<DynamicTest> = officialTests().map {
        DynamicTest.dynamicTest(it.title) {
            assertEquals(
                gson.toJson(it.expected).unStringify.noSpaces,
                JsonLogic().apply(it.rule, it.data).unStringify.noSpaces
            )
        }
    }

    @Test
    fun simple() {
        val jsonLogic = JsonLogic()
        val result = jsonLogic.apply("{\"==\":[1,1]}")
        assertEquals("true", result)
    }

    @Test
    fun compoundString() {
        val jsonLogic = JsonLogic()
        val result = jsonLogic.apply(
            "{\"and\" : [" +
                    "    { \">\" : [3,1] }," +
                    "    { \"<\" : [1,3] }" +
                    "] }"
        )
        assertEquals("true", result)
    }

    @Test
    fun compound() {
        val jsonLogic = JsonLogic()
        val logic = mapOf(
            "and" to listOf(
                mapOf(">" to listOf(3, 1)),
                mapOf("<" to listOf(1, 3))
            )
        )
        val result = jsonLogic.apply(logic)
        assertEquals("true", result)
    }

    @Test
    fun dataString() {
        val jsonLogic = JsonLogic()
        val result = jsonLogic.apply(
            "{ \"var\" : [\"a\"] }", // Rule
            "{ a : 1, b : 2 }" // Data
        )
        assertEquals("1.0", result)
    }

    @Test
    fun data() {
        val jsonLogic = JsonLogic()
        val rule = mapOf("var" to listOf("a"))
        val data = mapOf("a" to 1, "b" to 2)
        val result = jsonLogic.apply(rule, data)
        assertEquals("1", result)
    }

    @Test
    fun array() {
        val jsonLogic = JsonLogic()
        val result = jsonLogic.apply(
            "{\"var\" : 1 }", // Rule
            "[ \"apple\", \"banana\", \"carrot\" ]" // Data
        )
        assertEquals("\"banana\"", result)
    }

    @Test
    fun customOperation1() {
        fun plus(list: List<Any?>?, @Suppress("UNUSED_PARAMETER") data: Any?): Any? {
            try {
                if (list != null && list.size > 1) return list[0].toString().toDouble() + list[1].toString().toDouble()
            } catch (e: Exception) {
            }
            return null
        }

        val jsonLogic = JsonLogic()
        jsonLogic.addOperation("plus", ::plus)
        val result = jsonLogic.apply("{\"plus\":[23, 19]}", null)
        assertEquals("42.0", result)
    }

    @Test
    fun customOperation2() {
        val jsonLogic = JsonLogic()
        jsonLogic.addOperation("sqrt") { l, _ ->
            try {
                if (l != null && l.size > 0) Math.sqrt(l[0].toString().toDouble())
                else null
            } catch (e: Exception) {
                null
            }
        }
        val result = jsonLogic.apply("{\"sqrt\":\"9\"}")
        assertEquals("3.0", result)
    }

    @Test
    fun customOperation3() {
        val jsonLogic = JsonLogic()
        jsonLogic.addOperation("Math.random") { _, _ -> Math.random() }
        val result = jsonLogic.apply("{\"Math.random\":[]}", "{}")
        assert(result.toDouble() in 0.0..1.0)
    }

    @Test
    fun customOperation4() {
        val jsonLogic = JsonLogic()
        jsonLogic.addOperation("pow") { l, _ ->
            try {
                if (l != null && l.size > 1) Math.pow(l[0].toString().toDouble(), l[1].toString().toDouble())
                else null
            } catch (e: Exception) {
                null
            }
        }
        val result = jsonLogic.apply(mapOf("pow" to listOf(3, 2)))
        assertEquals("9.0", result)
    }

    @Test
    fun unknownCustomOperation() {
        val jsonLogic = JsonLogic()
        assertThrows(kotlin.NotImplementedError::class.java) {
            jsonLogic.apply(mapOf("hello" to listOf(1, 2, 3)))
        }
    }

    @Test
    fun stringComparisonBug() {
        val jsonLogic = JsonLogic()
        val logic = mapOf("===" to listOf(mapOf("var" to listOf("a")), "two"))
        val data = mapOf("a" to "one")
        val result = jsonLogic.apply(logic, data)
        assertEquals("false", result)
    }

    @Test
    fun nullTest() {
        val jsonLogic = JsonLogic()
        val result = jsonLogic.apply(null)
        assertEquals("null", result)
    }

    @Test
    fun log() {
        val jsonLogic = JsonLogic()
        val data = mapOf("log" to "apple")
        val result = jsonLogic.apply(data)
        assertEquals("apple", result)
    }

    @Test
    fun truthyNull() {
        assertEquals(false, null.truthy)
    }

    @Test
    fun truthyArray() {
        assertEquals(false, emptyArray<Int>().truthy)
    }

    @Test
    fun truthyUnknown() {
        assertEquals(false, emptyArray<Int>().truthy)
    }

    @Test
    fun truthyOther() {
        assertEquals(true, 1.truthy)
    }
}