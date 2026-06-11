package com.scholarvault.ui.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object ImageResizerProcessor {

    enum class UnitType { PIXEL, CM, INCH, MM }

    fun processImage(
        context: Context,
        uri: Uri,
        outFileName: String,
        targetWidth: Float,
        targetHeight: Float,
        unit: UnitType,
        format: Bitmap.CompressFormat,
        targetSizeKB: Int? // If true, compress until under this size
    ): File? {
        val cr = context.contentResolver
        
        // Convert target size to pixels
        val dpi = 300f 
        var pxWidth = convertToPixels(targetWidth, unit, dpi).toInt().coerceAtLeast(1)
        var pxHeight = convertToPixels(targetHeight, unit, dpi).toInt().coerceAtLeast(1)

        // Read orientation
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            cr.openInputStream(uri)?.use { 
                orientation = ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        } catch (e: Exception) { e.printStackTrace() }

        // Calculate inSampleSize
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        
        var srcWidth = options.outWidth
        var srcHeight = options.outHeight
        
        // Swap src dimensions if rotated
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            srcWidth = options.outHeight
            srcHeight = options.outWidth
        }

        options.inSampleSize = calculateInSampleSize(srcWidth, srcHeight, pxWidth, pxHeight)
        options.inJustDecodeBounds = false

        // Load original bitmap
        var bitmap: Bitmap? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(cr, uri)
            bitmap = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = 1.0f / options.inSampleSize
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            bitmap = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } 
        }
        
        if (bitmap == null) return null

        bitmap = rotateBitmap(bitmap, orientation)

        // Resize
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, pxWidth, pxHeight, true)
        if (scaledBitmap != bitmap) {
            bitmap.recycle()
        }

        // Compress
        val outBytes = if (targetSizeKB != null && targetSizeKB > 0 && format != Bitmap.CompressFormat.PNG) {
            compressToTargetSize(scaledBitmap, targetSizeKB * 1024, format)
        } else {
            val bos = ByteArrayOutputStream()
            scaledBitmap.compress(format, 100, bos)
            bos.toByteArray()
        }

        scaledBitmap.recycle()

        // Save
        val cacheDir = File(context.cacheDir, "image_resizer").apply { mkdirs() }
        val ext = when(format) {
            Bitmap.CompressFormat.JPEG -> "jpg"
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP -> "webp"
            else -> "jpg"
        }
        
        var name = outFileName.trim()
        if (name.isEmpty()) name = "resized_image"
        if (!name.lowercase().endsWith(".$ext")) name += ".$ext"

        val outFile = File(cacheDir, name)
        FileOutputStream(outFile).use { it.write(outBytes) }

        return outFile
    }

    private fun calculateInSampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.preScale(1.0f, -1.0f)
                matrix.postRotate(180f)
            }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    private fun compressToTargetSize(bitmap: Bitmap, targetBytes: Int, format: Bitmap.CompressFormat): ByteArray {
        var quality = 100
        var low = 0
        var high = 100
        var bestBytes: ByteArray? = null

        while (low <= high) {
            val mid = low + (high - low) / 2
            val bos = ByteArrayOutputStream()
            bitmap.compress(format, mid, bos)
            val bytes = bos.toByteArray()

            if (bytes.size <= targetBytes) {
                bestBytes = bytes
                low = mid + 1
                quality = mid
            } else {
                high = mid - 1
            }
        }

        // If even lowest quality is too big 
        if (bestBytes == null) {
            val bos = ByteArrayOutputStream()
            bitmap.compress(format, 0, bos)
            return bos.toByteArray()
        }

        return bestBytes
    }

    fun convertToPixels(value: Float, unit: UnitType, dpi: Float): Float {
        return when (unit) {
            UnitType.PIXEL -> value
            UnitType.INCH -> value * dpi
            UnitType.CM -> (value / 2.54f) * dpi
            UnitType.MM -> (value / 25.4f) * dpi
        }
    }
}

