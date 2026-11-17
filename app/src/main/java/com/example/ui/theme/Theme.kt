package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.firebase.AdminSecurityHelper

private val ZeluDarkColorScheme = darkColorScheme(
    primary = ElectricBlueBright,
    onPrimary = CrispWhite,
    primaryContainer = ElectricBlueContainerDark,
    onPrimaryContainer = ElectricBlueContainerLight,
    secondary = CyanBright,
    onSecondary = DeepNavy950,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanContainerLight,
    tertiary = RoyalPurpleBright,
    onTertiary = CrispWhite,
    tertiaryContainer = RoyalPurpleContainerDark,
    onTertiaryContainer = RoyalPurpleContainerLight,
    background = DeepNavy900,
    onBackground = TextPrimaryDark,
    surface = DeepNavy800,
    onSurface = TextPrimaryDark,
    surfaceVariant = DeepNavy700,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateBorderDark,
    error = ErrorCrimson,
    onError = CrispWhite,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainerLight
)

private val ZeluLightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = CrispWhite,
    primaryContainer = ElectricBlueContainerLight,
    onPrimaryContainer = DeepNavy900,
    secondary = CyanAccent,
    onSecondary = CrispWhite,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = DeepNavy900,
    tertiary = RoyalPurple,
    onTertiary = CrispWhite,
    tertiaryContainer = RoyalPurpleContainerLight,
    onTertiaryContainer = DeepNavy900,
    background = IceSurface,
    onBackground = TextPrimaryLight,
    surface = CrispWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = IceSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = SlateBorderLight,
    error = ErrorCrimson,
    onError = CrispWhite,
    errorContainer = ErrorContainerLight,
    onErrorContainer = DeepNavy900
)

val ZeluShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ZeluMathTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isUrdu: Boolean = false,
    accentHex: String = "#2563EB",
    content: @Composable () -> Unit
) {
    val customAccent = remember(accentHex) { AdminSecurityHelper.parseHexColorOrNull(accentHex) }
    val colorScheme = remember(darkTheme, customAccent) {
        val base = if (darkTheme) ZeluDarkColorScheme else ZeluLightColorScheme
        if (customAccent != null) {
            base.copy(
                primary = customAccent,
                primaryContainer = customAccent.copy(alpha = if (darkTheme) 0.25f else 0.14f)
            )
        } else {
            base
        }
    }
    val layoutDirection = if (isUrdu) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = zeluTypography(isUrdu = isUrdu),
            shapes = ZeluShapes,
            content = content
        )
    }
}
