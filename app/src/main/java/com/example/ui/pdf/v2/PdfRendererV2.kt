package com.scholarvault.ui.pdf.v2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

class PdfRendererV2 {
    private var pfd: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var pageCount = 0

    fun open(context: Context, docUri: Uri): Boolean {
        close()
        try {
            val contentResolver = context.contentResolver
            val parcelFd = contentResolver.openFileDescriptor(docUri, "r")
            if (parcelFd != null) {
                pfd = parcelFd
                val renderer = PdfRenderer(parcelFd)
                pdfRenderer = renderer
                pageCount = renderer.pageCount
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun openFile(file: File): Boolean {
        close()
        try {
            val parcelFd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            if (parcelFd != null) {
                pfd = parcelFd
                val renderer = PdfRenderer(parcelFd)
                pdfRenderer = renderer
                pageCount = renderer.pageCount
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun getPageCount(): Int = pageCount

    fun renderPage(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap? {
        val renderer = pdfRenderer ?: return null
        if (pageIndex < 0 || pageIndex >= pageCount) return null

        var page: PdfRenderer.Page? = null
        try {
            page = renderer.openPage(pageIndex)
            val pageWidth = page.width.toFloat()
            val pageHeight = page.height.toFloat()

            var finalWidth = targetWidth
            var finalHeight = targetHeight

            if (targetWidth > 0 && targetHeight > 0) {
                // Render at 2.5x the requested size to ensure high resolution upon zooming
                val highResTargetWidth = targetWidth * 2.5f
                val highResTargetHeight = targetHeight * 2.5f

                val scaleWidth = highResTargetWidth / pageWidth
                val scaleHeight = highResTargetHeight / pageHeight
                val scale = minOf(scaleWidth, scaleHeight)
                
                // Cap to prevent OutOfMemory on huge documents
                val maxAllowedDimension = 2400f
                val finalScale = if ((pageWidth * scale) > maxAllowedDimension || (pageHeight * scale) > maxAllowedDimension) {
                    minOf(maxAllowedDimension / pageWidth, maxAllowedDimension / pageHeight)
                } else {
                    scale
                }

                finalWidth = (pageWidth * finalScale).toInt()
                finalHeight = (pageHeight * finalScale).toInt()
            } else {
                finalWidth = page.width
                finalHeight = page.height
            }
            
            val widthSafe = if (finalWidth > 0) finalWidth else 1
            val heightSafe = if (finalHeight > 0) finalHeight else 1
            val bitmap = Bitmap.createBitmap(widthSafe, heightSafe, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                page?.close()
            } catch (ignored: Exception) {}
        }
    }

    fun close() {
        try {
            pdfRenderer?.close()
        } catch (ignored: Exception) {}
        pdfRenderer = null

        try {
            pfd?.close()
        } catch (ignored: Exception) {}
        pfd = null
        pageCount = 0
    }
}
