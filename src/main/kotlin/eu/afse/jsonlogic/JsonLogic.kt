package eu.afse.jsonlogic

/**
 * Kotlin native implementation of http://jsonlogic.com/
 */
class JsonLogic {

    /**
     * Apply logic on data and get a result for all supported operations http://jsonlogic.com/operations.html
     *
     * @param logic the logic as a json encoded string
     * @param data the data as a json encoded string
     * @param safe if true an exception is returned as false else exceptions are thrown
     * @return evaluation result
     */
    @JvmOverloads
    fun apply(logic: String?, data: String? = null, safe: Boolean = true) =
        evaluateSafe(logic.parse as? Map<*, *> ?: logic, data.parse, safe).toString()

    /**
     * Apply logic on data and get a result
     *
     * @param logic the logic
     * @param data the data
     * @param safe if true an exception is returned as false else exceptions are thrown
     * @return evaluation result
     */
    @JvmOverloads
    fun apply(logic: Any?, data: Any? = null, safe: Boolean = true) = evaluateSafe(logic, data, safe).toString()

    /**
     * Add new operations http://jsonlogic.com/add_operation.html
     *
     * @param operator the operator to be added
     * @param lambda the operation tha handles the operator
     */
    fun addOperation(operator: String, lambda: (List<Any?>?, Any?) -> Any?) = customOperations.put(operator, lambda)

    private fun evaluateSafe(logic: Any?, data: Any? = null, safe: Boolean = true) = if (safe) {
        try {
            evaluate(logic, data)
        } catch (e: kotlin.NotImplementedError) {
            false
        } catch (e: java.lang.Exception) {
            false
        }
    } else evaluate(logic, data)

    private fun evaluate(logic: Any?, data: Any? = null): Any? {
        val result = if (logic !is Map<*, *>) {
            logic
        } else if (logic.isNullOrEmpty()) {
            data
        } else {
            val operator = logic.keys.firstOrNull()
            val values = logic[operator]
            if (customOperations.keys.contains(operator))
                customOperations[operator]?.invoke(values.asList, data)
            else if (specialArrayOperations.keys.contains(operator))
                specialArrayOperations[operator]?.invoke(values.asList, data)
            else (operations[operator] ?: TODO("operator \"$operator\"")).invoke(when (values) {
                is List<*> -> values.map { evaluate(it, data) }
                is Map<*, *> -> evaluate(values, data)
                else -> evaluate(listOf(values), data)
            }.asList, data)
        }
        return result
    }

    private val customOperations = mutableMapOf<String, (List<Any?>?, Any?) -> Any?>()

