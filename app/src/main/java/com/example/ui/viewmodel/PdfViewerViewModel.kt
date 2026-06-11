package com.scholarvault.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

class PdfViewerViewModel : ViewModel() {

    private var prefetchJob: Job? = null

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<Int, Bitmap>> = _thumbnails.asStateFlow()

    private val _fullPages = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val fullPages: StateFlow<Map<Int, Bitmap>> = _fullPages.asStateFlow()

    private val _loadedFile = MutableStateFlow<File?>(null)
    val loadedFile: StateFlow<File?> = _loadedFile.asStateFlow()

    private val _loadedUri = MutableStateFlow<android.net.Uri?>(null)
    val loadedUri: StateFlow<android.net.Uri?> = _loadedUri.asStateFlow()

    private var renderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private val mutex = Mutex()
    private var isViewModelActive = true
    
    var externalCacheFile: File? = null

    // Dedicated single-threaded dispatcher to confine all native PdfRenderer operations
    private val pdfDispatcher = java.util.concurrent.Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    // Replace Maps with LruCache for proper memory management (increased capacity to 200 for fast scrolling over long PDFs)
    private val thumbnailCache = object : LruCache<Int, Bitmap>(200) {
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            // Let the garbage collector handle evicted thumbnail bitmaps
            // Calling recycle() here crashes Jetpack Compose if the image is still being rendered
        }
    }

    // Remove manual object pools and just let GC handle the bitmaps.
    private fun obtainBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    }

    private val fullPageCache = object : LruCache<Int, Bitmap>(8) {
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            // Let GC handle the bitmap
        }
    }

    fun loadPdf(file: File) {
        viewModelScope.launch(pdfDispatcher) {
            mutex.withLock {
                closeRenderer()
                _loadError.value = null
                try {
                    thumbnailCache.evictAll()
                    fullPageCache.evictAll()
                    
                    parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = PdfRenderer(parcelFileDescriptor!!)
                    _pageCount.value = renderer!!.pageCount
                    _thumbnails.value = emptyMap()
                    _fullPages.value = emptyMap()
                    _loadedFile.value = file
                    _loadedUri.value = null
                } catch (e: Exception) {
                    e.printStackTrace()
                    _loadError.value = "Failed to load PDF: ${e.localizedMessage ?: e.message}"
                    parcelFileDescriptor?.close()
                }
            }
        }
    }

    fun loadPdfUri(context: android.content.Context, uriString: String) {
        viewModelScope.launch(pdfDispatcher) {
            mutex.withLock {
                closeRenderer()
                _loadError.value = null
                try {
                    thumbnailCache.evictAll()
                    fullPageCache.evictAll()
                    
                    val uri = android.net.Uri.parse(uriString)
                    var tempPfd: ParcelFileDescriptor? = null
                    
                    try {
                        // Phase 2: Direct Memory Stream, open from uri without copy
                        tempPfd = context.contentResolver.openFileDescriptor(uri, "r")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    if (tempPfd != null) {
                        parcelFileDescriptor = tempPfd
                        _loadedUri.value = uri
                        _loadedFile.value = null
                    } else {
                        // Fallback: Copy to external cache
                        val cacheFile = File(context.cacheDir, "external_temp_${System.currentTimeMillis()}.pdf")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            cacheFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        externalCacheFile = cacheFile
                        parcelFileDescriptor = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        _loadedFile.value = cacheFile
                        _loadedUri.value = null
                    }
                    
                    if (parcelFileDescriptor != null) {
                        renderer = PdfRenderer(parcelFileDescriptor!!)
                        _pageCount.value = renderer!!.pageCount
                        _thumbnails.value = emptyMap()
                        _fullPages.value = emptyMap()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _loadError.value = "Failed to load PDF path: ${e.localizedMessage ?: e.message}"
                    parcelFileDescriptor?.close()
                }
            }
        }
    }

    /** Suspending function to get thumbnail for an index */
    suspend fun getThumbnailAsync(index: Int): Bitmap? {
        val cached = thumbnailCache.get(index)
        if (cached != null) return cached

        return withContext(pdfDispatcher) {
            if (!isActive) return@withContext null
            mutex.withLock {
                if (!isActive) return@withLock null
                
                val r = renderer ?: return@withLock null
                if (index < 0 || index >= r.pageCount) return@withLock null
                
                // double check cache
                val doubleCached = thumbnailCache.get(index)
                if (doubleCached != null) return@withLock doubleCached

                var page: PdfRenderer.Page? = null
                try {
                    page = r.openPage(index)
                    val originalWidth = page.width
                    val originalHeight = page.height
                    
                    // Fixed width for uniform layouts and sharp rendering in grid
                    val targetW = 180
                    val targetH = (targetW.toFloat() / originalWidth * originalHeight).toInt().coerceAtLeast(1)
                    
                    val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    thumbnailCache.put(index, bmp)
                    bmp
                } catch (e: Throwable) {
                    e.printStackTrace()
                    null
                } finally {
                    try {
                        page?.close()
                    } catch (ignored: Throwable) {}
                }
            }
        }
    }

    /** Render a full-resolution page for the single-page pager view */
    fun renderPage(index: Int) {
        viewModelScope.launch(pdfDispatcher) {
            if (!isViewModelActive) return@launch
            
            loadPageResource(index)
            
            prefetchJob?.cancel()
            prefetchJob = viewModelScope.launch(pdfDispatcher) {
                // Immediate prefetch for adjacent pages
                listOf(index - 1, index + 1).filter { it >= 0 && it < _pageCount.value }.forEach { preIdx ->
                    loadPageResource(preIdx)
                }
                
                // Wait for pause in scrolling
                delay(300)
                
                // Background pre-caching mechanism: load the next 3 pages into memory automatically (since next 1 is already loaded, load +2 and +3)
                listOf(index + 2, index + 3).filter { it >= 0 && it < _pageCount.value }.forEach { preIdx ->
                    loadPageResource(preIdx)
                }
            }
        }
    }

    private suspend fun loadPageResource(index: Int) {
        mutex.withLock {
            val r = renderer ?: return@withLock
            if (index < 0 || index >= r.pageCount) return@withLock
            
            if (fullPageCache.get(index) == null) {
                var page: PdfRenderer.Page? = null
                try {
                    page = r.openPage(index)
                    // Capping max dimension to 1200px to avoid memory overflow (OOM) on extremely large PDF sizes
                    val originalWidth = page.width
                    val originalHeight = page.height
                    val maxDimension = 1200f
                    val scale = if (originalWidth > maxDimension || originalHeight > maxDimension) {
                        maxDimension / maxOf(originalWidth, originalHeight)
                    } else {
                        1.0f
                    }
                    val targetW = (originalWidth * scale).toInt().coerceAtLeast(1)
                    val targetH = (originalHeight * scale).toInt().coerceAtLeast(1)
                    val bmp = obtainBitmap(targetW, targetH)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    fullPageCache.put(index, bmp)
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    try {
                        page?.close()
                    } catch (ignored: Throwable) {}
                }
                updateFullPageState()
            }
        }
    }

    private fun updateFullPageState() {
        val currentMap = mutableMapOf<Int, Bitmap>()
        for ((key, bmp) in fullPageCache.snapshot()) {
            currentMap[key] = bmp
        }
        _fullPages.value = currentMap
    }

    private fun closeRenderer() {
        thumbnailCache.evictAll()
        fullPageCache.evictAll()
        _thumbnails.value = emptyMap()
        _fullPages.value = emptyMap()
        _loadedFile.value = null
        _loadedUri.value = null
        try {
            renderer?.close()
        } catch (ignored: Throwable) {}
        try {
            parcelFileDescriptor?.close()
        } catch (ignored: Throwable) {}
        renderer = null
        parcelFileDescriptor = null
        try {
            externalCacheFile?.delete()
            externalCacheFile = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        isViewModelActive = false
        try {
            closeRenderer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            (pdfDispatcher as? kotlinx.coroutines.ExecutorCoroutineDispatcher)?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
