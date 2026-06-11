package com.scholarvault.ui.tools.pdf_nup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object NUpProcessor {

    private val processorMutex = Mutex()

    private fun mmToPts(mm: Float): Float = mm * 2.8346456f
    
    fun RectF.inset(l: Float, t: Float, r: Float, b: Float): RectF {
        return RectF(
            left + l,
            top + t,
            right - r,
            bottom - b
        )
    }

    private fun drawIndependentBorders(canvas: android.graphics.Canvas, rect: RectF, borderPtsArray: FloatArray, color: Int) {
        val (topW, bottomW, leftW, rightW) = borderPtsArray
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        
        // Draw each line separately if width > 0
        if (topW > 0f) {
            paint.strokeWidth = topW
            canvas.drawLine(rect.left - leftW/2, rect.top - topW/2, rect.right + rightW/2, rect.top - topW/2, paint)
        }
        if (bottomW > 0f) {
            paint.strokeWidth = bottomW
            canvas.drawLine(rect.left - leftW/2, rect.bottom + bottomW/2, rect.right + rightW/2, rect.bottom + bottomW/2, paint)
        }
        if (leftW > 0f) {
            paint.strokeWidth = leftW
            canvas.drawLine(rect.left - leftW/2, rect.top - topW/2, rect.left - leftW/2, rect.bottom + bottomW/2, paint)
        }
        if (rightW > 0f) {
            paint.strokeWidth = rightW
            canvas.drawLine(rect.right + rightW/2, rect.top - topW/2, rect.right + rightW/2, rect.bottom + bottomW/2, paint)
        }
    }

    suspend fun processCombined(
        context: Context,
        sequence: Sequence<VirtualPage>,
        config: NUpConfig,
        totalOutputPages: Int,
        onProgressUpdate: (Int) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        processorMutex.withLock {
            val renderers = mutableMapOf<String, PdfRenderer>()
            val fds = mutableMapOf<String, ParcelFileDescriptor>()
            var pdfDocument: PdfDocument? = null
            
            try {
                pdfDocument = PdfDocument()

                val cols = config.columns
                val rows = config.rows
                val pagesPerSheet = cols * rows

                val canvasWidth = config.actualCanvasWidthPoints
                val canvasHeight = config.actualCanvasHeightPoints

                val innerSpacing = mmToPts(config.innerSpacingDp)

                val marginTop = mmToPts(config.marginTopDp)
                val marginBottom = mmToPts(config.marginBottomDp)
                val marginLeft = mmToPts(config.marginLeftDp)
                val marginRight = mmToPts(config.marginRightDp)

                val availableWidth = canvasWidth - marginLeft - marginRight - ((cols - 1) * innerSpacing)
                val availableHeight = canvasHeight - marginTop - marginBottom - ((rows - 1) * innerSpacing)

                val cellWidth = availableWidth / cols
                val cellHeight = availableHeight / rows

                var currentSheet = 0
                var cellCount = 0
                var pageInfo = PdfDocument.PageInfo.Builder(canvasWidth.toInt(), canvasHeight.toInt(), 1).create()
                var currentPage: PdfDocument.Page? = pdfDocument.startPage(pageInfo)
                
                if (currentPage != null) {
                    currentPage.canvas.drawColor(Color.WHITE)
                }

                for (vPage in sequence) {
                    kotlinx.coroutines.yield()
                    try {
                        val isLTR = config.arrangementOrder == ArrangementOrder.LTR
                        val isHorizontalPrimary = config.flowLayout == FlowLayout.HORIZONTAL_TTB || config.flowLayout == FlowLayout.HORIZONTAL_BTT
                        val isTTB = config.flowLayout == FlowLayout.HORIZONTAL_TTB || config.flowLayout == FlowLayout.VERTICAL_TTB
                        
                        val rId = if (isHorizontalPrimary) cellCount / cols else cellCount % rows
                        val cId = if (isHorizontalPrimary) cellCount % cols else cellCount / rows
                        
                        val actualR = if (isTTB) rId else (rows - 1 - rId)
                        val actualC = if (isLTR) cId else (cols - 1 - cId)
                        
                        val cellLeft = marginLeft + actualC * (cellWidth + innerSpacing)
                        val cellTop = marginTop + actualR * (cellHeight + innerSpacing)
                        val cellRect = RectF(cellLeft, cellTop, cellLeft + cellWidth, cellTop + cellHeight)

                        val topPts = mmToPts(config.borderTopWidthDp)
                        val botPts = mmToPts(config.borderBottomWidthDp)
                        val leftPts = mmToPts(config.borderLeftWidthDp)
                        val rightPts = mmToPts(config.borderRightWidthDp)
                        val borderPtsArray = floatArrayOf(topPts, botPts, leftPts, rightPts)

                        when (vPage.mediaType) {
                            MediaType.PDF -> {
                                val uriString = vPage.sourceUri.toString()
                                if (!renderers.containsKey(uriString)) {
                                    val fd = context.contentResolver.openFileDescriptor(vPage.sourceUri, "r")
                                        ?: throw IOException("Could not open fd for ${vPage.sourceUri}")
                                    fds[uriString] = fd
                                    renderers[uriString] = PdfRenderer(fd)
                                }
                                val pdfRenderer = renderers[uriString]!!
                                if (vPage.sourcePageIndex < pdfRenderer.pageCount) {
                                    var srcPage: PdfRenderer.Page? = null
                                    try {
                                        srcPage = pdfRenderer.openPage(vPage.sourcePageIndex)
                                        val originalWidth = srcPage.width.toFloat()
                                        val originalHeight = srcPage.height.toFloat()
                                        
                                        val fittedRect = applyPageFit(originalWidth, originalHeight, cellRect, config.pageFit)
                                        val finalDestRect = fittedRect.inset(leftPts / 2f, topPts / 2f, rightPts / 2f, botPts / 2f)
                                        
                                        val bmpW = (finalDestRect.width() * (300f / 72f)).toInt().coerceAtLeast(1)
                                        val bmpH = (finalDestRect.height() * (300f / 72f)).toInt().coerceAtLeast(1)
                                        val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                                        bmp.eraseColor(Color.WHITE)
                                        
                                        srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                        
                                        if (currentPage != null) {
                                            currentPage.canvas.drawBitmap(bmp, null, finalDestRect, null)
                                            if (config.drawBorder) {
                                                drawIndependentBorders(currentPage.canvas, finalDestRect, borderPtsArray, config.defaultBorderColor)
                                            }
                                        }
                                        bmp.recycle()
                                    } catch (e: Throwable) {
                                        e.printStackTrace()
                                    } finally {
                                        try {
                                            srcPage?.close()
                                        } catch (ignored: Throwable) {}
                                    }
                                }
                            }
                            MediaType.IMAGE -> {
                                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    val source = ImageDecoder.createSource(context.contentResolver, vPage.sourceUri)
                                    ImageDecoder.decodeBitmap(source)
                                } else {
                                    context.contentResolver.openInputStream(vPage.sourceUri)?.use { stream ->
                                        BitmapFactory.decodeStream(stream)
                                    }
                                }
                                if (bitmap != null) {
                                    val originalWidth = bitmap.width.toFloat()
                                    val originalHeight = bitmap.height.toFloat()
                                    
                                    val fittedRect = applyPageFit(originalWidth, originalHeight, cellRect, config.pageFit)
                                    val finalDestRect = fittedRect.inset(leftPts / 2f, topPts / 2f, rightPts / 2f, botPts / 2f)
                                    
                                    if (currentPage != null) {
                                        currentPage.canvas.drawBitmap(bitmap, null, finalDestRect, null)
                                        if (config.drawBorder) {
                                            drawIndependentBorders(currentPage.canvas, finalDestRect, borderPtsArray, config.defaultBorderColor)
                                        }
                                    }
                                    bitmap.recycle()
                                }
                            }
                        }

                        cellCount++
                        if (cellCount >= pagesPerSheet) {
                            if (currentPage != null) {
                                pdfDocument.finishPage(currentPage)
                            }
                            currentSheet++
                            onProgressUpdate(currentSheet)
                            cellCount = 0
                            
                            if (currentSheet < totalOutputPages) {
                                pageInfo = PdfDocument.PageInfo.Builder(canvasWidth.toInt(), canvasHeight.toInt(), currentSheet + 1).create()
                                currentPage = pdfDocument.startPage(pageInfo)
                                currentPage.canvas.drawColor(Color.WHITE)
                            } else {
                                currentPage = null
                            }
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }

                if (currentPage != null && cellCount > 0) {
                    pdfDocument.finishPage(currentPage)
                }

                val tempFile = File(context.cacheDir, "nup_temp_${System.currentTimeMillis()}.pdf")
                FileOutputStream(tempFile).use { out ->
                    pdfDocument.writeTo(out)
                }

                return@withContext tempFile
                
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            } finally {
                pdfDocument?.close()
                renderers.values.forEach { it.close() }
                fds.values.forEach { it.close() }
            }
        }
    }

    private fun applyPageFit(srcWidth: Float, srcHeight: Float, targetRect: RectF, fit: PageFit): RectF {
        val targetWidth = targetRect.width()
        val targetHeight = targetRect.height()

        val srcRatio = srcWidth / srcHeight
        val targetRatio = targetWidth / targetHeight

        return when (fit) {
            PageFit.STRETCH -> {
                targetRect
            }
            PageFit.FULL_WIDTH -> {
                val finalHeight = targetWidth / srcRatio
                val top = targetRect.top + (targetHeight - finalHeight) / 2f
                RectF(targetRect.left, top, targetRect.right, top + finalHeight)
            }
            PageFit.FULL_HEIGHT -> {
                val finalWidth = targetHeight * srcRatio
                val left = targetRect.left + (targetWidth - finalWidth) / 2f
                RectF(left, targetRect.top, left + finalWidth, targetRect.bottom)
            }
            PageFit.AUTO -> {
                if (srcRatio > targetRatio) {
                    val finalHeight = targetWidth / srcRatio
                    val top = targetRect.top + (targetHeight - finalHeight) / 2f
                    RectF(targetRect.left, top, targetRect.right, top + finalHeight)
                } else {
                    val finalWidth = targetHeight * srcRatio
                    val left = targetRect.left + (targetWidth - finalWidth) / 2f
                    RectF(left, targetRect.top, left + finalWidth, targetRect.bottom)
                }
            }
        }
    }
}

