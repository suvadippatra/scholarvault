package com.scholarvault.ui.audio

import android.content.ComponentName
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.scholarvault.service.AudioPlaybackService
import com.scholarvault.util.SecurityVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.Executor

// Fast direct executor to add listener to ListenableFuture safely
private val DirectExecutor = Executor { command -> command.run() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    filePath: String,
    fileName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tempDecryptedFile by remember { mutableStateOf<File?>(null) }
    var actualFile by remember { mutableStateOf<File?>(null) }
    var loadError by remember { mutableStateOf(false) }

    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var repeatState by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) } // 0=Off, 1=One, 2=All

    // Meta details from retriever
    var metaArtist by remember { mutableStateOf<String?>(null) }
    var metaFormat by remember { mutableStateOf<String?>(null) }
    var metaBitrate by remember { mutableStateOf<String?>(null) }
    var metaSampleRate by remember { mutableStateOf<String?>(null) }
    var albumArtBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Interactivity
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showMetaSheet by remember { mutableStateOf(false) }
    var sleepTimerSeconds by remember { mutableLongStateOf(0L) } // Sleep Countdown
    var sleepTimerJob by remember { mutableStateOf<Job?>(null) }

    // Track playback updates
    LaunchedEffect(mediaController, isPlaying) {
        if (mediaController != null) {
            while (true) {
                playbackPosition = mediaController!!.currentPosition
                totalDuration = mediaController!!.duration.coerceAtLeast(0L)
                delay(500)
            }
        }
    }

    // Sleep Timer countdown processor
    LaunchedEffect(sleepTimerSeconds) {
        if (sleepTimerSeconds > 0) {
            while (sleepTimerSeconds > 0) {
                delay(1000)
                sleepTimerSeconds -= 1
            }
            // Timer expired -> PAUSE playback
            mediaController?.pause()
        }
    }

    // Decrypt source & bind to media controller
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

                            // Pull metadata details natively
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(decFile.absolutePath)
                                metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                                val bitrateRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                                metaBitrate = if (bitrateRaw != null) "${bitrateRaw.toLong() / 1000} kbps" else null
                                val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                                metaFormat = if (hasAudio == "yes") "Audio Layer Match" else null

                                val artBytes = retriever.embeddedPicture
                                if (artBytes != null) {
                                    val bmp = android.graphics.BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                                    withContext(Dispatchers.Main) {
                                        albumArtBitmap = bmp
                                    }
                                }
                            } catch (ignored: Exception) {
                            } finally {
                                retriever.release()
                            }

                            // Bind MediaController connection
                            withContext(Dispatchers.Main) {
                                val sessionToken = SessionToken(context, ComponentName(context, AudioPlaybackService::class.java))
                                val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
                                controllerFuture.addListener({
                                    try {
                                        val controllerRef = controllerFuture.get()
                                        mediaController = controllerRef
                                        // Load media item
                                        controllerRef.setMediaItem(MediaItem.fromUri(Uri.fromFile(decFile)))
                                        controllerRef.prepare()
                                        controllerRef.play()
                                        
                                        // Observe state updates
                                        controllerRef.addListener(object : Player.Listener {
                                            override fun onIsPlayingChanged(playing: Boolean) {
                                                isPlaying = playing
                                            }
                                        })
                                        isPlaying = controllerRef.isPlaying
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, DirectExecutor)
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

    DisposableEffect(Unit) {
        onDispose {
            // Unbind media controller safely to prevent memory leaks (Do NOT release player so music survives)
            mediaController?.release()
            if (tempDecryptedFile != null && tempDecryptedFile != actualFile) {
                tempDecryptedFile?.delete()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val file = tempDecryptedFile ?: return@IconButton
                        try {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "audio/*"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Audio"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { showMetaSheet = true }) {
                        Icon(Icons.Default.Info, contentDescription = "File Details")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (tempDecryptedFile != null) {
                // Waveform art placeholder
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (albumArtBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = albumArtBitmap!!.asImageBitmap(),
                            contentDescription = "Album Art",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Metadata Display
                Text(
                    text = fileName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = metaArtist ?: "Secure Local Workspace",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Seek slider status bar
                Spacer(modifier = Modifier.height(32.dp))
                Slider(
                    value = if (totalDuration > 0) playbackPosition.toFloat() / totalDuration.toFloat() else 0f,
                    onValueChange = {
                        val newPos = (it * totalDuration).toLong()
                        playbackPosition = newPos
                        mediaController?.seekTo(newPos)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatDuration(playbackPosition), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = formatDuration(totalDuration), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Media Controller navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sleep Timer
                    IconButton(onClick = {
                        // Cycles: Off -> 10m -> 30m -> 60m
                        sleepTimerSeconds = when (sleepTimerSeconds) {
                            0L -> 600L // 10 mins
                            600L -> 1800L // 30 mins
                            1800L -> 3600L // 60 mins
                            else -> 0L // Off
                        }
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerSeconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (sleepTimerSeconds > 0) {
                                Text(
                                    text = "${sleepTimerSeconds / 60}m",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Seek backward 10s
                    IconButton(onClick = {
                        val current = mediaController?.currentPosition ?: 0
                        mediaController?.seekTo((current - 10000L).coerceAtLeast(0L))
                    }) {
                        Icon(Icons.Default.Replay10, contentDescription = "Backward 10s", modifier = Modifier.size(32.dp))
                    }

                    // Main PLAY / PAUSE Button
                    IconButton(
                        onClick = {
                            val c = mediaController ?: return@IconButton
                            if (isPlaying) {
                                c.pause()
                            } else {
                                c.play()
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Seek forward 10s
                    IconButton(onClick = {
                        val current = mediaController?.currentPosition ?: 0
                        val duration = mediaController?.duration ?: 0
                        mediaController?.seekTo((current + 10000L).coerceAtMost(duration))
                    }) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", modifier = Modifier.size(32.dp))
                    }

                    // Repeat State toggler
                    IconButton(onClick = {
                        val nextRepeatState = when (repeatState) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                            else -> Player.REPEAT_MODE_OFF
                        }
                        repeatState = nextRepeatState
                        mediaController?.repeatMode = nextRepeatState
                    }) {
                        Icon(
                            imageVector = when (repeatState) {
                                Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                                else -> Icons.Default.RepeatOn
                            },
                            contentDescription = "Loop Mode",
                            tint = if (repeatState != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Playback speed pill
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.clickable { showSpeedDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SlowMotionVideo, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Speed: ${currentSpeed}x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else if (loadError) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, tint = MaterialTheme.colorScheme.error, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Error launching background audio player.", color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Go Back")
                        }
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }

    // Playback Speed Selector Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed") },
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentSpeed = speed
                                    mediaController?.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentSpeed == speed),
                                onClick = {
                                    currentSpeed = speed
                                    mediaController?.setPlaybackSpeed(speed)
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

    // Detailed metadata sheet
    if (showMetaSheet) {
        ModalBottomSheet(onDismissRequest = { showMetaSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Audio File Specifications",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DetailRow(label = "Document", value = fileName)
                DetailRow(label = "Estimated Artist", value = metaArtist ?: "Private Vault Workspace")
                DetailRow(label = "Bit-rate Speed", value = metaBitrate ?: "Variable stream decoding")
                DetailRow(label = "Format Tag", value = metaFormat ?: "Standard file audio stream")

                val file = tempDecryptedFile
                if (file != null) {
                    DetailRow(label = "Size on Disk", value = formatFileSize(file.length()))
                    DetailRow(label = "Access Stream Path", value = file.absolutePath)
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

// Format duration into standard mm:ss
private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "00:00"
    val minutes = (millis / 1000) / 60
    val seconds = (millis / 1000) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
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
