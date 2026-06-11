package com.scholarvault.ui.tools.pdf_nup

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun applyPageFit(srcWidth: Float, srcHeight: Float, left: Float, top: Float, right: Float, bottom: Float, fit: PageFit): android.graphics.RectF {
    val targetRect = android.graphics.RectF(left, top, right, bottom)
    val targetWidth = targetRect.width()
    val targetHeight = targetRect.height()

    if (targetWidth <= 0 || targetHeight <= 0 || srcWidth <= 0 || srcHeight <= 0) return targetRect

    val srcRatio = srcWidth / srcHeight
    val targetRatio = targetWidth / targetHeight

    return when (fit) {
        PageFit.STRETCH -> {
            targetRect
        }
        PageFit.FULL_WIDTH -> {
            val finalHeight = targetWidth / srcRatio
            val t = targetRect.top + (targetHeight - finalHeight) / 2f
            android.graphics.RectF(targetRect.left, t, targetRect.right, t + finalHeight)
        }
        PageFit.FULL_HEIGHT -> {
            val finalWidth = targetHeight * srcRatio
            val l = targetRect.left + (targetWidth - finalWidth) / 2f
            android.graphics.RectF(l, targetRect.top, l + finalWidth, targetRect.bottom)
        }
        PageFit.AUTO -> {
            if (srcRatio > targetRatio) {
                val finalHeight = targetWidth / srcRatio
                val t = targetRect.top + (targetHeight - finalHeight) / 2f
                android.graphics.RectF(targetRect.left, t, targetRect.right, t + finalHeight)
            } else {
                val finalWidth = targetHeight * srcRatio
                val l = targetRect.left + (targetWidth - finalWidth) / 2f
                android.graphics.RectF(l, targetRect.top, l + finalWidth, targetRect.bottom)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NUpPreviewPanel(
    totalOutputPages: Int,
    virtualPageSeq: Sequence<VirtualPage>,
    config: NUpConfig,
    cacheManager: PreviewCacheManager,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (totalOutputPages == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Add a document to preview", color = Color.Gray)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { totalOutputPages })

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            
            val pagesOnSheet = remember(pageIndex, virtualPageSeq, config) {
                val cellsPerSheet = config.columns * config.rows
                virtualPageSeq.drop(pageIndex * cellsPerSheet).take(cellsPerSheet).toList()
            }
            
            var cacheTrigger by remember(pageIndex, pagesOnSheet) { androidx.compose.runtime.mutableStateOf(0) }
            val isGenerating = remember { androidx.compose.runtime.mutableStateOf(false) }

            LaunchedEffect(pageIndex, pagesOnSheet) {
                isGenerating.value = true
                withContext(Dispatchers.IO) {
                    val renderThumbnail: suspend (VirtualPage, Int, Int) -> android.graphics.Bitmap? = { vPage, w, h ->
                        try {
                            when (vPage.mediaType) {
                                MediaType.PDF -> {
                                    val fd = context.contentResolver.openFileDescriptor(vPage.sourceUri, "r")
                                    if (fd != null) {
                                        val renderer = android.graphics.pdf.PdfRenderer(fd)
                                        if (vPage.sourcePageIndex < renderer.pageCount) {
                                            val page = renderer.openPage(vPage.sourcePageIndex)
                                            val scale = minOf(w.toFloat() / page.width, h.toFloat() / page.height)
                                            val bmpW = (page.width * scale).toInt().coerceAtLeast(1)
                                            val bmpH = (page.height * scale).toInt().coerceAtLeast(1)
                                            val bmp = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
                                            val canvas = android.graphics.Canvas(bmp)
                                            canvas.drawColor(android.graphics.Color.WHITE)
                                            page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                            page.close()
                                            renderer.close()
                                            fd.close()
                                            bmp
                                        } else {
                                            renderer.close()
                                            fd.close()
                                            null
                                        }
                                    } else null
                                }
                                MediaType.IMAGE -> {
                                    val bitmap = try {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, vPage.sourceUri)
                                            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                                                val s = minOf(w.toFloat() / info.size.width, h.toFloat() / info.size.height)
                                                decoder.setTargetSampleSize(maxOf(1, (1f / s).toInt()))
                                            }
                                        } else {
                                            context.contentResolver.openInputStream(vPage.sourceUri)?.use { stream ->
                                                val opts = android.graphics.BitmapFactory.Options()
                                                opts.inJustDecodeBounds = true
                                                android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                                                val s = minOf(w.toFloat() / opts.outWidth, h.toFloat() / opts.outHeight)
                                                opts.inSampleSize = maxOf(1, (1f / s).toInt())
                                                opts.inJustDecodeBounds = false
                                                context.contentResolver.openInputStream(vPage.sourceUri)?.use { stream2 ->
                                                    android.graphics.BitmapFactory.decodeStream(stream2, null, opts)
                                                }
                                            }
                                        }
                                    } catch (t: Throwable) {
                                        null
                                    }
                                    if (bitmap != null) {
                                        val scale = minOf(w.toFloat() / bitmap.width, h.toFloat() / bitmap.height)
                                        val bmpW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                                        val bmpH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                                        android.graphics.Bitmap.createScaledBitmap(bitmap, bmpW, bmpH, true).also {
                                            if (it != bitmap) bitmap.recycle()
                                        }
                                    } else null
                                }
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    for (vPage in pagesOnSheet) {
                        val key = "${vPage.sourceUri.toString().hashCode()}_${vPage.sourcePageIndex}"
                        if (cacheManager.getThumbnail(key) == null) {
                            val bitmap = renderThumbnail(vPage, 200, 280)
                            if (bitmap != null) {
                                cacheManager.putThumbnail(key, bitmap)
                                cacheTrigger++
                            }
                        }
                    }
                }
                isGenerating.value = false
            }

            Canvas(modifier = Modifier.fillMaxSize().pointerInput(config) {
                detectTapGestures(
                    onTap = {
                        val cellW = (config.actualCanvasWidthPoints / config.columns) * 0.352778f
                        val cellH = (config.actualCanvasHeightPoints / config.rows) * 0.352778f
                        Toast.makeText(context, "Each cell: ${String.format("%.1f", cellW)} × ${String.format("%.1f", cellH)} mm", Toast.LENGTH_SHORT).show()
                    }
                )
            }) {
                val trigger = cacheTrigger

                val canvasW = config.actualCanvasWidthPoints
                val canvasH = config.actualCanvasHeightPoints

                val scaleToFit = minOf(
                    size.width / canvasW,
                    size.height / canvasH
                ) * 0.95f

                val scaledW = canvasW * scaleToFit
                val scaledH = canvasH * scaleToFit

                val offsetX = (size.width - scaledW) / 2f
                val offsetY = (size.height - scaledH) / 2f

                drawRoundRect(
                    Color.Black.copy(alpha = 0.1f),
                    Offset(offsetX + 4.dp.toPx(), offsetY + 4.dp.toPx()),
                    Size(scaledW, scaledH)
                )

                drawRect(
                    color = Color.White,
                    topLeft = Offset(offsetX, offsetY),
                    size = Size(scaledW, scaledH)
                )

                drawRect(
                    color = Color.LightGray,
                    topLeft = Offset(offsetX, offsetY),
                    size = Size(scaledW, scaledH),
                    style = Stroke(width = 1.dp.toPx())
                )

                val marginTopPx = config.marginTopDp * 2.8346456f * scaleToFit
                val marginBottomPx = config.marginBottomDp * 2.8346456f * scaleToFit
                val marginLeftPx = config.marginLeftDp * 2.8346456f * scaleToFit
                val marginRightPx = config.marginRightDp * 2.8346456f * scaleToFit
                val innerSpacingPx = config.innerSpacingDp * 2.8346456f * scaleToFit

                val availableW = scaledW - marginLeftPx - marginRightPx - ((config.columns - 1) * innerSpacingPx)
                val availableH = scaledH - marginTopPx - marginBottomPx - ((config.rows - 1) * innerSpacingPx)

                val cellW = availableW / config.columns
                val cellH = availableH / config.rows

                val textStyle = TextStyle(fontSize = (cellH * 0.3f).toSp(), color = Color.Black.copy(alpha = 0.3f))

                for (i in 0 until (config.rows * config.columns)) {
                    val isLTR = config.arrangementOrder == ArrangementOrder.LTR
                    val isHorizontalPrimary = config.flowLayout == FlowLayout.HORIZONTAL_TTB || config.flowLayout == FlowLayout.HORIZONTAL_BTT
                    val isTTB = config.flowLayout == FlowLayout.HORIZONTAL_TTB || config.flowLayout == FlowLayout.VERTICAL_TTB
                    
                    val r = if (isHorizontalPrimary) i / config.columns else i % config.rows
                    val c = if (isHorizontalPrimary) i % config.columns else i / config.rows
                    
                    val actualR = if (isTTB) r else (config.rows - 1 - r)
                    val actualC = if (isLTR) c else (config.columns - 1 - c)
                    
                    val cellLeft = offsetX + marginLeftPx + actualC * (cellW + innerSpacingPx)
                    val cellTop = offsetY + marginTopPx + actualR * (cellH + innerSpacingPx)

                    drawRect(
                        color = Color.White,
                        topLeft = Offset(cellLeft, cellTop),
                        size = Size(cellW, cellH)
                    )

                    var fittedLeft = cellLeft
                    var fittedTop = cellTop
                    var fittedRight = cellLeft + cellW
                    var fittedBottom = cellTop + cellH

                    var thumbnailWidth = 200f
                    var thumbnailHeight = 280f
                    var thumbnailLoaded = false

                    if (i < pagesOnSheet.size) {
                        val vPage = pagesOnSheet[i]
                        val cacheKey = "${vPage.sourceUri.toString().hashCode()}_${vPage.sourcePageIndex}"
                        val thumbnail = cacheManager.getThumbnail(cacheKey)
                        if (thumbnail != null) {
                            thumbnailWidth = thumbnail.width.toFloat()
                            thumbnailHeight = thumbnail.height.toFloat()
                            thumbnailLoaded = true

                            val fitted = applyPageFit(
                                thumbnailWidth,
                                thumbnailHeight,
                                cellLeft,
                                cellTop,
                                cellLeft + cellW,
                                cellTop + cellH,
                                config.pageFit
                            )
                            fittedLeft = fitted.left
                            fittedTop = fitted.top
                            fittedRight = fitted.right
                            fittedBottom = fitted.bottom

                            val topPts = config.borderTopWidthDp * 2.8346456f * scaleToFit
                            val botPts = config.borderBottomWidthDp * 2.8346456f * scaleToFit
                            val leftPts = config.borderLeftWidthDp * 2.8346456f * scaleToFit
                            val rightPts = config.borderRightWidthDp * 2.8346456f * scaleToFit

                            val finalLeft = fittedLeft + leftPts / 2f
                            val finalTop = fittedTop + topPts / 2f
                            val finalRight = fittedRight - rightPts / 2f
                            val finalBottom = fittedBottom - botPts / 2f

                            val dw = maxOf(1f, finalRight - finalLeft)
                            val dh = maxOf(1f, finalBottom - finalTop)

                            drawImage(
                                image = thumbnail.asImageBitmap(),
                                dstSize = androidx.compose.ui.unit.IntSize(dw.toInt(), dh.toInt()),
                                dstOffset = androidx.compose.ui.unit.IntOffset(finalLeft.toInt(), finalTop.toInt())
                            )
                        } else {
                            val textLayoutResult = textMeasurer.measure(text = vPage.displayLabel, style = textStyle)
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(
                                    cellLeft + (cellW - textLayoutResult.size.width) / 2f,
                                    cellTop + (cellH - textLayoutResult.size.height) / 2f
                                )
                            )
                        }
                    }

                    drawRect(
                        color = Color(0xFFE2E2E2),
                        topLeft = Offset(cellLeft, cellTop),
                        size = Size(cellW, cellH),
                        style = Stroke(width = 0.5f.dp.toPx())
                    )

                    if (config.drawBorder) {
                        val topW = maxOf(0f, config.borderTopWidthDp * 2.8346456f * scaleToFit)
                        val botW = maxOf(0f, config.borderBottomWidthDp * 2.8346456f * scaleToFit)
                        val leftW = maxOf(0f, config.borderLeftWidthDp * 2.8346456f * scaleToFit)
                        val rightW = maxOf(0f, config.borderRightWidthDp * 2.8346456f * scaleToFit)
                        val borderColor = Color.DarkGray

                        val fitted = applyPageFit(
                            thumbnailWidth,
                            thumbnailHeight,
                            cellLeft,
                            cellTop,
                            cellLeft + cellW,
                            cellTop + cellH,
                            config.pageFit
                        )

                        val finalLeft = fitted.left + leftW / 2f
                        val finalTop = fitted.top + topW / 2f
                        val finalRight = fitted.right - rightW / 2f
                        val finalBottom = fitted.bottom - botW / 2f

                        if (topW > 0f) {
                            drawLine(borderColor, Offset(finalLeft - leftW/2, finalTop - topW/2), Offset(finalRight + rightW/2, finalTop - topW/2), strokeWidth = topW)
                        }
                        if (botW > 0f) {
                            drawLine(borderColor, Offset(finalLeft - leftW/2, finalBottom + botW/2), Offset(finalRight + rightW/2, finalBottom + botW/2), strokeWidth = botW)
                        }
                        if (leftW > 0f) {
                            drawLine(borderColor, Offset(finalLeft - leftW/2, finalTop - topW/2), Offset(finalLeft - leftW/2, finalBottom + botW/2), strokeWidth = leftW)
                        }
                        if (rightW > 0f) {
                            drawLine(borderColor, Offset(finalRight + rightW/2, finalTop - topW/2), Offset(finalRight + rightW/2, finalBottom + botW/2), strokeWidth = rightW)
                        }
                    }
                }
            }
        }
        
        Text(
            text = "Sheet ${pagerState.currentPage + 1} of $totalOutputPages",
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
            style = TextStyle(fontSize = 12.sp, color = Color.Gray)
        )
    }
}
