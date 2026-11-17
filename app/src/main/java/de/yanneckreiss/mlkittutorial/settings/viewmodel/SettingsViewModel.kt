package de.yanneckreiss.mlkittutorial.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.yanneckreiss.mlkittutorial.ocr.ModelDownloadStatus
import de.yanneckreiss.mlkittutorial.ocr.OcrModel
import de.yanneckreiss.mlkittutorial.ocr.OcrModelManager
import de.yanneckreiss.mlkittutorial.ocr.OcrModelRegistry
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ocrModelManager: OcrModelManager
) : ViewModel() {

    val selectedModel: StateFlow<OcrModel> = ocrModelManager.selectedModel
    val downloadStatusMap: StateFlow<Map<String, ModelDownloadStatus>> = ocrModelManager.downloadStatusMap
    val availableModels: List<OcrModel> = OcrModelRegistry.availableModels

    fun selectModel(model: OcrModel) {
        ocrModelManager.selectModel(model)
    }

    fun downloadModel(model: OcrModel) {
        viewModelScope.launch {
            ocrModelManager.downloadModel(model)
        }
    }

    fun deleteModel(model: OcrModel) {
        viewModelScope.launch {
            ocrModelManager.deleteModel(model)
        }
    }
}
