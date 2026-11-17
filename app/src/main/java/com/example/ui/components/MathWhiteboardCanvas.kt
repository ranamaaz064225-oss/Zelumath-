package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.localization.ZeluStrings
import com.example.math.MathEvaluator
import com.example.ui.theme.CyanBright
import com.example.ui.theme.DeepNavy900
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.RoyalPurpleBright
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WarningAmber
import kotlin.math.abs
import kotlin.math.hypot

enum class BoardTool { PEN, ERASER, LINE, RECT, CIRCLE, TRIANGLE }

sealed interface BoardItem {
    data class Freehand(
        val points: List<Offset>,
        val color: Color,
        val strokeWidth: Float
    ) : BoardItem

    data class Shape(
        val tool: BoardTool,
        val start: Offset,
        val end: Offset,
        val color: Color,
        val strokeWidth: Float
    ) : BoardItem
}

@Immutable
data class BoardColorSwatch(
    val id: String,
    val label: String,
    val color: Color
)

object MathWhiteboardDefaults {
    val boardBackground: Color = DeepNavy900
    val defaultColors: List<BoardColorSwatch> = listOf(
        BoardColorSwatch("cyan", "Cyan", CyanBright),
        BoardColorSwatch("white", "White Chalk", Color.White),
        BoardColorSwatch("blue", "Electric Blue", ElectricBlueBright),
        BoardColorSwatch("purple", "Violet", RoyalPurpleBright),
        BoardColorSwatch("amber", "Amber", WarningAmber),
        BoardColorSwatch("emerald", "Emerald", VerifiedEmerald)
    )
    val strokeSizes: List<Pair<String, Float>> = listOf(
        "Fine" to 5f,
        "Medium" to 8f,
        "Bold" to 13f
    )
}

/**
 * Interactive Canvas-based Whiteboard component for finger-drawn math equations and diagrams.
 * Consumes pointer drag gestures directly so parent scrollable layouts never steal vertical strokes.
 */
