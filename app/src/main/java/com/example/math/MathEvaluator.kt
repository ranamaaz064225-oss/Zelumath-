package com.example.math

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

sealed interface MathEvalResult {
    data class Numeric(val value: Double) : MathEvalResult
    data class EquationCheck(
        val lhsValue: Double,
        val rhsValue: Double,
        val isBalanced: Boolean,
        val difference: Double
    ) : MathEvalResult
    data class Invalid(val reason: String) : MathEvalResult
}

object MathEvaluator {

    private const val EPSILON = 1e-6

    fun evaluate(expression: String, variables: Map<String, Double> = emptyMap()): Double? {
        val cleaned = normalize(expression)
        if (cleaned.isBlank()) return null
        return try {
            val parser = Parser(cleaned, variables)
            val result = parser.parseExpression()
            parser.expectEnd()
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks an expression or equation. If it contains '=', tests whether LHS == RHS
     * at the supplied variables (or solves/checks simple numeric equality).
     */
    fun verifyExpressionOrEquation(
        input: String,
        variables: Map<String, Double> = emptyMap()
    ): MathEvalResult {
        val normalized = normalize(input)
        if (normalized.isBlank()) return MathEvalResult.Invalid("Empty expression")

        val parts = normalized.split("=")
        return when (parts.size) {
            1 -> {
                val valResult = evaluate(parts[0], variables)
                if (valResult != null) MathEvalResult.Numeric(valResult)
                else MathEvalResult.Invalid("Unable to evaluate expression deterministically")
            }
            2 -> {
                val lhs = evaluate(parts[0], variables)
                val rhs = evaluate(parts[1], variables)
                if (lhs != null && rhs != null) {
                    val diff = abs(lhs - rhs)
                    MathEvalResult.EquationCheck(
                        lhsValue = lhs,
                        rhsValue = rhs,
                        isBalanced = diff < EPSILON,
                        difference = diff
                    )
                } else {
                    MathEvalResult.Invalid("Symbolic equation requires variable assignment to verify numerically")
                }
            }
            else -> MathEvalResult.Invalid("Multiple '=' signs are not supported")
        }
    }

    /**
     * Extracts candidate variable assignment such as "x = 3" or "x = -2.5, y = 4"
     * and returns a map of variable -> value.
     */
    fun parseAssignments(answerText: String): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        val regex = Regex("""([a-zA-Z])\s*=\s*(-?\d+(?:\.\d+)?)""")
        for (match in regex.findAll(answerText)) {
            val name = match.groupValues[1].lowercase()
            val value = match.groupValues[2].toDoubleOrNull()
            if (value != null) {
                result[name] = value
            }
        }
        return result
    }

    fun formatNumber(value: Double): String {
        val rounded = kotlin.math.round(value * 1_000_000.0) / 1_000_000.0
        return if (abs(rounded - rounded.toLong().toDouble()) < 1e-9) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    private fun normalize(raw: String): String {
        return raw
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("–", "-")
            .replace("π", "pi")
            .replace("√(", "sqrt(")
            .replace(Regex("""√(\d+(?:\.\d+)?)"""), "sqrt($1)")
            .replace(Regex("""(\d)([a-zA-Z(])"""), "$1*$2")
            .replace(Regex("""(\))(\d|[a-zA-Z(])"""), "$1*$2")
            .trim()
    }

    private class Parser(
        private val str: String,
        private val variables: Map<String, Double>
    ) {
        private var pos = -1
        private var ch = 0

        init {
            nextChar()
        }

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun expectEnd() {
            while (ch == ' '.code) nextChar()
            if (ch != -1) {
                throw IllegalArgumentException("Unexpected character: ${ch.toChar()}")
            }
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val denom = parseFactor()
                        if (abs(denom) < 1e-12) throw ArithmeticException("Division by zero")
                        x /= denom
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing closing parenthesis")
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                val name = str.substring(startPos, pos).lowercase()
                if (eat('('.code)) {
                    val arg = parseExpression()
                    if (!eat(')'.code)) throw IllegalArgumentException("Missing ')' after $name")
                    x = when (name) {
                        "sqrt" -> {
                            if (arg < 0) throw ArithmeticException("Square root of negative")
                            sqrt(arg)
                        }
                        "sin" -> sin(arg)
                        "cos" -> cos(arg)
                        "tan" -> tan(arg)
                        "abs" -> abs(arg)
                        "ln" -> ln(arg)
                        "log" -> log10(arg)
                        "exp" -> exp(arg)
                        else -> throw IllegalArgumentException("Unknown function: $name")
                    }
                } else {
                    x = when (name) {
                        "pi" -> Math.PI
                        "e" -> Math.E
                        else -> variables[name]
                            ?: throw IllegalArgumentException("Unknown variable: $name")
                    }
                }
            } else {
                throw IllegalArgumentException("Unexpected character: ${ch.toChar()}")
            }

            if (eat('^'.code)) {
                x = x.pow(parseFactor())
            }
            return x
        }
    }
}
