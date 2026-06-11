package com.scholarvault.ui.tools.image_resizer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FloodFillBackgroundRemoval {

    fun removeBackground(bitmap: Bitmap, tolerance: Float, outBgColor: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        
        val mask = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var head = 0
        var tail = 0
        
        // Use top-left as reference color for background
        val refColor = pixels[0]
        val refR = Color.red(refColor)
        val refG = Color.green(refColor)
        val refB = Color.blue(refColor)
        
        fun colorDist(c: Int): Float {
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            return Math.sqrt(((r - refR)*(r - refR) + (g - refG)*(g - refG) + (b - refB)*(b - refB)).toDouble()).toFloat()
        }
        
        val threshold = tolerance * 2.55f // map 0-100 to 0-255 scaled
        
        // Push edges
        for (x in 0 until w) {
            if (colorDist(pixels[x]) <= threshold) { queue[tail++] = x; mask[x] = true }
            val b = (h - 1) * w + x
            if (!mask[b] && colorDist(pixels[b]) <= threshold) { queue[tail++] = b; mask[b] = true }
        }
        for (y in 0 until h) {
            val l = y * w
            if (!mask[l] && colorDist(pixels[l]) <= threshold) { queue[tail++] = l; mask[l] = true }
            val r = y * w + w - 1
            if (!mask[r] && colorDist(pixels[r]) <= threshold) { queue[tail++] = r; mask[r] = true }
        }
        
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        
        while (head < tail) {
            val idx = queue[head++]
            val cx = idx % w
            val cy = idx / w
            
            for (i in 0..3) {
                val nx = cx + dx[i]
                val ny = cy + dy[i]
                if (nx in 0 until w && ny in 0 until h) {
                    val nIdx = ny * w + nx
                    if (!mask[nIdx] && colorDist(pixels[nIdx]) <= threshold) {
                        mask[nIdx] = true
                        queue[tail++] = nIdx
                    }
                }
            }
        }
        
        val outPixels = IntArray(w * h)
        for (i in 0 until (w * h)) {
            outPixels[i] = if (mask[i]) outBgColor else pixels[i]
        }
        
        val res = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        res.setPixels(outPixels, 0, w, 0, 0, w, h)
        return res
    }

    suspend fun processImageFinal(
        context: Context,
        inputBitmap: Bitmap,
        doBgRemoval: Boolean,
        bgTolerance: Float,
        bgColor: Int,
        targetWidth: Int,
        targetHeight: Int,
        targetDpi: Int,
        targetKB: Int?,
        format: Bitmap.CompressFormat,
        outName: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            var bmp = inputBitmap
            if (doBgRemoval) {
                bmp = removeBackground(bmp, bgTolerance, bgColor)
            }
            
            if (targetWidth > 0 && targetHeight > 0 && (bmp.width != targetWidth || bmp.height != targetHeight)) {
                bmp = Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true)
            }
            
            val outFile = File(context.cacheDir, outName)
            var quality = 100
            
            if (targetKB != null && targetKB > 0 && format != Bitmap.CompressFormat.PNG) {
                var compressedBytes = ByteArray(0)
                do {
                    val stream = ByteArrayOutputStream()
                    bmp.compress(format, quality, stream)
                    compressedBytes = stream.toByteArray()
                    quality -= 5
                } while (compressedBytes.size / 1024 > targetKB && quality > 10)
                FileOutputStream(outFile).use { it.write(compressedBytes) }
            } else {
                val byteStream = ByteArrayOutputStream()
                bmp.compress(format, quality, byteStream)
                FileOutputStream(outFile).use { it.write(byteStream.toByteArray()) }
            }
            
            try {
                if (targetDpi > 0) {
                    val exif = ExifInterface(outFile.absolutePath)
                    exif.setAttribute(ExifInterface.TAG_X_RESOLUTION, "${targetDpi}/1")
                    exif.setAttribute(ExifInterface.TAG_Y_RESOLUTION, "${targetDpi}/1")
                    exif.setAttribute(ExifInterface.TAG_RESOLUTION_UNIT, "2")
                    exif.saveAttributes()
                }
            } catch (e: Exception) {}
            
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
