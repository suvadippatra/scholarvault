package com.scholarvault.ui.tools.pdf_inverter

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class PdfProcessingState {
    object Idle : PdfProcessingState()
    data class Processing(val fileName: String, val progress: Int, val currentPage: Int, val totalPages: Int) : PdfProcessingState()
    data class Completed(val resultUri: Uri, val fileName: String) : PdfProcessingState()
    data class Failed(val reason: String) : PdfProcessingState()
}

object PdfProcessingRepository {
    private val _processingState = MutableStateFlow<PdfProcessingState>(PdfProcessingState.Idle)
    val processingState: StateFlow<PdfProcessingState> = _processingState.asStateFlow()

    fun updateState(state: PdfProcessingState) {
        _processingState.value = state
    }
}
