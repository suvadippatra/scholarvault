package com.scholarvault.ui.tools.focustimer

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.ui.components.TopSearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    val keepScreenOn = FocusTimerManager.isFullScreen || FocusTimerManager.isRunning
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val isRunning = FocusTimerManager.isRunning
    val isSoundEnabled = FocusTimerManager.isSoundEnabled
    val currentSound = FocusTimerManager.currentSound
    val isUsingCustomAudio = FocusTimerManager.isUsingCustomAudio
    val customAudioUrl = FocusTimerManager.customAudioUrl

    DisposableEffect(isRunning, isSoundEnabled, currentSound, isUsingCustomAudio, customAudioUrl) {
        var synthJob: Job? = null
        var mediaPlayer: android.media.MediaPlayer? = null
        
        if (isRunning && isSoundEnabled) {
            if (isUsingCustomAudio && customAudioUrl.isNotBlank()) {
                try {
                    mediaPlayer = android.media.MediaPlayer().apply {
                        setDataSource(customAudioUrl)
                        setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
                        isLooping = true
                        setOnPreparedListener { start() }
                        prepareAsync()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (currentSound != TimerSound.None) {
                val freq = when (currentSound) {
                    TimerSound.SeaBeach -> 120.0
                    TimerSound.Raining -> 600.0
                    TimerSound.Autumn -> 320.0
                    TimerSound.Muse -> 440.0
                    TimerSound.Storm -> 85.0
                    else -> 200.0
                }
                
                synthJob = CoroutineScope(Dispatchers.Default).launch {
                    val sampleRate = 8000
                    val bufferSize = 4000
                    val generatedSnd = ByteArray(2 * bufferSize)
                    val audioTrack = android.media.AudioTrack(
                        android.media.AudioManager.STREAM_MUSIC,
                        sampleRate,
                        android.media.AudioFormat.CHANNEL_OUT_MONO,
                        android.media.AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize * 2,
                        android.media.AudioTrack.MODE_STREAM
                    )
                    
                    try {
                        audioTrack.play()
                        var phase = 0.0
                        while (isActive) {
                            for (i in 0 until bufferSize) {
                                val value = kotlin.math.sin(phase)
                                phase += 2.0 * kotlin.math.PI * (freq + (kotlin.random.Random.nextDouble() - 0.5) * (freq * 0.1)) / sampleRate
                                val valShort = (value * 12000).toInt().toShort()
                                val index = i * 2
                                generatedSnd[index] = (valShort.toInt() and 0x00ff).toByte()
                                generatedSnd[index + 1] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
                            }
                            audioTrack.write(generatedSnd, 0, generatedSnd.size)
                            delay(100)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            audioTrack.stop()
                            audioTrack.release()
                        } catch (e: Exception) {}
                    }
                }
            }
        }
        
        onDispose {
            synthJob?.cancel()
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {}
        }
    }

    // Auto-enter full screen when running
    LaunchedEffect(FocusTimerManager.isRunning) {
        if (FocusTimerManager.isRunning && !FocusTimerManager.isFullScreen) {
            FocusTimerManager.isFullScreen = true
        }
    }

    if (FocusTimerManager.isFullScreen) {
        FullScreenTimer()
        return
    }

    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            var showPreviousTimersDialog by remember { mutableStateOf(false) }
            if (showPreviousTimersDialog) {
                PreviousTimersDialog(onDismiss = { showPreviousTimersDialog = false })
            }
            TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Focus Timer",
                showProfileIcon = false,
                actions = {
                    IconButton(onClick = { showPreviousTimersDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "Saved Timers")
                    }
                    IconButton(onClick = { FocusTimerManager.isFullScreen = true }) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Full Screen")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isTablet = maxWidth > 600.dp
            
            if (isTablet) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1.5f).padding(16.dp)) {
                        TimerPreviewAndControls()
                    }
                    if (!FocusTimerManager.hasStarted) {
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                            PresetsAndSettingsPanel()
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimerPreviewAndControls()
                    if (!FocusTimerManager.hasStarted) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        Spacer(modifier = Modifier.height(16.dp))
                        PresetsAndSettingsPanel()
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenTimer() {
    val hapticFeedback = LocalHapticFeedback.current
    BackHandler { FocusTimerManager.isFullScreen = false }

    val context = LocalContext.current
    val activity = context as? Activity
    
    // Immersive Mode to prevent notification panel from interfering
    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            window?.let { w ->
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(w, w.decorView)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // OLED Screen Burn-In Protection via Pixel Shifting
    var burnInOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    LaunchedEffect(FocusTimerManager.isRunning) {
        if (FocusTimerManager.isRunning) {
            while (isActive) {
                delay(30000) // shift pixels every 30 seconds
                val dx = kotlin.random.Random.nextInt(-8, 9).toFloat()
                val dy = kotlin.random.Random.nextInt(-8, 9).toFloat()
                burnInOffset = Offset(dx, dy)
            }
        } else {
            burnInOffset = Offset(0f, 0f)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { FocusTimerManager.isFullScreen = false },
        contentAlignment = Alignment.Center
    ) {
        // Background rendering based on theme
        when (FocusTimerManager.currentTheme) {
            TimerTheme.Wonderspace -> WonderspaceBackground(FocusTimerManager.isRunning)
            TimerTheme.Minimalist -> Box(Modifier.fillMaxSize().background(Color(0xFF1E1E1E)))
            TimerTheme.SevenSegment -> Box(Modifier.fillMaxSize().background(Color.Black))
            TimerTheme.FlipClock -> Box(Modifier.fillMaxSize().background(Color(0xFF16161A)))
        }

        // Timer Content based on theme with OLED shift offset applied to graphicsLayer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                translationX = burnInOffset.x
                translationY = burnInOffset.y
            }
        ) {
            Text(
                text = FocusTimerManager.title.ifBlank { "Focus Timer" },
                fontSize = 32.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            val minutes = FocusTimerManager.remainingSeconds / 60
            val seconds = FocusTimerManager.remainingSeconds % 60
            
            when (FocusTimerManager.currentTheme) {
                TimerTheme.Minimalist -> {
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontSize = 140.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 4.sp
                    )
                }
                TimerTheme.Wonderspace -> {
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontSize = 160.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                TimerTheme.SevenSegment -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF090A0C), RoundedCornerShape(16.dp))
                            .border(2.dp, Color(0xFF1D2228), RoundedCornerShape(16.dp))
                            .padding(horizontal = 24.dp, vertical = 28.dp)
                            .shadow(16.dp, RoundedCornerShape(16.dp))
                    ) {
                        val minStr = String.format("%02d", minutes)
                        val secStr = String.format("%02d", seconds)
                        
                        SevenSegmentDigit(minStr[0], color = Color(0xFF00FF55), modifier = Modifier.size(64.dp, 110.dp))
                        SevenSegmentDigit(minStr[1], color = Color(0xFF00FF55), modifier = Modifier.size(64.dp, 110.dp))
                        
                        SevenSegmentColon(color = Color(0xFF00FF55), modifier = Modifier.size(24.dp, 110.dp))
                        
                        SevenSegmentDigit(secStr[0], color = Color(0xFF00FF55), modifier = Modifier.size(64.dp, 110.dp))
                        SevenSegmentDigit(secStr[1], color = Color(0xFF00FF55), modifier = Modifier.size(64.dp, 110.dp))
                    }
                }
                TimerTheme.FlipClock -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val minStr = String.format("%02d", minutes)
                        val secStr = String.format("%02d", seconds)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FlipCard(minStr[0].toString(), modifier = Modifier.size(65.dp, 100.dp))
                            FlipCard(minStr[1].toString(), modifier = Modifier.size(65.dp, 100.dp))
                        }
                        
                        Text(
                            text = ":",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FlipCard(secStr[0].toString(), modifier = Modifier.size(65.dp, 100.dp))
                            FlipCard(secStr[1].toString(), modifier = Modifier.size(65.dp, 100.dp))
                        }
                    }
                }
            }

            if (FocusTimerManager.isRunning) {
                Spacer(modifier = Modifier.height(48.dp))
                Icon(
                    imageVector = Icons.Default.Pause, 
                    contentDescription = "Running", 
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                )
            }
        }
    }
}

