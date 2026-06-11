package com.scholarvault.ui.pdf

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import eu.wewox.pagecurl.page.PageCurl
import eu.wewox.pagecurl.config.rememberPageCurlConfig
import eu.wewox.pagecurl.page.rememberPageCurlState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scholarvault.data.AppPreferences
import com.scholarvault.ui.viewmodel.PdfViewerViewModel
import com.scholarvault.util.PdfSearchUtil
import com.scholarvault.util.SecurityVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, eu.wewox.pagecurl.ExperimentalPageCurlApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun CustomPdfViewer(
    filePath: String,
    fileName: String,
    onBack: () -> Unit,
    isExternalUri: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val viewModel: PdfViewerViewModel = viewModel()
    val pageCount by viewModel.pageCount.collectAsState()
    val fullPages by viewModel.fullPages.collectAsState()
    val loadError by viewModel.loadError.collectAsState()

    val prefs = remember { AppPreferences(context) }
    val flipAnimEnabled by prefs.pdfFlipAnimation.collectAsState(initial = false)
    val scrollDir by prefs.pdfScrollDirection.collectAsState(initial = "vertical")
    val fitMode by prefs.pdfFitMode.collectAsState(initial = "fit")

    var loadingError by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(loadError) {
        if (loadError != null) {
            loadingError = loadError
        }
    }

    val tempDecryptedFile by viewModel.loadedFile.collectAsState()
    val loadedUri by viewModel.loadedUri.collectAsState()
    var actualFile by remember { mutableStateOf<File?>(null) }
    
    // Search query state
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMatches by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentMatchIndex by remember { mutableStateOf(-1) }
    var isSearching by remember { mutableStateOf(false) }
    
    // UI state
    var showMenu by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { pageCount.coerceAtLeast(1) })
    var isFullScreen by remember { mutableStateOf(false) }

    // On-the-fly Search worker triggered when searchQuery changes
    LaunchedEffect(searchQuery, tempDecryptedFile, loadedUri) {
        val file = tempDecryptedFile
        val uri = loadedUri
        if ((file != null || uri != null) && searchQuery.isNotBlank()) {
            isSearching = true
            val q = searchQuery
            withContext(Dispatchers.Default) {
                val matches = PdfSearchUtil.searchPdf(context, file, uri, q)
                withContext(Dispatchers.Main) {
                    searchMatches = matches
                    currentMatchIndex = if (matches.isNotEmpty()) 0 else -1
                    if (matches.isNotEmpty()) {
                        pagerState.scrollToPage(matches[0])
                    }
                    isSearching = false
                }
            }
        } else {
            searchMatches = emptyList()
            currentMatchIndex = -1
            isSearching = false
        }
    }

    var pageSearchSnippets by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(pagerState.currentPage, searchQuery, tempDecryptedFile, loadedUri) {
        val file = tempDecryptedFile
        val uri = loadedUri
        val index = pagerState.currentPage
        val q = searchQuery
        if ((file != null || uri != null) && q.isNotBlank()) {
            withContext(Dispatchers.Default) {
                val snippets = PdfSearchUtil.getPageSearchSnippets(context, file, uri, q, index)
                withContext(Dispatchers.Main) {
                    pageSearchSnippets = snippets
                }
            }
        } else {
            pageSearchSnippets = emptyList()
        }
    }

    // System Status & Navigation Bars hiding for seamless full screen mode (Immersive Mode)
    val view = LocalView.current
    DisposableEffect(isFullScreen) {
        val ctrl = activity?.let { WindowCompat.getInsetsController(it.window, view) }
        if (isFullScreen) {
            ctrl?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            ctrl?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            ctrl?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(filePath) {
        if (filePath.isNotBlank()) {
            scope.launch { prefs.setPdfFitMode("fit") }
            
            withContext(Dispatchers.IO) {
                try {
                    val isUri = filePath.startsWith("content://") || filePath.startsWith("file://") || isExternalUri
                    if (isUri) {
                        viewModel.loadPdfUri(context, filePath)
                        loadingError = null
                    } else {
                        val file = if (filePath.startsWith("wallet_secure/")) {
                            File(context.filesDir, filePath)
                        } else {
                            File(filePath)
                        }
                        if (file.exists()) {
                            actualFile = file
                            val vault = SecurityVault(context)
                            val tFile = vault.getFileForViewing(file, context.cacheDir) ?: file
                            viewModel.loadPdf(tFile)
                            loadingError = null
                        } else {
                            loadingError = "File does not exist."
                        }
                    }
                } catch (e: Exception) {
                    loadingError = "Error loading PDF: ${e.message}"
                }
            }
        }
    }

    // Thumbnails are rendered lazily from inside the grid list

    DisposableEffect(filePath) {
        onDispose {
            val file = tempDecryptedFile
            if (file != null && file != actualFile) {
                try {
                    file.delete()
                } catch (ignored: Exception) {}
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val gridColumns = if (screenWidthDp > 600) 4 else 2
    val showSidePanel = screenWidthDp > 600
    
    var notesPanelOpen by remember { mutableStateOf(false) }
    var documentId by remember { mutableStateOf<Int?>(null) }
    
    val docRepository = remember { 
        val app = context.applicationContext as com.scholarvault.MainApplication
        com.scholarvault.data.repository.DocumentRepository(app.database.documentDao(), app.database.walletDao())
    }
    
    LaunchedEffect(filePath, fileName) {
        if (!isExternalUri && !filePath.startsWith("content://") && !filePath.startsWith("file://")) {
            val fileToSearch = if (filePath.startsWith("wallet_secure/")) filePath else filePath
            var doc = docRepository.getFileByFilePath(fileToSearch)
            if (doc == null && fileName.isNotEmpty()) {
                doc = docRepository.getFileByName(fileName)
            }
            if (doc != null) {
                documentId = doc.id
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
            // MAIN VIEWPORT CONTAINER
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (loadingError != null) {
                    Card(
                        modifier = Modifier.padding(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(loadingError!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                Text("Go Back")
                            }
                        }
                    }
                } else if (pageCount > 0) {
                    if (fitMode == "grid") {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            modifier = Modifier.fillMaxSize().padding(top = if (isFullScreen) 0.dp else 120.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pageCount, key = { it }) { index ->
                                var thumbnailBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                                
                                LaunchedEffect(index) {
                                    thumbnailBitmap = viewModel.getThumbnailAsync(index)
                                }
                                val imageBitmap = remember(thumbnailBitmap) { thumbnailBitmap?.asImageBitmap() }
                                
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.71f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            scope.launch {
                                                prefs.setPdfFitMode("fit")
                                                pagerState.scrollToPage(index)
                                            }
                                        },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (imageBitmap != null) {
                                            Image(
                                                bitmap = imageBitmap,
                                                contentDescription = "Page ${index + 1}",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                                    .fillMaxWidth()
                                                    .padding(4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Custom Pager with three distinct custom Scroll/Animation directions (vertical, horizontal, animated book flip)
                        val key = scrollDir
                        key(key) {
                            if (scrollDir == "animated") {
                                val state = rememberPageCurlState()
                                val config = rememberPageCurlConfig()
                                PageCurl(
                                    count = pageCount,
                                    state = state,
                                    config = config,
                                    modifier = Modifier.fillMaxSize()
                                ) { pageIndex ->
                                    LaunchedEffect(pageIndex) {
                                        viewModel.renderPage(pageIndex)
                                    }

                                    var highlights by remember { mutableStateOf<List<com.scholarvault.util.SearchHighlight>>(emptyList()) }
                                    LaunchedEffect(pageIndex, searchQuery, tempDecryptedFile, loadedUri) {
                                        val file = tempDecryptedFile
                                        val uri = loadedUri
                                        val q = searchQuery
                                        if ((file != null || uri != null) && q.isNotBlank()) {
                                            highlights = withContext(Dispatchers.IO) {
                                                com.scholarvault.util.PdfSearchUtil.getSearchHighlights(context, file, uri, q, pageIndex)
                                            }
                                        } else {
                                            highlights = emptyList()
                                        }
                                    }
                                    
                                    var thumbnailBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                                    LaunchedEffect(pageIndex) {
                                        thumbnailBitmap = viewModel.getThumbnailAsync(pageIndex)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val pageBitmap = fullPages[pageIndex]
                                        if (pageBitmap != null || thumbnailBitmap != null) {
                                            var scale by remember { mutableFloatStateOf(1f) }
                                            var offset by remember { mutableStateOf(Offset.Zero) }
                                            val activeBitmap = pageBitmap ?: thumbnailBitmap!!

                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    bitmap = activeBitmap.asImageBitmap(),
                                                    contentDescription = "Page ${pageIndex + 1}",
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer(
                                                            scaleX = scale,
                                                            scaleY = scale,
                                                            translationX = offset.x,
                                                            translationY = offset.y
                                                        )
                                                        .pointerInput(scale) {
                                                            detectTransformGestures(panZoomLock = true, currentScale = scale) { centroid, pan, zoom, rotation ->
                                                                val newScale = (scale * zoom).coerceIn(1f, 15f)
                                                                if (scale > 1f || zoom > 1f) {
                                                                    scale = newScale
                                                                    val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                                                    val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                                                    offset = Offset(
                                                                        x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                                        y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                                                    )
                                                                } else {
                                                                    scale = 1f
                                                                    offset = Offset.Zero
                                                                }
                                                            }
                                                        }
                                                        .pointerInput(scale) {
                                                            detectTapGestures(
                                                                onTap = {
                                                                    isFullScreen = !isFullScreen
                                                                },
                                                                onDoubleTap = {
                                                                    scale = if (scale > 1f) 1f else 3f
                                                                    if (scale == 1f) offset = Offset.Zero
                                                                }
                                                            )
                                                        }
                                                )

                                                Canvas(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer(
                                                            scaleX = scale,
                                                            scaleY = scale,
                                                            translationX = offset.x,
                                                            translationY = offset.y
                                                        )
                                                ) {
                                                    val canvasWidth = size.width
                                                    val canvasHeight = size.height
                                                    val bitmapWidth = activeBitmap.width.toFloat()
                                                    val bitmapHeight = activeBitmap.height.toFloat()
                                                    
                                                    if (canvasWidth > 0 && canvasHeight > 0 && bitmapWidth > 0 && bitmapHeight > 0) {
                                                        val scaleX = canvasWidth / bitmapWidth
                                                        val scaleY = canvasHeight / bitmapHeight
                                                        val scaleFit = minOf(scaleX, scaleY)
                                                        
                                                        val actualW = bitmapWidth * scaleFit
                                                        val actualH = bitmapHeight * scaleFit
                                                        
                                                        val startX = (canvasWidth - actualW) / 2f
                                                        val startY = (canvasHeight - actualH) / 2f
                                                        
                                                        for (h in highlights) {
                                                            val highlightLeft = startX + h.left * actualW
                                                            val highlightTop = startY + h.top * actualH
                                                            val highlightWidth = (h.right - h.left) * actualW
                                                            val highlightHeight = (h.bottom - h.top) * actualH
                                                            
                                                            drawRect(
                                                                color = androidx.compose.ui.graphics.Color(0x66FFEB3B),
                                                                topLeft = androidx.compose.ui.geometry.Offset(highlightLeft, highlightTop),
                                                                size = androidx.compose.ui.geometry.Size(highlightWidth, highlightHeight)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            } else {
                                val pageContent: @Composable (Int) -> Unit = { pageIndex ->
                                    LaunchedEffect(pageIndex) {
                                        viewModel.renderPage(pageIndex)
                                    }

                                    var thumbnailBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                                    LaunchedEffect(pageIndex) {
                                        thumbnailBitmap = viewModel.getThumbnailAsync(pageIndex)
                                    }

                                    var highlights by remember { mutableStateOf<List<com.scholarvault.util.SearchHighlight>>(emptyList()) }
                                     LaunchedEffect(pageIndex, searchQuery, tempDecryptedFile, loadedUri) {
                                         val file = tempDecryptedFile
                                         val uri = loadedUri
                                         val q = searchQuery
                                         if ((file != null || uri != null) && q.isNotBlank()) {
                                             highlights = withContext(Dispatchers.IO) {
                                                 com.scholarvault.util.PdfSearchUtil.getSearchHighlights(context, file, uri, q, pageIndex)
                                             }
                                         } else {
                                             highlights = emptyList()
                                         }
                                     }
                                     val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                val absPageOffset = pageOffset.absoluteValue

                                                if (scrollDir == "horizontal") {
                                                    // UNAMBIGUOUS HORIZONTAL PAGE SLIDE & SCALE
                                                    val scaleVal = 1f - (absPageOffset.coerceIn(0f, 1f) * 0.08f)
                                                    scaleX = scaleVal
                                                    scaleY = scaleVal
                                                    alpha = 1f - (absPageOffset.coerceIn(0f, 1f) * 0.4f)
                                                    translationX = pageOffset * 16.dp.toPx()
                                                } else {
                                                    // UNAMBIGUOUS VERTICAL PAGE SLIDE & SCALE
                                                    val scaleVal = 1f - (absPageOffset.coerceIn(0f, 1f) * 0.08f)
                                                    scaleX = scaleVal
                                                    scaleY = scaleVal
                                                    alpha = 1f - (absPageOffset.coerceIn(0f, 1f) * 0.4f)
                                                    translationY = pageOffset * 16.dp.toPx()
                                                }
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val pageBitmap = fullPages[pageIndex]
                                            if (pageBitmap != null || thumbnailBitmap != null) {
                                                var scale by remember { mutableFloatStateOf(1f) }
                                                var offset by remember { mutableStateOf(Offset.Zero) }
                                                val activeBitmap = pageBitmap ?: thumbnailBitmap!!

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .pointerInput(scale) {
                                                            detectTransformGestures(panZoomLock = true, currentScale = scale) { centroid, pan, zoom, rotation ->
                                                                val newScale = (scale * zoom).coerceIn(1f, 15f)
                                                                if (scale > 1f || zoom > 1f) {
                                                                    scale = newScale
                                                                    val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                                                    val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                                                    offset = Offset(
                                                                        x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                                        y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                                                    )
                                                                } else {
                                                                    scale = 1f
                                                                    offset = Offset.Zero
                                                                }
                                                            }
                                                        }
                                                        .pointerInput(scale) {
                                                            detectTapGestures(
                                                                onTap = {
                                                                    isFullScreen = !isFullScreen
                                                                },
                                                                onDoubleTap = {
                                                                    scale = if (scale > 1f) 1f else 3f
                                                                    if (scale == 1f) offset = Offset.Zero
                                                                }
                                                            )
                                                        }
                                                ) {
                                                    Image(
                                                        bitmap = activeBitmap.asImageBitmap(),
                                                        contentDescription = "Page ${pageIndex + 1}",
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .graphicsLayer(
                                                                scaleX = scale,
                                                                scaleY = scale,
                                                                translationX = offset.x,
                                                                translationY = offset.y
                                                            )
                                                    )

                                                    Canvas(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .graphicsLayer(
                                                                scaleX = scale,
                                                                scaleY = scale,
                                                                translationX = offset.x,
                                                                translationY = offset.y
                                                            )
                                                    ) {
                                                        val canvasWidth = size.width
                                                        val canvasHeight = size.height
                                                        val bitmapWidth = activeBitmap.width.toFloat()
                                                        val bitmapHeight = activeBitmap.height.toFloat()
                                                        
                                                        if (canvasWidth > 0 && canvasHeight > 0 && bitmapWidth > 0 && bitmapHeight > 0) {
                                                            val scaleX = canvasWidth / bitmapWidth
                                                            val scaleY = canvasHeight / bitmapHeight
                                                            val scaleFit = minOf(scaleX, scaleY)
                                                            
                                                            val actualW = bitmapWidth * scaleFit
                                                            val actualH = bitmapHeight * scaleFit
                                                            
                                                            val startX = (canvasWidth - actualW) / 2f
                                                            val startY = (canvasHeight - actualH) / 2f
                                                            
                                                            for (h in highlights) {
                                                                val highlightLeft = startX + h.left * actualW
                                                                val highlightTop = startY + h.top * actualH
                                                                val highlightWidth = (h.right - h.left) * actualW
                                                                val highlightHeight = (h.bottom - h.top) * actualH
                                                                
                                                                drawRect(
                                                                    color = androidx.compose.ui.graphics.Color(0x66FFEB3B),
                                                                    topLeft = androidx.compose.ui.geometry.Offset(highlightLeft, highlightTop),
                                                                    size = androidx.compose.ui.geometry.Size(highlightWidth, highlightHeight)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                }

                                if (scrollDir == "horizontal") {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize(),
                                        pageContent = { page ->
                                            Box(
                                                modifier = Modifier.fillMaxSize()
                                                    .graphicsLayer {
                                                        // Calculate the offset of this page relative to the current scroll position
                                                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                                                        
                                                        // Animate the rotation based on the offset
                                                        if (pageOffset <= 1f) {
                                                            rotationY = (1f - pageOffset).let { if (pagerState.currentPage > page) -180f * it else 180f * pageOffset }.coerceIn(-180f, 180f)
                                                            cameraDistance = 8 * density
                                                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (pagerState.currentPage > page) 1f else 0f, 0.5f)
                                                            alpha = 1f - (pageOffset * 0.5f) // Add slight fade for depth
                                                        } else {
                                                            alpha = 0f
                                                        }
                                                    }
                                            ) {
                                                pageContent(page)
                                            }
                                        }
                                    )
                                } else {
                                    VerticalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize(),
                                        pageContent = { pageContent(it) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // GLASSMORPHIC TOP CONTROL BAR OVERLAY
            androidx.compose.animation.AnimatedVisibility(
                visible = !isFullScreen,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color(0xFF1E1E1E).copy(alpha = 0.94f),
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White,
                                actionIconContentColor = Color.White
                            ),
                            title = {
                                Column {
                                    Text(
                                        text = fileName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (pageCount > 0) {
                                        Text(
                                            text = "Page ${pagerState.currentPage + 1} of $pageCount",
                                            fontSize = 12.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                if (!isExternalUri) {
                                    IconButton(
                                        onClick = { notesPanelOpen = !notesPanelOpen }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit, 
                                            contentDescription = "Notes", 
                                            tint = if (notesPanelOpen) MaterialTheme.colorScheme.primary else Color.White
                                        )
                                    }
                                }
                                IconButton(onClick = { searchVisible = !searchVisible }) {
                                    Icon(
                                        Icons.Default.Search, 
                                        contentDescription = "Search", 
                                        tint = if (searchVisible) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                }
                                
                                // Inline "tap to change" Reading Mode switcher (one-line pattern from N-Up)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(end = 8.dp)
                                        .clickable {
                                            scope.launch {
                                                val modes = listOf("vertical", "horizontal", "animated")
                                                val currentIndex = modes.indexOf(scrollDir)
                                                val nextIndex = (currentIndex + 1) % modes.size
                                                prefs.setPdfScrollDirection(modes[nextIndex])
                                            }
                                        }
                                ) {
                                    val sizeLabel = when (scrollDir) {
                                        "vertical" -> "Vertical Scroll"
                                        "horizontal" -> "Horizontal Scroll"
                                        else -> "Animated Book Flip"
                                    }
                                    Text(
                                        text = sizeLabel,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }

                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Reading Mode", fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                                    ) {
                                                        val label = when (scrollDir) {
                                                            "vertical" -> "Vertical"
                                                            "horizontal" -> "Horizontal"
                                                            else -> "3D Flip"
                                                        }
                                                        Text(
                                                            text = label,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                }
                                            },
                                            leadingIcon = {
                                                val icon = when (scrollDir) {
                                                    "vertical" -> Icons.Default.SwapVert
                                                    "horizontal" -> Icons.Default.SwapHoriz
                                                    else -> Icons.Default.Book
                                                }
                                                Icon(icon, contentDescription = null)
                                            },
                                            onClick = {
                                                scope.launch {
                                                    val modes = listOf("vertical", "horizontal", "animated")
                                                    val currentIndex = modes.indexOf(scrollDir)
                                                    val nextIndex = (currentIndex + 1) % modes.size
                                                    prefs.setPdfScrollDirection(modes[nextIndex])
                                                }
                                            }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        DropdownMenuItem(
                                            text = { Text(if (fitMode == "grid") "Single Page View" else "Grid Overview", fontSize = 14.sp) },
                                            trailingIcon = {
                                                Icon(
                                                    if (fitMode == "grid") Icons.Default.SingleBed else Icons.Default.GridView,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                scope.launch {
                                                    prefs.setPdfFitMode(if (fitMode == "grid") "fit" else "grid")
                                                    showMenu = false
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Open in External Viewer", fontSize = 14.sp) },
                                            trailingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                                            onClick = {
                                                showMenu = false
                                                val file = tempDecryptedFile
                                                val uri = when {
                                                    loadedUri != null -> loadedUri
                                                    file != null -> {
                                                        try {
                                                             FileProvider.getUriForFile(
                                                                 context,
                                                                 "${context.packageName}.fileprovider",
                                                                 file
                                                             )
                                                        } catch (e: Exception) {
                                                             null
                                                        }
                                                    }
                                                    else -> null
                                                }
                                                if (uri == null) {
                                                    loadingError = "File not ready. Please wait."
                                                    return@DropdownMenuItem
                                                }
                                                try {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, "application/pdf")
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    try {
                                                        // Fallback to chooser immediately rather than hardcoding Docs
                                                        val chooser = android.content.Intent.createChooser(intent, "Open PDF with...")
                                                        context.startActivity(chooser)
                                                    } catch (e: Exception) {
                                                        loadingError = "Error loading PDF: ${e.localizedMessage ?: "Unknown error"}"
                                                    }
                                                } catch (e: Exception) {
                                                    loadingError = "Error loading PDF: ${e.localizedMessage ?: "Unknown error"}"
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Share PDF", fontSize = 14.sp) },
                                            trailingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                            onClick = {
                                                showMenu = false
                                                val file = tempDecryptedFile
                                                val uri = when {
                                                    loadedUri != null -> loadedUri
                                                    file != null -> {
                                                        try {
                                                             FileProvider.getUriForFile(
                                                                 context,
                                                                 "${context.packageName}.fileprovider",
                                                                 file
                                                             )
                                                        } catch (e: Exception) {
                                                             null
                                                        }
                                                    }
                                                    else -> null
                                                }
                                                if (uri == null) {
                                                    loadingError = "File not ready. Please wait."
                                                    return@DropdownMenuItem
                                                }
                                                try {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        type = "application/pdf"
                                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(intent, "Share PDF File"))
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Print Document", fontSize = 14.sp) },
                                            trailingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                            onClick = {
                                                showMenu = false
                                                val file = tempDecryptedFile
                                                val uri = when {
                                                    loadedUri != null -> loadedUri
                                                    file != null -> {
                                                        try {
                                                             FileProvider.getUriForFile(
                                                                 context,
                                                                 "${context.packageName}.fileprovider",
                                                                 file
                                                             )
                                                        } catch (e: Exception) {
                                                             null
                                                        }
                                                    }
                                                    else -> null
                                                }
                                                if (uri == null) {
                                                    loadingError = "File not ready. Please wait."
                                                    return@DropdownMenuItem
                                                }
                                                try {
                                                    com.scholarvault.ui.tools.SharedData.navigateToPrePrint.value = true
                                                    com.scholarvault.ui.tools.SharedData.pendingUris.value = listOf(uri)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("View Details", fontSize = 14.sp) },
                                            trailingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                            onClick = {
                                                showMenu = false
                                                showDetailsSheet = true
                                            }
                                        )
                                    }
                                }
                            }
                        )

                        if (searchVisible) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search text patterns...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                                }
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )
                                    
                                    if (searchMatches.isNotEmpty()) {
                                        Text(
                                            text = "${currentMatchIndex + 1}/${searchMatches.size}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            color = Color.White
                                        )
                                        IconButton(
                                            onClick = {
                                                if (currentMatchIndex > 0) {
                                                    currentMatchIndex--
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(searchMatches[currentMatchIndex])
                                                    }
                                                }
                                            },
                                            enabled = currentMatchIndex > 0
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match", tint = if (currentMatchIndex > 0) Color.White else Color.Gray)
                                        }
                                        IconButton(
                                            onClick = {
                                                if (currentMatchIndex < searchMatches.size - 1) {
                                                    currentMatchIndex++
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(searchMatches[currentMatchIndex])
                                                    }
                                                }
                                            },
                                            enabled = currentMatchIndex < searchMatches.size - 1
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match", tint = if (currentMatchIndex < searchMatches.size - 1) Color.White else Color.Gray)
                                        }
                                    } else if (searchQuery.isNotEmpty() && !isSearching) {
                                        Text(
                                            text = "No matches",
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else if (isSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp).padding(horizontal = 8.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FLOATING TEXT MATCH SNIPPETS CARD
            androidx.compose.animation.AnimatedVisibility(
                visible = pageSearchSnippets.isNotEmpty() && searchVisible && !isFullScreen && fitMode != "grid",
                enter = fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 75.dp)
                    .fillMaxWidth(0.9f)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Matching Text on Page ${pagerState.currentPage + 1}:",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        pageSearchSnippets.take(3).forEach { snippet ->
                            Text(
                                text = snippet,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // FLOATING INDICATOR PILL
            androidx.compose.animation.AnimatedVisibility(
                visible = !isFullScreen && pageCount > 0 && fitMode != "grid",
                enter = fadeIn() + androidx.compose.animation.scaleIn(),
                exit = fadeOut() + androidx.compose.animation.scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                    tonalElevation = 6.dp
                ) {
                    Text(
                        text = "Page ${pagerState.currentPage + 1} of $pageCount",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        } // End of outer Box

        // SIDE PANEL (Wide Screens)
        if (showSidePanel && notesPanelOpen && documentId != null) {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                PageNotesPanel(documentId = documentId!!, pageIndex = pagerState.currentPage)
            }
        }
    } // End of Row

    // BOTTOM SHEET (Narrow Screens)
    if (!showSidePanel && notesPanelOpen && documentId != null) {
        ModalBottomSheet(
            onDismissRequest = { notesPanelOpen = false }
        ) {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
                PageNotesPanel(documentId = documentId!!, pageIndex = pagerState.currentPage)
            }
        }
    } // End of BOTTOM SHEET
    } // End of Scaffold padding block

    // Modal Details Sheet
    if (showDetailsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Document Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                DetailItem(label = "File Name", value = fileName)
                
                val file = tempDecryptedFile
                val uri = loadedUri
                if (uri != null) {
                    var contentSize: Long = -1
                    try {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            if (cursor.moveToFirst() && sizeIndex != -1) {
                                contentSize = cursor.getLong(sizeIndex)
                            }
                        }
                    } catch (ignored: Exception) {}

                    val formattedSize = if (contentSize >= 0) formatFileSize(contentSize) else "Unknown"
                    DetailItem(label = "File Size", value = formattedSize)
                    DetailItem(label = "Page Count", value = pageCount.toString())
                    DetailItem(label = "Stream Source", value = "Direct Memory Stream (Zero-Copy)")
                    DetailItem(label = "URI Authority", value = uri.authority ?: "Unknown")
                } else if (file != null) {
                    val formattedSize = formatFileSize(file.length())
                    DetailItem(label = "File Size", value = formattedSize)
                    DetailItem(label = "Page Count", value = pageCount.toString())
                    
                    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.getDefault())
                    val sysDate = sdf.format(Date(file.lastModified()))
                    DetailItem(label = "Access Decrypted Time", value = sysDate)
                    DetailItem(label = "Temporary Safe Path", value = file.absolutePath)
                } else {
                    DetailItem(label = "Storage Status", value = "Loading metadata...")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showDetailsSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.toDouble())).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.toDouble(), digitGroups.toDouble()), units[digitGroups])
}

// Custom Printed Document Adapter
private class MyPdfDocumentAdapter(private val file: File) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: PrintDocumentAdapter.LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: PrintDocumentAdapter.WriteResultCallback
    ) {
        var input: FileInputStream? = null
        var output: FileOutputStream? = null
        try {
            input = FileInputStream(file)
            output = FileOutputStream(destination.fileDescriptor)
            val buf = ByteArray(16384)
            var bytesRead: Int
            while (input.read(buf).also { bytesRead = it } >= 0) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                output.write(buf, 0, bytesRead)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.toString())
        } finally {
            try { input?.close() } catch (ignored: Exception) {}
            try { output?.close() } catch (ignored: Exception) {}
        }
    }
}

private suspend fun PointerInputScope.detectTransformGestures(
    panZoomLock: Boolean = false,
    currentScale: Float,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanOrZoom = false

        awaitFirstDown()
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = kotlin.math.abs(1 - zoom) * centroidSize
                    val rotationMotion = kotlin.math.abs(rotation) * Math.PI.toFloat() * centroidSize / 180f
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanOrZoom = panZoomLock && (zoomMotion < touchSlop || rotationMotion < touchSlop)
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanOrZoom) 0f else rotationChange
                    val effectiveZoom = if (lockedToPanOrZoom) 1f else zoomChange
                    val effectivePan = if (lockedToPanOrZoom) Offset.Zero else panChange

                    if (effectiveRotation != 0f ||
                        effectiveZoom != 1f ||
                        effectivePan != Offset.Zero
                    ) {
                        onGesture(centroid, effectivePan, effectiveZoom, effectiveRotation)
                    }

                    // CRITICAL GESTURE BALANCE RULE:
                    // Only consume the pointer changes if:
                    // - We are zoomed in (currentScale > 1f) OR
                    // - We are actively zooming (effectiveZoom != 1f) OR
                    // - We have more than 1 finger down (event.changes.size > 1)
                    val shouldConsume = (currentScale > 1f) || effectiveZoom != 1f || event.changes.size > 1
                    if (shouldConsume) {
                        event.changes.forEach {
                            val moved = it.previousPosition != it.position
                            if (moved) {
                                it.consume()
                            }
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}
