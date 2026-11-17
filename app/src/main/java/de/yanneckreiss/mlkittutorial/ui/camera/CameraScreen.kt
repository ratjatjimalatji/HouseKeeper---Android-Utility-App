package de.yanneckreiss.mlkittutorial.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import de.yanneckreiss.mlkittutorial.entry.data.TaskViewModel.EntryViewModel
import de.yanneckreiss.mlkittutorial.entry.data.local.UtilityType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import de.yanneckreiss.mlkittutorial.ui.no_permission.NoPermissionScreen

// Color Palette
private val PanelColor = Color(0xFF14171C)
private val PanelMuted = Color(0xFF9AA3AF)
private val AccentGreen = Color(0xFF52C41A)
private val ScrimColor = Color.Black.copy(alpha = 0.55f)

@SuppressLint("ComposeViewModelForwarding")
@OptIn(ExperimentalPermissionsApi::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MainScreen(
    viewModel: EntryViewModel,
    utilityType: UtilityType = UtilityType.ELECTRICITY,
    onNavigateBack: () -> Unit
) {
    val cameraPermissionState: PermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    MainContent(
        hasPermission = cameraPermissionState.status.isGranted,
        onRequestPermission = cameraPermissionState::launchPermissionRequest,
        viewModel = viewModel,
        utilityType = utilityType,
        onNavigateBack = onNavigateBack
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun MainContent(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    viewModel: EntryViewModel,
    utilityType: UtilityType,
    onNavigateBack: () -> Unit
) {
    if (hasPermission) {
        CameraScreen(viewModel = viewModel, utilityType = utilityType, onNavigateBack = onNavigateBack)
    } else {
        NoPermissionScreen(onRequestPermission)
    }
}

@SuppressLint("ComposeModifierMissing")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun CameraScreen(
    viewModel: EntryViewModel = hiltViewModel(),
    utilityType: UtilityType = UtilityType.ELECTRICITY,
    onNavigateBack: () -> Unit
) {
    val context: Context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController: LifecycleCameraController = remember { LifecycleCameraController(context) }
    var detectedText: String by remember { mutableStateOf("Scanning meter reading..") }

    val extractedValue = remember(detectedText, utilityType) {
        MeterReadingParser.extract(detectedText, utilityType)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Scan ${utilityType.displayName} Meter", fontWeight = FontWeight.SemiBold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PanelColor)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.BottomCenter
        ) {
            // 1. Camera Preview & ML Kit Analyzer
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(AndroidColor.BLACK)
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_START
                    }.also { previewView ->
                        startTextRecognition(
                            context = ctx,
                            cameraController = cameraController,
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView,
                            onDetectedTextUpdated = { detectedText = it }
                        )
                    }
                }
            )

            ReceptiveFieldOverlay(hintText = "Position ${utilityType.displayName.lowercase()} numbers inside frame")

            BottomPanel(
                utilityType = utilityType,
                extractedValue = extractedValue,
                detectedText = detectedText,
                isSaving = isSaving,
                onConfirm = { reading, isManual ->
                    isSaving = true
                    viewModel.addScannedReading(
                        kwhStr = reading,
                        description = if (isManual) "${utilityType.displayName} Manual Entry" else null,
                        utilityType = utilityType
                    )
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Logged ${utilityType.displayName} reading ($reading ${utilityType.defaultUnit})!",
                            actionLabel = "UNDO",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            // Stay on the screen so the reading can be scanned or typed again
                            viewModel.undoLastAction()
                            isSaving = false
                        } else {
                            onNavigateBack()
                        }
                    }
                },
                onCancel = onNavigateBack
            )
        }
    }
}

