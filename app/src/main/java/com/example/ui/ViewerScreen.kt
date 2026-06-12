package com.scholarvault.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.scholarvault.ui.pdf.v2.CustomPdfViewer
import com.scholarvault.util.SecurityVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

// ─────────────────────────────────────────────────────────────
// Entry point router
// ─────────────────────────────────────────────────────────────
@Composable
fun ViewerScreen(
    fileType: String,
    filePath: String,
    fileName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Read PDF viewer preference from DataStore
    val prefs = remember { com.scholarvault.data.AppPreferences(context) }
    val pdfMode by prefs.pdfViewerMode.collectAsState(initial = "custom")

    when (fileType) {
        "pdf" -> {
            val isWalletFile = filePath.startsWith("wallet_secure/")
            if (!isWalletFile && pdfMode == "google_drive") {
                GoogleDrivePdfLauncher(filePath = filePath, onBack = onBack)
            } else {
                if (isWalletFile && pdfMode == "google_drive") {
                    // Show a one-time snackbar / toast explaining the override
                    LaunchedEffect(Unit) {
                        android.widget.Toast.makeText(
                            context,
                            "Wallet documents are opened in the secure built-in viewer only.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
                CustomPdfViewer(filePath = filePath, fileName = fileName, onBack = onBack)
            }
        }
        "pdf_external" -> {
            CustomPdfViewer(filePath = filePath, fileName = fileName, onBack = onBack, isExternalUri = true)
        }
        "image" -> ImageViewerScreen(filePath = filePath, fileName = fileName, onBack = onBack)
        "audio" -> AudioPlayerScreen(filePath = filePath, fileName = fileName, onBack = onBack)
        "video" -> VideoPlayerScreen(filePath = filePath, fileName = fileName, onBack = onBack)
        else -> {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.InsertDriveFile, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Unsupported file format", color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Google Drive launcher (unchanged logic, extracted cleanly)
// ─────────────────────────────────────────────────────────────
@Composable
fun GoogleDrivePdfLauncher(filePath: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var loadError by remember { mutableStateOf<String?>(null) }
    var launched by remember { mutableStateOf(false) }

    LaunchedEffect(filePath) {
        if (launched || filePath.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val file = if (filePath.startsWith("wallet_secure/")) File(context.filesDir, filePath)
                           else File(filePath)
                if (!file.exists()) { loadError = "File not found."; return@withContext }
                val vault = SecurityVault(context)
                val sharedDir = File(context.cacheDir, "shared").also { it.mkdirs() }
                val tFile = vault.getFileForViewing(file, sharedDir)
                    ?: run { loadError = "Failed to load file."; return@withContext }
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", tFile)
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = android.content.Intent.createChooser(intent, "Open PDF")
                val resInfoList = context.packageManager.queryIntentActivities(chooser, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(packageName, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try { context.startActivity(chooser) }
                catch (e: Exception) { loadError = "Error: ${e.message}" }
                launched = true
                withContext(Dispatchers.Main) { onBack() }
            } catch (e: Exception) { loadError = "Error: ${e.message}" }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (loadError != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(loadError!!, color = Color.Red, modifier = Modifier.padding(16.dp))
                Button(onClick = onBack) { Text("Go Back") }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Opening in PDF Viewer…", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// IMAGE VIEWER (Google Photos–style)
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(filePath: String, fileName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var tempFile by remember { mutableStateOf<File?>(null) }
    var actualFile by remember { mutableStateOf<File?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var overlayVisible by remember { mutableStateOf(true) }
    var showInfo by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { z, p, _ ->
        scale = (scale * z).coerceIn(1f, 6f)
        offset = if (scale > 1f) offset + p else Offset.Zero
    }
    val animatedScale by animateFloatAsState(scale, label = "imgScale")

    // System bars
    val view = LocalView.current
    val activity = context as? Activity
    DisposableEffect(isFullScreen) {
        val ctrl = activity?.let { WindowCompat.getInsetsController(it.window, view) }
        if (isFullScreen) ctrl?.hide(WindowInsetsCompat.Type.systemBars())
        else ctrl?.show(WindowInsetsCompat.Type.systemBars())
        onDispose { ctrl?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    LaunchedEffect(filePath) {
        if (filePath.isBlank()) { loadError = true; return@LaunchedEffect }
        withContext(Dispatchers.IO) {
            try {
                val file = if (filePath.startsWith("wallet_secure/")) File(context.filesDir, filePath)
                           else File(filePath)
                if (!file.exists()) { loadError = true; return@withContext }
                actualFile = file
                val vault = SecurityVault(context)
                val tFile = vault.getFileForViewing(file, context.cacheDir)
                tempFile = tFile ?: run { loadError = true; null }
            } catch (e: Exception) { loadError = true }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (tempFile != null && tempFile != actualFile) tempFile?.delete()
        }
    }

    BackHandler { if (isFullScreen) isFullScreen = false else onBack() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main image
        if (tempFile != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(tempFile)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = animatedScale, scaleY = animatedScale,
                        translationX = offset.x, translationY = offset.y
                    )
                    .transformable(state = transformableState)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { overlayVisible = !overlayVisible },
                            onDoubleTap = {
                                if (scale > 1f) { scale = 1f; offset = Offset.Zero }
                                else scale = 2.5f
                            }
                        )
                    }
            )
        } else if (loadError) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BrokenImage, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Failed to load image", color = Color.Gray)
            }
        } else {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        // Overlay top bar
        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()
        ) {
            Surface(color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Text(
                        fileName, color = Color.White,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Default.Info, "Info", tint = Color.White)
                    }
                    IconButton(onClick = {
                        val f = tempFile ?: return@IconButton
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", f)
                            com.scholarvault.ui.tools.SharedData.navigateToPrePrint.value = true
                            com.scholarvault.ui.tools.SharedData.pendingUris.value = listOf(uri)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) {
                        Icon(Icons.Default.Print, "Print", tint = Color.White)
                    }
                    IconButton(onClick = {
                        val f = tempFile ?: return@IconButton
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", f)
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share Image"))
                    }) {
                        Icon(Icons.Default.Share, "Share", tint = Color.White)
                    }
                }
            }
        }

        // Overlay bottom bar
        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
        ) {
            Surface(color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { isFullScreen = !isFullScreen }) {
                        Icon(
                            if (isFullScreen) Icons.Default.CloseFullscreen else Icons.Default.Fullscreen,
                            "Full Screen", tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // Info bottom sheet
    if (showInfo && tempFile != null) {
        ModalBottomSheet(onDismissRequest = { showInfo = false }) {
            Column(Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("Image Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                val opts = android.graphics.BitmapFactory.Options().also {
                    it.inJustDecodeBounds = true
                    android.graphics.BitmapFactory.decodeFile(tempFile!!.absolutePath, it)
                }
                ViewerDetailRow("Name", fileName)
                ViewerDetailRow("Dimensions", "${opts.outWidth} × ${opts.outHeight} px")
                ViewerDetailRow("Format", opts.outMimeType ?: "Unknown")
                ViewerDetailRow("Size", formatBytes(tempFile!!.length()))
                ViewerDetailRow("Modified", SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                    .format(Date(tempFile!!.lastModified())))
            }
        }
    }
}

@Composable
fun ViewerDetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp, modifier = Modifier.weight(0.35f))
        Text(value, fontSize = 14.sp, modifier = Modifier.weight(0.65f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            overflow = TextOverflow.Ellipsis, maxLines = 2)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// ─────────────────────────────────────────────────────────────
// AUDIO PLAYER
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(filePath: String, fileName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tempFile by remember { mutableStateOf<File?>(null) }
    var actualFile by remember { mutableStateOf<File?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var isPlaying by remember { mutableStateOf(false) }
    var currentMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var repeatMode by remember { mutableIntStateOf(0) } // 0=off 1=one 2=all
    var showSpeedMenu by remember { mutableStateOf(false) }

    LaunchedEffect(filePath) {
        if (filePath.isBlank()) { loadError = true; return@LaunchedEffect }
        withContext(Dispatchers.IO) {
            try {
                val file = if (filePath.startsWith("wallet_secure/")) File(context.filesDir, filePath)
                           else File(filePath)
                if (!file.exists()) { loadError = true; return@withContext }
                actualFile = file
                val vault = SecurityVault(context)
                val tFile = vault.getFileForViewing(file, context.cacheDir)
                tempFile = tFile ?: run { loadError = true; return@withContext }
                withContext(Dispatchers.Main) {
                    exoPlayer.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(tFile)))
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
            } catch (e: Exception) { loadError = true }
        }
    }

    // Progress ticker
    LaunchedEffect(exoPlayer) {
        while (true) {
            isPlaying = exoPlayer.isPlaying
            currentMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.takeIf { it > 0 } ?: 0L
            delay(300)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            if (tempFile != null && tempFile != actualFile) tempFile?.delete()
        }
    }

    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Text(
                    fileName, modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showDetails = true }) {
                    Icon(Icons.Default.Info, "Details")
                }
            }
        }

        if (loadError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Failed to load audio file", color = MaterialTheme.colorScheme.error)
            }
            return@Column
        }

        // Body
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Album art placeholder
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(200.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                fileName, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                formatMs(durationMs), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Seek bar
            Slider(
                value = if (durationMs > 0) currentMs.toFloat() / durationMs.toFloat() else 0f,
                onValueChange = { exoPlayer.seekTo((it * durationMs).toLong()) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs(currentMs), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMs(durationMs), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(20.dp))

            // Controls row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Repeat button
                IconButton(onClick = {
                    repeatMode = (repeatMode + 1) % 3
                    exoPlayer.repeatMode = when (repeatMode) {
                        1 -> androidx.media3.common.Player.REPEAT_MODE_ONE
                        2 -> androidx.media3.common.Player.REPEAT_MODE_ALL
                        else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                    }
                }) {
                    Icon(
                        when (repeatMode) {
                            1 -> Icons.Default.RepeatOne
                            2 -> Icons.Default.Repeat
                            else -> Icons.Default.Repeat
                        },
                        "Repeat",
                        tint = if (repeatMode > 0) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Skip -10s
                IconButton(onClick = { exoPlayer.seekTo((currentMs - 10000).coerceAtLeast(0)) }) {
                    Icon(Icons.Default.Replay10, "Rewind 10s")
                }

                // Play/Pause (big)
                FilledIconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) exoPlayer.pause()
                        else exoPlayer.play()
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause", modifier = Modifier.size(32.dp)
                    )
                }

                // Skip +10s
                IconButton(onClick = { exoPlayer.seekTo(currentMs + 10000) }) {
                    Icon(Icons.Default.Forward10, "Forward 10s")
                }

                // Speed
                Box {
                    IconButton(onClick = { showSpeedMenu = true }) {
                        Text(
                            "${if (playbackSpeed == playbackSpeed.toLong().toFloat())
                                "${playbackSpeed.toInt()}x" else "${playbackSpeed}x"}",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }) {
                        speeds.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s}x") },
                                onClick = {
                                    playbackSpeed = s
                                    exoPlayer.setPlaybackSpeed(s)
                                    showSpeedMenu = false
                                },
                                trailingIcon = {
                                    if (s == playbackSpeed)
                                        Icon(Icons.Default.Check, null,
                                            tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Details sheet
    if (showDetails && tempFile != null) {
        ModalBottomSheet(onDismissRequest = { showDetails = false }) {
            Column(Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("Audio Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                val mmr = remember(tempFile) {
                    android.media.MediaMetadataRetriever().also {
                        try { it.setDataSource(tempFile!!.absolutePath) } catch (_: Exception) {}
                    }
                }
                ViewerDetailRow("Name", fileName)
                ViewerDetailRow("Duration", formatMs(durationMs))
                ViewerDetailRow("Bitrate", "${(mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()?.div(1000) ?: "–")} kbps")
                ViewerDetailRow("Size", formatBytes(tempFile!!.length()))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// VIDEO PLAYER
// ─────────────────────────────────────────────────────────────
@Composable
fun VideoPlayerScreen(filePath: String, fileName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var tempFile by remember { mutableStateOf<File?>(null) }
    var actualFile by remember { mutableStateOf<File?>(null) }
    var loadError by remember { mutableStateOf(false) }
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(filePath) {
        if (filePath.isBlank()) { loadError = true; return@LaunchedEffect }
        withContext(Dispatchers.IO) {
            try {
                val file = if (filePath.startsWith("wallet_secure/")) File(context.filesDir, filePath)
                           else File(filePath)
                if (!file.exists()) { loadError = true; return@withContext }
                actualFile = file
                val vault = SecurityVault(context)
                val tFile = vault.getFileForViewing(file, context.cacheDir)
                tempFile = tFile ?: run { loadError = true; return@withContext }
                withContext(Dispatchers.Main) {
                    exoPlayer.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(tFile)))
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
            } catch (e: Exception) { loadError = true }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            if (tempFile != null && tempFile != actualFile) tempFile?.delete()
        }
    }

    BackHandler { onBack() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (loadError) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.VideocamOff, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Failed to load video", color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Go Back") }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────
private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
