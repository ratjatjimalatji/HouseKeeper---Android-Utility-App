package de.yanneckreiss.mlkittutorial.gas.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.yanneckreiss.mlkittutorial.gas.model.GasCylinder
import kotlinx.coroutines.delay

/**
 * A modern, beautifully rendered propane gas cylinder composable.
 * Displays dynamic colors, realistic 3D cylindrical specular highlights,
 * authentic propane valve & shroud collar, and dynamically adjusts its dimensions
 * according to the selected size (4kg, 9kg, 14kg, 48kg).
 * Supports [isPlaceholderNew] to display a greyed-out default gas cylinder with dashed border lines.
 */
@Composable
fun PropaneGasCylinderComposable(
    cylinder: GasCylinder,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    isPlaceholderNew: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    // Smooth animated color transition; use greyed-out slate color if placeholder for a new cylinder
    val targetColor = if (isPlaceholderNew) Color(0xFF90A4AE) else cylinder.color
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 450),
        label = "cylinderColor"
    )

    // Dynamic sizing based on weight
    val targetHeight: Dp = when (cylinder.sizeKg) {
        4 -> 190.dp
        9 -> 225.dp
        14 -> 265.dp
        48 -> 305.dp
        else -> 225.dp
    }

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cylinderHeight"
    )

    var isTapped by remember { mutableStateOf(false) }
    val tapScale by animateFloatAsState(
        targetValue = if (isTapped) 0.96f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tapScale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .scale(tapScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isInteractive
            ) {
                isTapped = true
                onClick?.invoke()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LaunchedEffect(isTapped) {
            if (isTapped) {
                delay(120)
                isTapped = false
            }
        }

        // Propane Cylinder Visual Drawing
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(animatedHeight),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawPropaneCylinder(
                    baseColor = animatedColor,
                    sizeKg = cylinder.sizeKg,
                    isPlaceholderNew = isPlaceholderNew
                )
            }

            // Embedded Stencil Label on the cylinder body
            CylinderDecalLabel(
                sizeKg = cylinder.sizeKg,
                isPlaceholderNew = isPlaceholderNew,
                modifier = Modifier
                    .padding(bottom = (animatedHeight.value * 0.28f).dp)
                    .width(105.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Floor Shadow
        Canvas(
            modifier = Modifier
                .width(130.dp)
                .height(14.dp)
        ) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = if (isPlaceholderNew) 0.15f else 0.28f),
                        Color.Black.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width / 2f
                )
            )
        }
    }
}

/**
 * Custom Canvas drawing routine for the realistic Propane Cylinder structure.
 */