@Composable
fun FlipCard(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF1E1E24), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .shadow(6.dp, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF25252D), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.8f)))
            Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF17171C), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)))
        }
        Text(
            text = text,
            fontSize = 54.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFB300),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun SevenSegmentDigit(
    char: Char,
    color: Color = Color(0xFF00FF55),
    modifier: Modifier = Modifier
) {
    val segments = when (char) {
        '0' -> booleanArrayOf(true, true, true, true, true, true, false)
        '1' -> booleanArrayOf(false, true, true, false, false, false, false)
        '2' -> booleanArrayOf(true, true, false, true, true, false, true)
        '3' -> booleanArrayOf(true, true, true, true, false, false, true)
        '4' -> booleanArrayOf(false, true, true, false, false, true, true)
        '5' -> booleanArrayOf(true, false, true, true, false, true, true)
        '6' -> booleanArrayOf(true, false, true, true, true, true, true)
        '7' -> booleanArrayOf(true, true, true, false, false, false, false)
        '8' -> booleanArrayOf(true, true, true, true, true, true, true)
        '9' -> booleanArrayOf(true, true, true, true, false, true, true)
        '-' -> booleanArrayOf(false, false, false, false, false, false, true)
        else -> booleanArrayOf(false, false, false, false, false, false, false)
    }
    val inactiveColor = color.copy(alpha = 0.08f)
    val strokeWidth = 8.dp
    
    Canvas(
        modifier = modifier
            .padding(2.dp)
    ) {
        val w = size.width
        val h = size.height
        val t = strokeWidth.toPx()
        val hPad = t / 2 + 1f
        
        // 0: Segment A (Top)
        drawLine(
            color = if (segments[0]) color else inactiveColor,
            start = Offset(hPad + 2f, hPad),
            end = Offset(w - hPad - 2f, hPad),
            strokeWidth = t,
            cap = StrokeCap.Round
        )
        // 1: Segment B (Top-Right)
        drawLine(
            color = if (segments[1]) color else inactiveColor,
            start = Offset(w - hPad, hPad + 2f),
            end = Offset(w - hPad, h / 2 - 1f),
            strokeWidth = t,
            cap = StrokeCap.Round
        )
        // 2: Segment C (Bottom-Right)
        drawLine(
            color = if (segments[2]) color else inactiveColor,
            start = Offset(w - hPad, h / 2 + 1f),
            end = Offset(w - hPad, h - hPad - 2f),
            strokeWidth = t,
            cap = StrokeCap.Round
        )
        // 3: Segment D (Bottom)
        drawLine(
            color = if (segments[3]) color else inactiveColor,
            start = Offset(hPad + 2f, h - hPad),
            end = Offset(w - hPad - 2f, h - hPad),
            strokeWidth = t,
            cap = StrokeCap.Round
        )
        // 4: Segment E (Bottom-Left)
        drawLine(
            color = if (segments[4]) color else inactiveColor,
            start = Offset(hPad, h / 2 + 1f),
            end = Offset(hPad, h - hPad - 2f),
            strokeWidth = t,
            cap = StrokeCap.Round
        )
        // 5: Segment F (Top-Left)
        drawLine(
            color = if (segments[5]) color else inactiveColor,
            start = Offset(hPad, hPad + 2f),
            end = Offset(hPad, h / 2 - 1f),
            strokeWidth = t,
            cap = StrokeCap.Round
        )
        // 6: Segment G (Middle)
        drawLine(
            color = if (segments[6]) color else inactiveColor,
            start = Offset(hPad + 2f, h / 2),
            end = Offset(w - hPad - 2f, h / 2),
            strokeWidth = t,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SevenSegmentColon(
    color: Color = Color(0xFF00FF55),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = 5.dp.toPx()
        drawCircle(
            color = color,
            radius = r,
            center = Offset(w / 2, h * 0.35f)
        )
        drawCircle(
            color = color,
            radius = r,
            center = Offset(w / 2, h * 0.65f)
        )
    }
}

@Composable
fun WonderspaceBackground(isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "Wonderspace")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .drawBehind {
                if (isRunning) {
                    val brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.5f),
                            Color(0xFF3B82F6).copy(alpha = 0.5f),
                            Color(0xFF10B981).copy(alpha = 0.5f),
                            Color(0xFF8B5CF6).copy(alpha = 0.5f)
                        ),
                        center = Offset(size.width / 2, size.height / 2)
                    )
                    drawCircle(
                        brush = brush,
                        radius = (size.minDimension / 1.5f) * pulse,
                        center = Offset(size.width / 2, size.height / 2),
                        alpha = 0.8f
                    )
                }
            }
    )
}

