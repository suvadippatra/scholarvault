package com.scholarvault.ui.tools.pdf_inverter

import android.app.Application
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfRenderer
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
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

class PdfInverterViewModel(application: Application) : AndroidViewModel(application) {
    private val _items = MutableStateFlow<List<PdfItem>>(emptyList())
    val items: StateFlow<List<PdfItem>> = _items.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            PdfInverterProcessor.pruneOldFiles(application)
        }
    }

    fun addUris(uris: List<Uri>, onLimitExceeded: () -> Unit = {}, onWarning: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val maxAllowed = 20
            val warningThreshold = 5
            val currentCount = _items.value.size
            
            var itemsToAdd = uris
            if (currentCount + itemsToAdd.size > maxAllowed) {
                itemsToAdd = itemsToAdd.take(maxAllowed - currentCount)
                withContext(Dispatchers.Main) { onLimitExceeded() }
            } else if (currentCount + itemsToAdd.size > warningThreshold) {
                withContext(Dispatchers.Main) { onWarning() }
            }

            val newItems = itemsToAdd.mapNotNull { uri ->
                try {
                    var name = "Document"
                    if (uri.scheme == "content") {
                        getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1 && cursor.moveToFirst()) {
                                name = cursor.getString(nameIndex)
                            }
                        }
                    } else {
                        name = uri.lastPathSegment ?: "document_${System.currentTimeMillis()}.pdf"
                    }
                    if (name.contains("/")) name = name.substringAfterLast("/")
                    if (!name.lowercase().endsWith(".pdf")) name += ".pdf"
                    
                    val cleanSafeName = name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                    val tempFile = File(getApplication<Application>().cacheDir, "pdf_inverter_${System.currentTimeMillis()}_$cleanSafeName")
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val finalUri = Uri.fromFile(tempFile)
                    
                    var pfd: ParcelFileDescriptor? = null
                    var pageCount = 0
                    try {
                        pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        if (pfd != null) {
                            val renderer = PdfRenderer(pfd)
                            pageCount = renderer.pageCount
                            renderer.close()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pfd?.close()
                    }
                    if (pageCount > 0) {
                        PdfItem(uri = finalUri, name = name, pageCount = pageCount, newName = "Inverted_$name")
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            _items.value = _items.value + newItems
        }
    }

    fun removeItem(id: String) {
        _items.value = _items.value.filter { it.id != id }
    }

    fun updateItemNewName(id: String, newName: String) {
        _items.value = _items.value.map { if (it.id == id) it.copy(newName = newName) else it }
    }

    fun updateItemPagesToInvert(id: String, pagesStr: String) {
        _items.value = _items.value.map { if (it.id == id) it.copy(pagesToInvertStr = pagesStr) else it }
    }

    fun updateItemMode(id: String, mode: InvertMode) {
        _items.value = _items.value.map { if (it.id == id) it.copy(mode = mode) else it }
    }

    fun processAll() {
        if (_items.value.isEmpty()) return
        
        val context = getApplication<Application>()
        val workManager = WorkManager.getInstance(context)

        // Only process those without a result
        val itemsToProcess = _items.value.filter { it.resultUri == null }
        if (itemsToProcess.isEmpty()) return

        val requests = itemsToProcess.map { item ->
            OneTimeWorkRequestBuilder<PdfInversionWorker>()
                .setInputData(
                    Data.Builder()
                        .putString("uri", item.uri.toString())
                        .putString("new_name", item.newName)
                        .putString("pages", item.pagesToInvertStr)
                        .putString("mode", item.mode.name)
                        .build()
                )
                .build()
        }

        var continuation = workManager.beginUniqueWork(
            "PdfInversionBatch",
            ExistingWorkPolicy.KEEP,
            requests.first()
        )
        for (i in 1 until requests.size) {
            continuation = continuation.then(requests[i])
        }
        
        continuation.enqueue()
    }
}
