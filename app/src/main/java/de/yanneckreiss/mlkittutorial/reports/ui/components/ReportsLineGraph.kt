package de.yanneckreiss.mlkittutorial.reports.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.yanneckreiss.mlkittutorial.entry.data.local.UtilityType
import de.yanneckreiss.mlkittutorial.reports.domain.model.ReportGraphPoint
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Line graph displaying solid dots for actual logged meter readings
 * and dotted/dashed line segments showing usage trends between logged readings.
 */
@SuppressLint("ComposeModifierReused")
@Composable
fun ReportsLineGraph(
    graphPoints: List<ReportGraphPoint>,
    utilityType: UtilityType,
    modifier: Modifier = Modifier
) {
    if (graphPoints.isEmpty()) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No report data for selected month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val animProgress = remember(graphPoints, utilityType) { Animatable(0f) }
    LaunchedEffect(graphPoints, utilityType) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 600))
    }

    var selectedIndex by remember(graphPoints) { mutableStateOf<Int?>(null) }
    val primaryColor = utilityType.composeColor
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val values = graphPoints.map { it.value }
    val maxValue = remember(values) { (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0) * 1.18 }
    val minValue = 0.0
    val avgValue = remember(values) { if (values.isNotEmpty()) values.average() else 0.0 }

    val activePoint = selectedIndex?.let { graphPoints.getOrNull(it) } ?: graphPoints.lastOrNull { it.isLoggedReading } ?: graphPoints.lastOrNull()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Title & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${utilityType.displayName} Usage Trends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                    Text(
                        text = "Logged readings & trend projections",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant
                    )
                }

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logged Dot Legend
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                        )
                        Text(
                            text = "Logged Reading",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Dotted Line Legend
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Canvas(modifier = Modifier.size(16.dp, 8.dp)) {
                            drawLine(
                                color = primaryColor,
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                            )
                        }
                        Text(
                            text = "Dotted Trend",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Active Point Inspection Tooltip
            if (activePoint != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = activePoint.dayLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurface
                                )

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (activePoint.isLoggedReading) primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (activePoint.isLoggedReading) "Logged Reading" else if (activePoint.isProjected) "Future Projection" else "Dotted Trend",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activePoint.isLoggedReading) primaryColor else onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", activePoint.value)} ${utilityType.defaultUnit}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = primaryColor
                                )

                                if (activePoint.meterReading != null) {
                                    Text(
                                        text = "(Meter: ${String.format(Locale.US, "%.0f", activePoint.meterReading)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (activePoint.notes.isNotBlank()) {
                            Text(
                                text = activePoint.notes,
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Canvas Drawing Line Graph (Solid Dots & Dotted Trend Lines)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(graphPoints) {
                        detectTapGestures { offset ->
                            val pointSpacing = size.width / (graphPoints.size - 1).coerceAtLeast(1)
                            val index = (offset.x / pointSpacing).roundToInt().coerceIn(0, graphPoints.size - 1)
                            selectedIndex = index
                        }
                    }
                    .pointerInput(graphPoints) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val pointSpacing = size.width / (graphPoints.size - 1).coerceAtLeast(1)
                            val index = (change.position.x / pointSpacing).roundToInt().coerceIn(0, graphPoints.size - 1)
                            selectedIndex = index
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val paddingBottom = 20.dp.toPx()
                    val chartHeight = height - paddingBottom
                    val pointCount = graphPoints.size

                    if (pointCount < 2) return@Canvas

                    val stepX = width / (pointCount - 1)

                    // 1. Grid Lines
                    val gridLines = listOf(0.0, maxValue * 0.5, maxValue)
                    for (gridVal in gridLines) {
                        val y = (chartHeight - ((gridVal / maxValue) * chartHeight)).toFloat()
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                    }

                    // 2. Average Baseline Line
                    val avgY = (chartHeight - ((avgValue / maxValue) * chartHeight)).toFloat()
                    drawLine(
                        color = primaryColor.copy(alpha = 0.35f),
                        start = Offset(0f, avgY),
                        end = Offset(width, avgY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
                    )

                    // 3. Compute Coordinates
                    val points = graphPoints.mapIndexed { index, dp ->
                        val x = index * stepX
                        val normalizedY = ((dp.value - minValue) / (maxValue - minValue)).toFloat()
                        val targetY = chartHeight - (normalizedY * chartHeight)
                        val animatedY = chartHeight - ((chartHeight - targetY) * animProgress.value)
                        Offset(x, animatedY)
                    }

                    // 4. Construct Solid vs Dotted Line Paths
                    val solidPath = Path()
                    val dottedPath = Path()
                    val fillPath = Path()

                    fillPath.moveTo(points.first().x, chartHeight)
                    fillPath.lineTo(points.first().x, points.first().y)

                    var inSolidSegment = false

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val dp0 = graphPoints[i]
                        val dp1 = graphPoints[i + 1]

                        val isLoggedSegment = dp0.isLoggedReading && dp1.isLoggedReading

                        if (isLoggedSegment) {
                            if (!inSolidSegment) {
                                solidPath.moveTo(p0.x, p0.y)
                                inSolidSegment = true
                            }
                            solidPath.lineTo(p1.x, p1.y)
                        } else {
                            inSolidSegment = false
                            dottedPath.moveTo(p0.x, p0.y)
                            dottedPath.lineTo(p1.x, p1.y)
                        }

                        fillPath.lineTo(p1.x, p1.y)
                    }

                    fillPath.lineTo(points.last().x, chartHeight)
                    fillPath.close()

                    // Draw Gradient Fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.20f),
                                primaryColor.copy(alpha = 0.02f)
                            ),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )

                    // Draw Dotted/Dashed Line Path for Trend Segments
                    drawPath(
                        path = dottedPath,
                        color = primaryColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        )
                    )

                    // Draw Solid Line Path for Logged Segments
                    drawPath(
                        path = solidPath,
                        color = primaryColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // 5. Draw Logged Reading Solid Dots vs Trend Points
                    points.forEachIndexed { i, pt ->
                        val dp = graphPoints[i]
                        val isSelected = (selectedIndex == i)

                        if (dp.isLoggedReading) {
                            // SOLID DOT FOR LOGGED READINGS
                            drawCircle(
                                color = primaryColor,
                                radius = if (isSelected) 8.dp.toPx() else 6.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = if (isSelected) 3.5.dp.toPx() else 2.5.dp.toPx(),
                                center = pt
                            )
                        } else if (isSelected) {
                            drawCircle(
                                color = primaryColor,
                                radius = 5.dp.toPx(),
                                center = pt
                            )
                        } else {
                            // Small subtle dot on trend line
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.4f),
                                radius = 2.dp.toPx(),
                                center = pt
                            )
                        }
                    }

                    // Scrubbing Cursor Line
                    selectedIndex?.let { idx ->
                        val selectedPt = points.getOrNull(idx)
                        if (selectedPt != null) {
                            drawLine(
                                color = onSurface.copy(alpha = 0.5f),
                                start = Offset(selectedPt.x, 0f),
                                end = Offset(selectedPt.x, chartHeight),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            )
                        }
                    }
                }
            }

            // X-Axis Date Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val firstLabel = graphPoints.firstOrNull()?.dayLabel ?: ""
                val midLabel = graphPoints.getOrNull(graphPoints.size / 2)?.dayLabel ?: ""
                val lastLabel = graphPoints.lastOrNull()?.dayLabel ?: ""

                Text(text = firstLabel, style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                if (graphPoints.size > 2) {
                    Text(text = midLabel, style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                }
                Text(text = lastLabel, style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
            }
        }
    }
}
