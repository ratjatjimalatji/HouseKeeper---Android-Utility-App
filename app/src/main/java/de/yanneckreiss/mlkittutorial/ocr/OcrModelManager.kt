package de.yanneckreiss.mlkittutorial.ocr

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("ocr_settings_prefs", Context.MODE_PRIVATE)

    private val _selectedModel = MutableStateFlow(loadSelectedModel())
    val selectedModel: StateFlow<OcrModel> = _selectedModel.asStateFlow()

    private val _downloadStatusMap = MutableStateFlow<Map<String, ModelDownloadStatus>>(emptyMap())
    val downloadStatusMap: StateFlow<Map<String, ModelDownloadStatus>> = _downloadStatusMap.asStateFlow()

    init {
        updateDownloadStatuses()
    }

    private fun loadSelectedModel(): OcrModel {
        val savedId = prefs.getString("active_ocr_model_id", OcrModelRegistry.DEFAULT_MODEL.id)
        return OcrModelRegistry.availableModels.find { it.id == savedId } ?: OcrModelRegistry.DEFAULT_MODEL
    }

    fun selectModel(model: OcrModel) {
        prefs.edit().putString("active_ocr_model_id", model.id).apply()
        _selectedModel.value = model
    }

    fun isModelDownloaded(model: OcrModel): Boolean {
        if (model.isDefault) return true
        val modelFile = getModelFile(model)
        return modelFile.exists() && modelFile.length() > 0
    }

    fun getModelFile(model: OcrModel): File {
        val modelsDir = File(context.filesDir, "ocr_models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        return File(modelsDir, model.filename)
    }

    fun updateDownloadStatuses() {
        val statusMap = mutableMapOf<String, ModelDownloadStatus>()
        OcrModelRegistry.availableModels.forEach { model ->
            if (model.isDefault) {
                statusMap[model.id] = ModelDownloadStatus.Downloaded("built-in", 0L)
            } else {
                val file = getModelFile(model)
                if (file.exists() && file.length() > 0) {
                    statusMap[model.id] = ModelDownloadStatus.Downloaded(file.absolutePath, file.length())
                } else {
                    statusMap[model.id] = ModelDownloadStatus.NotDownloaded
                }
            }
        }
        _downloadStatusMap.value = statusMap
    }

    suspend fun downloadModel(model: OcrModel) {
        if (model.isDefault || model.downloadUrl.isBlank()) return

        withContext(Dispatchers.IO) {
            val statusMap = _downloadStatusMap.value.toMutableMap()
            statusMap[model.id] = ModelDownloadStatus.Downloading(0)
            _downloadStatusMap.value = statusMap

            try {
                val url = URL(model.downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                val fileLength = connection.contentLength
                val inputStream = connection.inputStream
                val targetFile = getModelFile(model)
                val outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8192)
                var totalBytesRead = 0L
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (fileLength > 0) {
                        val progress = ((totalBytesRead * 100) / fileLength).toInt()
                        statusMap[model.id] = ModelDownloadStatus.Downloading(progress)
                        _downloadStatusMap.value = statusMap.toMap()
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                connection.disconnect()

                statusMap[model.id] = ModelDownloadStatus.Downloaded(targetFile.absolutePath, targetFile.length())
                _downloadStatusMap.value = statusMap.toMap()
            } catch (e: Exception) {
                val targetFile = getModelFile(model)
                if (targetFile.exists()) targetFile.delete()

                statusMap[model.id] = ModelDownloadStatus.Error(e.localizedMessage ?: "Download failed")
                _downloadStatusMap.value = statusMap.toMap()
            }
        }
    }

    suspend fun deleteModel(model: OcrModel) {
        if (model.isDefault) return
        withContext(Dispatchers.IO) {
            val file = getModelFile(model)
            if (file.exists()) file.delete()
            updateDownloadStatuses()

            if (_selectedModel.value.id == model.id) {
                selectModel(OcrModelRegistry.DEFAULT_MODEL)
            }
        }
    }

    /**
     * Normalizes detected OCR text, resolving slashed zeroes (Ø, 0/, [0]) to '0'
     * when a slashed-zero supported model is active.
     */
    fun normalizeDetectedText(rawText: String): String {
        val currentModel = _selectedModel.value
        if (!currentModel.supportsSlashedZero) return rawText

        // Normalize slashed zero characters (Ø, ø, 0/, O in numeric tokens) into clean digits '0'
        var normalized = rawText
            .replace("Ø", "0")
            .replace("ø", "0")
            .replace("0/", "0")
            .replace("0\\", "0")

        // In digit meter reading strings, convert 'O' or 'o' to '0'
        val regex = Regex("(?<=\\d)[Oo](?=\\d)|(?<=\\d)[Oo]$|^[Oo](?=\\d)")
        normalized = regex.replace(normalized, "0")

        return normalized
    }
}