private fun DrawScope.drawPropaneCylinder(
    baseColor: Color,
    sizeKg: Int,
    isPlaceholderNew: Boolean
) {
    val totalWidth = size.width
    val totalHeight = size.height

    // Calculate proportional regions
    val bodyWidth = totalWidth * 0.78f
    val bodyLeft = (totalWidth - bodyWidth) / 2f
    val bodyRight = bodyLeft + bodyWidth

    val collarHeight = totalHeight * 0.16f
    val valveHeight = collarHeight * 0.65f
    val domeHeight = totalHeight * 0.12f
    val footRingHeight = totalHeight * 0.09f

    val bodyTop = collarHeight + domeHeight * 0.65f
    val bodyBottom = totalHeight - footRingHeight - (domeHeight * 0.65f)
    val bodyHeight = bodyBottom - bodyTop

    // Derive lighting shades from base color
    val darkShade = baseColor.darken(if (isPlaceholderNew) 0.20f else 0.35f)
    val lightHighlight = baseColor.lighten(if (isPlaceholderNew) 0.20f else 0.35f)
    val specularWhite = Color.White.copy(alpha = if (isPlaceholderNew) 0.25f else 0.45f)

    // 3D Cylindrical lighting brush
    val cylinderBrush = Brush.horizontalGradient(
        0.00f to darkShade,
        0.18f to baseColor,
        0.35f to lightHighlight,
        0.55f to baseColor,
        0.85f to darkShade.darken(0.15f),
        1.00f to darkShade.darken(0.3f),
        startX = bodyLeft,
        endX = bodyRight
    )

    // Specular shine strip
    val shineBrush = Brush.horizontalGradient(
        0.00f to Color.Transparent,
        0.40f to specularWhite,
        1.00f to Color.Transparent,
        startX = bodyLeft + bodyWidth * 0.28f,
        endX = bodyLeft + bodyWidth * 0.45f
    )

    // ==========================================
    // 1. FOOT RING / BASE STAND
    // ==========================================
    val footRingWidth = when (sizeKg) {
        4 -> bodyWidth * 0.90f
        48 -> bodyWidth * 0.82f
        else -> bodyWidth * 0.85f
    }
    val footRingLeft = (totalWidth - footRingWidth) / 2f
    val footRingTop = totalHeight - footRingHeight

    drawRoundRect(
        color = darkShade.darken(0.4f),
        topLeft = Offset(footRingLeft, footRingTop),
        size = Size(footRingWidth, footRingHeight),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )

    drawRoundRect(
        brush = cylinderBrush,
        topLeft = Offset(footRingLeft + 2.dp.toPx(), footRingTop + 1.dp.toPx()),
        size = Size(footRingWidth - 4.dp.toPx(), footRingHeight - 2.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )

    val slotWidth = footRingWidth * 0.25f
    val slotHeight = footRingHeight * 0.4f
    drawRoundRect(
        color = Color(0x33000000),
        topLeft = Offset((totalWidth - slotWidth) / 2f, footRingTop + (footRingHeight - slotHeight) / 2f),
        size = Size(slotWidth, slotHeight),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )

    // ==========================================
    // 2. LOWER DOME
    // ==========================================
    val lowerDomeTop = bodyBottom - (domeHeight * 0.4f)
    drawRoundRect(
        brush = cylinderBrush,
        topLeft = Offset(bodyLeft, lowerDomeTop),
        size = Size(bodyWidth, domeHeight * 1.1f),
        cornerRadius = CornerRadius(bodyWidth * 0.45f, bodyWidth * 0.45f)
    )

    // ==========================================
    // 3. MAIN CYLINDRICAL TANK BODY
    // ==========================================
    drawRect(
        brush = cylinderBrush,
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyWidth, bodyHeight)
    )

    drawRect(
        brush = shineBrush,
        topLeft = Offset(bodyLeft + bodyWidth * 0.28f, bodyTop),
        size = Size(bodyWidth * 0.17f, bodyHeight)
    )

    // Circumferential Welded Seam Bead
    val weldY = bodyTop + bodyHeight * 0.52f
    drawLine(
        color = darkShade.darken(0.3f),
        start = Offset(bodyLeft, weldY),
        end = Offset(bodyRight, weldY),
        strokeWidth = 2.dp.toPx()
    )
    drawLine(
        color = lightHighlight.copy(alpha = 0.5f),
        start = Offset(bodyLeft, weldY + 2.dp.toPx()),
        end = Offset(bodyRight, weldY + 2.dp.toPx()),
        strokeWidth = 1.2.dp.toPx()
    )

    val topWeldY = bodyTop + 4.dp.toPx()
    drawLine(
        color = darkShade.darken(0.2f),
        start = Offset(bodyLeft, topWeldY),
        end = Offset(bodyRight, topWeldY),
        strokeWidth = 1.5.dp.toPx()
    )

    // ==========================================
    // 4. UPPER DOME
    // ==========================================
    drawRoundRect(
        brush = cylinderBrush,
        topLeft = Offset(bodyLeft, collarHeight),
        size = Size(bodyWidth, domeHeight * 1.3f),
        cornerRadius = CornerRadius(bodyWidth * 0.45f, bodyWidth * 0.45f)
    )

    drawRoundRect(
        brush = shineBrush,
        topLeft = Offset(bodyLeft + bodyWidth * 0.28f, collarHeight),
        size = Size(bodyWidth * 0.17f, domeHeight * 1.3f),
        cornerRadius = CornerRadius(bodyWidth * 0.45f, bodyWidth * 0.45f)
    )

    // ==========================================
    // 5. BRASS VALVE & REGULATOR OUTLET
    // ==========================================
    val valveCenterX = totalWidth / 2f
    val valveBaseY = collarHeight + (domeHeight * 0.35f)
    val valveTopY = valveBaseY - valveHeight

    val brassColor = if (isPlaceholderNew) Color(0xFFB0BEC5) else Color(0xFFD4AF37)
    val brassDark = if (isPlaceholderNew) Color(0xFF78909C) else Color(0xFF997A15)
    val brassBrush = Brush.horizontalGradient(
        listOf(brassDark, brassColor, brassDark),
        startX = valveCenterX - 10.dp.toPx(),
        endX = valveCenterX + 10.dp.toPx()
    )

    drawRoundRect(
        brush = brassBrush,
        topLeft = Offset(valveCenterX - 6.dp.toPx(), valveTopY + 8.dp.toPx()),
        size = Size(12.dp.toPx(), valveHeight - 6.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    drawRoundRect(
        brush = brassBrush,
        topLeft = Offset(valveCenterX + 3.dp.toPx(), valveTopY + 12.dp.toPx()),
        size = Size(10.dp.toPx(), 7.dp.toPx()),
        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
    )

    drawCircle(
        color = Color(0xFF333333),
        center = Offset(valveCenterX + 13.dp.toPx(), valveTopY + 15.5.dp.toPx()),
        radius = 2.dp.toPx()
    )

    val knobWidth = 24.dp.toPx()
    val knobHeight = 7.dp.toPx()
    drawRoundRect(
        color = if (isPlaceholderNew) Color(0xFF546E7A) else Color(0xFF1E1E1E),
        topLeft = Offset(valveCenterX - knobWidth / 2f, valveTopY),
        size = Size(knobWidth, knobHeight),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )

    for (i in 0..4) {
        val lineX = (valveCenterX - knobWidth / 2f) + (knobWidth / 5f) * i + 2.dp.toPx()
        drawLine(
            color = Color(0xFF424242),
            start = Offset(lineX, valveTopY + 1.dp.toPx()),
            end = Offset(lineX, valveTopY + knobHeight - 1.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }

    // ==========================================
    // 6. TOP PROTECTIVE SHROUD / COLLAR HANDLE
    // ==========================================
    val collarWidth = bodyWidth * 0.72f
    val collarLeft = (totalWidth - collarWidth) / 2f
    val collarTop = 4.dp.toPx()

    val shroudPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = collarLeft,
                top = collarTop,
                right = collarLeft + collarWidth,
                bottom = collarTop + collarHeight + 6.dp.toPx(),
                topLeftCornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                topRightCornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                bottomLeftCornerRadius = CornerRadius(0f, 0f),
                bottomRightCornerRadius = CornerRadius(0f, 0f)
            )
        )
    }

    drawPath(
        path = shroudPath,
        brush = Brush.horizontalGradient(
            0.0f to darkShade,
            0.3f to lightHighlight,
            0.6f to baseColor,
            1.0f to darkShade,
            startX = collarLeft,
            endX = collarLeft + collarWidth
        )
    )

    drawRoundRect(
        color = lightHighlight.copy(alpha = 0.7f),
        topLeft = Offset(collarLeft, collarTop),
        size = Size(collarWidth, 3.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    val handleSlotWidth = collarWidth * 0.28f
    val handleSlotHeight = collarHeight * 0.40f
    val handleSlotTop = collarTop + collarHeight * 0.32f

    drawRoundRect(
        color = Color(0x3D000000),
        topLeft = Offset(collarLeft + 5.dp.toPx(), handleSlotTop),
        size = Size(handleSlotWidth, handleSlotHeight),
        cornerRadius = CornerRadius(handleSlotHeight / 2f, handleSlotHeight / 2f)
    )

    drawRoundRect(
        color = Color(0x3D000000),
        topLeft = Offset(collarLeft + collarWidth - handleSlotWidth - 5.dp.toPx(), handleSlotTop),
        size = Size(handleSlotWidth, handleSlotHeight),
        cornerRadius = CornerRadius(handleSlotHeight / 2f, handleSlotHeight / 2f)
    )

    // ==========================================
    // 7. DASHED BORDER OVERLAY FOR NEW CYLINDER
    // ==========================================
    if (isPlaceholderNew) {
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        val outlinePath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = bodyLeft - 3.dp.toPx(),
                    top = collarTop - 2.dp.toPx(),
                    right = bodyRight + 3.dp.toPx(),
                    bottom = totalHeight - 2.dp.toPx(),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
            )
        }

        drawPath(
            path = outlinePath,
            color = Color(0xFF1976D2), // Accent blue dash border
            style = Stroke(
                width = 2.5.dp.toPx(),
                pathEffect = dashEffect
            )
        )
    }
}

/**
 * Modern stencil label and hazard badge on the cylinder tank.
 */
@Composable
private fun CylinderDecalLabel(
    sizeKg: Int,
    isPlaceholderNew: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPlaceholderNew) {
        Surface(
            modifier = modifier,
            color = Color(0xFFE3F2FD),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.5.dp, Color(0xFF1976D2)),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "+ NEW CYLINDER",
                    color = Color(0xFF1565C0),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "POPULATE",
                    color = Color(0xFF0D47A1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
            }
        }
    } else {
        Surface(
            modifier = modifier,
            color = Color.White.copy(alpha = 0.92f),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CYLINDER SIZE",
                        color = Color(0xFFC62828),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "$sizeKg kg",
                    color = Color(0xFF212121),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// Color lightening & darkening helper extensions
private fun Color.darken(factor: Float): Color {
    return Color(
        red = (red * (1f - factor)).coerceIn(0f, 1f),
        green = (green * (1f - factor)).coerceIn(0f, 1f),
        blue = (blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private fun Color.lighten(factor: Float): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}
