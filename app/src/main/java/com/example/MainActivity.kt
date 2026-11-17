package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.MathAiRepository
import com.example.data.ZeluFirestoreRepository
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.signOutUser
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.LanguagePreferencesRepository
import com.example.ui.components.AuthLandingScreen
import com.example.ui.components.ZeluTopBar
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.CameraSolverScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HomeSolverScreen
import com.example.ui.screens.LibraryAndPracticeScreen
import com.example.ui.screens.VoiceTeacherScreen
import com.example.ui.screens.WhiteboardScreen
import com.example.ui.theme.ZeluMathTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.UiState
import com.example.viewmodel.ZeluMainViewModel
import com.example.voice.VoiceTeacherManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class MainTab(val icon: ImageVector) {
    SOLVER(Icons.Filled.Calculate),
    CAMERA(Icons.Filled.CameraAlt),
    VOICE(Icons.Filled.RecordVoiceOver),
    BOARD(Icons.Filled.Draw),
    LIBRARY(Icons.AutoMirrored.Filled.MenuBook),
    DASHBOARD(Icons.Filled.Dashboard)
}

private data class ParsedAdminRoute(
    val openAdmin: Boolean = false,
    val resetToken: String = ""
)

private fun parseRouteString(rawInput: String?): ParsedAdminRoute {
    val raw = rawInput?.trim().orEmpty()
    if (raw.isEmpty()) return ParsedAdminRoute()
    val lower = raw.lowercase()
    val hasAdminRoute = lower.contains("#admin444") || lower.endsWith("/admin444") || lower == "admin444"
    val resetToken = runCatching {
        val afterReset = raw.substringAfter("reset=", "")
        if (afterReset.isNotEmpty()) {
            Uri.decode(afterReset.substringBefore("&").substringBefore("#"))
        } else {
            ""
        }
    }.getOrDefault("")
    return ParsedAdminRoute(openAdmin = hasAdminRoute || resetToken.isNotBlank(), resetToken = resetToken)
}

class MainActivity : ComponentActivity() {
    private val routeSignal = mutableStateOf(ParsedAdminRoute())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        routeSignal.value = parseRouteString(intent?.dataString)
        enableEdgeToEdge()

        setContent {
            val prefsRepo = remember { LanguagePreferencesRepository(applicationContext) }
            val currentLanguage by prefsRepo.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.ENGLISH)
            val darkOverride by prefsRepo.darkThemeOverrideFlow.collectAsStateWithLifecycle(initialValue = null)
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = darkOverride ?: systemDark
            val strings = remember(currentLanguage) { AppStrings.forLanguage(currentLanguage) }
            val scope = rememberCoroutineScope()

