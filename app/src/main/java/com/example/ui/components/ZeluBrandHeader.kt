package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.firebase.AdminSecurityHelper
import com.example.localization.AppLanguage
import com.example.localization.ZeluStrings
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepNavy900
import com.example.ui.theme.ElectricBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberDecodedBase64Bitmap(base64: String): ImageBitmap? {
    val decoded by produceState<ImageBitmap?>(initialValue = null, key1 = base64) {
        value = if (base64.isBlank()) {
            null
        } else {
            withContext(Dispatchers.Default) {
                AdminSecurityHelper.decodeBase64ToImageBitmap(base64)
            }
        }
    }
    return decoded
}

@Composable
fun ZeluLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    logoBase64: String = "",
    faviconBase64: String = ""
) {
    val logoBitmap = rememberDecodedBase64Bitmap(logoBase64)
    val faviconBitmap = rememberDecodedBase64Bitmap(faviconBase64)

    Box(modifier = modifier.size(size)) {
        if (logoBitmap != null) {
            Image(
                bitmap = logoBitmap,
                contentDescription = "Brand Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepNavy900, ElectricBlue, CyanAccent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.65f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val path = Path().apply {
                        moveTo(w * 0.85f, h * 0.15f)
                        lineTo(w * 0.15f, h * 0.15f)
                        lineTo(w * 0.58f, h * 0.50f)
                        lineTo(w * 0.15f, h * 0.85f)
                        lineTo(w * 0.85f, h * 0.85f)
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawCircle(
                        color = Color(0xFF22D3EE),
                        radius = w * 0.09f,
                        center = Offset(w * 0.82f, h * 0.50f)
                    )
                }
            }
        }

        if (faviconBitmap != null) {
            Image(
                bitmap = faviconBitmap,
                contentDescription = "Site Favicon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun LanguageSwitcherPill(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            .padding(3.dp)
            .testTag("language_switcher"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLanguage.entries.forEach { lang ->
            val selected = lang == currentLanguage
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onLanguageChange(lang) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("lang_${lang.code}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (lang == AppLanguage.ENGLISH) "EN" else "اردو",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZeluTopBar(
    strings: ZeluStrings,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSignOut: (() -> Unit)? = null,
    announcementText: String = "",
    headerNotice: String = "",
    customTitle: String = "",
    customTagline: String = "",
    logoBase64: String = "",
    faviconBase64: String = "",
    onOpenRoutePrompt: (() -> Unit)? = null
) {
    val displayTitle = customTitle.ifBlank { strings.brandName }
    val displayTagline = customTagline.ifBlank { strings.tagline }
    val effectiveBanner = announcementText.ifBlank { headerNotice }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { onOpenRoutePrompt?.invoke() }
                        )
                ) {
                    ZeluLogoMark(
                        size = 38.dp,
                        logoBase64 = logoBase64,
                        faviconBase64 = faviconBase64
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = displayTagline,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LanguageSwitcherPill(
                        currentLanguage = currentLanguage,
                        onLanguageChange = onLanguageChange
                    )
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("toggle_theme_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = strings.darkModeLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Admin button intentionally removed from User Panel; access is via domain/#admin444 route only.
                    if (onSignOut != null) {
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = strings.signOut,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = effectiveBanner.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(ElectricBlue.copy(alpha = 0.14f), CyanAccent.copy(alpha = 0.14f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = effectiveBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
