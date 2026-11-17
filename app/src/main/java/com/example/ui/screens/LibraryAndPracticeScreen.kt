package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.ZeluStrings
import com.example.model.CurriculumItem
import com.example.model.EducationCatalog
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.VerifiedEmerald
import com.example.viewmodel.PracticeUiState
import com.example.viewmodel.UiState

@Composable
fun LibraryAndPracticeScreen(
    strings: ZeluStrings,
    curriculumState: UiState<List<CurriculumItem>>,
    practiceState: PracticeUiState,
    initialSubject: String?,
    initialClass: String?,
    onInitialFiltersConsumed: () -> Unit,
    onSolveCurriculumPrompt: (String) -> Unit,
    onStartPracticeTest: (board: String, studentClass: String, subject: String) -> Unit,
    onSelectPracticeOption: (questionIdx: Int, optionIdx: Int) -> Unit,
    onSubmitPracticeTest: () -> Unit,
    onResetPracticeTest: () -> Unit
) {
    var selectedCountry by rememberSaveable { mutableStateOf("Pakistan") }
    var selectedBoard by rememberSaveable { mutableStateOf("All") }
    var selectedClass by rememberSaveable { mutableStateOf("All") }
    var selectedSubject by rememberSaveable { mutableStateOf("All") }

    if (initialSubject != null || initialClass != null) {
        if (initialSubject != null) selectedSubject = initialSubject
        if (initialClass != null) selectedClass = initialClass
        onInitialFiltersConsumed()
    }

    val boardsForCountry = listOf("All") + (EducationCatalog.boardsByCountry[selectedCountry] ?: emptyList())
    val classOptions = listOf("All") + EducationCatalog.classesList
    val subjectOptions = listOf("All") + EducationCatalog.subjectsList

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
                text = strings.libraryTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Filter Hierarchy Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.sourceAttributionNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Country chips
                    Text(strings.countryLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EducationCatalog.countries.forEach { country ->
                            FilterChip(
                                selected = selectedCountry == country,
                                onClick = {
                                    selectedCountry = country
                                    selectedBoard = "All"
                                },
                                label = { Text(country) }
                            )
                        }
                    }

                    // Board, Class, Subject Dropdown Selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectorDropdown(
                            label = strings.boardLabel,
                            selected = selectedBoard,
                            options = boardsForCountry,
                            onSelected = { selectedBoard = it },
                            modifier = Modifier.weight(1f)
                        )
                        SelectorDropdown(
                            label = strings.classLabel,
                            selected = selectedClass,
                            options = classOptions,
                            onSelected = { selectedClass = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SelectorDropdown(
                        label = strings.subjectLabel,
                        selected = selectedSubject,
                        options = subjectOptions,
                        onSelected = { selectedSubject = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    val testBoard = if (selectedBoard == "All") "Punjab Board" else selectedBoard
                    val testClass = if (selectedClass == "All") "Class 10" else selectedClass
                    val testSubject = if (selectedSubject == "All") "Algebra" else selectedSubject

                    Button(
                        onClick = { onStartPracticeTest(testBoard, testClass, testSubject) },
                        enabled = practiceState !is PracticeUiState.Generating,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_practice_test_button")
                    ) {
                        if (practiceState is PracticeUiState.Generating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.loadingLabel)
                        } else {
                            Icon(Icons.Filled.Quiz, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${strings.startPracticeTest} ($testSubject)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Practice Test Runner
            when (practiceState) {
                is PracticeUiState.Active -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${strings.practiceTestTitle}: ${practiceState.subject}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(onClick = onResetPracticeTest) { Text("Close") }
                            }

                            if (practiceState.submittedScore != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = VerifiedEmerald)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${strings.testScoreTitle}: ${practiceState.submittedScore}%",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            practiceState.questions.forEachIndexed { qIdx, mcq ->
                                val selectedOpt = practiceState.selectedIndices[qIdx]
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Q${qIdx + 1}. ${mcq.question}", fontWeight = FontWeight.SemiBold)
                                    mcq.options.forEachIndexed { optIdx, optText ->
                                        val isCorrect = optIdx == mcq.correctIndex
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = practiceState.submittedScore == null) {
                                                    onSelectPracticeOption(qIdx, optIdx)
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedOpt == optIdx,
                                                onClick = { onSelectPracticeOption(qIdx, optIdx) },
                                                enabled = practiceState.submittedScore == null
                                            )
                                            Text(
                                                text = optText,
                                                color = when {
                                                    practiceState.submittedScore != null && isCorrect -> VerifiedEmerald
                                                    practiceState.submittedScore != null && selectedOpt == optIdx && !isCorrect -> MaterialTheme.colorScheme.error
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                    if (practiceState.submittedScore != null && mcq.explanation.isNotBlank()) {
                                        Text(
                                            text = mcq.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (practiceState.submittedScore == null) {
                                Button(
                                    onClick = onSubmitPracticeTest,
                                    modifier = Modifier.fillMaxWidth().testTag("submit_practice_test_button")
                                ) {
                                    Text(strings.submitTest, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                is PracticeUiState.Error -> {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                        Text("${strings.errorPrefix}: ${practiceState.message}", modifier = Modifier.padding(14.dp))
                    }
                }
                else -> Unit
            }

            // Curriculum List from Firestore
            when (curriculumState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                        Text("${strings.errorPrefix}: ${curriculumState.message}", modifier = Modifier.padding(14.dp))
                    }
                }
                is UiState.Success -> {
                    val filtered = curriculumState.data.filter { item ->
                        item.country == selectedCountry &&
                            (selectedBoard == "All" || item.board == selectedBoard) &&
                            (selectedClass == "All" || item.studentClass == selectedClass) &&
                            (selectedSubject == "All" || item.subject == selectedSubject)
                    }

                    if (filtered.isEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(strings.curriculumEmpty, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        filtered.forEach { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AssistChip(onClick = {}, label = { Text(item.board) })
                                        AssistChip(onClick = {}, label = { Text(item.studentClass) })
                                        AssistChip(onClick = {}, label = { Text(item.subject) })
                                    }
                                    Text(
                                        text = "${item.bookTitle} • ${item.chapterTitle} • ${item.exerciseTitle}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(item.questionPrompt, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(item.sourceNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Button(
                                        onClick = { onSolveCurriculumPrompt(item.questionPrompt) },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.solveThisQuestion)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectorDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: $selected", maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