            val credentialManager = remember { CredentialManager.create(this) }
            val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
                initialValue = Firebase.auth.currentUser
            )

            LaunchedEffect(Unit) {
                delay(450)
                if (Firebase.auth.currentUser == null) {
                    attemptAutoSignIn(
                        context = this@MainActivity,
                        credentialManager = credentialManager,
                        onAuthSuccess = {},
                        onUnauthenticated = {},
                        scope = scope
                    )
                }
            }

            val parsedRoute = routeSignal.value
            val user = currentUser
            if (user == null) {
                ZeluMathTheme(
                    darkTheme = isDarkTheme,
                    isUrdu = currentLanguage == AppLanguage.URDU
                ) {
                    AuthLandingScreen(
                        strings = strings,
                        currentLanguage = currentLanguage,
                        onLanguageChange = { lang -> scope.launch { prefsRepo.setLanguage(lang) } },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { scope.launch { prefsRepo.setDarkThemeOverride(!isDarkTheme) } },
                        onAuthSuccess = {}
                    )
                }
            } else {
                key(user.uid) {
                    val mainViewModel: ZeluMainViewModel = viewModel(
                        key = "main_${user.uid}",
                        factory = viewModelFactory {
                            initializer {
                                ZeluMainViewModel(
                                    repository = ZeluFirestoreRepository(applicationContext),
                                    aiRepository = MathAiRepository(),
                                    currentUserId = user.uid
                                )
                            }
                        }
                    )

                    LaunchedEffect(user.uid) {
                        mainViewModel.syncUserSession(user, currentLanguage)
                    }

                    val settingsState by mainViewModel.systemSettings.collectAsStateWithLifecycle()
                    val systemSettings = (settingsState as? UiState.Success)?.data
                    val resolvedDark = darkOverride ?: when (systemSettings?.defaultThemeMode) {
                        "light" -> false
                        "dark" -> true
                        else -> systemDark
                    }

                    val voiceManager = remember { VoiceTeacherManager(applicationContext) }
                    DisposableEffect(voiceManager) {
                        onDispose { voiceManager.release() }
                    }

                    ZeluMathTheme(
                        darkTheme = resolvedDark,
                        isUrdu = currentLanguage == AppLanguage.URDU,
                        accentHex = systemSettings?.themeAccentHex ?: "#2563EB"
                    ) {
                        ZeluAuthenticatedShell(
                            userId = user.uid,
                            mainViewModel = mainViewModel,
                            voiceManager = voiceManager,
                            strings = strings,
                            currentLanguage = currentLanguage,
                            onLanguageChange = { lang -> scope.launch { prefsRepo.setLanguage(lang) } },
                            isDarkTheme = resolvedDark,
                            onToggleTheme = { scope.launch { prefsRepo.setDarkThemeOverride(!resolvedDark) } },
                            onSignOut = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            },
                            routeFromIntent = parsedRoute,
                            onRouteConsumed = { routeSignal.value = ParsedAdminRoute() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val parsed = parseRouteString(intent.dataString)
        if (parsed.openAdmin || parsed.resetToken.isNotBlank()) {
            routeSignal.value = parsed
        }
    }
}

@Composable
private fun ZeluAuthenticatedShell(
    userId: String,
    mainViewModel: ZeluMainViewModel,
    voiceManager: VoiceTeacherManager,
    strings: com.example.localization.ZeluStrings,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit,
    routeFromIntent: ParsedAdminRoute,
    onRouteConsumed: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isAdmin by mainViewModel.isAdmin.collectAsStateWithLifecycle()
    val profileState by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val questionsState by mainViewModel.solvedQuestions.collectAsStateWithLifecycle()
    val scansState by mainViewModel.scanHistory.collectAsStateWithLifecycle()
    val attemptsState by mainViewModel.practiceAttempts.collectAsStateWithLifecycle()
    val notificationsState by mainViewModel.notifications.collectAsStateWithLifecycle()
    val ticketsState by mainViewModel.userTickets.collectAsStateWithLifecycle()
    val curriculumState by mainViewModel.publishedCurriculum.collectAsStateWithLifecycle()
    val plansState by mainViewModel.activePlans.collectAsStateWithLifecycle()
    val paymentMethodsState by mainViewModel.paymentMethods.collectAsStateWithLifecycle()
    val settingsState by mainViewModel.systemSettings.collectAsStateWithLifecycle()

    val solverState by mainViewModel.solverState.collectAsStateWithLifecycle()
    val followUpState by mainViewModel.followUpState.collectAsStateWithLifecycle()
    val practiceState by mainViewModel.practiceState.collectAsStateWithLifecycle()
    val activeVoiceQuestion by mainViewModel.activeVoiceQuestion.collectAsStateWithLifecycle()
    val actionMessage by mainViewModel.actionMessage.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.SOLVER) }
    var showAdmin by rememberSaveable { mutableStateOf(false) }
    var activeResetToken by rememberSaveable { mutableStateOf("") }
    var showRouteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(routeFromIntent) {
        if (routeFromIntent.openAdmin) {
            showAdmin = true
            if (routeFromIntent.resetToken.isNotBlank()) {
                activeResetToken = routeFromIntent.resetToken
            }
            onRouteConsumed()
        }
    }

    val systemSettings = (settingsState as? UiState.Success)?.data
    LaunchedEffect(systemSettings, currentLanguage) {
        systemSettings?.let {
            voiceManager.syncAdminDefaults(
                personaId = it.defaultVoicePersona,
                pitch = it.defaultVoicePitch.toFloat(),
                speechRate = it.defaultSpeechRate.toFloat(),
                preferUrdu = currentLanguage == AppLanguage.URDU
            )
        }
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingSolverPrompt by rememberSaveable { mutableStateOf("") }
    var pendingLibrarySubject by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingLibraryClass by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDashboardSection by rememberSaveable { mutableIntStateOf(-1) }

    BackHandler(enabled = showAdmin) {
        showAdmin = false
    }
    BackHandler(enabled = !showAdmin && selectedTab != MainTab.SOLVER) {
        selectedTab = MainTab.SOLVER
    }

    val announcement = if (currentLanguage == AppLanguage.URDU) {
        systemSettings?.announcementBannerUr.orEmpty()
    } else {
        systemSettings?.announcementBannerEn.orEmpty()
    }

    val plansList = (plansState as? UiState.Success)?.data.orEmpty()

    if (showRouteDialog) {
        RouteNavigatorDialog(
            onDismiss = { showRouteDialog = false },
            onNavigate = { route ->
                val parsed = parseRouteString(route)
                showRouteDialog = false
                if (parsed.openAdmin) {
                    showAdmin = true
                    if (parsed.resetToken.isNotBlank()) {
                        activeResetToken = parsed.resetToken
                    }
                } else if (route.contains("dashboard", true)) {
                    showAdmin = false
                    selectedTab = MainTab.DASHBOARD
                } else if (route.contains("library", true)) {
                    showAdmin = false
                    selectedTab = MainTab.LIBRARY
                } else {
                    showAdmin = false
                    selectedTab = MainTab.SOLVER
                }
            }
        )
    }

    fun tabLabel(tab: MainTab): String = when (tab) {
        MainTab.SOLVER -> strings.navSolver
        MainTab.CAMERA -> strings.navCamera
        MainTab.VOICE -> strings.navVoice
        MainTab.BOARD -> strings.navBoard
        MainTab.LIBRARY -> strings.navLibrary
        MainTab.DASHBOARD -> strings.navDashboard
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedWidth = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                ZeluTopBar(
                    strings = strings,
                    currentLanguage = currentLanguage,
                    onLanguageChange = onLanguageChange,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onSignOut = onSignOut,
                    announcementText = announcement,
                    headerNotice = systemSettings?.headerNotice.orEmpty(),
                    customTitle = systemSettings?.siteTitle.orEmpty(),
                    customTagline = systemSettings?.siteTagline.orEmpty(),
                    logoBase64 = systemSettings?.logoBase64.orEmpty(),
                    faviconBase64 = systemSettings?.faviconBase64.orEmpty(),
                    onOpenRoutePrompt = { showRouteDialog = true }
                )
            },
            bottomBar = {
                if (!isExpandedWidth && !showAdmin) {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            val label = tabLabel(tab)
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedWidth && !showAdmin) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        MainTab.entries.forEach { tab ->
                            val label = tabLabel(tab)
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (showAdmin) {
                        val adminViewModel: AdminViewModel? = if (isAdmin) {
                            viewModel(
                                key = "admin_$userId",
                                factory = viewModelFactory {
                                    initializer {
                                        AdminViewModel(ZeluFirestoreRepository(context.applicationContext))
                                    }
                                }
                            )
                        } else {
                            null
                        }
                        AdminScreen(
                            strings = strings,
                            isAdmin = isAdmin,
                            adminViewModel = adminViewModel,
                            initialResetToken = activeResetToken,
                            onClearResetToken = { activeResetToken = "" },
                            onBack = { showAdmin = false }
                        )
                    } else {
                        when (selectedTab) {
                            MainTab.SOLVER -> {
                                HomeSolverScreen(
                                    strings = strings,
                                    language = currentLanguage,
                                    solverState = solverState,
                                    followUpState = followUpState,
                                    plans = plansList,
                                    systemSettings = systemSettings,
                                    prefillQuestion = pendingSolverPrompt,
                                    onPrefillConsumed = { pendingSolverPrompt = "" },
                                    onSolveText = { q, diff ->
                                        mainViewModel.solveTextQuestion(q, currentLanguage, diff, inputMode = "text")
                                    },
                                    onOpenCamera = { selectedTab = MainTab.CAMERA },
                                    onImagePickedForSolver = { uri ->
                                        pendingCameraUri = uri
                                        selectedTab = MainTab.CAMERA
                                    },
                                    onToggleBookmark = { mainViewModel.toggleBookmark(it) },
                                    onListenVoice = { q ->
                                        mainViewModel.setVoiceQuestion(q)
                                        selectedTab = MainTab.VOICE
                                    },
                                    onAskFollowUp = { q, idx, prompt ->
                                        mainViewModel.askFollowUp(q, idx, prompt, currentLanguage)
                                    },
                                    onSelectSubjectOrClass = { subj, cls ->
                                        pendingLibrarySubject = subj
                                        pendingLibraryClass = cls
                                        selectedTab = MainTab.LIBRARY
                                    },
                                    onOpenPlansTab = {
                                        pendingDashboardSection = 5
                                        selectedTab = MainTab.DASHBOARD
                                    },
                                    onOpenRoutePrompt = { showRouteDialog = true }
                                )
                            }

                            MainTab.CAMERA -> {
                                CameraSolverScreen(
                                    strings = strings,
                                    pendingImageUri = pendingCameraUri,
                                    onPendingUriConsumed = { pendingCameraUri = null },
                                    solverState = solverState,
                                    followUpState = followUpState,
                                    onSolveImage = { bmp, hint, diff ->
                                        mainViewModel.solveImageQuestion(bmp, hint, currentLanguage, diff, inputMode = "camera")
                                    },
                                    onToggleBookmark = { mainViewModel.toggleBookmark(it) },
                                    onListenVoice = { q ->
                                        mainViewModel.setVoiceQuestion(q)
                                        selectedTab = MainTab.VOICE
                                    },
                                    onAskFollowUp = { q, idx, prompt ->
                                        mainViewModel.askFollowUp(q, idx, prompt, currentLanguage)
                                    }
                                )
                            }

                            MainTab.VOICE -> {
                                VoiceTeacherScreen(
                                    strings = strings,
                                    language = currentLanguage,
                                    activeQuestion = activeVoiceQuestion,
                                    voiceManager = voiceManager,
                                    followUpState = followUpState,
                                    onAskFollowUp = { q, idx, prompt ->
                                        mainViewModel.askFollowUp(q, idx, prompt, currentLanguage)
                                    }
                                )
                            }

                            MainTab.BOARD -> {
                                WhiteboardScreen(
                                    strings = strings,
                                    solverState = solverState,
                                    followUpState = followUpState,
                                    onSolveBoardBitmap = { bmp, hint ->
                                        mainViewModel.solveImageQuestion(bmp, hint, currentLanguage, "detailed", inputMode = "whiteboard")
                                    },
                                    onToggleBookmark = { mainViewModel.toggleBookmark(it) },
                                    onListenVoice = { q ->
                                        mainViewModel.setVoiceQuestion(q)
                                        selectedTab = MainTab.VOICE
                                    },
                                    onAskFollowUp = { q, idx, prompt ->
                                        mainViewModel.askFollowUp(q, idx, prompt, currentLanguage)
                                    }
                                )
                            }

                            MainTab.LIBRARY -> {
                                LibraryAndPracticeScreen(
                                    strings = strings,
                                    curriculumState = curriculumState,
                                    practiceState = practiceState,
                                    initialSubject = pendingLibrarySubject,
                                    initialClass = pendingLibraryClass,
                                    onInitialFiltersConsumed = {
                                        pendingLibrarySubject = null
                                        pendingLibraryClass = null
                                    },
                                    onSolveCurriculumPrompt = { prompt ->
                                        pendingSolverPrompt = prompt
                                        selectedTab = MainTab.SOLVER
                                    },
                                    onStartPracticeTest = { board, cls, subj ->
                                        mainViewModel.startPracticeTest(board, cls, subj, currentLanguage)
                                    },
                                    onSelectPracticeOption = { qIdx, optIdx ->
                                        mainViewModel.selectPracticeOption(qIdx, optIdx)
                                    },
                                    onSubmitPracticeTest = { mainViewModel.submitPracticeTest() },
                                    onResetPracticeTest = { mainViewModel.resetPracticeTest() }
                                )
                            }

                            MainTab.DASHBOARD -> {
                                DashboardScreen(
                                    strings = strings,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = onLanguageChange,
                                    profileState = profileState,
                                    questionsState = questionsState,
                                    scansState = scansState,
                                    practiceAttemptsState = attemptsState,
                                    notificationsState = notificationsState,
                                    ticketsState = ticketsState,
                                    plansState = plansState,
                                    paymentMethodsState = paymentMethodsState,
                                    initialSectionIndex = pendingDashboardSection,
                                    onInitialSectionConsumed = { pendingDashboardSection = -1 },
                                    actionMessage = actionMessage,
                                    onClearActionMessage = { mainViewModel.clearActionMessage() },
                                    onToggleBookmark = { mainViewModel.toggleBookmark(it) },
                                    onListenVoice = { q ->
                                        mainViewModel.setVoiceQuestion(q)
                                        selectedTab = MainTab.VOICE
                                    },
                                    onDeleteQuestion = { mainViewModel.deleteQuestion(it) },
                                    onDeleteScan = { mainViewModel.deleteScan(it) },
                                    onSaveProfile = { name, lang, country, board, cls ->
                                        mainViewModel.updateProfile(name, lang, country, board, cls)
                                    },
                                    onSubmitTicket = { email, subject, msg, cat ->
                                        mainViewModel.submitSupportTicket(email, subject, msg, cat)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteNavigatorDialog(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var routeInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Direct Route Navigator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter target route path (for example: domain/#admin444 or #admin444).",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = routeInput,
                    onValueChange = { routeInput = it },
                    placeholder = { Text("domain/#...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("route_prompt_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onNavigate(routeInput) },
                modifier = Modifier.testTag("route_prompt_go")
            ) {
                Text("Open")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
