package eu.afse.jsonlogic

import com.google.gson.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.net.URL

/**
 * Runs official tests from jsonlogic.com
 */

private const val testUrl = "http://jsonlogic.com/tests.json"

class JsonLogicTests {

    private data class JsonLogicTest(val title: String, val rule: String, val data: String, val expected: String)

    private fun officialTests() = mutableListOf<JsonLogicTest>().apply {
        val gson = GsonBuilder().create()
        var category = ""
        gson.fromJson(URL(testUrl).readText(), List::class.java).forEach {
            when (it) {
                is String -> category = it
                is List<*> -> {
                    val rule = gson.toJson(it[0])
                    val data = gson.toJson(it[1])
                    val expected = gson.toJson(it[2])
                    add(JsonLogicTest("${it[0]}, ${it[1]} $category", rule, data, expected))
                }
            }
        }
    }

    @TestFactory
    fun translateDynamicTests(): Collection<DynamicTest> = officialTests().map {
        DynamicTest.dynamicTest(it.title) { assertEquals(it.expected, JsonLogic().apply(it.rule, it.data)) }
    }

    @Test
    fun simple() {
        val jsonLogic = JsonLogic()
        val result = jsonLogic.apply("{\"==\":[1,1]}")
        assertEquals("true", result)
    }

    @Test
    fun compound() {
        val jsonLogic = JsonLogic()
        val result = jsonLogic.apply(
                "{\"and\" : [" +
                "    { \">\" : [3,1] }," +
                "    { \"<\" : [1,3] }" +
                "] }")
        assertEquals("true", result)
    }

    @Test
    fun data() {
        val jsonLogic = JsonLogic()
        val result = jsonLogic.apply(
            "{ \"var\" : [\"a\"] }", // Rule
            "{ a : 1, b : 2 }" // Data
        )
        assertEquals("1.0", result)
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
}