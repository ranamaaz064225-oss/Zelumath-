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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.localization.AppLanguage
import com.example.localization.ZeluStrings
import com.example.model.EducationCatalog
import com.example.model.PaymentMethod
import com.example.model.PracticeAttempt
import com.example.model.ScanRecord
import com.example.model.SolvedQuestion
import com.example.model.SubscriptionPlan
import com.example.model.SupportTicket
import com.example.model.UserNotification
import com.example.model.UserProfile
import com.example.viewmodel.UiState

@Composable
fun DashboardScreen(
    strings: ZeluStrings,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    profileState: UiState<UserProfile?>,
    questionsState: UiState<List<SolvedQuestion>>,
    scansState: UiState<List<ScanRecord>>,
    practiceAttemptsState: UiState<List<PracticeAttempt>>,
    notificationsState: UiState<List<UserNotification>>,
    ticketsState: UiState<List<SupportTicket>>,
    plansState: UiState<List<SubscriptionPlan>>,
    paymentMethodsState: UiState<List<PaymentMethod>>,
    initialSectionIndex: Int,
    onInitialSectionConsumed: () -> Unit,
    actionMessage: String?,
    onClearActionMessage: () -> Unit,
    onToggleBookmark: (SolvedQuestion) -> Unit,
    onListenVoice: (SolvedQuestion) -> Unit,
    onDeleteQuestion: (String) -> Unit,
    onDeleteScan: (String) -> Unit,
    onSaveProfile: (name: String, lang: AppLanguage, country: String, board: String, studentClass: String) -> Unit,
    onSubmitTicket: (email: String, subject: String, message: String, category: String) -> Unit
) {
    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    if (initialSectionIndex >= 0) {
        activeTab = initialSectionIndex
        onInitialSectionConsumed()
    }

    val tabs = listOf(
        strings.tabOverview,
        strings.tabHistory,
        strings.tabScans,
        strings.tabSaved,
        strings.tabPractice,
        strings.tabPlans,
        strings.tabProfile,
        strings.tabSupport,
        strings.tabInfo
    )

    val profile = (profileState as? UiState.Success)?.data
    val questions = (questionsState as? UiState.Success)?.data.orEmpty()
    val scans = (scansState as? UiState.Success)?.data.orEmpty()
    val attempts = (practiceAttemptsState as? UiState.Success)?.data.orEmpty()
    val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()
    val tickets = (ticketsState as? UiState.Success)?.data.orEmpty()
    val plans = (plansState as? UiState.Success)?.data.orEmpty().ifEmpty { EducationCatalog.defaultPlans }
    val paymentMethods = (paymentMethodsState as? UiState.Success)?.data.orEmpty()
    val activePlan = plans.firstOrNull { it.tier == (profile?.planTier ?: "FREE") } ?: EducationCatalog.defaultPlans.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.dashboardTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        label = { Text(label) },
                        modifier = Modifier.testTag("dashboard_tab_$idx")
                    )
                }
            }

            if (actionMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClearActionMessage() }
                ) {
                    Text(actionMessage, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            when (activeTab) {
                0 -> {
                    // Overview & Usage Limits & Notifications
                    if (profileState is UiState.Loading) {
                        CircularProgressIndicator()
                    } else if (profile != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                label = strings.totalSolvedLabel,
                                value = "${profile.totalSolved}",
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = strings.currentPlanLabel,
                                value = profile.planTier,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "${strings.dailyQuestionsLabel}: ${profile.dailyQuestionsUsed} / ${activePlan.dailyQuestionLimit}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                LinearProgressIndicator(
                                    progress = {
                                        (profile.dailyQuestionsUsed.toFloat() / activePlan.dailyQuestionLimit.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    "${strings.dailyScansLabel}: ${profile.dailyScansUsed} / ${activePlan.dailyScanLimit}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                LinearProgressIndicator(
                                    progress = {
                                        (profile.dailyScansUsed.toFloat() / activePlan.dailyScanLimit.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (notifications.isEmpty()) {
                        EmptyDashboardCard(text = "No notifications yet.", icon = Icons.Filled.Notifications)
                    } else {
                        notifications.forEach { n ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(n.title, fontWeight = FontWeight.Bold)
                                    Text(n.body, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // My Questions
                    if (questions.isEmpty()) {
                        EmptyDashboardCard(text = strings.emptyHistory)
                    } else {
                        questions.forEach { q ->
                            QuestionHistoryRow(
                                question = q,
                                strings = strings,
                                onToggleBookmark = { onToggleBookmark(q) },
                                onListenVoice = { onListenVoice(q) },
                                onDelete = { onDeleteQuestion(q.id) }
                            )
                        }
                    }
                }
                2 -> {
                    // Scan History
                    if (scans.isEmpty()) {
                        EmptyDashboardCard(text = strings.emptyScans)
                    } else {
                        scans.forEach { s ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${s.subject} • ${s.clarityStatus}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(s.detectedText, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    IconButton(onClick = { onDeleteScan(s.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = strings.deleteItem, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Bookmarked / Saved
                    val bookmarked = questions.filter { it.bookmarked }
                    if (bookmarked.isEmpty()) {
                        EmptyDashboardCard(text = strings.emptySaved, icon = Icons.Filled.Bookmark)
                    } else {
                        bookmarked.forEach { q ->
                            QuestionHistoryRow(
                                question = q,
                                strings = strings,
                                onToggleBookmark = { onToggleBookmark(q) },
                                onListenVoice = { onListenVoice(q) },
                                onDelete = { onDeleteQuestion(q.id) }
                            )
                        }
                    }
                }
                4 -> {
                    // Practice Progress
                    if (attempts.isEmpty()) {
                        EmptyDashboardCard(text = strings.emptyPractice)
                    } else {
                        attempts.forEach { a ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(a.subject, fontWeight = FontWeight.Bold)
                                        Text("${a.board} • ${a.studentClass}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("${a.scorePercent}% (${a.correctCount}/${a.totalQuestions})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                5 -> {
                    // Subscription Plans & Honest Payment Gateway Notice
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.paymentGatewayNotice, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
                    }
                    plans.forEach { p ->
                        val isCurrent = p.tier == (profile?.planTier ?: "FREE")
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${p.name} (${p.tier})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (p.monthlyPriceUsd <= 0.0) "Free" else "$${p.monthlyPriceUsd} / PKR ${p.monthlyPricePkr}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (isCurrent) {
                                    AssistChip(onClick = {}, label = { Text(strings.currentPlanLabel) })
                                }
                                p.features.forEach { feat ->
                                    Text("• $feat", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    if (paymentMethods.isNotEmpty()) {
                        Text(
                            text = strings.paymentMethodsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        paymentMethods.forEach { pm ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(pm.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Text(pm.providerType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (pm.accountTitle.isNotBlank()) {
                                        Text("Account Title: ${pm.accountTitle}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Account / Number: ${pm.accountNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    if (pm.instructions.isNotBlank()) {
                                        Text(pm.instructions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                6 -> {
                    // Profile Settings
                    var name by remember(profile?.displayName) { mutableStateOf(profile?.displayName.orEmpty()) }
                    var country by remember(profile?.country) { mutableStateOf(profile?.country ?: "Pakistan") }
                    var board by remember(profile?.board) { mutableStateOf(profile?.board ?: "Punjab Board") }
                    var studentClass by remember(profile?.studentClass) { mutableStateOf(profile?.studentClass ?: "Class 10") }
                    var selectedLang by remember(currentLanguage) { mutableStateOf(currentLanguage) }

                    LaunchedEffect(profile?.preferredLanguage) {
                        profile?.preferredLanguage?.let {
                            selectedLang = AppLanguage.fromCode(it)
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(strings.displayNameLabel) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            SelectorDropdown(
                                label = strings.countryLabel,
                                selected = country,
                                options = EducationCatalog.countries,
                                onSelected = {
                                    country = it
                                    board = EducationCatalog.boardsByCountry[it]?.firstOrNull() ?: "Punjab Board"
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            SelectorDropdown(
                                label = strings.boardLabel,
                                selected = board,
                                options = EducationCatalog.boardsByCountry[country] ?: emptyList(),
                                onSelected = { board = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            SelectorDropdown(
                                label = strings.classLabel,
                                selected = studentClass,
                                options = EducationCatalog.classesList,
                                onSelected = { studentClass = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppLanguage.entries.forEach { lang ->
                                    FilterChip(
                                        selected = selectedLang == lang,
                                        onClick = { selectedLang = lang },
                                        label = { Text(lang.nativeName) }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    onLanguageChange(selectedLang)
                                    onSaveProfile(name, selectedLang, country, board, studentClass)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
                            ) {
                                Text(strings.saveProfile, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                7 -> {
                    // Support Tickets
                    var subject by remember { mutableStateOf("") }
                    var message by remember { mutableStateOf("") }

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = subject,
                                onValueChange = { subject = it },
                                label = { Text(strings.ticketSubjectLabel) },
                                modifier = Modifier.fillMaxWidth().testTag("ticket_subject_input")
                            )
                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                label = { Text(strings.ticketMessageLabel) },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth().testTag("ticket_message_input")
                            )
                            Button(
                                onClick = {
                                    onSubmitTicket(profile?.email.orEmpty(), subject, message, "Support")
                                    subject = ""
                                    message = ""
                                },
                                modifier = Modifier.fillMaxWidth().testTag("submit_ticket_button")
                            ) {
                                Text(strings.submitTicket)
                            }
                        }
                    }

                    tickets.forEach { t ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(t.subject, fontWeight = FontWeight.Bold)
                                    AssistChip(onClick = {}, label = { Text(t.status) })
                                }
                                Text(t.message, style = MaterialTheme.typography.bodySmall)
                                if (t.adminReply.isNotBlank()) {
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                                        Text("Admin Reply: ${t.adminReply}", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                8 -> {
                    // Info Pages: About Us, Help Center, FAQ, Privacy Policy, Terms & Conditions
                    InfoCard(strings.aboutUsTitle, strings.aboutUsBody)
                    InfoCard(strings.helpCenterTitle, strings.helpCenterBody)
                    InfoCard(strings.privacyPolicyTitle, strings.privacyPolicyBody)
                    InfoCard(strings.termsTitle, strings.termsBody)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmptyDashboardCard(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Person
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun QuestionHistoryRow(
    question: SolvedQuestion,
    strings: ZeluStrings,
    onToggleBookmark: () -> Unit,
    onListenVoice: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text("${question.subject} • ${question.inputMode}") })
                Row {
                    IconButton(onClick = onListenVoice) {
                        Icon(Icons.Filled.RecordVoiceOver, contentDescription = strings.listenInVoice)
                    }
                    IconButton(onClick = onToggleBookmark) {
                        Icon(Icons.Filled.Bookmark, contentDescription = strings.bookmark, tint = if (question.bookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = strings.deleteItem, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(question.questionText, fontWeight = FontWeight.SemiBold)
            Text("${strings.step7FinalAnswer}: ${question.finalAnswer}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
