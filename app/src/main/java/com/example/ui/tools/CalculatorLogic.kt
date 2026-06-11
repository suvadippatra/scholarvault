package com.scholarvault.ui.tools

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// --- ADVANCED MATH EXPRESSION PARSER ENGINE ---
class SimpleMathParser(private val str: String, private val angleUnit: AngleUnit) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        pos++
        ch = if (pos < str.length) str[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(): Double {
        nextChar()
        val x = parseExpression()
        if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
        return x
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            if (eat('+'.code)) x += parseTerm()
            else if (eat('-'.code)) x -= parseTerm()
            else break
        }
        return x
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.code)) x *= parseFactor()
            else if (eat('/'.code)) {
                val next = parseFactor()
                if (next == 0.0) throw ArithmeticException("Division by zero")
                x /= next
            } else if (eat('m'.code)) {
                if (eat('o'.code) && eat('d'.code)) {
                    val next = parseFactor()
                    if (next == 0.0) throw ArithmeticException("Modulo by zero")
                    x %= next
                } else {
                    throw RuntimeException("Unknown identifier starting with m")
                }
            } else break
        }
        return x
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor()
        if (eat('-'.code)) return -parseFactor()

        var x = parsePrimary()

        if (eat('^'.code)) {
            x = Math.pow(x, parseFactor())
        }
        return x
    }

    private fun evaluateFunction(func: String, arg: Double): Double {
        return when (func) {
            "sin" -> when (angleUnit) {
                AngleUnit.DEG -> Math.sin(Math.toRadians(arg))
                AngleUnit.RAD -> Math.sin(arg)
                AngleUnit.GRA -> Math.sin(arg * Math.PI / 200.0)
            }
            "cos" -> when (angleUnit) {
                AngleUnit.DEG -> Math.cos(Math.toRadians(arg))
                AngleUnit.RAD -> Math.cos(arg)
                AngleUnit.GRA -> Math.cos(arg * Math.PI / 200.0)
            }
            "tan" -> when (angleUnit) {
                AngleUnit.DEG -> Math.tan(Math.toRadians(arg))
                AngleUnit.RAD -> Math.tan(arg)
                AngleUnit.GRA -> Math.tan(arg * Math.PI / 200.0)
            }
            "asin" -> {
                val res = Math.asin(arg)
                when (angleUnit) {
                    AngleUnit.DEG -> Math.toDegrees(res)
                    AngleUnit.RAD -> res
                    AngleUnit.GRA -> res * 200.0 / Math.PI
                }
            }
            "acos" -> {
                val res = Math.acos(arg)
                when (angleUnit) {
                    AngleUnit.DEG -> Math.toDegrees(res)
                    AngleUnit.RAD -> res
                    AngleUnit.GRA -> res * 200.0 / Math.PI
                }
            }
            "atan" -> {
                val res = Math.atan(arg)
                when (angleUnit) {
                    AngleUnit.DEG -> Math.toDegrees(res)
                    AngleUnit.RAD -> res
                    AngleUnit.GRA -> res * 200.0 / Math.PI
                }
            }
            "log" -> Math.log10(arg)
            "ln" -> Math.log(arg)
            "sqrt" -> Math.sqrt(arg)
            "cbrt" -> Math.cbrt(arg)
            "abs" -> Math.abs(arg)
            else -> throw RuntimeException("Unknown function: $func")
        }
    }

    private fun parsePrimary(): Double {
        var x: Double
        val startPos = this.pos
        if (eat('('.code)) {
            x = parseExpression()
            eat(')'.code)
        } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
            while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                nextChar()
            }
            val numStr = str.substring(startPos, this.pos)
            x = numStr.toDoubleOrNull() ?: 0.0
        } else if (ch >= 'a'.code && ch <= 'z'.code) {
            while (ch >= 'a'.code && ch <= 'z'.code) {
                nextChar()
            }
            val func = str.substring(startPos, this.pos)
            if (eat('('.code)) {
                val arg = parseExpression()
                eat(')'.code)
                x = evaluateFunction(func, arg)
            } else {
                if (func in listOf("sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt", "cbrt", "abs")) {
                    val arg = parseFactor()
                    x = evaluateFunction(func, arg)
                } else {
                    x = when (func) {
                        "pi" -> Math.PI
                        "e" -> Math.E
                        else -> throw RuntimeException("Unknown identifier: $func")
                    }
                }
            }
        } else {
            throw RuntimeException("Unexpected token: " + ch.toChar())
        }

        while (true) {
            if (eat('%'.code)) {
                x *= 0.01
            } else if (eat('!'.code)) {
                x = factorial(x)
            } else {
                break
            }
        }

        return x
    }

    private fun factorial(n: Double): Double {
        if (n < 0.0) return Double.NaN
        val check = n.toLong()
        if (check.toDouble() != n) return Double.NaN
        if (check > 100) return Double.POSITIVE_INFINITY
        var res = 1.0
        for (i in 1..check) {
            res *= i
        }
        return res
    }
}