    private val operations = mapOf<String, (List<Any?>?, Any?) -> Any?>(
        "var" to { l, d ->
            getVar(d, l)
        },
        "missing" to { l, d ->
            missing(d, l).toString().noSpaces.also {
                println("'missing' of $l is $it")
            }
        },
        "missing_some" to { l, d ->
            missingSome(d, l).toString().noSpaces.also {
                println("'missing_some' of $l is $it")
            }
        },
        "==" to { l, _ ->
            with(l?.comparableList) {
                val firstArg = this?.getOrNull(0)
                val secondArg = this?.getOrNull(1)
                val res = compare(firstArg, secondArg) == 0
                println("$firstArg == $secondArg is $res")
                res
            }
        },
        "===" to { l, _ ->
            with(l?.comparableList) {
                val firstArg = this?.getOrNull(0)
                val secondArg = this?.getOrNull(1)
                val res = compareStrict(firstArg, secondArg) == 0
                println("$firstArg === $secondArg is $res")
                res
            }
        },
        "!=" to { l, _ ->
            with(l?.comparableList) {
                val firstArg = this?.getOrNull(0)
                val secondArg = this?.getOrNull(1)
                val res = compare(firstArg, secondArg) != 0
                println("$firstArg != $secondArg is $res")
                res
            }
        },
        "!==" to { l, _ ->
            with(l?.comparableList) {
                compareStrict(this?.getOrNull(0), this?.getOrNull(1)) != 0
            }
        },
        ">" to { l, _ ->
            l.compareListOfThree { a, b ->
                val res = a > b
                println("$a > $b is $res")
                res
            }
        },
        ">=" to { l, _ ->
            l.compareListOfThree { a, b ->
                val res = a >= b
                println("$a >= $b is $res")
                res
            }
        },
        "<" to { l, _ ->
            l.compareListOfThree { a, b ->
                val res = a < b
                println("$a < $b is $res")
                res
            }
        },
        "<=" to { l, _ ->
            l.compareListOfThree { a, b ->
                val res = a <= b
                println("$a <= $b is $res")
                res
            }
        },
        "!" to { l, _ ->
            !l?.getOrNull(0).truthy.also {
                println("!$l is $it")
            }
        },
        "!!" to { l, _ ->
            l?.getOrNull(0).truthy.also {
                println("!$l is $it")
            }
        },
        "%" to { l, _ ->
            with(l?.doubleList ?: listOf()) {
                val res = if (size > 1) this[0] % this[1] else null
                println("${this[0]} % ${this[1]} is $res")
                res
            }
        },
        "and" to { l, _ ->
            val and = if (l?.all { it is Boolean } == true) l.all { it.truthy }
            else (l?.firstOrNull { !it.truthy } ?: l?.last())?.asString
            println("and: $l, Result: $and")
            and
        },
        "or" to { l, _ ->
            val or = if (l?.all { it is Boolean } == true) l.firstOrNull { it.truthy } != null
            else (l?.firstOrNull { it.truthy } ?: l?.last())?.asString
            println("or: $l, Result: $or")
            or
        },
        "?:" to { l, _ ->
            l?.recursiveIf.also {
                println("?:$l is $it")
            }
        },
        "if" to { l, _ ->
            l?.recursiveIf.also {
                println("if $l is $it")
            }
        },
        "log" to { l, _ ->
            l?.getOrNull(0).also {
                println("log $l is $it")
            }
        },
        "in" to { l, _ ->
            val first = l?.getOrNull(0).toString().unStringify
            val second = l?.getOrNull(1)
            val res = when (second) {
                is String -> second.contains(first)
                is List<*> -> second.contains(first)
                else -> false
            }
            println("$first in $second is $res")
            res
        },
        "cat" to { l, _ ->
            l?.map { if (it is Number && it.toDouble() == it.toInt().toDouble()) it.toInt() else it }
                ?.joinToString("")?.asString.also {
                    println("'cat' of $l is $it")
                }
        },
        "+" to { l, _ ->
            l?.doubleList?.sum().also {
                println("'+' of $l is $it")
            }
        },
        "*" to { l, _ ->
            l?.doubleList?.reduce { sum, cur -> sum * cur }.also {
                println("'*' of $l is $it")
            }
        },
        "-" to { l, _ ->
            with(l?.doubleList ?: listOf()) {
                val res = when (size) {
                    0 -> null
                    1 -> -this[0]
                    else -> this[0] - this[1]
                }
                println("'-' of $l is $res")
                res
            }
        },
        "/" to { l, _ ->
            with(l?.doubleList ?: listOf()) {
                val res = this[0] / this[1]
                println("${this[0]} / ${this[1]} is $res")
                res
            }
        },
        "min" to { l, _ ->
            l?.filter { it is Number }?.minBy { (it as Number).toDouble() }.also {
                println("min of $l is $it")
            }
        },
        "max" to { l, _ ->
            l?.filter { it is Number }?.maxBy { (it as Number).toDouble() }.also {
                println("max of $l is $it")
            }
        },
        "merge" to { l, _ ->
            l?.flat.toString().noSpaces
                .also {
                    println("merge of $l is $it")
                }
        },
        "substr" to { l, _ ->
            val str = l?.getOrNull(0).toString()
            val a = l?.getOrNull(1).toString().intValue
            val b = if (l?.size ?: 0 > 2) l?.getOrNull(2).toString().intValue else 0
            val res = when (l?.size) {
                2 -> if (a > 0) str.substring(a).asString else str.substring(str.length + a).asString
                3 -> when {
                    a >= 0 && b > 0 -> str.substring(a, a + b).asString
                    a >= 0 && b < 0 -> str.substring(a, str.length + b).asString
                    a < 0 && b < 0 -> str.substring(str.length + a, str.length + b).asString
                    a < 0 -> str.substring(str.length + a).asString
                    else -> null
                }
                else -> null
            }
            println("substr of $l is $res")
            res
        }
    )

