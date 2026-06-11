package com.scholarvault.ui.pdf.v2

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scholarvault.ui.pdf.PageNotesPanel
import com.scholarvault.ui.pdf.flip.PageFlipGeometry
import com.scholarvault.ui.pdf.flip.PageFlipShadowOverlay
import com.scholarvault.ui.pdf.flip.pageFlipApply
import com.scholarvault.util.SecurityVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPdfViewer(
    filePath: String,
    fileName: String,
    onBack: () -> Unit,
    isExternalUri: Boolean = false,
    viewModel: PdfViewerV2ViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val activity = context as? Activity

    val uiState by viewModel.uiState.collectAsState()
    val successState = uiState as? PdfV2UiState.Success
    val currentPage by viewModel.currentPage.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchMatches by viewModel.searchMatches.collectAsState()
    val currentMatchIndex by viewModel.currentMatchIndex.collectAsState()

    var isFullScreen by rememberSaveable { mutableStateOf(false) }
    var notesPanelOpen by rememberSaveable { mutableStateOf(false) }
    var gridViewMode by rememberSaveable { mutableStateOf(false) }
    var documentId by rememberSaveable { mutableStateOf<Int?>(null) }
    var doublePageSpread by rememberSaveable { mutableStateOf(false) }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQueryInput by rememberSaveable { mutableStateOf("") }

    val docRepository = remember {
        val app = context.applicationContext as com.scholarvault.MainApplication
        com.scholarvault.data.repository.DocumentRepository(app.database.documentDao(), app.database.walletDao())
    }

    val prefs = remember { com.scholarvault.data.AppPreferences(context) }
    var initialPageRestored by remember(filePath) { mutableStateOf(false) }
    val scrollDir by prefs.pdfScrollDirection.collectAsState(initial = "vertical")
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showScrollModeDialog by rememberSaveable { mutableStateOf(false) }

    // Set side by side defaults based on width classes
    val isTablet = configuration.screenWidthDp >= 840
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val canShowDoublePage = isTablet && isLandscape

    LaunchedEffect(canShowDoublePage) {
        if (canShowDoublePage) {
            doublePageSpread = true
        } else {
            doublePageSpread = false
        }
    }

    // Attempt to match the documentId to load notes correctly
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

    // Auto-restore reading progress when PDF successfully loads
    LaunchedEffect(successState) {
        if (successState != null && !initialPageRestored) {
            try {
                val savedPage = prefs.getPdfReadingProgress(filePath).first()
                if (savedPage in 0 until successState.pageCount) {
                    viewModel.setCurrentPage(savedPage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                initialPageRestored = true
            }
        }
    }

    // Auto-save reading progress on page changes
    LaunchedEffect(currentPage) {
        if (successState != null && initialPageRestored) {
            prefs.setPdfReadingProgress(filePath, currentPage)
        }
    }

    // Load PDF resources safely off UI thread
    LaunchedEffect(filePath) {
        if (filePath.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val isUri = filePath.startsWith("content://") || filePath.startsWith("file://") || isExternalUri
                    if (isUri) {
                        viewModel.loadPdf(context, Uri.parse(filePath), fileName, isExternalUri)
                    } else {
                        val file = if (filePath.startsWith("wallet_secure/")) {
                            File(context.filesDir, filePath)
                        } else {
                            File(filePath)
                        }
                        if (file.exists()) {
                            val vault = SecurityVault(context)
                            val decodedCachedFile = vault.getFileForViewing(file, context.cacheDir) ?: file
                            viewModel.loadPdfFile(decodedCachedFile)
                        } else {
                            // Empty state
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Seamless full screen (Immersive mode) toggle
    val view = LocalView.current
    DisposableEffect(isFullScreen) {
        val insetsController = activity?.let { WindowCompat.getInsetsController(it.window, view) }
        if (isFullScreen) {
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val isLandscapeMode = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val useSideControls = false

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // SIDE CONTROLS SIDEBAR FOR LANDSCAPE / TABLETS
            val showSideControls = useSideControls && !isFullScreen && successState != null
            androidx.compose.animation.AnimatedVisibility(
                visible = showSideControls,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            ) {
                if (successState != null) {
                    val maxPage = successState.pageCount
                    Surface(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight(),
                        color = Color(0xFF1E1E1E),
                        border = BorderStroke(1.dp, Color(0xFF2E2E2E))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Text(
                                    "ScholarVault V2",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                Text(
                                    text = successState.fileName,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = "Workspace Document",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            HorizontalDivider(color = Color(0xFF2E2E2E))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF282828), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "DOCUMENT TRACKER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (doublePageSpread && currentPage + 1 < maxPage) {
                                        "Pages ${currentPage + 1}-${currentPage + 2} of $maxPage"
                                    } else {
                                        "Page ${currentPage + 1} of $maxPage"
                                    },
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val viewLeftEnabled = currentPage > 0
                                    val stepOffset = if (doublePageSpread) 2 else 1
                                    Button(
                                        onClick = { viewModel.setCurrentPage((currentPage - stepOffset).coerceAtLeast(0)) },
                                        enabled = viewLeftEnabled,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF444444),
                                            disabledContainerColor = Color(0xFF222222)
                                        ),
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back Page", tint = if (viewLeftEnabled) Color.White else Color.DarkGray, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Prev", color = if (viewLeftEnabled) Color.White else Color.DarkGray, fontSize = 12.sp)
                                    }
                                    
                                    val viewRightEnabled = if (doublePageSpread) {
                                        currentPage + 2 < maxPage
                                    } else {
                                        currentPage + 1 < maxPage
                                    }
                                    Button(
                                        onClick = { viewModel.setCurrentPage((currentPage + stepOffset).coerceAtMost(maxPage - 1)) },
                                        enabled = viewRightEnabled,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF444444),
                                            disabledContainerColor = Color(0xFF222222)
                                        ),
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Next", color = if (viewRightEnabled) Color.White else Color.DarkGray, fontSize = 12.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page", tint = if (viewRightEnabled) Color.White else Color.DarkGray, modifier = Modifier.size(16.dp))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Slider(
                                    value = currentPage.toFloat(),
                                    onValueChange = { viewModel.setCurrentPage(it.toInt().coerceIn(0, maxPage - 1)) },
                                    valueRange = 0f..(maxPage - 1).toFloat(),
                                    steps = if (maxPage > 2) maxPage - 2 else 0,
                                    colors = RiderSliderDefaults(),
                                    modifier = Modifier.fillMaxWidth().height(24.dp)
                                )
                            }
                            
                            HorizontalDivider(color = Color(0xFF2E2E2E))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF282828), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "DOCUMENT SEARCH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = searchQueryInput,
                                    onValueChange = {
                                        searchQueryInput = it
                                        viewModel.search(context, it)
                                    },
                                    placeholder = { Text("Search matches...", color = Color.Gray, fontSize = 13.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedContainerColor = Color(0xFF1E1E1E),
                                        unfocusedContainerColor = Color(0xFF1E1E1E)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                
                                if (searchMatches.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Hit ${currentMatchIndex + 1} of ${searchMatches.size}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row {
                                            IconButton(onClick = { viewModel.previousMatch() }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Prev Hit", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { viewModel.nextMatch() }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Next Hit", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                } else if (searchQueryInput.isNotBlank() && !isSearching) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No matches found", color = Color.Red, fontSize = 12.sp)
                                } else if (isSearching) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                }
                            }
                            
                            HorizontalDivider(color = Color(0xFF2E2E2E))
                            
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "DOCUMENT CONTROLS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                
                                if (canShowDoublePage) {
                                    InputChip(
                                        selected = doublePageSpread,
                                        onClick = { doublePageSpread = !doublePageSpread },
                                        label = { Text("Dual Page Spread", fontSize = 12.sp, color = Color.White) },
                                        leadingIcon = {
                                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                if (documentId != null) {
                                    InputChip(
                                        selected = notesPanelOpen,
                                        onClick = { notesPanelOpen = !notesPanelOpen },
                                        label = { Text("Page Notes Panel", fontSize = 12.sp, color = Color.White) },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                InputChip(
                                    selected = false,
                                    onClick = { isFullScreen = true },
                                    label = { Text("Immersive Max View", fontSize = 12.sp, color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                InputChip(
                                    selected = gridViewMode,
                                    onClick = { gridViewMode = !gridViewMode },
                                    label = { Text("Grid Wrap View", fontSize = 12.sp, color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Default.GridView, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                            }
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF141414)),
                contentAlignment = Alignment.Center
            ) {
                when (val state = uiState) {
                    is PdfV2UiState.Loading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading ScholarVault PDF Engine v2...", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                    is PdfV2UiState.Error -> {
                        Card(
                            modifier = Modifier.padding(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(state.message, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                    Text("Go Back")
                                }
                            }
                        }
                    }
                    is PdfV2UiState.Success -> {
                        val maxPage = state.pageCount

                        // MAIN INTERACTIVE PREVIEW VIEWPORT
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (gridViewMode) {
                                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 120.dp),
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentPadding = PaddingValues(bottom = 100.dp, top = if(useSideControls) 8.dp else 64.dp)
                                ) {
                                    items(maxPage) { pageIdx ->
                                        Box(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .aspectRatio(0.7f)
                                                .clickable {
                                                    viewModel.setCurrentPage(pageIdx)
                                                    gridViewMode = false
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AnimatedPageImageV2(
                                                viewModel = viewModel,
                                                index = pageIdx,
                                                availableWidth = 140.dp,
                                                availableHeight = 200.dp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .background(Color.Black.copy(alpha=0.6f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal=6.dp, vertical=2.dp)
                                            ) {
                                                Text("${pageIdx + 1}", color=Color.White, fontSize=10.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                PdfViewportV2(
                                    viewModel = viewModel,
                                    pageIndex = currentPage,
                                    totalPages = maxPage,
                                    doubleSpread = doublePageSpread && (currentPage + 1 < maxPage),
                                    scrollDir = scrollDir,
                                    onViewportTap = { isFullScreen = !isFullScreen }
                                )
                            }

                            // TOP HEADER BAR overlay
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !isFullScreen && !useSideControls,
                                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                                modifier = Modifier.align(Alignment.TopCenter)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.82f),
                                        modifier = Modifier.fillMaxWidth(),
                                        shadowElevation = 4.dp
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .statusBarsPadding()
                                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(onClick = onBack) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                                }
                                                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                                    Text(
                                                        text = state.fileName,
                                                        color = Color.White,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "V2 Custom engine",
                                                        color = Color.LightGray,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                IconButton(onClick = { searchVisible = !searchVisible }) {
                                                    Icon(
                                                        imageVector = if (searchVisible) Icons.Default.SearchOff else Icons.Default.Search,
                                                        contentDescription = "Search",
                                                        tint = if (searchVisible) MaterialTheme.colorScheme.primary else Color.White
                                                    )
                                                }

                                                if (documentId != null) {
                                                    IconButton(onClick = { notesPanelOpen = !notesPanelOpen }) {
                                                        Icon(Icons.AutoMirrored.Filled.Note, contentDescription = "Page Notes", tint = Color.White)
                                                    }
                                                }

                                                IconButton(onClick = { gridViewMode = !gridViewMode }) {
                                                    Icon(Icons.Default.GridView, contentDescription = "Grid View", tint = if (gridViewMode) MaterialTheme.colorScheme.primary else Color.White)
                                                }

                                                IconButton(onClick = { isFullScreen = true }) {
                                                    Icon(Icons.Default.Fullscreen, contentDescription = "Immersive Mode", tint = Color.White)
                                                }
                                                
                                                Box {
                                                    IconButton(onClick = { showMenu = !showMenu }) {
                                                        Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.White)
                                                    }
                                                    com.scholarvault.ui.components.CustomPopupMenu(
                                                        expanded = showMenu,
                                                        onDismissRequest = { showMenu = false },
                                                        items = mutableListOf<com.scholarvault.ui.components.MenuItem>().apply {
                                                            add(com.scholarvault.ui.components.MenuItem(
                                                                title = "Save to Downloads",
                                                                icon = Icons.Default.Download,
                                                                onClick = {
                                                                    viewModel.runBackgroundTask(context, "Save to Downloads") {
                                                                        try {
                                                                            val file = if (isExternalUri) {
                                                                                val uri = android.net.Uri.parse(filePath)
                                                                                val inStream = context.contentResolver.openInputStream(uri)
                                                                                inStream?.readBytes()
                                                                            } else {
                                                                                java.io.File(filePath).readBytes()
                                                                            }
                                                                            if (file != null) {
                                                                                val resolver = context.contentResolver
                                                                                val contentValues = android.content.ContentValues().apply {
                                                                                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "Export_$fileName")
                                                                                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                                                                                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                                                                                }
                                                                                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                                                                if (uri != null) {
                                                                                    resolver.openOutputStream(uri)?.use { outStream ->
                                                                                        outStream.write(file)
                                                                                    }
                                                                                    android.widget.Toast.makeText(context, "Saved to Downloads", android.widget.Toast.LENGTH_SHORT).show()
                                                                                }
                                                                            }
                                                                        } catch (e: Exception) {
                                                                            android.widget.Toast.makeText(context, "Failed to save: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    }
                                                                }
                                                            ))
                                                            if (isExternalUri) {
                                                                add(com.scholarvault.ui.components.MenuItem(
                                                                    title = "Import to App",
                                                                    icon = Icons.Default.Save,
                                                                    onClick = {
                                                                        viewModel.runBackgroundTask(context, "Import to App") {
                                                                            val app = context.applicationContext as com.scholarvault.MainApplication
                                                                            val docRepository = com.scholarvault.data.repository.DocumentRepository(app.database.documentDao(), app.database.walletDao())
                                                                            val uri = android.net.Uri.parse(filePath)
                                                                            val resolver = context.contentResolver
                                                                            val pName = fileName.ifBlank { "Imported_PDF.pdf" }
                                                                            
                                                                            val cleanName = "${System.currentTimeMillis()}_$pName"
                                                                            val destFile = java.io.File(context.filesDir, cleanName)
                                                                            
                                                                            var size: Long = 0
                                                                            resolver.openInputStream(uri)?.use { input ->
                                                                                java.io.FileOutputStream(destFile).use { output ->
                                                                                    size = input.copyTo(output)
                                                                                 }
                                                                            }
                                                                            
                                                                            val docFile = com.scholarvault.data.model.DocumentFile(
                                                                                name = pName,
                                                                                isFolder = false,
                                                                                parentFolderId = null,
                                                                                filePath = cleanName,
                                                                                extension = "pdf",
                                                                                sizeBytes = size,
                                                                                isEncrypted = false,
                                                                                tags = listOf("Imported")
                                                                            )
                                                                            docRepository.insertFile(docFile)
                                                                        }
                                                                    }
                                                                ))
                                                            } else {
                                                                add(com.scholarvault.ui.components.MenuItem(
                                                                    title = "Process with Tools",
                                                                    icon = Icons.Default.Build,
                                                                    onClick = {
                                                                        try {
                                                                            val file = java.io.File(filePath)
                                                                            if (file.exists()) {
                                                                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                                                com.scholarvault.ui.tools.SharedData.pendingUris.value = listOf(uri)
                                                                                android.widget.Toast.makeText(context, "PDF selected. Please select a tool.", android.widget.Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        } catch (e: Exception) {
                                                                            e.printStackTrace()
                                                                        }
                                                                    }
                                                                ))
                                                            }
                                                            
                                                            add(com.scholarvault.ui.components.MenuItem(
                                                                title = "Open in another app",
                                                                icon = Icons.Default.OpenInNew,
                                                                onClick = {
                                                                    try {
                                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                                            val uri = if (isExternalUri) android.net.Uri.parse(filePath) else {
                                                                                val f = java.io.File(filePath)
                                                                                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                                                                            }
                                                                            setDataAndType(uri, "application/pdf")
                                                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                        }
                                                                        context.startActivity(android.content.Intent.createChooser(intent, "Open in another app"))
                                                                    } catch (e: Exception) {
                                                                        android.widget.Toast.makeText(context, "No app to open PDF.", android.widget.Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            ))
                                                            add(com.scholarvault.ui.components.MenuItem(
                                                                title = "Share PDF",
                                                                icon = Icons.Default.Share,
                                                                onClick = {
                                                                    viewModel.runBackgroundTask(context, "Share PDF") {
                                                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                            type = "application/pdf"
                                                                            val uri = if (isExternalUri) android.net.Uri.parse(filePath) else androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(filePath))
                                                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                        }
                                                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                            context.startActivity(android.content.Intent.createChooser(intent, "Share Document"))
                                                                        }
                                                                    }
                                                                }
                                                            ))
                                                            add(com.scholarvault.ui.components.MenuItem(
                                                                title = "Print / Setup",
                                                                icon = Icons.Default.Print,
                                                                onClick = {
                                                                    scope.launch {
                                                                        try {
                                                                            val uri = if (isExternalUri) android.net.Uri.parse(filePath) else android.net.Uri.fromFile(java.io.File(filePath))
                                                                            com.scholarvault.ui.tools.SharedData.pendingUris.value = com.scholarvault.ui.tools.SharedData.pendingUris.value + uri
                                                                            com.scholarvault.ui.tools.SharedData.navigateToPrePrint.value = true
                                                                            android.widget.Toast.makeText(context, "Added to print queue.", android.widget.Toast.LENGTH_SHORT).show()
                                                                        } catch (e: Exception) {
                                                                            e.printStackTrace()
                                                                        }
                                                                    }
                                                                }
                                                            ))
                                                            add(com.scholarvault.ui.components.MenuItem(
                                                                title = "Scroll Settings",
                                                                icon = Icons.Default.Settings,
                                                                onClick = {
                                                                    scope.launch { 
                                                                        showScrollModeDialog = true
                                                                    }
                                                                }
                                                            ))
                                                        }
                                                    )
                                                }
                                            }

                                            // Search Input Strip
                                            androidx.compose.animation.AnimatedVisibility(visible = searchVisible) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.Black.copy(alpha = 0.9f))
                                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedTextField(
                                                        value = searchQueryInput,
                                                        onValueChange = {
                                                            searchQueryInput = it
                                                            viewModel.search(context, it)
                                                        },
                                                        placeholder = { Text("Search text...", color = Color.Gray, fontSize = 14.sp) },
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White,
                                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedBorderColor = Color.DarkGray,
                                                            focusedContainerColor = Color(0xFF222222),
                                                            unfocusedContainerColor = Color(0xFF1A1A1A)
                                                        ),
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )

                                                    if (searchMatches.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "${currentMatchIndex + 1}/${searchMatches.size}",
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        IconButton(onClick = { viewModel.previousMatch() }) {
                                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Previous Match", tint = Color.White)
                                                        }
                                                        IconButton(onClick = { viewModel.nextMatch() }) {
                                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Next Match", tint = Color.White)
                                                        }
                                                    } else if (searchQueryInput.isNotBlank() && !isSearching) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("No hits", color = Color.Red, fontSize = 13.sp)
                                                    } else if (isSearching) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                                    }

                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(onClick = {
                                                        searchQueryInput = ""
                                                        viewModel.clearSearch()
                                                        searchVisible = false
                                                    }) {
                                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // FOOTER CONTROLS overlay
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !isFullScreen && !useSideControls,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.82f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding(),
                                    shadowElevation = 8.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Page navigation scrubber
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val viewLeftEnabled = currentPage > 0
                                            val stepOffset = if (doublePageSpread) 2 else 1
                                            IconButton(
                                                onClick = { viewModel.setCurrentPage((currentPage - stepOffset).coerceAtLeast(0)) },
                                                enabled = viewLeftEnabled
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Previous",
                                                    tint = if (viewLeftEnabled) Color.White else Color.DarkGray
                                                )
                                            }

                                            Text(
                                                text = if (doublePageSpread && currentPage + 1 < maxPage) {
                                                    "Pages ${currentPage + 1}-${currentPage + 2} of $maxPage"
                                                } else {
                                                    "Page ${currentPage + 1} of $maxPage"
                                                },
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )

                                            val viewRightEnabled = if (doublePageSpread) {
                                                currentPage + 2 < maxPage
                                            } else {
                                                currentPage + 1 < maxPage
                                            }
                                            IconButton(
                                                onClick = { viewModel.setCurrentPage((currentPage + stepOffset).coerceAtMost(maxPage - 1)) },
                                                enabled = viewRightEnabled
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "Next",
                                                    tint = if (viewRightEnabled) Color.White else Color.DarkGray
                                                )
                                            }
                                        }

                                        Slider(
                                            value = currentPage.toFloat(),
                                            onValueChange = { viewModel.setCurrentPage(it.toInt().coerceIn(0, maxPage - 1)) },
                                            valueRange = 0f..(maxPage - 1).toFloat(),
                                            steps = if (maxPage > 2) maxPage - 2 else 0,
                                            colors = RiderSliderDefaults(),
                                            modifier = Modifier.fillMaxWidth().height(24.dp)
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState())
                                                .padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (canShowDoublePage) {
                                                InputChip(
                                                    selected = doublePageSpread,
                                                    onClick = { doublePageSpread = !doublePageSpread },
                                                    label = { Text("Dual Page Spread", fontSize = 11.sp, color = Color.White) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
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

        // Slide out Notes Panel
            androidx.compose.animation.AnimatedVisibility(
                visible = notesPanelOpen && documentId != null,
                enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }) + androidx.compose.animation.fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Page Comments & Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { notesPanelOpen = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close notes")
                            }
                        }
                        if (documentId != null) {
                            PageNotesPanel(documentId = documentId!!, pageIndex = currentPage)
                        }
                    }
                }
            }
        }
        
        if (showScrollModeDialog) {
            AlertDialog(
                onDismissRequest = { showScrollModeDialog = false },
                title = { Text("Scroll Strategy") },
                text = {
                    Column {
                        listOf(
                            "vertical" to "Vertical Paginated",
                            "horizontal" to "Horizontal Paginated",
                            "continuous" to "Continuous Scroll (Drive-like)",
                            "animated" to "Page Flip (Book-like)"
                        ).forEach { (mode, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch { prefs.setPdfScrollDirection(mode) }
                                        showScrollModeDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = scrollDir == mode,
                                    onClick = { 
                                        scope.launch { prefs.setPdfScrollDirection(mode) }
                                        showScrollModeDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showScrollModeDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun PdfViewportV2(
    viewModel: PdfViewerV2ViewModel,
    pageIndex: Int,
    totalPages: Int,
    doubleSpread: Boolean,
    scrollDir: String,
    onViewportTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onViewportTap() })
            },
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        val zoomState = rememberTransformableState { zoomChange, offsetChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            val computedOffset = offset + offsetChange * scale
            // Bound panning logically
            val maxOffsetW = (scale - 1f) * containerWidth.value * 1.5f
            val maxOffsetY = (scale - 1f) * containerHeight.value * 1.5f
            offset = Offset(
                computedOffset.x.coerceIn(-maxOffsetW, maxOffsetW),
                computedOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
            )
        }

        val themeController = com.scholarvault.ui.theme.LocalThemeController.current
        val animationsEnabled = themeController.animationsEnabled

        if (!doubleSpread) {
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                initialPage = pageIndex,
                pageCount = { totalPages }
            )

            // Bi-directional synchronization:
            // 1) From VM/sidebar/footer/search hit to pager
            LaunchedEffect(pageIndex) {
                if (pagerState.currentPage != pageIndex) {
                    pagerState.scrollToPage(pageIndex)
                }
            }

            // 2) From pager swipe gesture to VM/sidebar/footer/notes context
            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage != pageIndex) {
                    viewModel.setCurrentPage(pagerState.currentPage)
                }
            }

            Box(
                modifier = Modifier
                    .transformable(state = zoomState)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            },
                            onTap = { onViewportTap() }
                        )
                    }
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val densityValue = androidx.compose.ui.platform.LocalDensity.current.density
                val containerWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { containerWidth.toPx() }

                val pageContent: @Composable (Int, androidx.compose.foundation.layout.PaddingValues?) -> Unit = { page, customPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(customPadding ?: PaddingValues(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedPageImageV2(
                            viewModel = viewModel,
                            index = page,
                            availableWidth = containerWidth,
                            availableHeight = containerHeight
                        )
                    }
                }

                if (scrollDir == "continuous") {
                    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = pageIndex)
                    
                    LaunchedEffect(pageIndex) {
                        if (lazyListState.firstVisibleItemIndex != pageIndex) {
                            lazyListState.scrollToItem(pageIndex)
                        }
                    }
                    LaunchedEffect(lazyListState.firstVisibleItemIndex) {
                        if (lazyListState.firstVisibleItemIndex != pageIndex && !lazyListState.isScrollInProgress) {
                            viewModel.setCurrentPage(lazyListState.firstVisibleItemIndex)
                        }
                    }
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = (scale == 1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp) // ~2mm gap
                    ) {
                        items(totalPages) { page ->
                            Box(modifier = Modifier.fillMaxWidth().height(containerHeight)) {
                                pageContent(page, PaddingValues(horizontal = 16.dp, vertical = 0.dp))
                            }
                        }
                    }
                } else if (scrollDir == "vertical") {
                    androidx.compose.foundation.pager.VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = (scale == 1f),
                        beyondViewportPageCount = 1
                    ) { page ->
                        pageContent(page, null)
                    }
                } else {
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = (scale == 1f),
                        beyondViewportPageCount = 1
                    ) { page ->
                        val pageOffset = page - pagerState.currentPage - pagerState.currentPageOffsetFraction
                        
                        if (scrollDir == "animated") {
                            val flipState = PageFlipGeometry.calculateFlipState(
                                pageOffset = pageOffset,
                                screenWidth = containerWidthPx,
                                density = densityValue
                            )
        
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(1f - pageOffset)
                                    .pageFlipApply(flipState),
                                contentAlignment = Alignment.Center
                            ) {
                                PageFlipShadowOverlay(state = flipState) {
                                    pageContent(page, null)
                                }
                            }
                        } else {
                            // "horizontal" standard
                            Box(modifier = Modifier.fillMaxSize()) {
                                pageContent(page, null)
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .transformable(state = zoomState)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            },
                            onTap = { onViewportTap() }
                        )
                    }
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (doubleSpread && pageIndex + 1 < totalPages) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            AnimatedPageImageV2(
                                viewModel = viewModel,
                                index = pageIndex,
                                availableWidth = containerWidth / 2,
                                availableHeight = containerHeight
                            )
                        }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            AnimatedPageImageV2(
                                viewModel = viewModel,
                                index = pageIndex + 1,
                                availableWidth = containerWidth / 2,
                                availableHeight = containerHeight
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedPageImageV2(
                            viewModel = viewModel,
                            index = pageIndex,
                            availableWidth = containerWidth,
                            availableHeight = containerHeight
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedPageImageV2(
    viewModel: PdfViewerV2ViewModel,
    index: Int,
    availableWidth: androidx.compose.ui.unit.Dp,
    availableHeight: androidx.compose.ui.unit.Dp
) {
    val context = LocalContext.current
    var bitmap by remember(index) { mutableStateOf<Bitmap?>(null) }
    var loadedIndex by remember(index) { mutableStateOf<Int?>(null) }

    val density = LocalDensity.current
    val wPx = with(density) { availableWidth.roundToPx() }
    val hPx = with(density) { availableHeight.roundToPx() }

    val searchQuery by viewModel.searchQuery.collectAsState()
    var highlights by remember(index, searchQuery) { mutableStateOf<List<com.scholarvault.util.SearchHighlight>>(emptyList()) }

    LaunchedEffect(index, wPx, hPx) {
        if (wPx > 0 && hPx > 0) {
            val bmp = viewModel.getPageBitmap(index, wPx, hPx)
            if (bmp != null) {
                bitmap = bmp
                loadedIndex = index
            }
        }
    }

    LaunchedEffect(index, searchQuery) {
        val file = viewModel.loadedFile
        val uri = viewModel.loadedUri
        if (searchQuery.isNotBlank()) {
            highlights = withContext(Dispatchers.IO) {
                com.scholarvault.util.PdfSearchUtil.getSearchHighlights(context, file, uri, searchQuery, index)
            }
        } else {
            highlights = emptyList()
        }
    }

    Crossfade(
        targetState = bitmap,
        animationSpec = tween(250),
        label = "pageCrossfade"
    ) { currentBitmap ->
        if (currentBitmap != null && loadedIndex == index) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 4.dp,
                color = Color.White,
                border = BorderStroke(1.dp, Color.DarkGray)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "PDF Page ${index + 1}",
                        modifier = Modifier
                            .wrapContentSize()
                            .background(Color.White)
                    )

                    if (highlights.isNotEmpty()) {
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier.matchParentSize()
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val bitmapWidth = currentBitmap.width.toFloat()
                            val bitmapHeight = currentBitmap.height.toFloat()

                            if (canvasWidth > 0 && canvasHeight > 0 && bitmapWidth > 0 && bitmapHeight > 0) {
                                val scaleWidth = canvasWidth / bitmapWidth
                                val scaleHeight = canvasHeight / bitmapHeight
                                val scaleFit = minOf(scaleWidth, scaleHeight)

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
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun RiderSliderDefaults(): SliderColors {
    return SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = Color.DarkGray,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent
    )
}
