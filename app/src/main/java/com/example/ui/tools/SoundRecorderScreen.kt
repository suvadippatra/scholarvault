package com.scholarvault.ui.tools

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.data.model.DocumentFile
import com.scholarvault.service.AudioRecordingService
import com.scholarvault.ui.Screen
import com.scholarvault.ui.formatFileSize
import com.scholarvault.ui.viewmodel.DocumentViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundRecorderScreen(
    docViewModel: com.scholarvault.ui.viewmodel.DocumentViewModel,
    onBack: () -> Unit,
    onNavigateToViewer: (String) -> Unit,
    onNavigateToRecentRecordings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Observe service states
    val isRecording by AudioRecordingService.isRecordingState.collectAsState()
    val isPaused by AudioRecordingService.isPausedState.collectAsState()
    val durationMs by AudioRecordingService.durationMsState.collectAsState()
    val currentFile by AudioRecordingService.currentFileState.collectAsState()
    val completedRecording by AudioRecordingService.completedRecordingFile.collectAsState()

    // Preferences storing settings
    val sharedPrefs = remember { context.getSharedPreferences("sound_recorder_prefs", Context.MODE_PRIVATE) }
    val prefs = remember { com.scholarvault.data.AppPreferences(context) }
    val recordingPrefix by prefs.recordingPrefix.collectAsState(initial = "Recording_")
    var defaultPrefix by remember { mutableStateOf("") }
    LaunchedEffect(recordingPrefix) {
        if (recordingPrefix.isNotBlank()) {
            defaultPrefix = recordingPrefix
        }
    }
    val lowSizeRecording by prefs.lowSizeRecording.collectAsState(initial = false)
    var lowBitrateMode by remember { mutableStateOf(false) }
    LaunchedEffect(lowSizeRecording) {
        lowBitrateMode = lowSizeRecording
    }
    
    val audioFormat by prefs.audioFormat.collectAsState(initial = "m4a")
    var selectedFormat by remember(audioFormat) { mutableStateOf(audioFormat) }
    var showSettings by remember { mutableStateOf(false) }

    // Waveform amplitudes
    val amplitudes = remember { mutableStateListOf<Float>() }

    // Synchronize local amplitudes with Service's dynamicAmplitudes list
    LaunchedEffect(durationMs) {
        if (isRecording && !isPaused) {
            val latest = AudioRecordingService.amplitudeState.value
            amplitudes.add(latest.toFloat())
            if (amplitudes.size > 200) {
                amplitudes.removeAt(0)
            }
        }
    }

    // Reset amplitudes if stopped
    LaunchedEffect(isRecording) {
        if (!isRecording) {
            amplitudes.clear()
        }
    }

    // Track state for save dialog
    var showSaveDialog by remember { mutableStateOf(false) }
    var fileToSave by remember { mutableStateOf<File?>(null) }

    // Handle completed recordings from service
    LaunchedEffect(completedRecording) {
        if (completedRecording != null) {
            val file = completedRecording
            AudioRecordingService.completedRecordingFile.value = null // clear trigger
            if (file != null && file.exists() && file.length() > 0) {
                fileToSave = file
                showSaveDialog = true
            }
        }
    }

    // Recovery check: if there is an autosaved or abandoned recovery file in pref
    var recoveryFileByPrefPath by remember { mutableStateOf(sharedPrefs.getString("pending_recovery_path", null)) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var fileToRecover by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(recoveryFileByPrefPath) {
        if (recoveryFileByPrefPath != null) {
            val file = File(recoveryFileByPrefPath!!)
            if (file.exists() && file.length() > 0) {
                fileToRecover = file
                showRecoveryDialog = true
            } else {
                sharedPrefs.edit().remove("pending_recovery_path").apply()
                recoveryFileByPrefPath = null
            }
        }
    }

    // Handler to detect disposing the screen while recording/paused
    DisposableEffect(Unit) {
        onDispose {
            // "If user paused and leave the page or the app and never come back then typical work will be auto saved..."
            // If the user navigates away, and state is paused, we auto-save it!
            if (AudioRecordingService.isRecordingState.value) {
                if (AudioRecordingService.isPausedState.value) {
                    // It is paused and they left the page. Let's stop and autosave it!
                    val file = AudioRecordingService.currentFileState.value
                    if (file != null) {
                        sharedPrefs.edit().putString("pending_recovery_path", file.absolutePath).apply()
                    }
                    val stopIntent = Intent(context, AudioRecordingService::class.java).apply {
                        action = AudioRecordingService.ACTION_STOP
                    }
                    context.startService(stopIntent)
                } else {
                    // It is actively recording, let it continue in background (foreground service) as requested.
                }
            }
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(
        androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    ) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    Scaffold(
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Sound Recorder",
                showProfileIcon = false,
                showSearchBar = false,
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Recording Settings")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Recent Recordings") },
                                leadingIcon = { Icon(Icons.Default.List, null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToRecentRecordings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, null) },
                                onClick = {
                                    showMenu = false
                                    showSettings = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Upper Content (Timer & Waveform Card)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format("%02d:%02d", durationMs / 1000 / 60, (durationMs / 1000) % 60),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Waveform drawing
                val primaryColor = MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerY = size.height / 2
                        val barWidth = 6f
                        val spacing = 4f
                        val totalBars = (size.width / (barWidth + spacing)).toInt()
                        
                        val renderList = if (isRecording) {
                            amplitudes.takeLast(totalBars)
                        } else {
                            emptyList()
                        }

                        if (renderList.isEmpty()) {
                            // Draw silent midline
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.5f),
                                start = Offset(0f, centerY),
                                end = Offset(size.width, centerY),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )
                        } else {
                            renderList.forEachIndexed { index, amp ->
                                val normalized = kotlin.math.sqrt(amp / 32768f).coerceIn(0.04f, 1f)
                                val barHeight = normalized * size.height
                                val x = index * (barWidth + spacing)
                                
                                drawLine(
                                    color = primaryColor,
                                    start = Offset(x, centerY - barHeight / 2),
                                    end = Offset(x, centerY + barHeight / 2),
                                    strokeWidth = barWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Content (Controls)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 54.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isRecording) {
                    // Record starts
                    Button(
                        onClick = {
                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@Button
                            }
                            
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val defaultName = "${defaultPrefix}_$timestamp"
                            val out = File(context.cacheDir, "$defaultName.$selectedFormat")
                            
                            val startIntent = Intent(context, AudioRecordingService::class.java).apply {
                                action = AudioRecordingService.ACTION_START
                                putExtra(AudioRecordingService.EXTRA_FILE_PATH, out.absolutePath)
                                putExtra(AudioRecordingService.EXTRA_FORMAT, selectedFormat)
                                putExtra(AudioRecordingService.EXTRA_LOW_SIZE, lowBitrateMode)
                            }
                            context.startService(startIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Mic, "Start Recording", modifier = Modifier.size(40.dp))
                    }
                } else {
                    // Controls during active runs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Discard/Cancel
                        IconButton(
                            onClick = {
                                val cancelIntent = Intent(context, AudioRecordingService::class.java).apply {
                                    action = AudioRecordingService.ACTION_CANCEL
                                }
                                context.startService(cancelIntent)
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, "Cancel Recording", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }

                        // Play/Pause / Resume
                        Button(
                            onClick = {
                                val pauseAction = if (isPaused) AudioRecordingService.ACTION_RESUME else AudioRecordingService.ACTION_PAUSE
                                val pauseIntent = Intent(context, AudioRecordingService::class.java).apply {
                                    action = pauseAction
                                }
                                context.startService(pauseIntent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(76.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Save / Stop
                        IconButton(
                            onClick = {
                                val stopIntent = Intent(context, AudioRecordingService::class.java).apply {
                                    action = AudioRecordingService.ACTION_STOP
                                }
                                context.startService(stopIntent)
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(Icons.Default.Check, "Save Recording", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }

        // Save Dialog
        if (showSaveDialog && fileToSave != null) {
            SaveRecordingDialog(
                tempFile = fileToSave!!,
                docViewModel = docViewModel,
                onDismiss = { 
                    // Auto-saves file with default timestamp if dismissed
                    val finalExt = fileToSave!!.extension
                    val size = fileToSave!!.length()
                    
                    docViewModel.insertFile(
                        context = context,
                        file = DocumentFile(
                            name = "${fileToSave!!.nameWithoutExtension}.$finalExt",
                            isFolder = false,
                            parentFolderId = null,
                            filePath = "", 
                            extension = finalExt,
                            sizeBytes = size,
                            tags = listOf("recording", "auto-saved")
                        ),
                        uri = Uri.fromFile(fileToSave!!)
                    )
                    showSaveDialog = false
                    fileToSave = null
                },
                onSave = { 
                    showSaveDialog = false
                    fileToSave = null
                },
                onDiscard = {
                    showSaveDialog = false
                    fileToSave?.delete()
                    fileToSave = null
                }
            )
        }

        // Recovery dialog (for autosaved abandoned recordings next session)
        if (showRecoveryDialog && fileToRecover != null) {
            AlertDialog(
                onDismissRequest = { 
                    // Clear pref if dismissed without action
                    sharedPrefs.edit().remove("pending_recovery_path").apply()
                    showRecoveryDialog = false 
                },
                title = { Text("Recover Unsaved Recording") },
                text = {
                    Text("We found an unsaved session from your last visit. Would you like to save, organize, or rename it now?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            fileToSave = fileToRecover
                            showSaveDialog = true
                            sharedPrefs.edit().remove("pending_recovery_path").apply()
                            showRecoveryDialog = false
                        }
                    ) {
                        Text("Save/Organize")
                    }
                },
                dismissButton = {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            fileToRecover?.delete()
                            sharedPrefs.edit().remove("pending_recovery_path").apply()
                            showRecoveryDialog = false
                        }
                    ) {
                        Text("Discard")
                    }
                }
            )
        }

        // Setting modal dialog
        if (showSettings) {
             AlertDialog(
                 onDismissRequest = { showSettings = false },
                 title = { Text("Quality settings") },
                 text = {
                     Column {
                         Text("Default File Name Prefix:", fontWeight = FontWeight.Bold)
                         Spacer(modifier = Modifier.height(8.dp))
                         OutlinedTextField(
                             value = defaultPrefix,
                             onValueChange = {
                                 defaultPrefix = it
                                 sharedPrefs.edit().putString("default_prefix", it).apply()
                                 scope.launch {
                                     prefs.setRecordingPrefix(it)
                                 }
                             },
                             modifier = Modifier.fillMaxWidth(),
                             shape = RoundedCornerShape(12.dp)
                         )
                         
                         Spacer(modifier = Modifier.height(24.dp))
                         
                         Row(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .clickable {
                                     lowBitrateMode = !lowBitrateMode
                                     scope.launch { prefs.setLowSizeRecording(lowBitrateMode) }
                                 },
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Checkbox(
                                 checked = lowBitrateMode,
                                 onCheckedChange = {
                                     lowBitrateMode = it
                                     scope.launch { prefs.setLowSizeRecording(it) }
                                 }
                             )
                             Spacer(modifier = Modifier.width(8.dp))
                             Column {
                                 Text("Low Size Recording Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                 Text("Decreases bitrate and sample rate for significantly smaller files.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                             }
                         }
                     }
                 },
                 confirmButton = {
                     TextButton(onClick = { showSettings = false }) { Text("Close") }
                 }
             )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SaveRecordingDialog(
    tempFile: File,
    docViewModel: DocumentViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(tempFile.nameWithoutExtension) }
    val allFolders by docViewModel.allFolders.collectAsState()
    var selectedFolderId by remember { mutableStateOf<Int?>(null) }
    var folderMenuExpanded by remember { mutableStateOf(false) }

    // Tag manager
    var tagInput by remember { mutableStateOf("") }
    val tagsList = remember { mutableStateListOf("recording") }

    val allNonFolderFiles by docViewModel.allNonFolderFiles.collectAsState()
    val linkableDocuments = remember(allNonFolderFiles) {
        allNonFolderFiles.filter { file ->
            val ext = file.extension?.lowercase() ?: ""
            ext !in listOf("m4a", "ogg", "wav", "mp3", "amr", "3gp") && !file.isTrashed
        }
    }
    var selectedLinkedDoc by remember { mutableStateOf<com.scholarvault.data.model.DocumentFile?>(null) }
    var linkDocMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Recording") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Assign a name and select a storage folder for this typical audio file.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Name Input
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Recording Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Storage Folder Selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { folderMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        val currentFolderText = if (selectedFolderId == null) "Root Directory" else {
                            allFolders.find { it.id == selectedFolderId }?.name ?: "Unknown Folder"
                        }
                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(currentFolderText, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }

                    DropdownMenu(
                        expanded = folderMenuExpanded,
                        onDismissRequest = { folderMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Root Directory") },
                            onClick = {
                                selectedFolderId = null
                                folderMenuExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Folder, null) }
                        )
                        allFolders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = {
                                    selectedFolderId = folder.id
                                    folderMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.secondary) }
                            )
                        }
                    }
                }

                // Link to Academic Document Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { linkDocMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        val currentDocText = if (selectedLinkedDoc == null) "No linked document" else {
                            selectedLinkedDoc!!.name
                        }
                        Text(currentDocText, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        if (selectedLinkedDoc != null) {
                            IconButton(
                                onClick = { 
                                    selectedLinkedDoc = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }

                    DropdownMenu(
                        expanded = linkDocMenuExpanded,
                        onDismissRequest = { linkDocMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedLinkedDoc = null
                                linkDocMenuExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Link, null) }
                        )
                        linkableDocuments.forEach { file ->
                            DropdownMenuItem(
                                text = { Text(file.name) },
                                onClick = {
                                    selectedLinkedDoc = file
                                    linkDocMenuExpanded = false
                                },
                                leadingIcon = { 
                                    val icon = if (file.extension?.lowercase() == "pdf") Icons.Default.PictureAsPdf else Icons.Default.InsertDriveFile
                                    Icon(icon, null, tint = MaterialTheme.colorScheme.secondary) 
                                }
                            )
                        }
                    }
                }

                // Custom Tags Addition
                Column {
                    Text("Tags", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Simple flow-row for tags
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tagsList.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { tagsList.remove(tag) },
                                label = { Text(tag) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            placeholder = { Text("Add tag...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (tagInput.isNotBlank()) {
                                    val cleaned = tagInput.trim().lowercase()
                                    if (!tagsList.contains(cleaned)) {
                                        tagsList.add(cleaned)
                                    }
                                    tagInput = ""
                                }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalExt = tempFile.extension
                    val finalName = if (nameInput.isNotBlank()) nameInput.trim() else tempFile.nameWithoutExtension
                    val finalTags = tagsList.toMutableList()
                    selectedLinkedDoc?.let {
                        finalTags.add("linked-doc-id:${it.id}")
                        finalTags.add("linked-doc-name:${it.name}")
                    }
                    val destFile = DocumentFile(
                        name = "$finalName.$finalExt",
                        isFolder = false,
                        parentFolderId = selectedFolderId,
                        filePath = "", 
                        extension = finalExt,
                        sizeBytes = tempFile.length(),
                        tags = finalTags.toList()
                    )
                    docViewModel.insertFile(context, destFile, Uri.fromFile(tempFile))
                    onSave()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                onClick = onDiscard
            ) {
                Text("Discard")
            }
        }
    )
}
