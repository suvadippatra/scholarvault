package com.scholarvault.ui.tools.pdf_inverter

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asComposeColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    uri: Uri,
    initialMode: InvertMode? = null,
    pagesToInvertStr: String = "All",
    onModeSelected: ((InvertMode) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val viewModel: PdfPreviewViewModel = viewModel()
    
    val pageCount by viewModel.pageCount.collectAsState()
    val pageBitmap by viewModel.pageBitmap.collectAsState()
    
    var previewPagesStr by remember { mutableStateOf("") }
    var currentPage by remember { mutableStateOf(0) }
    
    val previewPages = remember(previewPagesStr, pageCount) {
        PdfInverterProcessor.parsePages(previewPagesStr, pageCount).sorted()
    }
    
    LaunchedEffect(previewPages) {
        if (previewPages.isNotEmpty() && !previewPages.contains(currentPage)) {
            currentPage = previewPages.firstOrNull() ?: 0
        }
    }
    
    val currentIndexInPreview = previewPages.indexOf(currentPage).takeIf { it != -1 } ?: 0
    val actualCurrentPage = previewPages.getOrNull(currentIndexInPreview) ?: 0
    
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current
    val pixelWidth = with(density) { screenWidth.toPx().toInt() }
    
    LaunchedEffect(uri) {
        viewModel.openUri(uri, pixelWidth)
    }

    LaunchedEffect(actualCurrentPage) {
        viewModel.requestedPage.value = actualCurrentPage
    }
    
    // Inversion adjustment state
    var activeMode by remember { mutableStateOf(initialMode) }
    var showExpectedInverted by remember { mutableStateOf(initialMode != null) }

    val parsedPagesToInvert = remember(pagesToInvertStr, pageCount) {
        PdfInverterProcessor.parsePages(pagesToInvertStr, pageCount)
    }
    val shouldInvertThisPage = remember(parsedPagesToInvert, actualCurrentPage, showExpectedInverted) {
        showExpectedInverted && parsedPagesToInvert.contains(actualCurrentPage)
    }

    val colorFilter = remember(shouldInvertThisPage, activeMode) {
        if (shouldInvertThisPage && activeMode != null) {
            val matrix = when (activeMode!!) {
                InvertMode.WORD_PDF -> {
                    ColorMatrix(floatArrayOf(
                        -1f,  0f,  0f,  0f, 255f,
                         0f, -1f,  0f,  0f, 255f,
                         0f,  0f, -1f,  0f, 255f,
                         0f,  0f,  0f,  1f,   0f
                    ))
                }
                InvertMode.SCANNED_PDF -> {
                    ColorMatrix(floatArrayOf(
                        -1.2f,  0f,  0f,  0f, 255f,
                         0f, -1.2f,  0f,  0f, 255f,
                         0f,  0f, -1.2f,  0f, 255f,
                         0f,  0f,  0f,  1f,   0f
                    ))
                }
                InvertMode.SMART_INVERT -> {
                    ColorMatrix(floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f,  0f,  0f, 1f,   0f
                    ))
                }
            }
            ColorMatrixColorFilter(matrix).asComposeColorFilter()
        } else {
            null
        }
    }
    
    Scaffold(
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = onDismiss,
                isBackButton = true,
                title = if (initialMode != null) "Interactive Output Preview" else "PDF Preview",
                showProfileIcon = false,
                showSearchBar = false
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        if (currentIndexInPreview > 0) {
                            currentPage = previewPages[currentIndexInPreview - 1]
                        }
                    }, enabled = currentIndexInPreview > 0) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous")
                    }
                    Text("Page ${actualCurrentPage + 1} / ${kotlin.math.max(1, pageCount)}", fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        if (currentIndexInPreview < previewPages.size - 1) {
                            currentPage = previewPages[currentIndexInPreview + 1]
                        }
                    }, enabled = currentIndexInPreview < previewPages.size - 1) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            // Expected Output Preview Adjuster
            if (initialMode != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Preview Expected Output", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Instantly simulates color inversion on device", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = showExpectedInverted,
                                onCheckedChange = { showExpectedInverted = it }
                            )
                        }
                        
                        if (showExpectedInverted && activeMode != null) {
                            Spacer(Modifier.height(8.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                            Spacer(Modifier.height(8.dp))
                            Text("Inverting Options (Tap to change and compare):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InvertMode.values().forEach { mode ->
                                    val isSelected = activeMode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            activeMode = mode
                                            onModeSelected?.invoke(mode)
                                        },
                                        label = { Text(mode.label, fontSize = 12.sp) }
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(6.dp)
                            ) {
                                Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = activeMode!!.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = previewPagesStr,
                    onValueChange = { previewPagesStr = it },
                    label = { Text("Filter Preview Pages (e.g., 1-2, 5, 8-)") },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("all") },
                    singleLine = true
                )
            }
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (pageBitmap != null) {
                    var fraction by remember { mutableStateOf(0.5f) }
                    
                    if (showExpectedInverted && colorFilter != null) {
                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page Canvas Inverted",
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = colorFilter
                        )
                    } else {
                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page Canvas",
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else if (pageCount > 0) {
                    CircularProgressIndicator()
                } else {
                    Text("Loading or Error")
                }
            }
        }
    }
}
