package com.scholarvault.ui.tools.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

object FilterEngine {

    fun applyFilter(bitmap: Bitmap, filterMode: String): Bitmap {
        return when (filterMode) {
            "Magic Enhance" -> applyMagicEnhance(bitmap)
            "Grayscale" -> applyGrayscale(bitmap)
            "B&W Document" -> applyAdaptiveThreshold(bitmap)
            else -> bitmap
        }
    }

    private fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val bmp = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        
        // Slight contrast bump for Grayscale
        val contrast = 1.2f
        val brightness = 5f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(contrastMatrix)
        
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return bmp
    }

    private fun applyMagicEnhance(bitmap: Bitmap): Bitmap {
        val bmp = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        val matrix = ColorMatrix()
        
        // Enhance text contrast (1.5x) and bump white point (brightness +30)
        val contrast = 1.5f
        val brightness = 30f
        val colorMatrix = floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        )
        matrix.postConcat(ColorMatrix(colorMatrix))
        
        // Saturate colors
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(1.4f)
        matrix.postConcat(satMatrix)
        
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return bmp
    }

    private fun applyAdaptiveThreshold(bitmap: Bitmap): Bitmap {
        // Fast Adaptive Thresholding using Integral Image
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val integral = LongArray(width * height)
        
        for (i in 0 until height) {
            var sum = 0L
            for (j in 0 until width) {
                val index = i * width + j
                val color = pixels[index]
                // Convert to grayscale: (R+G+B)/3
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                val gray = (r + g + b) / 3L
                pixels[index] = gray.toInt() // store intermediate grayscale in pixels array
                
                sum += gray
                if (i == 0) {
                    integral[index] = sum
                } else {
                    integral[index] = integral[(i - 1) * width + j] + sum
                }
            }
        }

        // Aggressive Window size for crisp ink strokes
        val s = width / 12
        val outPixels = IntArray(width * height)
        // Aggressive thresholding to clear background to pure white
        val t = 0.90f

        for (i in 0 until height) {
            for (j in 0 until width) {
                val x1 = maxOf(j - s / 2, 0)
                val x2 = minOf(j + s / 2, width - 1)
                val y1 = maxOf(i - s / 2, 0)
                val y2 = minOf(i + s / 2, height - 1)

                val count = (x2 - x1) * (y2 - y1)
                
                // Calculate local sum from integral image
                var sum = integral[y2 * width + x2]
                if (x1 > 0 && y1 > 0) {
                    sum += integral[(y1 - 1) * width + (x1 - 1)]
                }
                if (x1 > 0) {
                    sum -= integral[y2 * width + (x1 - 1)]
                }
                if (y1 > 0) {
                    sum -= integral[(y1 - 1) * width + x2]
                }

                val index = i * width + j
                val gray = pixels[index].toLong()
                
                if ((gray * count) < (sum * t).toLong()) {
                    outPixels[index] = 0xFF000000.toInt() // Black
                } else {
                    outPixels[index] = 0xFFFFFFFF.toInt() // White
                }
            }
        }

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
        return outBitmap
    }
}
