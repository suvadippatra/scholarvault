package com.scholarvault.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.ui.theme.LocalThemeController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToWalletSecurity: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val securityManager = remember { com.scholarvault.security.WalletSecurityManager(context) }
    
    val themeController = LocalThemeController.current
    val isSecurityEnabled by securityManager.isSecurityEnabled.collectAsState()
    
    val prefs = remember { com.scholarvault.data.AppPreferences(context) }
    val pdfMode by prefs.pdfViewerMode.collectAsState(initial = "google_drive")
    val flipAnim by prefs.pdfFlipAnimation.collectAsState(initial = false)

    var showPdfModeDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Settings",
                showProfileIcon = false,
                showSearchBar = false
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Text(
                        text = "Account & Security",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                    )
                }

                item {
                    SettingClickItem(
                        title = "User Profile",
                        subtitle = "Manage personal information and goals",
                        onClick = { onNavigateToProfile() }
                    )
                }
                
                item {
                    SettingClickItem(
                        title = "Wallet Security Settings",
                        subtitle = if (isSecurityEnabled) "ON - Secured" else "OFF - Unsecured",
                        onClick = { onNavigateToWalletSecurity() }
                    )
                }
                
                item {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }
                
                item {
                    Text(
                        text = "Appearance & Performance",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }

                item {
                    SettingClickItem(
                        title = "App Theme",
                        subtitle = when (themeController.themeMode) {
                            "light" -> "Light Mode"
                            "dark" -> "Dark Mode"
                            else -> "System Default"
                        },
                        onClick = { showThemeDialog = true }
                    )
                }
                
                item {
                    SettingToggleItem(
                        title = "Enable Animations",
                        subtitle = "Disable to improve performance on low-end devices",
                        checked = themeController.animationsEnabled,
                        onCheckedChange = { themeController.toggleAnimations() }
                    )
                }

                item {
                    SettingToggleItem(
                        title = "Reader Page Flip Animations",
                        subtitle = if (flipAnim) "Flip Transition Enabled" else "Flip Transition Disabled",
                        checked = flipAnim,
                        onCheckedChange = { checked ->
                            scope.launch { prefs.setPdfFlipAnimation(checked) }
                        }
                    )
                }
                
                item {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }
                
                item {
                    Text(
                        text = "App Integrations",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }
                
                item {
                    val subtitleText = when (pdfMode) {
                        "google_drive" -> "Google Drive Viewer"
                        "custom_v2" -> "Built-in Custom Viewer V2 (Modular & Adaptive)"
                        else -> "Built-in Custom Viewer V1 (Classic)"
                    }
                    SettingClickItem(
                        title = "Default PDF Viewer",
                        subtitle = subtitleText,
                        onClick = { showPdfModeDialog = true }
                    )
                }

                item {
                    val scannerEngine by prefs.scannerEngine.collectAsState(initial = "custom")
                    var showScannerEngineDialog by remember { mutableStateOf(false) }
                    
                    val scannerSubtitle = when (scannerEngine) {
                        "system" -> "System Default Scanner (ML Kit)"
                        else -> "Custom Camera Scanner"
                    }
                    SettingClickItem(
                        title = "Default Document Scanner",
                        subtitle = scannerSubtitle,
                        onClick = { showScannerEngineDialog = true }
                    )

                    if (showScannerEngineDialog) {
                        AlertDialog(
                            onDismissRequest = { showScannerEngineDialog = false },
                            title = { Text("Default Scanner Engine") },
                            text = {
                                Column {
                                    listOf(
                                        "custom" to "Custom Camera Scanner",
                                        "system" to "System Scanner (ML Kit)"
                                    ).forEach { (key, label) ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    scope.launch { prefs.setScannerEngine(key) }
                                                    showScannerEngineDialog = false
                                                }
                                                .padding(vertical = 12.dp, horizontal = 16.dp)
                                        ) {
                                            RadioButton(
                                                selected = (scannerEngine == key),
                                                onClick = {
                                                    scope.launch { prefs.setScannerEngine(key) }
                                                    showScannerEngineDialog = false
                                                }
                                            )
                                            Text(label, modifier = Modifier.padding(start = 12.dp), fontSize = 16.sp)
                                        }
                                    }
                                }
                            },
                            confirmButton = {}
                        )
                    }
                }

                item {
                    val audioFormat by prefs.audioFormat.collectAsState(initial = "m4a")
                    var formatName = "M4A (AAC / High Quality)"
                    if (audioFormat == "ogg") formatName = "OGG (Standard Quality)"
                    if (audioFormat == "wav") formatName = "WAV (Uncompressed)"
                    
                    var showAudioFormatDialog by remember { mutableStateOf(false) }

                    SettingClickItem(
                        title = "Audio Recording Quality",
                        subtitle = formatName,
                        onClick = { showAudioFormatDialog = true }
                    )

                    if (showAudioFormatDialog) {
                         AlertDialog(
                             onDismissRequest = { showAudioFormatDialog = false },
                             title = { Text("Audio Recording format") },
                             text = {
                                 Column {
                                     listOf(
                                         "m4a" to "M4A (AAC / High Quality)",
                                         "ogg" to "OGG (Standard Quality)",
                                         "wav" to "WAV (Uncompressed)"
                                     ).forEach { (key, label) ->
                                         Row(
                                             verticalAlignment = Alignment.CenterVertically,
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .clickable {
                                                     scope.launch { prefs.setAudioFormat(key) }
                                                     showAudioFormatDialog = false
                                                 }
                                                 .padding(vertical = 12.dp, horizontal = 16.dp)
                                         ) {
                                             RadioButton(
                                                 selected = (audioFormat == key),
                                                 onClick = {
                                                     scope.launch { prefs.setAudioFormat(key) }
                                                     showAudioFormatDialog = false
                                                 }
                                             )
                                             Text(label, modifier = Modifier.padding(start = 12.dp), fontSize = 16.sp)
                                         }
                                     }
                                 }
                             },
                             confirmButton = {}
                         )
                    }
                }

                item {
                    val prefixFlow by prefs.recordingPrefix.collectAsState(initial = "Recording_")
                    var prefixInput by remember(prefixFlow) { mutableStateOf(prefixFlow) }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(text = "Default Audio Name Prefix", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = "Initial file name prefix used when launching recording (e.g., CS101_Lecture_)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = prefixInput,
                            onValueChange = { newValue ->
                                prefixInput = newValue
                                scope.launch {
                                    prefs.setRecordingPrefix(newValue)
                                }
                            },
                            placeholder = { Text("Recording_") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                item {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                item {
                    Text(
                        text = "Storage & Persistence",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }

                item {
                    SettingClickItem(
                        title = "Clear Document Cache",
                        subtitle = "Deletes temporary decrypted files and thumbnails",
                        onClick = {
                            scope.launch {
                                val cacheDir = context.cacheDir
                                cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                                android.widget.Toast.makeText(context, "Cache cleared successfully", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                item {
                    SettingClickItem(
                        title = "Clear Document Scanner Drafts",
                        subtitle = "Deletes unsaved scanned pages and partial sessions",
                        onClick = {
                            scope.launch {
                                val prefs = context.getSharedPreferences("scanner_draft", android.content.Context.MODE_PRIVATE)
                                val savedDraftStr = prefs.getString("draft_uris", null)
                                if (savedDraftStr != null) {
                                    try {
                                        val array = org.json.JSONArray(savedDraftStr)
                                        for (i in 0 until array.length()) {
                                            val uri = android.net.Uri.parse(array.getString(i))
                                            uri.path?.let { java.io.File(it).delete() }
                                        }
                                    } catch (e: Exception) {}
                                }
                                prefs.edit().remove("draft_uris").apply()
                                
                                val draftsDir = java.io.File(context.cacheDir, "scanner_drafts")
                                if (draftsDir.exists()) draftsDir.deleteRecursively()
                                
                                android.widget.Toast.makeText(context, "Scanner drafts cleared", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                item {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                item {
                    Text(
                        text = "About",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }

                item {
                    SettingClickItem(
                        title = "About ScholarVault",
                        subtitle = "Detailed app description, security architecture, and tools",
                        onClick = onNavigateToAbout
                    )
                }
            }

            if (showThemeDialog) {
                AlertDialog(
                    onDismissRequest = { showThemeDialog = false },
                    title = { Text("Select Theme") },
                    text = {
                        Column {
                            listOf(
                                "system" to "System Default",
                                "light" to "Light Mode",
                                "dark" to "Dark Mode"
                            ).forEach { (mode, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            themeController.setThemeMode(mode)
                                            showThemeDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 16.dp)
                                ) {
                                    RadioButton(
                                        selected = (themeController.themeMode == mode),
                                        onClick = {
                                            themeController.setThemeMode(mode)
                                            showThemeDialog = false
                                        }
                                    )
                                    Text(label, modifier = Modifier.padding(start = 12.dp), fontSize = 16.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }

            if (showPdfModeDialog) {
                AlertDialog(
                    onDismissRequest = { showPdfModeDialog = false },
                    title = { Text("PDF Viewer Mode") },
                    text = {
                        Column {
                            listOf(
                                "google_drive" to "Google Drive Viewer",
                                "custom" to "Built-in Custom Viewer V1 (Classic)",
                                "custom_v2" to "Built-in Custom Viewer V2 (Modular & Adaptive)"
                            ).forEach { (key, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch { prefs.setPdfViewerMode(key) }
                                            showPdfModeDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 16.dp)
                                ) {
                                    RadioButton(
                                        selected = (pdfMode == key),
                                        onClick = {
                                            scope.launch { prefs.setPdfViewerMode(key) }
                                            showPdfModeDialog = false
                                        }
                                    )
                                    Text(label, modifier = Modifier.padding(start = 12.dp), fontSize = 16.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }
        }
    }
}

@Composable
fun SettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(text = subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingClickItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(text = subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

