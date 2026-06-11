package com.scholarvault.ui.tools.pdf_nup

import android.graphics.Bitmap
import android.util.LruCache

class PreviewCacheManager(
    private val cacheSizeBytes: Int = 50 * 1024 * 1024 // 50MB budget
) {
    private val cache = object : LruCache<String, Bitmap>(cacheSizeBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.allocationByteCount
        }
    }

    suspend fun cacheSheet(sheetIndex: Int, vPages: List<VirtualPage>, renderThumbnail: suspend (VirtualPage, Int, Int) -> Bitmap?) {
        for (vPage in vPages) {
            val key = "${vPage.sourceUri.toString().hashCode()}_${vPage.sourcePageIndex}"
            if (cache.get(key) == null) {
                val bitmap = renderThumbnail(vPage, 200, 280) // Downsampled
                if (bitmap != null) {
                    cache.put(key, bitmap)
                }
            }
        }
    }

    fun putThumbnail(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun getThumbnail(key: String): Bitmap? = cache.get(key)

    fun clearCache() = cache.evictAll()
}
