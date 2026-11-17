package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import com.example.firebase.AdminSecurityHelper
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.math.MathEvaluator
import com.example.ui.components.BoardItem
import com.example.ui.components.BoardTool
import com.example.ui.components.MathWhiteboardCanvas
import com.example.ui.theme.CyanBright
import com.example.ui.theme.ZeluMathTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read ZELU Math AI app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ZELU Math AI", appName)
    }

    @Test
    fun `verify graph plotting math formula under Android context`() {
        val y = MathEvaluator.evaluate("x^2 - 3", mapOf("x" to 2.0))
        assertNotNull(y)
        assertEquals(1.0, y!!, 1e-6)
    }

    @Test
    fun `verify admin password hashing reset token and 2FA TOTP under Android context`() {
        val salt = AdminSecurityHelper.generateSalt()
        val hash = AdminSecurityHelper.hashPassword("ZeluAdmin#444", salt)
        assertTrue(AdminSecurityHelper.verifyPassword("ZeluAdmin#444", salt, hash))
        assertFalse(AdminSecurityHelper.verifyPassword("WrongPass", salt, hash))

        val resetToken = AdminSecurityHelper.generateResetToken()
        val tokenHash = AdminSecurityHelper.hashToken(resetToken)
        assertTrue(
            AdminSecurityHelper.verifyResetToken(
                token = resetToken,
                expectedHash = tokenHash,
                expiresAtMillis = System.currentTimeMillis() + 60_000L
            )
        )
        assertFalse(
            AdminSecurityHelper.verifyResetToken(
                token = resetToken,
                expectedHash = tokenHash,
                expiresAtMillis = System.currentTimeMillis() - 10_000L
            )
        )

        val secret = AdminSecurityHelper.generateBase32Secret()
        val now = 1_700_000_000_000L
        val code = AdminSecurityHelper.generateTotpCode(secret, now / 30_000L)
        assertTrue(AdminSecurityHelper.verifyTotpCode(secret, code, now))
        assertFalse(AdminSecurityHelper.verifyTotpCode(secret, "000000", now))
    }

    @Test
    fun `verify whiteboard finger drawing color switching undo and clear strokes`() {
        lateinit var capturedItems: MutableList<BoardItem>
        composeTestRule.setContent {
            ZeluMathTheme {
                val strings = remember { AppStrings.forLanguage(AppLanguage.ENGLISH) }
                val items = remember { mutableStateListOf<BoardItem>() }
                capturedItems = items
                var tool by remember { mutableStateOf(BoardTool.PEN) }
                var color by remember { mutableStateOf(CyanBright) }
                var showGrid by remember { mutableStateOf(true) }

                MathWhiteboardCanvas(
                    strings = strings,
                    items = items,
                    activeTool = tool,
                    onToolChange = { tool = it },
                    activeColor = color,
                    onColorChange = { color = it },
                    showGrid = showGrid,
                    onToggleGrid = { showGrid = !showGrid },
                    plottedGraphs = emptyList(),
                    onClearAll = { items.clear() },
                    onCanvasSizeChanged = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Draw first finger stroke with default CyanBright color
        composeTestRule.onNodeWithTag("board_canvas").performTouchInput {
            swipe(start = Offset(40f, 80f), end = Offset(200f, 140f))
        }
        composeTestRule.waitForIdle()
        assertEquals(1, capturedItems.size)
        assertEquals(CyanBright, (capturedItems[0] as BoardItem.Freehand).color)

        // 2. Switch color to White and draw second stroke
        composeTestRule.onNodeWithTag("board_color_white").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("board_canvas").performTouchInput {
            swipe(start = Offset(60f, 180f), end = Offset(220f, 220f))
        }
        composeTestRule.waitForIdle()
        assertEquals(2, capturedItems.size)
        assertEquals(Color.White, (capturedItems[1] as BoardItem.Freehand).color)

        // 3. Undo removes latest stroke, Clear removes remaining strokes
        composeTestRule.onNodeWithTag("board_undo_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, capturedItems.size)

        composeTestRule.onNodeWithTag("board_clear_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(capturedItems.isEmpty())
    }
}