    private val specialArrayOperations = mapOf<String, (List<Any?>?, Any?) -> Any?>(
        "map" to { l, d ->
            if (d == null) "[]"
            else {
                val data = evaluate(l?.getOrNull(0), d).toString().parse as? List<*>
                (data?.map { evaluate(l?.getOrNull(1), it) } ?: "[]").toString().noSpaces
            }
        },
        "filter" to { l, d ->
            if (d == null) "[]"
            else {
                val data = evaluate(l?.getOrNull(0), d).toString().parse as? List<*>
                (data?.filter { evaluate(l?.getOrNull(1), it).truthy }
                    ?: "[]").toString().noSpaces
            }
        },
        "all" to { l, d ->
            if (d == null) false
            else {
                val data = evaluate(l?.getOrNull(0), d).toString().parse as? List<*>
                (data?.all { evaluate(l?.getOrNull(1), it).truthy }
                    ?: false).toString().noSpaces
            }
        },
        "none" to { l, d ->
            if (d == null) true
            else {
                val data = evaluate(l?.getOrNull(0), d).toString().parse as? List<*>
                (data?.none { evaluate(l?.getOrNull(1), it).truthy }
                    ?: true).toString().noSpaces
            }
        },
        "some" to { l, d ->
            if (d == null) "[]"
            else {
                val data = evaluate(l?.getOrNull(0), d).toString().parse as? List<*>
                (data?.any { evaluate(l?.getOrNull(1), it).truthy }
                    ?: false).toString().noSpaces
            }
        },
        "reduce" to { l, d ->
            if (d == null) 0.0
            else {
                val data = evaluate(l?.getOrNull(0), d).toString().parse as? List<Any?>
                val logic = l?.getOrNull(1)
                val initial: Double = if (l != null && l.size > 2) l.getOrNull(2).toString().doubleValue else 0.0
                data?.fold(initial) { sum, cur ->
                    evaluate(logic, mapOf("current" to cur, "accumulator" to sum)).toString().doubleValue
                }
            }
        }
    )

    private fun getVar(data: Any?, values: Any?): String? {
        var value: Any? = data
        val varName = if (values is List<*>) values.getOrNull(0).toString() else values.toString()
        when (value) {
            is List<*> -> {
                val indexParts = varName.unStringify.split(".")
                value = if (indexParts.size == 1) value[indexParts[0].intValue] else getRecursive(indexParts, value)
            }
            is Map<*, *> -> varName.unStringify.split(".").forEach {
                value = (value as? Map<*, *>)?.get(it)
            }
        }
        if ((value == data || value == null) && values is List<*> && values.size > 1) {
            return values.getOrNull(1)?.asString.toString()
        }
        return value?.asString.toString()
    }

    private val List<Any?>.recursiveIf: Any?
        get() = when (size) {
            0 -> null
            1 -> getOrNull(0)?.asString
            2 -> if (getOrNull(0).truthy) getOrNull(1)?.asString else null
            3 -> if (getOrNull(0).truthy) getOrNull(1)?.asString else getOrNull(2)?.asString
            else -> if (getOrNull(0).truthy) getOrNull(1)?.asString else subList(2, size).recursiveIf
        }

    private fun missing(data: Any?, vars: List<Any?>?) = arrayListOf<Any?>().apply {
        vars?.forEach { if (getVar(data, it) == "null") add(it?.asString) }
    }

    private fun missingSome(data: Any?, vars: List<Any?>?): List<Any?> {
        val min = vars?.getOrNull(0)?.toString()?.intValue ?: 0
        val keys = vars?.getOrNull(1) as? List<Any?> ?: listOf()
        val missing = missing(data, keys)
        return if (keys.size - missing.size >= min) listOf() else missing
    }

    private fun compare(a: Comparable<*>?, b: Comparable<*>?) = when {
        a is Number && b is Number -> compareValues(a.toDouble(), b.toDouble())
        a is String && b is Number -> compareValues(a.doubleValue, b.toDouble())
        a is Number && b is String -> compareValues(a.toDouble(), b.doubleValue)
        a is String && b is String -> compareValues(a.toString().unStringify, b.toString().unStringify)
        a is Boolean || b is Boolean -> compareValues(a.truthy, b.truthy)
        else -> compareValues(a, b)
    }

    private fun compareStrict(a: Comparable<*>?, b: Comparable<*>?) = when {
        a is Number && b is Number -> compareValues(a.toDouble(), b.toDouble())
        a is String && b is String -> compareValues(a.unStringify, b.unStringify)
        else -> -1
    }

    private fun List<Any?>?.compareListOfThree(operator: (Int, Int) -> Boolean) = with(this?.comparableList) {
        when {
            this?.size == 2 -> operator(compare(this.getOrNull(0), this.getOrNull(1)), 0)
            this?.size == 3 -> operator(compare(this.getOrNull(0), this.getOrNull(1)), 0)
                && operator(compare(this.getOrNull(1), this.getOrNull(2)), 0)
            else -> false
        }
    }
}
