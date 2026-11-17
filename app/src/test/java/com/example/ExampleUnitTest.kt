package com.example

import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.math.MathEvalResult
import com.example.math.MathEvaluator
import com.example.model.EducationCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun mathEvaluator_evaluatesArithmeticAndPrecedence() {
        val result = MathEvaluator.evaluate("(2 + 3) * 4 - 6 / 2")
        assertNotNull(result)
        assertEquals(17.0, result!!, 1e-6)
    }

    @Test
    fun mathEvaluator_evaluatesSqrtPowersAndImplicitMultiplication() {
        val result = MathEvaluator.evaluate("2√9 + 3^2")
        assertNotNull(result)
        assertEquals(15.0, result!!, 1e-6)
    }

    @Test
    fun mathEvaluator_verifiesBalancedEquationWithAssignments() {
        val vars = MathEvaluator.parseAssignments("x = 2")
        val check = MathEvaluator.verifyExpressionOrEquation("2*x^2 - 5*x + 2 = 0", vars)
        assertTrue(check is MathEvalResult.EquationCheck)
        val eq = check as MathEvalResult.EquationCheck
        assertTrue(eq.isBalanced)
        assertEquals(0.0, eq.lhsValue, 1e-6)
        assertEquals(0.0, eq.rhsValue, 1e-6)
    }

    @Test
    fun mathEvaluator_detectsUnbalancedEquation() {
        val vars = MathEvaluator.parseAssignments("x = 3")
        val check = MathEvaluator.verifyExpressionOrEquation("2*x + 1 = 10", vars)
        assertTrue(check is MathEvalResult.EquationCheck)
        val eq = check as MathEvalResult.EquationCheck
        assertFalse(eq.isBalanced)
    }

    @Test
    fun bilingualStrings_coverEnglishAndUrdu() {
        val en = AppStrings.forLanguage(AppLanguage.ENGLISH)
        val ur = AppStrings.forLanguage(AppLanguage.URDU)
        assertEquals("ZELU Math AI", en.brandName)
        assertEquals("ZELU Math AI", ur.brandName)
        assertTrue(ur.tagline.isNotBlank())
        assertTrue(en.faqs.isNotEmpty() && ur.faqs.isNotEmpty())
    }

    @Test
    fun educationCatalog_includesPakistanBoardsAndDefaultPlans() {
        val pkBoards = EducationCatalog.boardsByCountry["Pakistan"].orEmpty()
        assertTrue(pkBoards.contains("Punjab Board"))
        assertTrue(pkBoards.contains("Federal Board"))
        assertTrue(pkBoards.contains("Cambridge O Level"))
        assertEquals(3, EducationCatalog.defaultPlans.size)
    }
}
