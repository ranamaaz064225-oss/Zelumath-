package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.ZeluStrings
import com.example.model.AdminSecurityConfig
import com.example.model.CurriculumItem
import com.example.model.EducationCatalog
import com.example.model.SystemSettings
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.UiState

@Composable
fun AdminScreen(
    strings: ZeluStrings,
    isAdmin: Boolean,
    adminViewModel: AdminViewModel?,
    initialResetToken: String = "",
    onClearResetToken: () -> Unit = {},
    onBack: () -> Unit
) {
    AdminSecurityGate(
        strings = strings,
        isAdmin = isAdmin,
        adminViewModel = adminViewModel,
        initialResetToken = initialResetToken,
        onClearResetToken = onClearResetToken,
        onBack = onBack
    ) { verifiedVm, securityConfig ->
        AdminPortalDashboard(
            strings = strings,
            adminViewModel = verifiedVm,
            securityConfig = securityConfig,
            onBack = onBack
        )
    }
}

@Composable
private fun AdminPortalDashboard(
    strings: ZeluStrings,
    adminViewModel: AdminViewModel,
    securityConfig: AdminSecurityConfig,
    onBack: () -> Unit
) {
    val usersState by adminViewModel.users.collectAsStateWithLifecycle()
    val curriculumState by adminViewModel.curriculum.collectAsStateWithLifecycle()
    val plansState by adminViewModel.plans.collectAsStateWithLifecycle()
    val paymentsState by adminViewModel.paymentMethods.collectAsStateWithLifecycle()
    val ticketsState by adminViewModel.tickets.collectAsStateWithLifecycle()
    val logsState by adminViewModel.auditLogs.collectAsStateWithLifecycle()
    val settingsState by adminViewModel.systemSettings.collectAsStateWithLifecycle()
    val statusMsg by adminViewModel.statusMessage.collectAsStateWithLifecycle()

    val users = (usersState as? UiState.Success)?.data.orEmpty()
    val curriculum = (curriculumState as? UiState.Success)?.data.orEmpty()
    val plans = (plansState as? UiState.Success)?.data.orEmpty()
    val paymentMethods = (paymentsState as? UiState.Success)?.data.orEmpty()
    val tickets = (ticketsState as? UiState.Success)?.data.orEmpty()
    val logs = (logsState as? UiState.Success)?.data.orEmpty()
    val settings = (settingsState as? UiState.Success)?.data ?: SystemSettings()

    var activeSection by rememberSaveable { mutableIntStateOf(0) }
    val sections = listOf(
        strings.adminOverview,
        strings.adminUsers,
        strings.adminEducation,
        strings.adminAiVoice,
        "Plans & Payments",
        "Website, SEO & Theme",
        strings.adminTickets,
        "2FA, Password & Logs"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 820.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(strings.adminTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Route: domain/#admin444 • 2FA ${if (securityConfig.twoFactorEnabled) "Active" else "Off"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                OutlinedButton(onClick = onBack) { Text("Exit Admin") }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sections.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = activeSection == idx,
                        onClick = { activeSection = idx },
                        label = { Text(label) },
                        modifier = Modifier.testTag("admin_tab_$idx")
                    )
                }
            }

            if (statusMsg != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().clickable { adminViewModel.clearStatusMessage() }
                ) {
                    Text(statusMsg.orEmpty(), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            when (activeSection) {
                0 -> {
                    val totalSolved = users.sumOf { it.totalSolved }
                    val activeUsers = users.count { it.status == "active" }
                    val openTickets = tickets.count { it.status == "open" }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminMetricCard("Users", "${users.size} ($activeUsers active)", Modifier.weight(1f))
                        AdminMetricCard("Questions Solved", "$totalSolved", Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminMetricCard("Curriculum Items", "${curriculum.size}", Modifier.weight(1f))
                        AdminMetricCard("Payment Methods", "${paymentMethods.count { it.enabled }} active", Modifier.weight(1f))
                    }
                    AdminMetricCard("Open Support Tickets", "$openTickets", Modifier.fillMaxWidth())
                    Button(
                        onClick = { adminViewModel.seedStarterCatalog() },
                        modifier = Modifier.fillMaxWidth().testTag("admin_seed_catalog_button")
                    ) {
                        Text(strings.seedDefaultData)
                    }
                }

                1 -> {
                    var search by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("Search users by name or email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val filteredUsers = users.filter {
                        search.isBlank() || it.displayName.contains(search, true) || it.email.contains(search, true)
                    }
                    filteredUsers.forEach { u ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(u.displayName, fontWeight = FontWeight.Bold)
                                Text(u.email.ifBlank { u.userId }, style = MaterialTheme.typography.bodySmall)
                                Text("Plan: ${u.planTier} • Solved: ${u.totalSolved} • Status: ${u.status}", style = MaterialTheme.typography.labelSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        val nextStatus = if (u.status == "suspended") "active" else "suspended"
                                        adminViewModel.updateUserStatusAndPlan(u.userId, nextStatus, u.planTier)
                                    }) {
                                        Text(if (u.status == "suspended") strings.reactivateUser else strings.suspendUser)
                                    }
                                    listOf("FREE", "PREMIUM", "PRO").forEach { tier ->
                                        FilterChip(
                                            selected = u.planTier == tier,
                                            onClick = { adminViewModel.updateUserStatusAndPlan(u.userId, u.status, tier) },
                                            label = { Text(tier) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    var board by remember { mutableStateOf("Punjab Board") }
                    var studentClass by remember { mutableStateOf("Class 10") }
                    var subject by remember { mutableStateOf("Algebra") }
                    var book by remember { mutableStateOf("ZELU Open Algebra") }
                    var chapter by remember { mutableStateOf("Chapter 1") }
                    var exercise by remember { mutableStateOf("Practice Set 1") }
                    var prompt by remember { mutableStateOf("") }

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectorDropdown(strings.boardLabel, board, EducationCatalog.boardsByCountry["Pakistan"] ?: emptyList(), { board = it }, Modifier.fillMaxWidth())
                            SelectorDropdown(strings.classLabel, studentClass, EducationCatalog.classesList, { studentClass = it }, Modifier.fillMaxWidth())
                            SelectorDropdown(strings.subjectLabel, subject, EducationCatalog.subjectsList, { subject = it }, Modifier.fillMaxWidth())
                            OutlinedTextField(value = book, onValueChange = { book = it }, label = { Text("Book") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = chapter, onValueChange = { chapter = it }, label = { Text("Chapter") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = exercise, onValueChange = { exercise = it }, label = { Text("Exercise") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Authorized Question") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    if (prompt.isNotBlank()) {
                                        adminViewModel.addCurriculumItem(
                                            CurriculumItem(
                                                country = "Pakistan",
                                                board = board,
                                                studentClass = studentClass,
                                                subject = subject,
                                                bookTitle = book,
                                                chapterTitle = chapter,
                                                exerciseTitle = exercise,
                                                questionPrompt = prompt,
                                                sourceNote = "Admin Authored — ZELU Open Curriculum",
                                                published = true
                                            )
                                        )
                                        prompt = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(strings.publishCurriculum) }
                        }
                    }

                    curriculum.forEach { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${item.board} • ${item.studentClass} • ${item.subject}", fontWeight = FontWeight.Bold)
                                Text(item.questionPrompt, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Published")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(checked = item.published, onCheckedChange = { adminViewModel.toggleCurriculumPublish(item.id, it) })
                                    }
                                    IconButton(onClick = { adminViewModel.deleteCurriculumItem(item.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = strings.deleteItem, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    var aiModel by remember(settings.aiModel) { mutableStateOf(settings.aiModel) }
                    var freeLimit by remember(settings.dailyFreeLimit) { mutableStateOf(settings.dailyFreeLimit.toString()) }
                    var voiceLang by remember(settings.defaultVoiceLang) { mutableStateOf(settings.defaultVoiceLang) }
                    var voicePersona by remember(settings.defaultVoicePersona) { mutableStateOf(settings.defaultVoicePersona) }
                    var voicePitch by remember(settings.defaultVoicePitch) { mutableStateOf(settings.defaultVoicePitch.toString()) }
                    var speechRate by remember(settings.defaultSpeechRate) { mutableStateOf(settings.defaultSpeechRate.toString()) }

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SelectorDropdown(
                                label = "Gemini Model",
                                selected = aiModel,
                                options = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon"),
                                onSelected = { aiModel = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(value = freeLimit, onValueChange = { freeLimit = it }, label = { Text("Daily Free Question Limit") }, modifier = Modifier.fillMaxWidth())
                            SelectorDropdown("Default Voice Language (PK Urdu / English)", voiceLang, listOf("ur", "en"), { voiceLang = it }, Modifier.fillMaxWidth())
                            Text("Default Teacher / Madam Voice", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = voicePersona == "madam",
                                    onClick = { voicePersona = "madam"; voicePitch = "1.06" },
                                    label = { Text("Madam (Warm Female)") }
                                )
                                FilterChip(
                                    selected = voicePersona == "teacher",
                                    onClick = { voicePersona = "teacher"; voicePitch = "0.88" },
                                    label = { Text("Teacher / Sir (Deep Male)") }
                                )
                            }
                            OutlinedTextField(value = voicePitch, onValueChange = { voicePitch = it }, label = { Text("Default Voice Pitch (0.70 - 1.40)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = speechRate, onValueChange = { speechRate = it }, label = { Text("Default Speech Rate (0.60 - 1.75)") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    adminViewModel.saveSystemSettings(
                                        settings.copy(
                                            aiModel = aiModel,
                                            dailyFreeLimit = freeLimit.toLongOrNull() ?: 10L,
                                            defaultVoiceLang = voiceLang,
                                            defaultVoicePersona = voicePersona,
                                            defaultVoicePitch = voicePitch.toDoubleOrNull() ?: 1.04,
                                            defaultSpeechRate = speechRate.toDoubleOrNull() ?: 0.96
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(strings.saveSettings) }
                        }
                    }
                }

                4 -> {
                    val activeList = plans.ifEmpty { EducationCatalog.defaultPlans }
                    activeList.forEach { p ->
                        var priceUsd by remember(p.id, p.monthlyPriceUsd) { mutableStateOf(p.monthlyPriceUsd.toString()) }
                        var pricePkr by remember(p.id, p.monthlyPricePkr) { mutableStateOf(p.monthlyPricePkr.toString()) }
                        var qLimit by remember(p.id, p.dailyQuestionLimit) { mutableStateOf(p.dailyQuestionLimit.toString()) }
                        var sLimit by remember(p.id, p.dailyScanLimit) { mutableStateOf(p.dailyScanLimit.toString()) }

                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${p.name} (${p.tier})", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = priceUsd, onValueChange = { priceUsd = it }, label = { Text("USD/mo") }, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = pricePkr, onValueChange = { pricePkr = it }, label = { Text("PKR/mo") }, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = qLimit, onValueChange = { qLimit = it }, label = { Text("Q/day") }, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = sLimit, onValueChange = { sLimit = it }, label = { Text("Scans/day") }, modifier = Modifier.weight(1f))
                                }
                                Button(onClick = {
                                    adminViewModel.savePlan(
                                        p.copy(
                                            monthlyPriceUsd = priceUsd.toDoubleOrNull() ?: p.monthlyPriceUsd,
                                            monthlyPricePkr = pricePkr.toLongOrNull() ?: p.monthlyPricePkr,
                                            dailyQuestionLimit = qLimit.toLongOrNull() ?: p.dailyQuestionLimit,
                                            dailyScanLimit = sLimit.toLongOrNull() ?: p.dailyScanLimit
                                        )
                                    )
                                }) { Text("Save ${p.tier}") }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    AdminPaymentMethodsSection(
                        methods = paymentMethods,
                        onSaveMethod = { adminViewModel.savePaymentMethod(it) },
                        onDeleteMethod = { adminViewModel.deletePaymentMethod(it) }
                    )
                }

                5 -> {
                    AdminWebsiteAndBrandingSection(
                        settings = settings,
                        saveButtonLabel = strings.saveSettings,
                        onSaveSettings = { adminViewModel.saveSystemSettings(it) }
                    )
                }

                6 -> {
                    if (tickets.isEmpty()) {
                        Text("No support tickets submitted yet.")
                    } else {
                        tickets.forEach { t ->
                            var replyText by remember(t.id, t.adminReply) { mutableStateOf(t.adminReply) }
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${t.subject} (${t.userEmail.ifBlank { t.userId }})", fontWeight = FontWeight.Bold)
                                        AssistChip(onClick = {}, label = { Text(t.status) })
                                    }
                                    Text(t.message, style = MaterialTheme.typography.bodySmall)
                                    OutlinedTextField(value = replyText, onValueChange = { replyText = it }, label = { Text("Admin Reply") }, modifier = Modifier.fillMaxWidth())
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { adminViewModel.updateSupportTicket(t.id, "in_progress", replyText) }) { Text("In Progress") }
                                        Button(onClick = { adminViewModel.updateSupportTicket(t.id, "resolved", replyText) }) { Text("Resolve") }
                                    }
                                }
                            }
                        }
                    }
                }

                7 -> {
                    AdminSecuritySettingsSection(
                        config = securityConfig,
                        adminViewModel = adminViewModel,
                        logs = logs
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminMetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
