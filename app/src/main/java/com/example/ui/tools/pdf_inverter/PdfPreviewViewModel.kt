package com.scholarvault.ui.tools.pdf_inverter

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfPreviewViewModel(application: Application) : AndroidViewModel(application) {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    var reusedBitmap: Bitmap? = null

    val pageCount = MutableStateFlow(0)
    val pageBitmap = MutableStateFlow<Bitmap?>(null)
    
    val requestedPage = MutableStateFlow(0)

    fun openUri(uri: Uri, bitmapWidth: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    renderer?.close()
                    pfd?.close()
                    pfd = getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        renderer = PdfRenderer(pfd!!)
                        pageCount.value = renderer!!.pageCount
                        
                        requestedPage.collectLatest { pageIdx ->
                            if (renderer != null && pageIdx < renderer!!.pageCount) {
                                val page = renderer!!.openPage(pageIdx)
                                val w = bitmapWidth
                                val h = (w.toFloat() / page.width.toFloat() * page.height).toInt()
                                
                                if (reusedBitmap == null || reusedBitmap!!.width != w || reusedBitmap!!.height != h) {
                                    reusedBitmap?.recycle()
                                    reusedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                }
                                
                                reusedBitmap!!.eraseColor(android.graphics.Color.WHITE)
                                page.render(reusedBitmap!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page.close()
                                
                                pageBitmap.value = reusedBitmap
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        reusedBitmap?.recycle()
        renderer?.close()
        pfd?.close()
    }
}
