package com.scholarvault.ui.tools.pdf_nup

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NUpGraphicalPreviewPanel(
    totalOutputPages: Int,
    virtualPageSeq: Sequence<VirtualPage>,
    config: NUpConfig,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current

    val finalTotalPages = if (totalOutputPages == 0) 1 else totalOutputPages
    val pagerState = rememberPagerState(pageCount = { finalTotalPages })

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            // Source reading order list
            val pagesOnSheet = remember(pageIndex, virtualPageSeq, config, totalOutputPages) {
                val cellsPerSheet = config.columns * config.rows
                if (totalOutputPages == 0) {
                    List(cellsPerSheet) { i ->
                        VirtualPage(
                            fileAlias = ' ',
                            sourcePageIndex = i,
                            displayLabel = "${i + 1}",
                            mediaType = MediaType.PDF,
                            sourceUri = android.net.Uri.EMPTY
                        )
                    }
                } else {
                    virtualPageSeq.drop(pageIndex * cellsPerSheet).take(cellsPerSheet).toList()
                }
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
                val canvasW = config.actualCanvasWidthPoints
                val canvasH = config.actualCanvasHeightPoints

                val sx = size.width / canvasW
                val sy = size.height / canvasH
                val scaleToFit = minOf(sx, sy)

                val drawW = canvasW * scaleToFit
                val drawH = canvasH * scaleToFit
                val offsetX = (size.width - drawW) / 2f
                val offsetY = (size.height - drawH) / 2f

                drawRect(
                    color = Color.White,
                    topLeft = Offset(offsetX, offsetY),
                    size = Size(drawW, drawH)
                )

                drawRect(
                    color = Color.LightGray,
                    topLeft = Offset(offsetX, offsetY),
                    size = Size(drawW, drawH),
                    style = Stroke(width = 1.dp.toPx())
                )

                val innerSpacing = (config.innerSpacingDp * 2.8346456f) * scaleToFit
                
                val marginL = (config.marginLeftDp * 2.8346456f) * scaleToFit
                val marginT = (config.marginTopDp * 2.8346456f) * scaleToFit
                val marginR = (config.marginRightDp * 2.8346456f) * scaleToFit
                val marginB = (config.marginBottomDp * 2.8346456f) * scaleToFit

                val areaW = drawW - marginL - marginR
                val areaH = drawH - marginT - marginB

                val cellW = (areaW - innerSpacing * (config.columns - 1)) / config.columns
                val cellH = (areaH - innerSpacing * (config.rows - 1)) / config.rows

                // We need to map reading order index to physical cell Grid(c, r).
                // Standardized and aligned with NUpProcessor logic:
                val isLTR = config.arrangementOrder == ArrangementOrder.LTR
                val isHorizontalPrimary = config.flowLayout == FlowLayout.HORIZONTAL_TTB || config.flowLayout == FlowLayout.HORIZONTAL_BTT
                val isTTB = config.flowLayout == FlowLayout.HORIZONTAL_TTB || config.flowLayout == FlowLayout.VERTICAL_TTB

                val getCellCol = { i: Int ->
                    val cId = if (isHorizontalPrimary) i % config.columns else i / config.rows
                    if (isLTR) cId else (config.columns - 1 - cId)
                }

                val getCellRow = { i: Int ->
                    val rId = if (isHorizontalPrimary) i / config.columns else i % config.rows
                    if (isTTB) rId else (config.rows - 1 - rId)
                }

                for (i in pagesOnSheet.indices) {
                    val c = getCellCol(i)
                    val r = getCellRow(i)
                    val vPage = pagesOnSheet[i]

                    val cellTop = offsetY + marginT + r * (cellH + innerSpacing)
                    val cellLeft = offsetX + marginL + c * (cellW + innerSpacing)

                    // Always draw an elegant ultra-light boundary to clearly delineate page sheets and spacing differences
                    drawRect(
                        color = Color(0xFFE2E2E2),
                        topLeft = Offset(cellLeft, cellTop),
                        size = Size(cellW, cellH),
                        style = Stroke(width = 0.5f.dp.toPx())
                    )

                    // Draw output outline preview only if drawBorder is enabled
                    if (config.drawBorder) {
                        val topW = maxOf(0f, config.borderTopWidthDp * 2.8346456f * scaleToFit)
                        val botW = maxOf(0f, config.borderBottomWidthDp * 2.8346456f * scaleToFit)
                        val leftW = maxOf(0f, config.borderLeftWidthDp * 2.8346456f * scaleToFit)
                        val rightW = maxOf(0f, config.borderRightWidthDp * 2.8346456f * scaleToFit)
                        val borderColor = Color.Black.copy(alpha = 0.6f)

                        if (topW > 0f) drawLine(borderColor, Offset(cellLeft, cellTop), Offset(cellLeft + cellW, cellTop), strokeWidth = topW)
                        if (botW > 0f) drawLine(borderColor, Offset(cellLeft, cellTop + cellH), Offset(cellLeft + cellW, cellTop + cellH), strokeWidth = botW)
                        if (leftW > 0f) drawLine(borderColor, Offset(cellLeft, cellTop), Offset(cellLeft, cellTop + cellH), strokeWidth = leftW)
                        if (rightW > 0f) drawLine(borderColor, Offset(cellLeft + cellW, cellTop), Offset(cellLeft + cellW, cellTop + cellH), strokeWidth = rightW)
                    }

                    // Draw layout indicator!
                    val str = if (vPage.fileAlias == ' ') "${vPage.sourcePageIndex + 1}" else "${vPage.fileAlias}${vPage.sourcePageIndex + 1}"
                    val computedFontSize = (cellH * 0.20f).toSp()
                    val finalFontSize = computedFontSize.value.coerceIn(11f, 24f).sp
                    val textLayoutResult = textMeasurer.measure(
                        text = str,
                        style = TextStyle(
                            fontSize = finalFontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )
                    )
                    
                    val txtW = textLayoutResult.size.width
                    val txtH = textLayoutResult.size.height
                    
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(cellLeft + (cellW - txtW)/2f, cellTop + (cellH - txtH)/2f),
                    )
                }
            }
            // dots indicator
            if (totalOutputPages > 1) {
                Box(modifier = Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val maxDots = 5
                        val dotCount = minOf(totalOutputPages, maxDots)
                        val startIx = maxOf(0, minOf(pageIndex - maxDots/2, totalOutputPages - maxDots))
                        for(i in 0 until dotCount) {
                            val active = (startIx + i == pageIndex)
                            Box(modifier = Modifier.size(if(active) 6.dp else 4.dp).background(if(active) MaterialTheme.colorScheme.primary else Color.LightGray, shape = androidx.compose.foundation.shape.CircleShape))
                        }
                    }
                }
            }
        }
    }
}
