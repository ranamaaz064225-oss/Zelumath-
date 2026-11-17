package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.firebase.AdminSecurityHelper
import com.example.model.AdminAuditLog
import com.example.model.AdminSecurityConfig
import com.example.model.PaymentMethod
import com.example.model.SystemSettings
import com.example.ui.components.rememberDecodedBase64Bitmap
import com.example.viewmodel.AdminViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun AdminWebsiteAndBrandingSection(
    settings: SystemSettings,
    saveButtonLabel: String,
    onSaveSettings: (SystemSettings) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var logoBase64 by remember(settings.logoBase64) { mutableStateOf(settings.logoBase64) }
    var bannerBase64 by remember(settings.bannerBase64) { mutableStateOf(settings.bannerBase64) }
    var faviconBase64 by remember(settings.faviconBase64) { mutableStateOf(settings.faviconBase64) }

    var siteTitle by remember(settings.siteTitle) { mutableStateOf(settings.siteTitle) }
    var siteTagline by remember(settings.siteTagline) { mutableStateOf(settings.siteTagline) }
    var headerNotice by remember(settings.headerNotice) { mutableStateOf(settings.headerNotice) }
    var bannerEn by remember(settings.announcementBannerEn) { mutableStateOf(settings.announcementBannerEn) }
    var bannerUr by remember(settings.announcementBannerUr) { mutableStateOf(settings.announcementBannerUr) }

    var themeAccentHex by remember(settings.themeAccentHex) { mutableStateOf(settings.themeAccentHex) }
    var defaultThemeMode by remember(settings.defaultThemeMode) { mutableStateOf(settings.defaultThemeMode) }

    var seoTitle by remember(settings.seoTitle) { mutableStateOf(settings.seoTitle) }
    var seoDesc by remember(settings.seoDescription) { mutableStateOf(settings.seoDescription) }
    var seoKeywords by remember(settings.seoKeywords) { mutableStateOf(settings.seoKeywords) }

    var footerText by remember(settings.footerText) { mutableStateOf(settings.footerText) }
    var socialFb by remember(settings.socialFacebook) { mutableStateOf(settings.socialFacebook) }
    var socialYt by remember(settings.socialYoutube) { mutableStateOf(settings.socialYoutube) }
    var socialIg by remember(settings.socialInstagram) { mutableStateOf(settings.socialInstagram) }
    var socialWa by remember(settings.socialWhatsapp) { mutableStateOf(settings.socialWhatsapp) }
    var socialEmail by remember(settings.socialEmail) { mutableStateOf(settings.socialEmail) }

    var adsOn by remember(settings.adsEnabled) { mutableStateOf(settings.adsEnabled) }
    var adBannerText by remember(settings.adBannerText) { mutableStateOf(settings.adBannerText) }
    var headerScript by remember(settings.customHeaderScript) { mutableStateOf(settings.customHeaderScript) }
    var footerScript by remember(settings.customFooterScript) { mutableStateOf(settings.customFooterScript) }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                AdminSecurityHelper.compressImageUriToBase64(context, uri, maxDimension = 192, jpegQuality = 78)?.let {
                    logoBase64 = it
                }
            }
        }
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                AdminSecurityHelper.compressImageUriToBase64(context, uri, maxDimension = 720, jpegQuality = 72)?.let {
                    bannerBase64 = it
                }
            }
        }
    }
    val faviconPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                AdminSecurityHelper.compressImageUriToBase64(context, uri, maxDimension = 96, jpegQuality = 80)?.let {
                    faviconBase64 = it
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Gallery Media Assets Card
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Logo, Banner & Favicon (Gallery Upload)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                AdminImagePickerRow(
                    title = "Website / App Logo",
                    base64 = logoBase64,
                    previewSizeDp = 56,
                    onPick = { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onClear = { logoBase64 = "" }
                )
                AdminImagePickerRow(
                    title = "Hero Banner Image",
                    base64 = bannerBase64,
                    previewSizeDp = 72,
                    onPick = { bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onClear = { bannerBase64 = "" }
                )
                AdminImagePickerRow(
                    title = "Favicon Badge",
                    base64 = faviconBase64,
                    previewSizeDp = 40,
                    onPick = { faviconPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onClear = { faviconBase64 = "" }
                )
            }
        }

        // Theme & Colors Card
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Theme & Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val swatches = listOf("#2563EB", "#059669", "#7C3AED", "#DC2626", "#D97706", "#4F46E5")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    swatches.forEach { hex ->
                        val parsed = AdminSecurityHelper.parseHexColorOrNull(hex) ?: MaterialTheme.colorScheme.primary
                        val selected = themeAccentHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.Gray, CircleShape)
                                .clickable { themeAccentHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = themeAccentHex,
                    onValueChange = { themeAccentHex = it },
                    label = { Text("Theme Accent Hex (#RRGGBB)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (id, label) ->
                        FilterChip(
                            selected = defaultThemeMode == id,
                            onClick = { defaultThemeMode = id },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // Header, SEO, Footer & Social Links Card
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Header, SEO, Footer & Social Media", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = siteTitle, onValueChange = { siteTitle = it }, label = { Text("Site Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = siteTagline, onValueChange = { siteTagline = it }, label = { Text("Site Tagline") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = headerNotice, onValueChange = { headerNotice = it }, label = { Text("Header Notice Bar") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bannerEn, onValueChange = { bannerEn = it }, label = { Text("Announcement Banner (English)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bannerUr, onValueChange = { bannerUr = it }, label = { Text("Announcement Banner (Urdu)") }, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = seoTitle, onValueChange = { seoTitle = it }, label = { Text("SEO Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = seoDesc, onValueChange = { seoDesc = it }, label = { Text("SEO Meta Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = seoKeywords, onValueChange = { seoKeywords = it }, label = { Text("SEO Keywords") }, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = footerText, onValueChange = { footerText = it }, label = { Text("Footer Copyright & Notice Text") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = socialFb, onValueChange = { socialFb = it }, label = { Text("Facebook URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = socialYt, onValueChange = { socialYt = it }, label = { Text("YouTube URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = socialIg, onValueChange = { socialIg = it }, label = { Text("Instagram URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = socialWa, onValueChange = { socialWa = it }, label = { Text("WhatsApp Link or Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = socialEmail, onValueChange = { socialEmail = it }, label = { Text("Footer Contact Email") }, modifier = Modifier.fillMaxWidth())
            }
        }

        // Ads & Custom Header/Footer Scripts Card
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ads & Custom Scripts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Ads Placement Enabled")
                    Switch(checked = adsOn, onCheckedChange = { adsOn = it })
                }
                OutlinedTextField(value = adBannerText, onValueChange = { adBannerText = it }, label = { Text("Ad Banner Text / Promo Copy") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = headerScript, onValueChange = { headerScript = it }, label = { Text("Custom Header Script / Meta Tag") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = footerScript, onValueChange = { footerScript = it }, label = { Text("Custom Footer Script / Analytics") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        onSaveSettings(
                            settings.copy(
                                logoBase64 = logoBase64,
                                bannerBase64 = bannerBase64,
                                faviconBase64 = faviconBase64,
                                siteTitle = siteTitle.ifBlank { "ZELU Math AI" },
                                siteTagline = siteTagline,
                                headerNotice = headerNotice,
                                announcementBannerEn = bannerEn,
                                announcementBannerUr = bannerUr,
                                themeAccentHex = themeAccentHex.ifBlank { "#2563EB" },
                                defaultThemeMode = defaultThemeMode,
                                seoTitle = seoTitle,
                                seoDescription = seoDesc,
                                seoKeywords = seoKeywords,
                                footerText = footerText,
                                socialFacebook = socialFb,
                                socialYoutube = socialYt,
                                socialInstagram = socialIg,
                                socialWhatsapp = socialWa,
                                socialEmail = socialEmail,
                                adsEnabled = adsOn,
                                adBannerText = adBannerText,
                                customHeaderScript = headerScript,
                                customFooterScript = footerScript
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_save_website_settings")
                ) {
                    Text(saveButtonLabel)
                }
            }
        }
    }
}

@Composable
private fun AdminImagePickerRow(
    title: String,
    base64: String,
    previewSizeDp: Int,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    val decoded = rememberDecodedBase64Bitmap(base64)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (decoded != null) {
                Image(
                    bitmap = decoded,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(previewSizeDp.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(previewSizeDp.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (decoded != null) "Custom image active" else "Default built-in asset",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onPick) { Text("Gallery") }
            if (base64.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove $title", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AdminPaymentMethodsSection(
    methods: List<PaymentMethod>,
    onSaveMethod: (PaymentMethod) -> Unit,
    onDeleteMethod: (String) -> Unit
) {
    var editingId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var providerType by remember { mutableStateOf("Mobile Wallet") }
    var accountTitle by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(true) }

    fun resetForm() {
        editingId = ""
        name = ""
        providerType = "Mobile Wallet"
        accountTitle = ""
        accountNumber = ""
        instructions = ""
        enabled = true
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Payment Methods (Add / Edit / Delete)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (editingId.isBlank()) "Add New Payment Method" else "Edit Payment Method", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Method Name (e.g. JazzCash, EasyPaisa, Bank)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = providerType, onValueChange = { providerType = it }, label = { Text("Provider Type (Wallet / Bank / Card)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = accountTitle, onValueChange = { accountTitle = it }, label = { Text("Account Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("Account Number / IBAN") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("Payment Instructions for Students") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Visible to Students")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingId.isNotBlank()) {
                        OutlinedButton(onClick = { resetForm() }, modifier = Modifier.weight(1f)) { Text("Cancel Edit") }
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSaveMethod(
                                    PaymentMethod(
                                        id = editingId,
                                        name = name,
                                        providerType = providerType,
                                        accountTitle = accountTitle,
                                        accountNumber = accountNumber,
                                        instructions = instructions,
                                        enabled = enabled
                                    )
                                )
                                resetForm()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("admin_save_payment_method")
                    ) {
                        Icon(if (editingId.isBlank()) Icons.Filled.Add else Icons.Filled.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (editingId.isBlank()) "Add Method" else "Update Method")
                    }
                }
            }
        }

        methods.forEach { method ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${method.name} (${method.providerType})", fontWeight = FontWeight.Bold)
                            Text("${method.accountTitle} • ${method.accountNumber}", style = MaterialTheme.typography.bodySmall)
                            if (method.instructions.isNotBlank()) {
                                Text(method.instructions, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = method.enabled,
                            onCheckedChange = { isChecked ->
                                onSaveMethod(method.copy(enabled = isChecked))
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                editingId = method.id
                                name = method.name
                                providerType = method.providerType
                                accountTitle = method.accountTitle
                                accountNumber = method.accountNumber
                                instructions = method.instructions
                                enabled = method.enabled
                            }
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit")
                        }
                        OutlinedButton(onClick = { onDeleteMethod(method.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSecuritySettingsSection(
    config: AdminSecurityConfig,
    adminViewModel: AdminViewModel,
    logs: List<AdminAuditLog>
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var recoveryEmail by remember(config.recoveryEmail) { mutableStateOf(config.recoveryEmail.ifBlank { "ranamaaz064225@gmail.com" }) }

    val otpUri = remember(config.totpSecret, config.recoveryEmail) {
        if (config.totpSecret.isBlank()) "" else AdminSecurityHelper.buildOtpAuthUri(config.recoveryEmail, config.totpSecret)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Password Management
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Admin Password & Recovery Email", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = currentPass,
                    onValueChange = { currentPass = it },
                    label = { Text("Current Admin Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("New Admin Password (min 6 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = recoveryEmail,
                    onValueChange = { recoveryEmail = it },
                    label = { Text("Recovery Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (newPass != confirmPass) {
                                adminViewModel.setStatusMessage("New password and confirmation do not match.")
                            } else {
                                adminViewModel.saveNewAdminPassword(
                                    currentConfig = config,
                                    currentPassword = currentPass,
                                    newPassword = newPass,
                                    recoveryEmail = recoveryEmail,
                                    requireCurrentCheck = true
                                ) { ok, _ ->
                                    if (ok) {
                                        currentPass = ""
                                        newPass = ""
                                        confirmPass = ""
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Update Password") }
                    OutlinedButton(
                        onClick = {
                            adminViewModel.issuePasswordResetToken(
                                currentConfig = config,
                                emailInput = recoveryEmail,
                                onTokenIssued = { token, target ->
                                    val webLink = "https://ais-pre-uw6q3wpj3egqlczdzl4g5m-471535351601.asia-southeast1.run.app/#admin444?reset=$token"
                                    val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$target")).apply {
                                        putExtra(Intent.EXTRA_SUBJECT, "ZELU Admin Password Reset Link")
                                        putExtra(Intent.EXTRA_TEXT, "Reset link: $webLink\nToken: $token")
                                    }
                                    runCatching { context.startActivity(mailIntent) }
                                    runCatching { Firebase.auth.sendPasswordResetEmail(target) }
                                    adminViewModel.setStatusMessage("Reset email link prepared for $target.")
                                },
                                onError = { adminViewModel.setStatusMessage(it) }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Reset Link")
                    }
                }
            }
        }

        // 2FA Authenticator Card
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Two-Factor Authentication (2FA)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("6-digit Authenticator TOTP required on #admin444 login.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = config.twoFactorEnabled,
                        onCheckedChange = { adminViewModel.setTwoFactorEnabled(config, it) }
                    )
                }

                if (config.totpSecret.isNotBlank()) {
                    Text("TOTP Secret Key: ${config.totpSecret}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(config.totpSecret))
                                adminViewModel.setStatusMessage("2FA secret key copied to clipboard.")
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Key")
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(otpUri))
                                adminViewModel.setStatusMessage("Authenticator otpauth:// link copied.")
                            }
                        ) { Text("Copy URI") }
                        OutlinedButton(
                            onClick = { adminViewModel.setTwoFactorEnabled(config, config.twoFactorEnabled, regenerateSecret = true) }
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rotate")
                        }
                    }
                }
            }
        }

        // Zero-Trust Rules + Audit Logs
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Zero-Trust Security & Route Isolation Active", fontWeight = FontWeight.Bold)
                Text(
                    "• Admin panel removed from user UI; accessible strictly via domain/#admin444.\n" +
                        "• /adminSecurity/config & admin collections enforced by Firestore isAdmin() rules.\n" +
                        "• Audit logs are append-only (update/delete denied).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        logs.forEach { log ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${log.action} • ${log.targetPath}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text("${log.adminEmail}: ${log.details}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
