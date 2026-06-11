package com.scholarvault.ui.tools

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun FileDropBox(
    onUrisSelected: (List<Uri>) -> Unit,
    onClose: (() -> Unit)? = null,
    mimeType: String = "*/*",
    isCompact: Boolean = false
) {
    val context = LocalContext.current
    
    // Launchers
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) onUrisSelected(uris)
    }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) onUrisSelected(uris)
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let { onUrisSelected(listOf(it)) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val tempFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show()
        }
    }

    val launchCamera = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val tempFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    var showMyFilesSelector by remember { mutableStateOf(false) }
    var showWalletSelector by remember { mutableStateOf(false) }

    if (showMyFilesSelector) {
        MyFilesSelectorDialog(
            onUrisSelected = { 
                showMyFilesSelector = false
                onUrisSelected(it) 
            },
            onDismiss = { showMyFilesSelector = false }
        )
    }
    
    if (showWalletSelector) {
        WalletFilesSelectorDialog(
            onUrisSelected = {
                showWalletSelector = false
                onUrisSelected(it)
            },
            onDismiss = { showWalletSelector = false }
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 130.dp else 190.dp)
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                drawRoundRect(
                    color = primaryColor,
                    style = Stroke(width = strokeWidth, pathEffect = dashPathEffect),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                android.view.View(viewContext).apply {
                    setOnDragListener { _, event ->
                        when (event.action) {
                            android.view.DragEvent.ACTION_DRAG_STARTED -> true
                            android.view.DragEvent.ACTION_DROP -> {
                                val activity = (viewContext as? android.app.Activity)
                                    ?: ((viewContext as? android.content.ContextWrapper)?.baseContext as? android.app.Activity)
                                val permissions = activity?.requestDragAndDropPermissions(event)
                                
                                val clipData = event.clipData
                                if (clipData != null) {
                                    val uris = mutableListOf<Uri>()
                                    for (i in 0 until clipData.itemCount) {
                                        clipData.getItemAt(i).uri?.let { uris.add(it) }
                                    }
                                    if (uris.isNotEmpty()) {
                                        onUrisSelected(uris)
                                    }
                                }
                                true
                            }
                            else -> true
                        }
                    }
                }
            }
        )
        
        if (onClose != null) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close, 
                    contentDescription = "Close drop box",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (!isCompact) {
                Icon(
                    Icons.Default.UploadFile, 
                    contentDescription = "Upload icon", 
                    modifier = Modifier.size(36.dp), 
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Drag & Drop Files Here", 
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        Icons.Default.UploadFile, 
                        contentDescription = "Upload icon", 
                        modifier = Modifier.size(20.dp), 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Drop Files Here", 
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Buttons row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                val buttonHeight = if (isCompact) 28.dp else 34.dp
                val buttonFontSize = if (isCompact) 10.sp else 11.sp
                val buttonPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)

                Button(
                    onClick = { filePicker.launch(mimeType) },
                    contentPadding = buttonPadding,
                    modifier = Modifier.height(buttonHeight)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    if (!isCompact) {
                        Spacer(Modifier.width(4.dp))
                        Text("Device", fontSize = buttonFontSize)
                    } else {
                        Spacer(Modifier.width(4.dp))
                        Text("Device", fontSize = buttonFontSize)
                    }
                }

                OutlinedButton(
                    onClick = { showWalletSelector = true },
                    contentPadding = buttonPadding,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                    modifier = Modifier.height(buttonHeight)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                    if (!isCompact) {
                        Spacer(Modifier.width(4.dp))
                        Text("Vault", fontSize = buttonFontSize)
                    } else {
                        Spacer(Modifier.width(4.dp))
                        Text("Vault", fontSize = buttonFontSize)
                    }
                }

                OutlinedButton(
                    onClick = { showMyFilesSelector = true },
                    contentPadding = buttonPadding,
                    modifier = Modifier.height(buttonHeight)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                    if (!isCompact) {
                        Spacer(Modifier.width(4.dp))
                        Text("My Files", fontSize = buttonFontSize)
                    } else {
                        Spacer(Modifier.width(4.dp))
                        Text("My Files", fontSize = buttonFontSize)
                    }
                }
            }
        }
    }
}