@Composable
fun MathWhiteboardCanvas(
    strings: ZeluStrings,
    items: MutableList<BoardItem>,
    activeTool: BoardTool,
    onToolChange: (BoardTool) -> Unit,
    activeColor: Color,
    onColorChange: (Color) -> Unit,
    showGrid: Boolean,
    onToggleGrid: () -> Unit,
    plottedGraphs: List<Pair<String, Color>>,
    onClearAll: () -> Unit,
    onCanvasSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier
) {
    val boardBg = MathWhiteboardDefaults.boardBackground
    var strokeWidth by remember { mutableFloatStateOf(8f) }
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Drawing Tools Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = activeTool == BoardTool.PEN,
                onClick = { onToolChange(BoardTool.PEN) },
                label = { Text(strings.toolPen) },
                leadingIcon = { Icon(Icons.Filled.Draw, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("board_tool_pen")
            )
            FilterChip(
                selected = activeTool == BoardTool.ERASER,
                onClick = { onToolChange(BoardTool.ERASER) },
                label = { Text(strings.toolEraser) },
                leadingIcon = { Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("board_tool_eraser")
            )
            FilterChip(
                selected = activeTool == BoardTool.LINE,
                onClick = { onToolChange(BoardTool.LINE) },
                label = { Text(strings.toolLine) },
                leadingIcon = { Icon(Icons.Filled.HorizontalRule, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = activeTool == BoardTool.RECT,
                onClick = { onToolChange(BoardTool.RECT) },
                label = { Text(strings.toolRect) },
                leadingIcon = { Icon(Icons.Filled.CropSquare, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = activeTool == BoardTool.CIRCLE,
                onClick = { onToolChange(BoardTool.CIRCLE) },
                label = { Text(strings.toolCircle) },
                leadingIcon = { Icon(Icons.Filled.Circle, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = activeTool == BoardTool.TRIANGLE,
                onClick = { onToolChange(BoardTool.TRIANGLE) },
                label = { Text(strings.toolTriangle) },
                leadingIcon = { Icon(Icons.Filled.Timeline, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = showGrid,
                onClick = onToggleGrid,
                label = { Text(strings.toggleGrid) },
                leadingIcon = { Icon(Icons.Filled.GridOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("board_toggle_grid")
            )
        }

        // Ink Color Swatches (48dp touch targets) + Stroke Width Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MathWhiteboardDefaults.defaultColors.forEach { swatch ->
                val selected = activeColor == swatch.color && activeTool != BoardTool.ERASER
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.RadioButton) {
                            onColorChange(swatch.color)
                            if (activeTool == BoardTool.ERASER) onToolChange(BoardTool.PEN)
                        }
                        .semantics { contentDescription = "Ink color ${swatch.label}" }
                        .testTag("board_color_${swatch.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (selected) 30.dp else 24.dp)
                            .clip(CircleShape)
                            .background(swatch.color)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.7f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            val checkTint = if (swatch.color == Color.White) DeepNavy900 else Color.White
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = checkTint,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            MathWhiteboardDefaults.strokeSizes.forEach { (label, widthValue) ->
                FilterChip(
                    selected = strokeWidth == widthValue,
                    onClick = { strokeWidth = widthValue },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("board_stroke_${label.lowercase()}")
                )
            }
        }

        // Undo & Clear Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { if (items.isNotEmpty()) items.removeAt(items.lastIndex) },
                enabled = items.isNotEmpty(),
                modifier = Modifier.testTag("board_undo_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = strings.undo, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.undo)
            }
            OutlinedButton(
                onClick = {
                    activePoints = emptyList()
                    dragStart = null
                    dragEnd = null
                    onClearAll()
                },
                enabled = items.isNotEmpty() || plottedGraphs.isNotEmpty(),
                modifier = Modifier.testTag("board_clear_button")
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = strings.clearBoard, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.clearBoard)
            }
        }

        // Finger Drawing Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(boardBg)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                .onSizeChanged(onCanvasSizeChanged)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .testTag("board_canvas")
                    .semantics { contentDescription = strings.whiteboardTitle }
                    .pointerInput(activeTool, activeColor, strokeWidth) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()

                            fun clampOffset(raw: Offset): Offset = Offset(
                                x = raw.x.coerceIn(0f, size.width.toFloat()),
                                y = raw.y.coerceIn(0f, size.height.toFloat())
                            )

                            val startPos = clampOffset(down.position)
                            val isFreehandTool = activeTool == BoardTool.PEN || activeTool == BoardTool.ERASER

                            if (isFreehandTool) {
                                activePoints = listOf(startPos)
                            } else {
                                dragStart = startPos
                                dragEnd = startPos
                            }

                            val dragSuccess = drag(down.id) { change ->
                                change.consume()
                                val clamped = clampOffset(change.position)
                                if (isFreehandTool) {
                                    val last = activePoints.lastOrNull()
                                    if (last == null || hypot(clamped.x - last.x, clamped.y - last.y) >= 2f) {
                                        activePoints = activePoints + clamped
                                    }
                                } else {
                                    dragEnd = clamped
                                }
                            }

                            if (dragSuccess || activePoints.isNotEmpty() || dragStart != null) {
                                if (isFreehandTool) {
                                    if (activePoints.isNotEmpty()) {
                                        items.add(
                                            BoardItem.Freehand(
                                                points = activePoints,
                                                color = if (activeTool == BoardTool.ERASER) boardBg else activeColor,
                                                strokeWidth = if (activeTool == BoardTool.ERASER) (strokeWidth * 3.2f) else strokeWidth
                                            )
                                        )
                                    }
                                    activePoints = emptyList()
                                } else {
                                    val s = dragStart
                                    val e = dragEnd
                                    if (s != null && e != null) {
                                        items.add(BoardItem.Shape(activeTool, s, e, activeColor, strokeWidth))
                                    }
                                    dragStart = null
                                    dragEnd = null
                                }
                            }
                        }
                    }
            ) {
                if (showGrid) {
                    drawCoordinateGrid()
                }
                plottedGraphs.forEach { (formula, color) ->
                    drawPlottedFunction(formula, color)
                }
                items.forEach { item ->
                    drawBoardItem(item)
                }
                if (activePoints.isNotEmpty()) {
                    drawBoardItem(
                        BoardItem.Freehand(
                            points = activePoints,
                            color = if (activeTool == BoardTool.ERASER) boardBg else activeColor,
                            strokeWidth = if (activeTool == BoardTool.ERASER) (strokeWidth * 3.2f) else strokeWidth
                        )
                    )
                }
                val s = dragStart
                val e = dragEnd
                if (s != null && e != null) {
                    drawBoardItem(BoardItem.Shape(activeTool, s, e, activeColor, strokeWidth))
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = items.isEmpty() && activePoints.isEmpty() && plottedGraphs.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Draw equations with your finger (e.g. 2x + 5 = 15)",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCoordinateGrid() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val step = minOf(size.width, size.height) / 14f
    val gridColor = Color(0xFF1E3A63)
    var x = cx % step
    while (x < size.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = cy % step
    while (y < size.height) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
    drawLine(Color(0xFF64748B), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 2.5f)
    drawLine(Color(0xFF64748B), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 2.5f)
}

private fun DrawScope.drawPlottedFunction(formula: String, color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val scale = minOf(size.width, size.height) / 14f
    val path = Path()
    var started = false
    val samples = 180
    for (i in 0..samples) {
        val px = (i.toFloat() / samples) * size.width
        val mathX = ((px - cx) / scale).toDouble()
        val mathY = MathEvaluator.evaluate(formula, mapOf("x" to mathX))
        if (mathY != null && abs(mathY) < 50.0) {
            val py = cy - (mathY.toFloat() * scale)
            if (!started) {
                path.moveTo(px, py)
                started = true
            } else {
                path.lineTo(px, py)
            }
        } else {
            started = false
        }
    }
    drawPath(path, color = color, style = Stroke(width = 4f, cap = StrokeCap.Round))
}

private fun buildSmoothComposePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }
    for (i in 1 until points.size - 1) {
        val midX = (points[i].x + points[i + 1].x) / 2f
        val midY = (points[i].y + points[i + 1].y) / 2f
        path.quadraticTo(points[i].x, points[i].y, midX, midY)
    }
    val last = points.last()
    path.lineTo(last.x, last.y)
    return path
}

private fun DrawScope.drawBoardItem(item: BoardItem) {
    when (item) {
        is BoardItem.Freehand -> {
            if (item.points.isEmpty()) return
            if (item.points.size == 1) {
                drawCircle(
                    color = item.color,
                    radius = (item.strokeWidth / 2f).coerceAtLeast(2.5f),
                    center = item.points[0]
                )
            } else {
                drawPath(
                    path = buildSmoothComposePath(item.points),
                    color = item.color,
                    style = Stroke(width = item.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        is BoardItem.Shape -> {
            val s = item.start
            val e = item.end
            val stroke = Stroke(width = item.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (item.tool) {
                BoardTool.LINE -> drawLine(item.color, s, e, strokeWidth = item.strokeWidth, cap = StrokeCap.Round)
                BoardTool.RECT -> {
                    val left = minOf(s.x, e.x)
                    val top = minOf(s.y, e.y)
                    drawRect(
                        color = item.color,
                        topLeft = Offset(left, top),
                        size = Size(abs(e.x - s.x), abs(e.y - s.y)),
                        style = stroke
                    )
                }

                BoardTool.CIRCLE -> {
                    val r = hypot(e.x - s.x, e.y - s.y)
                    drawCircle(color = item.color, radius = r, center = s, style = stroke)
                }

                BoardTool.TRIANGLE -> {
                    val topPoint = Offset((s.x + e.x) / 2f, minOf(s.y, e.y))
                    val leftPoint = Offset(minOf(s.x, e.x), maxOf(s.y, e.y))
                    val rightPoint = Offset(maxOf(s.x, e.x), maxOf(s.y, e.y))
                    val triPath = Path().apply {
                        moveTo(topPoint.x, topPoint.y)
                        lineTo(leftPoint.x, leftPoint.y)
                        lineTo(rightPoint.x, rightPoint.y)
                        close()
                    }
                    drawPath(triPath, color = item.color, style = stroke)
                }

                else -> Unit
            }
        }
    }
}

fun renderBoardToBitmap(
    width: Int,
    height: Int,
    bgColor: Color,
    showGrid: Boolean,
    graphs: List<Pair<String, Color>>,
    items: List<BoardItem>
): Bitmap {
    val safeW = width.coerceAtLeast(100)
    val safeH = height.coerceAtLeast(100)
    val bmp = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    canvas.drawColor(bgColor.toArgb())

    if (showGrid) {
        val gridPaint = AndroidPaint().apply {
            color = android.graphics.Color.parseColor("#1E3A63")
            strokeWidth = 1.5f
        }
        val cx = safeW / 2f
        val cy = safeH / 2f
        val step = minOf(safeW, safeH) / 14f
        var x = cx % step
        while (x < safeW) {
            canvas.drawLine(x, 0f, x, safeH.toFloat(), gridPaint)
            x += step
        }
        var y = cy % step
        while (y < safeH) {
            canvas.drawLine(0f, y, safeW.toFloat(), y, gridPaint)
            y += step
        }
        val axisPaint = AndroidPaint().apply {
            color = android.graphics.Color.parseColor("#64748B")
            strokeWidth = 3f
        }
        canvas.drawLine(0f, cy, safeW.toFloat(), cy, axisPaint)
        canvas.drawLine(cx, 0f, cx, safeH.toFloat(), axisPaint)
    }

    graphs.forEach { (formula, color) ->
        val cx = safeW / 2f
        val cy = safeH / 2f
        val scale = minOf(safeW, safeH) / 14f
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            strokeWidth = 4f
            style = AndroidPaint.Style.STROKE
        }
        val path = AndroidPath()
        var started = false
        for (i in 0..180) {
            val px = (i.toFloat() / 180f) * safeW
            val mx = ((px - cx) / scale).toDouble()
            val my = MathEvaluator.evaluate(formula, mapOf("x" to mx))
            if (my != null && abs(my) < 50.0) {
                val py = cy - (my.toFloat() * scale)
                if (!started) {
                    path.moveTo(px, py)
                    started = true
                } else {
                    path.lineTo(px, py)
                }
            } else {
                started = false
            }
        }
        canvas.drawPath(path, paint)
    }

    items.forEach { item ->
        when (item) {
            is BoardItem.Freehand -> {
                if (item.points.isEmpty()) return@forEach
                if (item.points.size == 1) {
                    val dotPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                        color = item.color.toArgb()
                        style = AndroidPaint.Style.FILL
                    }
                    canvas.drawCircle(
                        item.points[0].x,
                        item.points[0].y,
                        (item.strokeWidth / 2f).coerceAtLeast(2.5f),
                        dotPaint
                    )
                } else {
                    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                        color = item.color.toArgb()
                        strokeWidth = item.strokeWidth
                        style = AndroidPaint.Style.STROKE
                        strokeCap = AndroidPaint.Cap.ROUND
                        strokeJoin = AndroidPaint.Join.ROUND
                    }
                    val path = AndroidPath()
                    path.moveTo(item.points[0].x, item.points[0].y)
                    if (item.points.size == 2) {
                        path.lineTo(item.points[1].x, item.points[1].y)
                    } else {
                        for (i in 1 until item.points.size - 1) {
                            val midX = (item.points[i].x + item.points[i + 1].x) / 2f
                            val midY = (item.points[i].y + item.points[i + 1].y) / 2f
                            path.quadTo(item.points[i].x, item.points[i].y, midX, midY)
                        }
                        val last = item.points.last()
                        path.lineTo(last.x, last.y)
                    }
                    canvas.drawPath(path, paint)
                }
            }

            is BoardItem.Shape -> {
                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = item.color.toArgb()
                    strokeWidth = item.strokeWidth
                    style = AndroidPaint.Style.STROKE
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                }
                val s = item.start
                val e = item.end
                when (item.tool) {
                    BoardTool.LINE -> canvas.drawLine(s.x, s.y, e.x, e.y, paint)
                    BoardTool.RECT -> canvas.drawRect(minOf(s.x, e.x), minOf(s.y, e.y), maxOf(s.x, e.x), maxOf(s.y, e.y), paint)
                    BoardTool.CIRCLE -> canvas.drawCircle(s.x, s.y, hypot(e.x - s.x, e.y - s.y), paint)
                    BoardTool.TRIANGLE -> {
                        val path = AndroidPath().apply {
                            moveTo((s.x + e.x) / 2f, minOf(s.y, e.y))
                            lineTo(minOf(s.x, e.x), maxOf(s.y, e.y))
                            lineTo(maxOf(s.x, e.x), maxOf(s.y, e.y))
                            close()
                        }
                        canvas.drawPath(path, paint)
                    }

                    else -> Unit
                }
            }
        }
    }
    return bmp
}
