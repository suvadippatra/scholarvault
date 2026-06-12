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

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FloodFillBackgroundRemoval {

    suspend fun removeBackground(bitmap: Bitmap, outBgColor: Int): Bitmap = suspendCancellableCoroutine { continuation ->
        try {
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
            val segmenter = Segmentation.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)

            segmenter.process(image)
                .addOnSuccessListener { result ->
                    try {
                        val mask = result.buffer
                        val maskWidth = result.width
                        val maskHeight = result.height
                        
                        val pixels = IntArray(maskWidth * maskHeight)
                        bitmap.getPixels(pixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)

                        mask.rewind()
                        for (i in 0 until (maskWidth * maskHeight)) {
                            val confidence = mask.float
                            if (confidence < 0.5f) { // Background
                                pixels[i] = outBgColor
                            }
                        }
                        val bgBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
                        bgBitmap.setPixels(pixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)
                        continuation.resume(bgBitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        continuation.resume(bitmap) // Fallback to original
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    continuation.resume(bitmap) // Fallback to original
                }
        } catch (e: Exception) {
            e.printStackTrace()
            continuation.resume(bitmap) // Fallback to original
        }
    }

    suspend fun processImageFinal(
        context: Context,
        inputBitmap: Bitmap,
        doBgRemoval: Boolean,
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
                bmp = removeBackground(bmp, bgColor)
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
