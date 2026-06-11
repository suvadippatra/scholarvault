package com.scholarvault.ui.tools

import android.net.Uri
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FastComposePreview(
    items: List<PrintJobItem>,
    pagesPerSheet: Int,
    keepBorder: Boolean,
    pageMarginMm: Float,
    pageGapMm: Float,
    bookletMode: Boolean,
    pageFit: PdfNUpProcessor.PageFit
) {
    val context = LocalContext.current
    
    data class ResolvedPage(val uri: Uri, val isImage: Boolean, val originalPageIndex: Int)
    
    var resolvedPages by remember(items) { mutableStateOf<List<ResolvedPage>?>(null) }
    
    LaunchedEffect(items) {
        resolvedPages = withContext(Dispatchers.IO) {
            val result = mutableListOf<ResolvedPage>()
            for (item in items) {
                if (item.isImage) {
                    result.add(ResolvedPage(item.uri, true, 0))
                } else {
                    var pageCount = 0
                    try {
                        context.contentResolver.openFileDescriptor(item.uri, "r")?.use { pfd ->
                            val renderer = PdfRenderer(pfd)
                            pageCount = renderer.pageCount
                            renderer.close()
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                    
                    val parts = item.pageSequence.split(",")
                    val targetPages = mutableListOf<Int>()
                    if (item.pageSequence.isBlank()) {
                        targetPages.addAll(0 until pageCount)
                    } else {
                        for (part in parts) {
                            val trimmed = part.trim()
                            if (trimmed.isEmpty()) continue
                            if (trimmed.contains("-")) {
                                val bounds = trimmed.split("-")
                                if (bounds.size == 2) {
                                    val start = bounds[0].trim().toIntOrNull()
                                    val end = bounds[1].trim().toIntOrNull()
                                    if (start != null && end != null && start > 0 && end >= start) {
                                        for (i in start..end) targetPages.add(i - 1)
                                    }
                                }
                            } else {
                                val page = trimmed.toIntOrNull()
                                if (page != null && page > 0) targetPages.add(page - 1)
                            }
                        }
                        if (targetPages.isEmpty()) targetPages.addAll(0 until pageCount)
                    }
                    
                    for (p in targetPages) {
                        if (p in 0 until pageCount) result.add(ResolvedPage(item.uri, false, p))
                    }
                }
            }
            result
        }
    }
    
    if (resolvedPages == null) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    val finalPages = remember(resolvedPages, bookletMode) {
        val list = resolvedPages!!
        if (bookletMode) {
            val paddedCount = kotlin.math.ceil(list.size / 4.0).toInt() * 4
            val paddedPages = list.toMutableList<ResolvedPage?>()
            while (paddedPages.size < paddedCount) paddedPages.add(null)
            val result = mutableListOf<ResolvedPage?>()
            for (i in 0 until (paddedCount / 2) step 2) {
                result.add(paddedPages[paddedCount - 1 - i])
                result.add(paddedPages[i])
                result.add(paddedPages[i + 1])
                result.add(paddedPages[paddedCount - 2 - i])
            }
            result
        } else {
            list
        }
    }
    
    val sheets = remember(finalPages, pagesPerSheet) {
        finalPages.chunked(pagesPerSheet)
    }
    
    val a4Ratio = 0.7071f
    
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 600.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(sheets.size, key = { it }) { sheetIndex ->
            val sheetPages = sheets[sheetIndex]
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().aspectRatio(a4Ratio).background(Color.White).clip(RoundedCornerShape(4.dp))
            ) {
                val realWidth = maxWidth
                val mmToDpResult = realWidth.value / 210f
                val dpMargin = (pageMarginMm * mmToDpResult).dp
                val dpGap = (pageGapMm * mmToDpResult).dp
                
                val rows = if (pagesPerSheet == 2) 2 else if (pagesPerSheet == 4) 2 else if (pagesPerSheet == 6) 3 else if (pagesPerSheet == 9) 3 else 1
                val cols = if (pagesPerSheet == 2) 1 else if (pagesPerSheet == 4) 2 else if (pagesPerSheet == 6) 2 else if (pagesPerSheet == 9) 3 else 1
                
                Column(modifier = Modifier.fillMaxSize().padding(dpMargin), verticalArrangement = Arrangement.spacedBy(dpGap)) {
                    for (r in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(dpGap)) {
                            for (c in 0 until cols) {
                                val cellIndex = r * cols + c
                                val pageSource = sheetPages.getOrNull(cellIndex)
                                Box(modifier = Modifier.fillMaxHeight().weight(1f)) {
                                    if (pageSource != null) {
                                        val borderMod = if (keepBorder) Modifier.border(0.5.dp, Color.Black) else Modifier
                                        Box(
                                            modifier = Modifier.fillMaxSize().then(borderMod),
                                        ) {
                                            if (pageSource.isImage) {
                                                AsyncImage(
                                                    model = pageSource.uri,
                                                    contentDescription = null,
                                                    contentScale = when (pageFit) {
                                                        PdfNUpProcessor.PageFit.FIT_BEST -> ContentScale.Fit
                                                        PdfNUpProcessor.PageFit.FIT_WIDTH -> ContentScale.FillWidth
                                                        PdfNUpProcessor.PageFit.FIT_HEIGHT -> ContentScale.FillHeight
                                                        PdfNUpProcessor.PageFit.STRETCH -> ContentScale.FillBounds
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                NativePdfPageThumbnail(
                                                    uri = pageSource.uri,
                                                    pageIndex = pageSource.originalPageIndex,
                                                    contentScale = when(pageFit) {
                                                        PdfNUpProcessor.PageFit.FIT_BEST -> ContentScale.Fit
                                                        PdfNUpProcessor.PageFit.FIT_WIDTH -> ContentScale.FillWidth
                                                        PdfNUpProcessor.PageFit.FIT_HEIGHT -> ContentScale.FillHeight
                                                        PdfNUpProcessor.PageFit.STRETCH -> ContentScale.FillBounds
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
