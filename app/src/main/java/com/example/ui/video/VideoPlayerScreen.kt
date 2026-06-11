package com.scholarvault.ui.video

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.scholarvault.util.SecurityVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    filePath: String,
    fileName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var tempDecryptedFile by remember { mutableStateOf<File?>(null) }
    var actualFile by remember { mutableStateOf<File?>(null) }
    var loadError by remember { mutableStateOf(false) }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    
    // Playback control state
    var isPlaying by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showMetaSheet by remember { mutableStateOf(false) }

    // Swipe Hud States
    var volumeLevel by remember { mutableFloatStateOf(0f) } // 0 to 1
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0f) } // 0 to 1
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var indicatorDismissJob by remember { mutableStateOf<Job?>(null) }

    // Metadata details
    var metaResolution by remember { mutableStateOf<String?>(null) }
    var metaCodec by remember { mutableStateOf<String?>(null) }
    var metaDuration by remember { mutableLongStateOf(0L) }

    // Orientation and Immersive setup
    DisposableEffect(Unit) {
        if (activity != null) {
            // 1. Lock screen orientation to LANDSCAPE safely
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            
            // 2. Hide all system bars for a premium immersive widescreen look
            val window = activity.window
            val decorView = window.decorView
            val controller = WindowCompat.getInsetsController(window, decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            if (activity != null) {
                // Restore orientation
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                
                // Show system bars back
                val window = activity.window
                val decorView = window.decorView
                WindowCompat.getInsetsController(window, decorView).show(WindowInsetsCompat.Type.systemBars())
            }
            exoPlayer.release()
            if (tempDecryptedFile != null && tempDecryptedFile != actualFile) {
                tempDecryptedFile?.delete()
            }
        }
    }

    // Decrypt and load ExoPlayer
    LaunchedEffect(filePath) {
        if (filePath.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val file = if (filePath.startsWith("wallet_secure/")) {
                        File(context.filesDir, filePath)
                    } else {
                        File(filePath)
                    }
                    if (file.exists()) {
                        actualFile = file
                        val vault = SecurityVault(context)
                        val decFile = vault.getFileForViewing(file, context.cacheDir)
                        if (decFile != null) {
                            tempDecryptedFile = decFile

                            // Parse Video specifications natively
                            val extractor = android.media.MediaExtractor()
                            try {
                                extractor.setDataSource(decFile.absolutePath)
                                for (i in 0 until extractor.trackCount) {
                                    val format = extractor.getTrackFormat(i)
                                    val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                                    if (mime.startsWith("video/")) {
                                        val width = format.getInteger(android.media.MediaFormat.KEY_WIDTH)
                                        val height = format.getInteger(android.media.MediaFormat.KEY_HEIGHT)
                                        metaResolution = "${width}x${height}"
                                        
                                        if (format.containsKey(android.media.MediaFormat.KEY_DURATION)) {
                                            metaDuration = format.getLong(android.media.MediaFormat.KEY_DURATION) / 1000
                                        }
                                        metaCodec = mime.substringAfter("video/")
                                        break
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                extractor.release()
                            }

                            withContext(Dispatchers.Main) {
                                exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(decFile)))
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                                isPlaying = exoPlayer.isPlaying
                                
                                exoPlayer.addListener(object : Player.Listener {
                                    override fun onIsPlayingChanged(playing: Boolean) {
                                        isPlaying = playing
                                    }
                                    override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                                        currentSpeed = playbackParameters.speed
                                    }
                                })
                            }
                        } else {
                            loadError = true
                        }
                    } else {
                        loadError = true
                    }
                } catch (e: Exception) {
                    loadError = true
                }
            }
        }
    }

    // Screen Gestures Volume/Brightness layout
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenWidth = constraints.maxWidth

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val width = size.width
                            if (offset.x < width / 2) {
                                val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                            } else {
                                val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(newPos)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            indicatorDismissJob?.cancel()
                        },
                        onDragEnd = {
                            indicatorDismissJob = scope.launch {
                                delay(1200)
                                showVolumeIndicator = false
                                showBrightnessIndicator = false
                            }
                        },
                        onDrag = { change, dragAmount ->
                            // Left Half = Brightness, Right Half = Volume
                            if (change.position.x < screenWidth / 2) {
                                // Adjust Brightness
                                activity?.let { act ->
                                    val attributes = act.window.attributes
                                    val delta = -dragAmount.y / 600f
                                    val currentBrightness = if (attributes.screenBrightness < 0) 0.5f else attributes.screenBrightness
                                    val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1.0f)
                                    attributes.screenBrightness = newBrightness
                                    act.window.attributes = attributes
                                    
                                    brightnessLevel = newBrightness
                                    showBrightnessIndicator = true
                                    showVolumeIndicator = false
                                }
                            } else {
                                // Adjust Vol
                                val maxMusicVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val currentMusicVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val stepHeight = 120f
                                val volDelta = if (dragAmount.y < 0) 1 else -1

                                if (Math.abs(dragAmount.y) > 2f) {
                                    val finalVol = (currentMusicVol + volDelta).coerceIn(0, maxMusicVol)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, finalVol, 0)
                                    volumeLevel = finalVol.toFloat() / maxMusicVol.toFloat()
                                    showVolumeIndicator = true
                                    showBrightnessIndicator = false
                                }
                            }
                        }
                    )
                }
        ) {
            if (tempDecryptedFile != null) {
                // ExoPlayer View embedded in immersive layout
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            setShowFastForwardButton(true)
                            setShowRewindButton(true)
                        }
                    }
                )

                // Navigation Top overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = fileName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    
                    // Core video controls
                    IconButton(onClick = { showSpeedDialog = true }) {
                        Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
                    }
                    IconButton(onClick = {
                        val file = tempDecryptedFile ?: return@IconButton
                        try {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "video/*"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Video"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                    IconButton(onClick = { showMetaSheet = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                    }
                }

                // Volume slider HUD indicator
                AnimatedVisibility(
                    visible = showVolumeIndicator,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 40.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .width(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (volumeLevel <= 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${(volumeLevel * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Brightness slider HUD indicator
                AnimatedVisibility(
                    visible = showBrightnessIndicator,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 40.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .width(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessMedium,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${(brightnessLevel * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (loadError) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, tint = MaterialTheme.colorScheme.error, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Error preparing immersive video decoder.", color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Go Back")
                        }
                    }
                }
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Widescreen Playback Speed") },
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    exoPlayer.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentSpeed == speed),
                                onClick = {
                                    exoPlayer.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                            )
                            Text(
                                text = "${speed}x",
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Modal bottom sheet metadata
    if (showMetaSheet) {
        ModalBottomSheet(onDismissRequest = { showMetaSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Video Stream Specifications",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DetailRow(label = "Document", value = fileName)
                DetailRow(label = "Hardware Codec Mapping", value = metaCodec ?: "Hardware standard decoder")
                DetailRow(label = "Widescreen Resolution", value = metaResolution ?: "Standard Aspect Ratio")
                
                if (metaDuration > 0) {
                    val minutes = (metaDuration / 1000) / 60
                    val seconds = (metaDuration / 1000) % 60
                    DetailRow(label = "Duration", value = String.format(Locale.getDefault(), "%02d:%02d mins", minutes, seconds))
                }

                val file = tempDecryptedFile
                if (file != null) {
                    DetailRow(label = "Size on Disk", value = formatFileSize(file.length()))
                    DetailRow(label = "Cache Path", value = file.absolutePath)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showMetaSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
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