@Composable
private fun BottomPanel(
    utilityType: UtilityType,
    extractedValue: String,
    detectedText: String,
    isSaving: Boolean,
    onConfirm: (reading: String, isManual: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var isManualEntry by rememberSaveable { mutableStateOf(false) }
    var manualReading by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val manualValue = manualReading.trim().replace(',', '.')
    val readingToSave = if (isManualEntry) manualValue else extractedValue
    val isReadyToConfirm = !isSaving && readingToSave.toDoubleOrNull()?.let { it > 0.0 } == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(PanelColor)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isManualEntry) {
            OutlinedTextField(
                value = manualReading,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("""\d*[.,]?\d*"""))) manualReading = input
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("${utilityType.displayName} reading") },
                suffix = { Text(utilityType.defaultUnit, color = PanelMuted) },
                isError = manualReading.isNotEmpty() && manualValue.toDoubleOrNull() == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (isReadyToConfirm) onConfirm(readingToSave, true)
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                    focusedLabelColor = AccentGreen,
                    unfocusedLabelColor = PanelMuted,
                    cursorColor = AccentGreen
                )
            )
        } else {
            // Extracted reading feedback
            Text(
                text = if (extractedValue.isNotBlank()) {
                    "${utilityType.emoji} Detected: $extractedValue ${utilityType.defaultUnit}"
                } else {
                    "Align meter in frame..."
                },
                style = TextStyle(
                    color = if (extractedValue.isNotBlank()) AccentGreen else Color.Yellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = "Raw OCR: $detectedText",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = PanelMuted, fontSize = 12.sp)
            )
        }

        TextButton(
            onClick = {
                if (!isManualEntry && manualReading.isEmpty()) manualReading = extractedValue
                isManualEntry = !isManualEntry
                if (!isManualEntry) focusManager.clearFocus()
            },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (isManualEntry) "Scan with camera instead" else "Can't scan? Type the reading",
                color = AccentGreen,
                fontWeight = FontWeight.Medium
            )
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (isReadyToConfirm) onConfirm(readingToSave, isManualEntry)
                },
                enabled = isReadyToConfirm,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    disabledContainerColor = Color.DarkGray
                )
            ) {
                Text("Confirm & Save", fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ReceptiveFieldOverlay(
    modifier: Modifier = Modifier,
    hintText: String = "Position electricity numbers inside frame"
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val boxWidth = size.width * 0.82f
        val boxHeight = size.height * 0.25f
        val left = (size.width - boxWidth) / 2f
        val top = (size.height - boxHeight) / 2f - 40.dp.toPx()

        // Scrim background
        drawRect(color = ScrimColor)

        // Punch out scanning window
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // Corner brackets guide
        drawCornerBrackets(left, top, boxWidth, boxHeight, 32.dp.toPx(), 24.dp.toPx(), 4.dp.toPx())

        // Hint text
        val hint = textMeasurer.measure(
            text = hintText,
            style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        )
        drawText(
            textLayoutResult = hint,
            topLeft = Offset(x = (size.width - hint.size.width) / 2f, y = top - hint.size.height - 16.dp.toPx())
        )
    }
}

private fun DrawScope.drawCornerBrackets(
    left: Float, top: Float, width: Float, height: Float,
    armLength: Float, radius: Float, strokeWidth: Float
) {
    val right = left + width
    val bottom = top + height
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val white = Color.White

    // Top-left
    drawPath(Path().apply { moveTo(left, top + armLength); lineTo(left, top + radius); quadraticBezierTo(left, top, left + radius, top); lineTo(left + armLength, top) }, white, style = stroke)
    // Top-right
    drawPath(Path().apply { moveTo(right - armLength, top); lineTo(right - radius, top); quadraticBezierTo(right, top, right, top + radius); lineTo(right, top + armLength) }, white, style = stroke)
    // Bottom-right
    drawPath(Path().apply { moveTo(right, bottom - armLength); lineTo(right, bottom - radius); quadraticBezierTo(right, bottom, right - radius, bottom); lineTo(right - armLength, bottom) }, white, style = stroke)
    // Bottom-left
    drawPath(Path().apply { moveTo(left + armLength, bottom); lineTo(left + radius, bottom); quadraticBezierTo(left, bottom, left, bottom - radius); lineTo(left, bottom - armLength) }, white, style = stroke)
}

private fun startTextRecognition(
    context: Context,
    cameraController: LifecycleCameraController,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onDetectedTextUpdated: (String) -> Unit
) {
    cameraController.imageAnalysisTargetSize = CameraController.OutputSize(AspectRatio.RATIO_16_9)
    // Assuming TextRecognitionAnalyzer is your ML Kit Analyzer implementation
    cameraController.setImageAnalysisAnalyzer(
        ContextCompat.getMainExecutor(context),
        de.yanneckreiss.mlkittutorial.ui.camera.TextRecognitionAnalyzer(onDetectedTextUpdated = onDetectedTextUpdated)
    )
    cameraController.bindToLifecycle(lifecycleOwner)
    previewView.controller = cameraController
}