@Composable
fun TimerPreviewAndControls() {
    val hapticFeedback = LocalHapticFeedback.current
    var isEditingTitle by remember { mutableStateOf(false) }
    var titleTextField by remember { mutableStateOf(FocusTimerManager.title) }
    var isEditingDuration by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isEditingTitle) {
            OutlinedTextField(
                value = titleTextField,
                onValueChange = { titleTextField = it },
                label = { Text("Session Title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val mins = if (FocusTimerManager.totalSeconds > 0) FocusTimerManager.totalSeconds / 60 else 25
                    FocusTimerManager.saveAsPreset(titleTextField, mins)
                    isEditingTitle = false
                }),
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        } else {
            Text(
                text = FocusTimerManager.title.ifBlank { "Study Session" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        if (!FocusTimerManager.isRunning) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            titleTextField = FocusTimerManager.title
                            isEditingTitle = true
                        }
                    })
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (FocusTimerManager.isRunning) "Running (Double-tap lock)" else "Double-tap title to edit & save as preset",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        val progress by animateFloatAsState(
            targetValue = if (FocusTimerManager.totalSeconds == 0) 0f else FocusTimerManager.remainingSeconds / FocusTimerManager.totalSeconds.toFloat(),
            animationSpec = tween(1000),
            label = "progress"
        )

        if (FocusTimerManager.hasStarted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val circleColor = MaterialTheme.colorScheme.surfaceVariant
                val progressColor = MaterialTheme.colorScheme.primary
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = circleColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isEditingDuration) {
                        var durTextField by remember { mutableStateOf((FocusTimerManager.remainingSeconds / 60).toString()) }
                        OutlinedTextField(
                            value = durTextField,
                            onValueChange = { durTextField = it.filter { char -> char.isDigit() }.take(3) },
                            label = { Text("Minutes") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val mins = durTextField.toIntOrNull() ?: 25
                                FocusTimerManager.setTimer(mins)
                                isEditingDuration = false
                            }),
                            modifier = Modifier.width(100.dp)
                        )
                    } else {
                        val minutes = FocusTimerManager.remainingSeconds / 60
                        val seconds = FocusTimerManager.remainingSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = {
                                    if (!FocusTimerManager.isRunning) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isEditingDuration = true
                                    }
                                })
                            }
                        )
                        Text(
                            text = if (FocusTimerManager.isRunning) "Locked" else "Double-tap to Edit Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            // Compact Mode - initially hides circular border to keep layout compact!
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isEditingDuration) {
                        var durTextField by remember { mutableStateOf((FocusTimerManager.remainingSeconds / 60).toString()) }
                        OutlinedTextField(
                            value = durTextField,
                            onValueChange = { durTextField = it.filter { char -> char.isDigit() }.take(3) },
                            label = { Text("Minutes") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val mins = durTextField.toIntOrNull() ?: 25
                                FocusTimerManager.setTimer(mins)
                                isEditingDuration = false
                            }),
                            modifier = Modifier.width(100.dp)
                        )
                    } else {
                        val minutes = FocusTimerManager.remainingSeconds / 60
                        val seconds = FocusTimerManager.remainingSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = {
                                    if (!FocusTimerManager.isRunning) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isEditingDuration = true
                                    }
                                })
                            }
                        )
                        Text(
                            text = "Double-tap to Edit Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    FocusTimerManager.reset()
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Replay, contentDescription = "Reset")
            }
            
            Spacer(modifier = Modifier.width(24.dp))
            
            FloatingActionButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    FocusTimerManager.toggle()
                    if (FocusTimerManager.isRunning) {
                        FocusTimerManager.isFullScreen = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = if (FocusTimerManager.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (FocusTimerManager.isRunning) "Pause" else "Play",
                    modifier = Modifier.size(36.dp)
                )
            }

            if (FocusTimerManager.hasStarted) {
                Spacer(modifier = Modifier.width(24.dp))
                
                // Exit timer button: resets hasStarted and returns screen to compact setup mode
                FloatingActionButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        FocusTimerManager.isRunning = false
                        FocusTimerManager.hasStarted = false
                        FocusTimerManager.reset()
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit Timer")
                }
            }
        }
    }
}

