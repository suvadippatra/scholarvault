package com.scholarvault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scholarvault.data.model.ScannedDocumentEntity
import com.scholarvault.data.repository.ScannedDocumentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScannerViewModel(private val repository: ScannedDocumentRepository) : ViewModel() {

    val allScans: StateFlow<List<ScannedDocumentEntity>> = repository.getAllScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertScan(scan: ScannedDocumentEntity) {
        viewModelScope.launch {
            repository.insertScan(scan)
        }
    }

    private val _isProcessing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    fun setProcessing(processing: Boolean) {
        _isProcessing.value = processing
    }

    private val _newlyGeneratedScan = kotlinx.coroutines.flow.MutableStateFlow<ScannedDocumentEntity?>(null)
    val newlyGeneratedScan: StateFlow<ScannedDocumentEntity?> = _newlyGeneratedScan

    fun setNewlyGeneratedScan(scan: ScannedDocumentEntity?) {
        _newlyGeneratedScan.value = scan
    }

    fun deleteScan(scan: ScannedDocumentEntity) {
        viewModelScope.launch {
            repository.deleteScan(scan)
        }
    }
    
    fun updateScan(scan: ScannedDocumentEntity) {
        viewModelScope.launch {
            repository.insertScan(scan)
        }
    }
}

class ScannerViewModelFactory(private val repository: ScannedDocumentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScannerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