// --- UTILITY METHODS ---
fun preprocessExpr(expr: String): String {
    var s = expr
        .replace("×", "*")
        .replace("÷", "/")
        .replace("√", "sqrt")
        .replace("∛", "cbrt")
    s = s.replace(",", "")

    val sb = java.lang.StringBuilder()
    for (i in 0 until s.length) {
        val curr = s[i]
        sb.append(curr)
        if (i < s.length - 1) {
            val next = s[i + 1]
            val isLeftSymbol = curr.isDigit() || curr == ')' || curr == '%' || curr == '!' || curr == 'π' || curr == 'e'
            val isNextStartOfFunc = next == '(' || next == 'π' || next == 'e' || next == 's' || next == 'c' || next == 't' || next == 'l' || next == 'a' || next == '√' || next == '∛'
            
            if (isLeftSymbol && isNextStartOfFunc) {
                sb.append('*')
            }
        }
    }
    return sb.toString().replace("π", "pi")
}

fun evaluateExpression(expr: String, angleUnit: AngleUnit): Double? {
    if (expr.isBlank() || expr == "0") return null
    try {
        var cleanExpr = expr
        cleanExpr = preprocessExpr(cleanExpr)

        while (cleanExpr.isNotEmpty() && (cleanExpr.last() in listOf('+', '-', '*', '/', '(', '.', '^'))) {
            cleanExpr = cleanExpr.dropLast(1)
        }

        val openCount = cleanExpr.count { it == '(' }
        val closeCount = cleanExpr.count { it == ')' }
        if (openCount > closeCount) {
            cleanExpr += ")".repeat(openCount - closeCount)
        }

        if (cleanExpr.isBlank()) return null
        val parser = SimpleMathParser(cleanExpr, angleUnit)
        val res = parser.parse()
        if (res.isNaN() || res.isInfinite()) return null
        return res
    } catch (e: Exception) {
        return null
    }
}

fun formatResult(value: Double): String {
    if (value == value.toLong().toDouble()) {
        return value.toLong().toString()
    }
    val symbols = DecimalFormatSymbols(Locale.US)
    val df = DecimalFormat("#.##########", symbols)
    return df.format(value)
}

fun handleParentheses(display: String): String {
    val openCount = display.count { it == '(' }
    val closeCount = display.count { it == ')' }
    val lastChar = display.lastOrNull()

    return if (display == "0") {
        "("
    } else if (lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == '%' || lastChar == 'π' || lastChar == 'e') && openCount > closeCount) {
        display + ")"
    } else if (lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == '%' || lastChar == 'π' || lastChar == 'e')) {
        display + "×("
    } else {
        display + "("
    }
}

fun handleAppend(display: String, input: String): String {
    if (display == "0") {
        if (input in listOf("+", "×", "÷", "%", "^", "!", "∛", "mod")) {
            return "0$input"
        }
        if (input == ".") {
            return "0."
        }
        return input
    }
    
    val lastChar = display.lastOrNull()
    
    if (display.endsWith("mod") && input in listOf("+", "×", "÷", "-", "mod")) {
        return display.dropLast(3) + input
    }
    
    if (lastChar != null && lastChar in listOf('+', '×', '÷', '-') && input in listOf("+", "×", "÷", "mod")) {
        return display.dropLast(1) + input
    }
    
    return display + input
}
