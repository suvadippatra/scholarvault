package com.scholarvault.ui.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.multipdf.LayerUtility
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.ceil

object PdfNUpProcessor {

    enum class PageFit {
        FIT_BEST, FIT_WIDTH, FIT_HEIGHT, STRETCH
    }

    suspend fun processFiles(
        context: Context,
        items: List<PrintJobItem>,
        pagesPerSheet: Int,
        keepBorder: Boolean,
        pageSize: PDRectangle,
        marginMm: Float = 0f,
        gapMm: Float = 0f,
        bookletMode: Boolean = false,
        pageFit: PageFit = PageFit.FIT_BEST
    ): File = withContext(Dispatchers.IO) {
        val marginPt = marginMm * 2.83465f 
        val gapPt = gapMm * 2.83465f
        val cacheDir = File(context.cacheDir, "pre_print_temp").apply { mkdirs() }
        val outputPdf = File(cacheDir, "output_${System.currentTimeMillis()}.pdf")
        
        PDDocument().use { outDoc ->
            val layerUtility = LayerUtility(outDoc)
            var extractedPages = mutableListOf<PDFormXObject>()

            for (item in items) {
                val uri = item.uri
                val type = context.contentResolver.getType(uri) ?: ""
                try {
                    if (type.startsWith("image/")) {
                        var bmp = decodeUriToBitmap(context, uri)
                        if (bmp != null) {
                            // Scale bitmap to match requested output DPI for A4 page size
                            val a4WidthInches = PDRectangle.A4.width / 72f
                            val a4HeightInches = PDRectangle.A4.height / 72f
                            val maxTargetWidth = (a4WidthInches * item.outputDpi).toInt()
                            val maxTargetHeight = (a4HeightInches * item.outputDpi).toInt()

                            val scaleFactor = Math.min(
                                maxTargetWidth.toFloat() / bmp.width,
                                maxTargetHeight.toFloat() / bmp.height
                            )
                            
                            if (scaleFactor < 0.95f || scaleFactor > 1.05f) {
                                val newWidth = (bmp.width * scaleFactor).toInt()
                                val newHeight = (bmp.height * scaleFactor).toInt()
                                if (newWidth > 0 && newHeight > 0) {
                                    val scaled = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true)
                                    bmp.recycle()
                                    bmp = scaled
                                }
                            }

                            val tempDoc = PDDocument()
                            val pdImage = LosslessFactory.createFromImage(tempDoc, bmp)
                            val page = PDPage(PDRectangle.A4) 
                            tempDoc.addPage(page)
                            PDPageContentStream(tempDoc, page).use { cs ->
                                val scale = Math.min(
                                    (PDRectangle.A4.width - 2 * marginPt) / bmp.width,
                                    (PDRectangle.A4.height - 2 * marginPt) / bmp.height
                                )
                                cs.drawImage(pdImage, marginPt, marginPt, bmp.width * scale, bmp.height * scale)
                            }
                            bmp.recycle()
                            val form = layerUtility.importPageAsForm(tempDoc, 0)
                            extractedPages.add(form)
                            tempDoc.close()
                        }
                    } else {
                        // PDF
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            PDDocument.load(stream).use { inDoc ->
                                val pageCount = inDoc.numberOfPages
                                val targetPages = parsePageSequence(item.pageSequence, pageCount)
                                for (i in targetPages) {
                                    if (i in 0 until pageCount) {
                                        val form = layerUtility.importPageAsForm(inDoc, i)
                                        extractedPages.add(form)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            if (bookletMode) {
                extractedPages = rearrangeForBooklet(extractedPages)
            }

            if (pagesPerSheet == 1) {
                // Default 1-up, but we apply margin and fit if needed
                for (form in extractedPages) {
                    val currentSheet = PDPage(pageSize)
                    outDoc.addPage(currentSheet)
                    PDPageContentStream(outDoc, currentSheet).use { cs ->
                        val bbox = form.bBox
                        val availableWidth = pageSize.width - 2 * marginPt
                        val availableHeight = pageSize.height - 2 * marginPt
                        
                        val scaleX = availableWidth / bbox.width
                        val scaleY = availableHeight / bbox.height
                        
                        val (finalScaleX, finalScaleY) = when (pageFit) {
                            PageFit.FIT_BEST -> {
                                val minScale = Math.min(scaleX, scaleY)
                                minScale to minScale
                            }
                            PageFit.FIT_WIDTH -> scaleX to scaleX
                            PageFit.FIT_HEIGHT -> scaleY to scaleY
                            PageFit.STRETCH -> scaleX to scaleY
                        }
                        
                        val transX = marginPt + (availableWidth - bbox.width * finalScaleX) / 2f
                        val transY = marginPt + (availableHeight - bbox.height * finalScaleY) / 2f
                        
                        val matrix = Matrix.getTranslateInstance(transX, transY)
                        matrix.scale(finalScaleX, finalScaleY)
                        
                        cs.saveGraphicsState()
                        cs.transform(matrix)
                        cs.drawForm(form)
                        cs.restoreGraphicsState()
                        
                        if (keepBorder) {
                            cs.setStrokingColor(0, 0, 0)
                            cs.setLineWidth(1f)
                            cs.addRect(marginPt, marginPt, availableWidth, availableHeight)
                            cs.stroke()
                        }
                    }
                }
            } else if (pagesPerSheet > 1 && extractedPages.isNotEmpty()) {
                // N-Up logic
                val rows = if (pagesPerSheet == 2) 2 else if (pagesPerSheet == 4) 2 else if (pagesPerSheet == 6) 3 else if (pagesPerSheet == 9) 3 else 1
                val cols = if (pagesPerSheet == 2) 1 else if (pagesPerSheet == 4) 2 else if (pagesPerSheet == 6) 2 else if (pagesPerSheet == 9) 3 else 1
                
                val sheetWidth = pageSize.width
                val sheetHeight = pageSize.height
                
                val contentSpanW = sheetWidth - 2 * marginPt
                val contentSpanH = sheetHeight - 2 * marginPt
                
                val cellWidth = (contentSpanW - (cols - 1) * gapPt) / cols
                val cellHeight = (contentSpanH - (rows - 1) * gapPt) / rows
                
                var currentSheet: PDPage? = null
                var cs: PDPageContentStream? = null
                var pIndex = 0
                
                for (form in extractedPages) {
                    if (pIndex % pagesPerSheet == 0) {
                        cs?.close()
                        currentSheet = PDPage(pageSize)
                        outDoc.addPage(currentSheet)
                        cs = PDPageContentStream(outDoc, currentSheet)
                    }
                    
                    val cellIndex = pIndex % pagesPerSheet
                    val r = cellIndex / cols
                    val c = cellIndex % cols
                    
                    val x = marginPt + c * (cellWidth + gapPt)
                    val y = sheetHeight - marginPt - ((r + 1) * cellHeight) - (r * gapPt) // Top to bottom
                    
                    val bbox = form.bBox
                    
                    val availWidth = cellWidth
                    val availHeight = cellHeight
                    
                    val scaleX = availWidth / bbox.width
                    val scaleY = availHeight / bbox.height
                    
                    val (finalScaleX, finalScaleY) = when (pageFit) {
                        PageFit.FIT_BEST -> {
                            val minScale = Math.min(scaleX, scaleY) * 0.95f
                            minScale to minScale
                        }
                        PageFit.FIT_WIDTH -> scaleX to scaleX
                        PageFit.FIT_HEIGHT -> scaleY to scaleY
                        PageFit.STRETCH -> scaleX to scaleY
                    }
                    
                    val transX = x + (availWidth - bbox.width * finalScaleX) / 2f
                    val transY = y + (availHeight - bbox.height * finalScaleY) / 2f
                    
                    val matrix = Matrix.getTranslateInstance(transX, transY)
                    matrix.scale(finalScaleX, finalScaleY)
                    
                    cs?.saveGraphicsState()
                    cs?.transform(matrix)
                    cs?.drawForm(form)
                    cs?.restoreGraphicsState()
                    
                    if (keepBorder) {
                        cs?.setStrokingColor(0, 0, 0)
                        cs?.setLineWidth(1f)
                        cs?.addRect(x, y, availWidth, availHeight)
                        cs?.stroke()
                    }
                    
                    pIndex++
                }
                cs?.close()
            }

            outDoc.save(outputPdf)
        }
        outputPdf
    }

    private fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parsePageSequence(seq: String, totalPages: Int): List<Int> {
        if (seq.isBlank()) return (0 until totalPages).toList()
        val result = mutableListOf<Int>()
        val parts = seq.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.contains("-")) {
                val bounds = trimmed.split("-")
                if (bounds.size == 2) {
                    val start = bounds[0].trim().toIntOrNull()
                    val end = bounds[1].trim().toIntOrNull()
                    if (start != null && end != null && start > 0 && end >= start) {
                        for (i in start..end) {
                            result.add(i - 1)
                        }
                    }
                }
            } else {
                val page = trimmed.toIntOrNull()
                if (page != null && page > 0) {
                    result.add(page - 1)
                }
            }
        }
        return if (result.isEmpty()) (0 until totalPages).toList() else result
    }

    private fun rearrangeForBooklet(pages: List<PDFormXObject>): MutableList<PDFormXObject> {
        if (pages.isEmpty()) return mutableListOf()
        val numPages = pages.size
        val paddedCount = ceil(numPages / 4.0).toInt() * 4
        
        val paddedPages = pages.toMutableList()
        // Pad with first page for simplicity
        while (paddedPages.size < paddedCount) {
            paddedPages.add(pages.first())
        }

        val result = mutableListOf<PDFormXObject>()
        var left = 0
        var right = paddedCount - 1
        while (left < right) {
            // Back cover
            result.add(paddedPages[right])
            result.add(paddedPages[left])
            // Front cover
            result.add(paddedPages[left + 1])
            result.add(paddedPages[right - 1])
            left += 2
            right -= 2
        }
        return result
    }
}
