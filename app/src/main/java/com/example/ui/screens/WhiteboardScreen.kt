package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.localization.ZeluStrings
import com.example.model.SolvedQuestion
import com.example.ui.components.BoardItem
import com.example.ui.components.BoardTool
import com.example.ui.components.MathSolutionCard
import com.example.ui.components.MathWhiteboardCanvas
import com.example.ui.components.MathWhiteboardDefaults
import com.example.ui.components.renderBoardToBitmap
import com.example.ui.theme.CyanBright
import com.example.ui.theme.ElectricBlue
import com.example.viewmodel.FollowUpUiState
import com.example.viewmodel.SolverUiState
import java.io.File
import java.io.FileOutputStream

@Composable
fun WhiteboardScreen(
    strings: ZeluStrings,
    solverState: SolverUiState,
    followUpState: FollowUpUiState,
    onSolveBoardBitmap: (Bitmap, String) -> Unit,
    onToggleBookmark: (SolvedQuestion) -> Unit,
    onListenVoice: (SolvedQuestion) -> Unit,
    onAskFollowUp: (SolvedQuestion, Int?, String) -> Unit
) {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<BoardItem>() }
    var activeTool by remember { mutableStateOf(BoardTool.PEN) }
    var activeColor by remember { mutableStateOf(CyanBright) }
    var showGrid by rememberSaveable { mutableStateOf(true) }
    var graphInput by rememberSaveable { mutableStateOf("x^2 - 3") }
    val plottedGraphs = remember { mutableStateListOf<Pair<String, Color>>() }
    var canvasSize by remember { mutableStateOf(IntSize(900, 700)) }
    var saveBanner by remember { mutableStateOf<String?>(null) }

    val boardBg = MathWhiteboardDefaults.boardBackground

    val boardArea: @Composable (Modifier) -> Unit = { mod ->
        Column(modifier = mod, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Function Graph Plotter Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = graphInput,
                    onValueChange = { graphInput = it },
                    placeholder = { Text(strings.graphFormulaPlaceholder) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("board_graph_input")
                )
                Button(
                    onClick = {
                        if (graphInput.isNotBlank()) {
                            showGrid = true
                            plottedGraphs.add(graphInput.trim() to activeColor)
                        }
                    },
                    modifier = Modifier.testTag("board_plot_button")
                ) {
                    Icon(Icons.Filled.Functions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.plotGraph)
                }
            }

            MathWhiteboardCanvas(
                strings = strings,
                items = items,
                activeTool = activeTool,
                onToolChange = { activeTool = it },
                activeColor = activeColor,
                onColorChange = { activeColor = it },
                showGrid = showGrid,
                onToggleGrid = { showGrid = !showGrid },
                plottedGraphs = plottedGraphs,
                onClearAll = {
                    items.clear()
                    plottedGraphs.clear()
                    saveBanner = null
                },
                onCanvasSizeChanged = { canvasSize = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Export & Solve with AI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val bmp = renderBoardToBitmap(
                            width = canvasSize.width.coerceAtLeast(800),
                            height = canvasSize.height.coerceAtLeast(600),
                            bgColor = boardBg,
                            showGrid = showGrid,
                            graphs = plottedGraphs.toList(),
                            items = items.toList()
                        )
                        val saved = saveBitmapToPictures(context, bmp)
                        saveBanner = if (saved) strings.imageSavedSuccess else "Unable to save PNG image."
                    },
                    modifier = Modifier.weight(1f).testTag("board_save_png_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.downloadImage)
                }

                Button(
                    onClick = {
                        val bmp = renderBoardToBitmap(
                            width = canvasSize.width.coerceAtLeast(800),
                            height = canvasSize.height.coerceAtLeast(600),
                            bgColor = boardBg,
                            showGrid = showGrid,
                            graphs = plottedGraphs.toList(),
                            items = items.toList()
                        )
                        val graphHint = if (plottedGraphs.isNotEmpty()) {
                            "Plotted functions: ${plottedGraphs.joinToString { it.first }}"
                        } else ""
                        onSolveBoardBitmap(bmp, graphHint)
                    },
                    enabled = solverState !is SolverUiState.Solving,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.weight(1.3f).testTag("board_solve_ai_button")
                ) {
                    if (solverState is SolverUiState.Solving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.solveBoardWithAi, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (saveBanner != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(saveBanner.orEmpty(), modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    val solutionArea: @Composable (Modifier) -> Unit = { mod ->
        Column(modifier = mod, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (solverState) {
                is SolverUiState.Solved -> {
                    MathSolutionCard(
                        question = solverState.question,
                        strings = strings,
                        uncertaintyNote = solverState.uncertaintyNote,
                        followUpState = followUpState,
                        onToggleBookmark = { onToggleBookmark(solverState.question) },
                        onListenVoice = { onListenVoice(solverState.question) },
                        onAskFollowUp = { stepIdx, text -> onAskFollowUp(solverState.question, stepIdx, text) }
                    )
                }

                is SolverUiState.Error -> {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                        Text("${strings.errorPrefix}: ${solverState.message}", modifier = Modifier.padding(14.dp))
                    }
                }

                else -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AsyncImage(
                                model = R.drawable.img_whiteboard_art,
                                contentDescription = strings.whiteboardTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Text(strings.whiteboardTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Draw equations, geometry diagrams, or plot f(x) graphs, then tap \"${strings.solveBoardWithAi}\" to view step-by-step teaching alongside the board.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                boardArea(Modifier.weight(1.1f).verticalScroll(rememberScrollState()))
                solutionArea(Modifier.weight(0.9f).verticalScroll(rememberScrollState()))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                boardArea(Modifier.fillMaxWidth())
                solutionArea(Modifier.fillMaxWidth())
            }
        }
    }
}

private fun saveBitmapToPictures(context: Context, bitmap: Bitmap): Boolean {
    return try {
        val fileName = "zelu_board_${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ZELU_Math_AI")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ZELU_Math_AI")
            dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        true
    } catch (_: Exception) {
        false
    }
}
