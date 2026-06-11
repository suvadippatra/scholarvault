package com.scholarvault.ui.pdf.v2

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scholarvault.util.PdfSearchUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

sealed interface PdfV2UiState {
    object Loading : PdfV2UiState
    data class Success(val pageCount: Int, val fileName: String, val fileUri: Uri, val isExternal: Boolean) : PdfV2UiState
    data class Error(val message: String) : PdfV2UiState
}

class PdfViewerV2ViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<PdfV2UiState>(PdfV2UiState.Loading)
    val uiState: StateFlow<PdfV2UiState> = _uiState.asStateFlow()

    private val _backgroundQueueStatus = MutableStateFlow<String?>(null)
    val backgroundQueueStatus: StateFlow<String?> = _backgroundQueueStatus.asStateFlow()

    fun runBackgroundTask(context: Context, taskName: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _backgroundQueueStatus.value = "Active: $taskName"
            withContext(Dispatchers.IO) {
                try {
                    block()
                    withContext(Dispatchers.Main) {
                        _backgroundQueueStatus.value = "Success: $taskName"
                        android.widget.Toast.makeText(context, "$taskName finished successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _backgroundQueueStatus.value = "Failed: ${e.message}"
                        android.widget.Toast.makeText(context, "Failed $taskName: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private val renderer = PdfRendererV2()
    private val rendererMutex = Mutex()

    private val bitmapCache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 6).toInt()) {
        override fun sizeOf(key: String?, value: Bitmap?): Int {
            return (value?.byteCount ?: 0) / 1024
        }
    }

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchMatches = MutableStateFlow<List<Int>>(emptyList())
    val searchMatches: StateFlow<List<Int>> = _searchMatches.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(-1)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()

    var loadedFile: File? = null
        private set
    var loadedUri: Uri? = null
        private set

    fun loadPdf(context: Context, uri: Uri, altFileName: String = "Untitled.pdf", isExternal: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = PdfV2UiState.Loading
            _currentPage.value = 0
            clearSearch()
            bitmapCache.evictAll()

            withContext(Dispatchers.IO) {
                val opened = rendererMutex.withLock {
                    renderer.open(context, uri)
                }

                if (opened) {
                    val count = renderer.getPageCount()
                    loadedUri = uri
                    val displayName = getFileNameFromUri(context, uri) ?: altFileName
                    _uiState.value = PdfV2UiState.Success(
                        pageCount = count,
                        fileName = displayName,
                        fileUri = uri,
                        isExternal = isExternal
                    )
                } else {
                    _uiState.value = PdfV2UiState.Error("Failed to render PDF document content safely.")
                }
            }
        }
    }

    fun loadPdfFile(file: File) {
        viewModelScope.launch {
            _uiState.value = PdfV2UiState.Loading
            _currentPage.value = 0
            clearSearch()
            bitmapCache.evictAll()

            withContext(Dispatchers.IO) {
                val opened = rendererMutex.withLock {
                    renderer.openFile(file)
                }

                if (opened) {
                    val count = renderer.getPageCount()
                    loadedFile = file
                    _uiState.value = PdfV2UiState.Success(
                        pageCount = count,
                        fileName = file.name,
                        fileUri = Uri.fromFile(file),
                        isExternal = false
                    )
                } else {
                    _uiState.value = PdfV2UiState.Error("Unable to open secure local file document.")
                }
            }
        }
    }

    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    suspend fun getPageBitmap(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap? {
        val cacheKey = "${pageIndex}_${targetWidth}_${targetHeight}"
        val cached = bitmapCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return withContext(Dispatchers.Default) {
            rendererMutex.withLock {
                val bmp = renderer.renderPage(pageIndex, targetWidth, targetHeight)
                if (bmp != null) {
                    bitmapCache.put(cacheKey, bmp)
                }
                bmp
            }
        }
    }

    fun search(context: Context, query: String) {
        val uri = loadedUri
        val file = loadedFile
        if (query.isBlank() || (uri == null && file == null)) {
            clearSearch()
            return
        }

        _isSearching.value = true
        _searchQuery.value = query

        viewModelScope.launch(Dispatchers.Default) {
            try {
                PdfSearchUtil.searchPdfFlow(context, file, uri, query).collect { matches ->
                    withContext(Dispatchers.Main) {
                        _searchMatches.value = matches
                        if (_currentMatchIndex.value == -1 && matches.isNotEmpty()) {
                            _currentMatchIndex.value = 0
                            _currentPage.value = matches[0]
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    _isSearching.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    clearSearch()
                }
            }
        }
    }

    fun nextMatch() {
        val matches = _searchMatches.value
        val currentIndex = _currentMatchIndex.value
        if (matches.isEmpty() || currentIndex == -1) return

        val nextIndex = (currentIndex + 1) % matches.size
        _currentMatchIndex.value = nextIndex
        _currentPage.value = matches[nextIndex]
    }

    fun previousMatch() {
        val matches = _searchMatches.value
        val currentIndex = _currentMatchIndex.value
        if (matches.isEmpty() || currentIndex == -1) return

        val prevIndex = if (currentIndex - 1 < 0) matches.size - 1 else currentIndex - 1
        _currentMatchIndex.value = prevIndex
        _currentPage.value = matches[prevIndex]
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchMatches.value = emptyList()
        _currentMatchIndex.value = -1
        _isSearching.value = false
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return uri.lastPathSegment
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) {
            rendererMutex.withLock {
                renderer.close()
            }
            withContext(Dispatchers.Main) {
                bitmapCache.evictAll()
            }
        }
    }
}