@Composable
fun PresetsAndSettingsPanel() {
    val hapticFeedback = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Saved Presets (Drag to Reorder)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        var draggedIndex by remember { mutableStateOf<Int?>(null) }
        var offsetX by remember { mutableStateOf(0f) }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(FocusTimerManager.historyPresets, key = { _, p -> p.title + "_" + p.minutes }) { index, preset ->
                val isSelected = FocusTimerManager.title == preset.title && FocusTimerManager.totalSeconds == preset.minutes * 60
                val isDragged = draggedIndex == index
                val zIndex = if (isDragged) 1f else 0f
                val translationX = if (isDragged) offsetX else 0f

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        FocusTimerManager.title = preset.title
                        FocusTimerManager.setTimer(preset.minutes)
                    },
                    label = { Text("${preset.title} (${preset.minutes}m)") },
                    modifier = Modifier
                        .zIndex(zIndex)
                        .graphicsLayer { this.translationX = translationX }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggedIndex = index; offsetX = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                    val itemWidth = size.width.toFloat()
                                    if (offsetX > itemWidth && index < FocusTimerManager.historyPresets.size - 1) {
                                        FocusTimerManager.movePreset(index, index + 1)
                                        draggedIndex = index + 1
                                        offsetX -= itemWidth
                                    } else if (offsetX < -itemWidth && index > 0) {
                                        FocusTimerManager.movePreset(index, index - 1)
                                        draggedIndex = index - 1
                                        offsetX += itemWidth
                                    }
                                },
                                onDragEnd = { draggedIndex = null; offsetX = 0f },
                                onDragCancel = { draggedIndex = null; offsetX = 0f }
                            )
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Full Screen Themes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimerTheme.values().forEach { theme ->
                val isSelected = FocusTimerManager.currentTheme == theme
                Card(
                    modifier = Modifier
                        .width(130.dp)
                        .clickable { FocusTimerManager.currentTheme = theme },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Small preview thumbnail container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(
                                    when (theme) {
                                        TimerTheme.Wonderspace -> Color(0xFF0F172A)
                                        TimerTheme.Minimalist -> Color(0xFF1E1E1E)
                                        TimerTheme.SevenSegment -> Color.Black
                                        TimerTheme.FlipClock -> Color(0xFF16161A)
                                    },
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (theme) {
                                TimerTheme.Wonderspace -> {
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFF8B5CF6).copy(alpha = 0.6f), CircleShape))
                                }
                                TimerTheme.Minimalist -> {
                                    Text("25:00", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                TimerTheme.SevenSegment -> {
                                    Text("25:00", fontSize = 14.sp, color = Color.Green, fontFamily = FontFamily.Monospace)
                                }
                                TimerTheme.FlipClock -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Box(modifier = Modifier.size(16.dp, 20.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center) {
                                            Text("2", fontSize = 10.sp, color = Color(0xFFFFB300))
                                        }
                                        Box(modifier = Modifier.size(16.dp, 20.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center) {
                                            Text("5", fontSize = 10.sp, color = Color(0xFFFFB300))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = theme.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Background Audio Player",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Background Audio", fontWeight = FontWeight.Bold)
                    Switch(
                        checked = FocusTimerManager.isSoundEnabled,
                        onCheckedChange = { FocusTimerManager.isSoundEnabled = it }
                    )
                }
                
                if (FocusTimerManager.isSoundEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { FocusTimerManager.isUsingCustomAudio = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!FocusTimerManager.isUsingCustomAudio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (!FocusTimerManager.isUsingCustomAudio) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Soothe Synthesizer")
                        }
                        
                        Button(
                            onClick = { FocusTimerManager.isUsingCustomAudio = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (FocusTimerManager.isUsingCustomAudio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (FocusTimerManager.isUsingCustomAudio) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Custom Stream URL")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!FocusTimerManager.isUsingCustomAudio) {
                        Text("Select a Soothing sound & tap 🔊 to preview:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimerSound.values().forEach { sound ->
                                val isSelected = FocusTimerManager.currentSound == sound
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { FocusTimerManager.currentSound = sound }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { FocusTimerManager.currentSound = sound }
                                        )
                                        Text(
                                            sound.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    
                                    if (sound != TimerSound.None) {
                                        IconButton(onClick = {
                                            val freq = when (sound) {
                                                TimerSound.SeaBeach -> 120.0
                                                TimerSound.Raining -> 600.0
                                                TimerSound.Autumn -> 320.0
                                                TimerSound.Muse -> 440.0
                                                TimerSound.Storm -> 85.0
                                                else -> 200.0
                                            }
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                            playFocusSynthPreview(freq, durationSeconds = 2.5)
                                        }) {
                                            Text("🔊", fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Enter a Custom Streamable Audio/Radio URL:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = FocusTimerManager.customAudioUrl,
                            onValueChange = { FocusTimerManager.customAudioUrl = it },
                            placeholder = { Text("https://streams.radio.com/ambient.mp3") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (FocusTimerManager.customAudioUrl.isNotBlank()) {
                                        try {
                                            android.media.MediaPlayer().apply {
                                                setDataSource(FocusTimerManager.customAudioUrl)
                                                setOnPreparedListener { 
                                                    start()
                                                    CoroutineScope(Dispatchers.Main).launch {
                                                        delay(4000)
                                                        try {
                                                            this@apply.stop()
                                                            this@apply.release()
                                                        } catch (e: Exception) {}
                                                    }
                                                }
                                                prepareAsync()
                                            }
                                        } catch (e: Exception) {}
                                    }
                                },
                                enabled = FocusTimerManager.customAudioUrl.isNotBlank()
                            ) {
                                Text("🔊 Preview Stream")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Background Style",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimerBackground.values().forEach { bg ->
                ChoiceChip(
                    text = bg.name,
                    selected = FocusTimerManager.currentBackground == bg,
                    onSelect = { FocusTimerManager.currentBackground = bg }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Secondary Audio Overlay (Lo-Fi)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LofiTrack.values().forEach { track ->
                ChoiceChip(
                    text = track.name,
                    selected = FocusTimerManager.currentLofiTrack == track,
                    onSelect = { FocusTimerManager.currentLofiTrack = track }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceChip(text: String, selected: Boolean, onSelect: () -> Unit) {
    ElevatedFilterChip(
        selected = selected,
        onClick = onSelect,
        label = { Text(text) },
        colors = FilterChipDefaults.elevatedFilterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun PreviousTimersDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "My Saved Timers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = "Manage and prioritize your frequent intervals:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                if (FocusTimerManager.historyPresets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No saved timers yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(FocusTimerManager.historyPresets) { index, preset ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (FocusTimerManager.title == preset.title && FocusTimerManager.totalSeconds == preset.minutes * 60)
                                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "${preset.minutes} minutes",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            FocusTimerManager.title = preset.title
                                            FocusTimerManager.setTimer(preset.minutes)
                                            onDismiss()
                                        }) {
                                            Icon(Icons.Default.Check, contentDescription = "Use", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        
                                        IconButton(
                                            onClick = { FocusTimerManager.movePreset(index, index - 1) },
                                            enabled = index > 0
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                                        }
                                        
                                        IconButton(
                                            onClick = { FocusTimerManager.movePreset(index, index + 1) },
                                            enabled = index < FocusTimerManager.historyPresets.size - 1
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun playFocusSynthPreview(freq: Double, durationSeconds: Double = 1.5) {
    CoroutineScope(Dispatchers.Default).launch {
        val sampleRate = 8000
        val numSamples = (durationSeconds * sampleRate).toInt()
        val sample = DoubleArray(numSamples)
        val generatedSnd = ByteArray(2 * numSamples)
        
        for (i in 0 until numSamples) {
            sample[i] = kotlin.math.sin(2.0 * kotlin.math.PI * i / (sampleRate / freq))
        }
        
        var idx = 0
        for (dVal in sample) {
            val valShort = (dVal * 32767).toInt().toShort()
            generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
            generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
        }
        
        try {
            val audioTrack = android.media.AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.size,
                android.media.AudioTrack.MODE_STATIC
            )
            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}

