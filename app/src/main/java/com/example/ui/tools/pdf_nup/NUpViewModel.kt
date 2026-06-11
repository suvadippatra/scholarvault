package com.scholarvault.ui.tools.pdf_nup

import android.content.ContentValues
import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class NUpViewModel : ViewModel() {

    private val _config = MutableStateFlow(NUpConfig())
    val config: StateFlow<NUpConfig> = _config.asStateFlow()

    val items = mutableStateListOf<NUpItem>()
    val previewCacheManager = PreviewCacheManager()

    val virtualPageSequenceGenerator: VirtualPageSequenceGenerator
        get() = VirtualPageSequenceGenerator(items.toList(), config.value)

    val isProcessing: StateFlow<Boolean> = NUpProcessingService.isProcessing
    val processProgress: StateFlow<Float> = NUpProcessingService.processProgress
    val processStatusMessage: StateFlow<String> = NUpProcessingService.processStatusMessage

    init {
        viewModelScope.launch {
            NUpProcessingService.itemStatesFlow.collect { statesMap ->
                statesMap.forEach { (id, update) ->
                    val index = items.indexOfFirst { it.id == id }
                    if (index != -1) {
                        val current = items[index]
                        if (current.state != update.state || current.resultUri != update.resultUri || current.errorMessage != update.errorMessage) {
                            items[index] = current.copy(
                                state = update.state,
                                resultUri = update.resultUri,
                                errorMessage = update.errorMessage
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateConfig(modifier: (NUpConfig) -> NUpConfig) {
        _config.value = modifier(_config.value)
        resetSuccessStatesToPending()
    }

    fun resetSuccessStatesToPending() {
        for (i in items.indices) {
            val item = items[i]
            if (item.state == NUpProcessingState.SUCCESS) {
                items[i] = item.copy(
                    state = NUpProcessingState.PENDING,
                    resultUri = null,
                    errorMessage = null
                )
            }
        }
        NUpProcessingService.markSuccessStatesAsPending()
    }

    fun resetItemStatesToIdle() {
        for (i in items.indices) {
            val item = items[i]
            if (item.state != NUpProcessingState.IDLE || item.resultUri != null) {
                items[i] = item.copy(
                    state = NUpProcessingState.IDLE,
                    resultUri = null,
                    errorMessage = null
                )
            }
        }
        NUpProcessingService.clearItemStates()
    }

    fun addFile(context: Context, uri: Uri, name: String = "Document") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Determine real file name from ContentResolver if content uri, otherwise last path segment
                var realName = name
                if (uri.scheme == "content") {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            realName = cursor.getString(nameIndex)
                        }
                    }
                } else {
                    val last = uri.lastPathSegment
                    if (last != null && !last.contains("/")) {
                        realName = last
                    }
                }
                if (realName.isBlank() || realName == "document") {
                    realName = "Document_${System.currentTimeMillis()}"
                }

                val mimeType = context.contentResolver.getType(uri) ?: ""
                val isImage = mimeType.startsWith("image/") == true || 
                              realName.lowercase().endsWith(".png") || 
                              realName.lowercase().endsWith(".jpg") || 
                              realName.lowercase().endsWith(".jpeg")
                val mediaType = if (isImage) MediaType.IMAGE else MediaType.PDF
                var pageCount = 1
                
                // Copy all source files to caches immediately to avoid security limitations and seekability issues
                val ext = if (mediaType == MediaType.IMAGE) "img" else "pdf"
                val cleanSafeName = realName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                val tempFile = File(context.cacheDir, "nup_src_${System.currentTimeMillis()}_$cleanSafeName.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val finalUri = Uri.fromFile(tempFile)
                
                if (mediaType == MediaType.PDF) {
                    val fd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fd)
                    pageCount = renderer.pageCount
                    renderer.close()
                    fd.close()
                }
                
                withContext(Dispatchers.Main) {
                    val currentItems = items.toList()
                    val nextAlias = if (currentItems.isEmpty()) 'A' else (currentItems.maxOf { it.aliasIdentifier } + 1).coerceAtMost('Z')
                    items.add(NUpItem(
                         uri = finalUri, 
                         name = realName, 
                         pageCount = pageCount, 
                         mediaType = mediaType, 
                         aliasIdentifier = nextAlias,
                         pageSelectionText = "1-$pageCount" // default to all pages
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removePdf(item: NUpItem) {
        if (item.uri.scheme == "file") {
            try {
                val path = item.uri.path
                if (path != null) {
                    val file = File(path)
                    if (file.exists() && file.name.contains("nup_src_")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        items.remove(item)
    }

    override fun onCleared() {
        super.onCleared()
        if (!NUpProcessingService.isProcessing.value) {
            items.forEach { item ->
                if (item.uri.scheme == "file") {
                    try {
                        val path = item.uri.path
                        if (path != null) {
                            val file = File(path)
                            if (file.exists() && file.name.contains("nup_src_")) {
                                file.delete()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            items.clear()
            previewCacheManager.clearCache()
        }
    }

    fun moveItemUp(item: NUpItem) {
        val index = items.indexOf(item)
        if (index > 0) {
            items.removeAt(index)
            items.add(index - 1, item)
        }
    }

    fun moveItemDown(item: NUpItem) {
        val index = items.indexOf(item)
        if (index != -1 && index < items.size - 1) {
            items.removeAt(index)
            items.add(index + 1, item)
        }
    }

    fun getExpectedPageCount(currentConfig: NUpConfig): Int {
        return virtualPageSequenceGenerator.calculateTotalOutputPages(currentConfig.rows, currentConfig.columns)
    }

    fun cancelProcessing(context: Context) {
        NUpProcessingService.cancel(context)
    }

    fun processAll(context: Context) {
        if (items.isEmpty() || NUpProcessingService.isProcessing.value) return
        NUpProcessingService.startAndProcess(context, items.toList(), _config.value)
    }

    fun pruneOldFiles(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            val threshold = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L) // 3 days
            cacheDir.listFiles { _, name -> name.startsWith("nup_temp_") }?.forEach { file ->
                if (file.lastModified() < threshold) {
                    file.delete()
                }
            }
        }
    }
}
