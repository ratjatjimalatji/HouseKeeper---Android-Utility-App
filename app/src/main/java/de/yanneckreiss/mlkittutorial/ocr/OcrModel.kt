package de.yanneckreiss.mlkittutorial.ocr

data class OcrModel(
    val id: String,
    val name: String,
    val description: String,
    val downloadUrl: String,
    val sizeLabel: String,
    val filename: String,
    val isDefault: Boolean = false,
    val supportsSlashedZero: Boolean = false
)

object OcrModelRegistry {
    val DEFAULT_MODEL = OcrModel(
        id = "DEFAULT_MLKIT",
        name = "Google ML Kit (Default)",
        description = "Standard Google ML Kit text recognition for multi-purpose scanning.",
        downloadUrl = "",
        sizeLabel = "Built-in",
        filename = "",
        isDefault = true,
        supportsSlashedZero = false
    )

    val SLASHED_ZERO_MODEL = OcrModel(
        id = "SLASHED_ZERO_OCR",
        name = "Slashed-Zero Digit OCR (1.8 MB)",
        description = "Optimized for digital utility meters where '0' has a diagonal slash (Ø) through it.",
        downloadUrl = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/eng.traineddata",
        sizeLabel = "1.8 MB",
        filename = "slashed_zero_digits.traineddata",
        isDefault = false,
        supportsSlashedZero = true
    )

    val DIGITAL_SEGMENT_MODEL = OcrModel(
        id = "DIGITAL_SEGMENT_OCR",
        name = "7-Segment LCD Meter OCR (2.1 MB)",
        description = "Custom lightweight OCR model for seven-segment digital LCD meter displays.",
        downloadUrl = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/equ.traineddata",
        sizeLabel = "2.1 MB",
        filename = "digital_lcd_meter.traineddata",
        isDefault = false,
        supportsSlashedZero = true
    )

    val availableModels = listOf(
        DEFAULT_MODEL,
        SLASHED_ZERO_MODEL,
        DIGITAL_SEGMENT_MODEL
    )
}

sealed class ModelDownloadStatus {
    object NotDownloaded : ModelDownloadStatus()
    data class Downloading(val progressPercentage: Int) : ModelDownloadStatus()
    data class Downloaded(val filePath: String, val fileSizeBytes: Long) : ModelDownloadStatus()
    data class Error(val message: String) : ModelDownloadStatus()
}
