package com.scholarvault.ui.tools.scanner

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CustomCameraScannerScreen(
    onBack: () -> Unit,
    onImagesCaptured: (List<Uri>, Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var dpiMode by remember { mutableStateOf("Medium") } // Low, Medium, High
    var filterMode by remember { mutableStateOf("Original") }
    var autoCropEnabled by remember { mutableStateOf(false) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    
    val capturedImages = remember { mutableStateListOf<Uri>() }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (!cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required to use the scanner.")
        }
        return
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    val imageCapture = remember(dpiMode) { 
        val builder = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        when (dpiMode) {
            "High" -> builder.setTargetResolution(android.util.Size(3840, 2160)) // 4k-ish
            "Low" -> builder.setTargetResolution(android.util.Size(1280, 720))
            else -> builder.setTargetResolution(android.util.Size(1920, 1080))
        }
        builder.build() 
    }
    
    LaunchedEffect(flashMode) {
        imageCapture.flashMode = flashMode
    }
    
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        this.scaleType = PreviewView.ScaleType.FILL_CENTER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    previewViewRef = previewView

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (exc:Exception) {
                            Log.e("CameraScanner", "Use case binding failed", exc)
                        }

                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )
            
            // Top Overlay Toolbar (Liquid Glass Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.DarkGray.copy(alpha = 0.6f), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // Flash Quick Toggle
                IconButton(onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                        else -> ImageCapture.FLASH_MODE_AUTO
                    }
                }) {
                    Icon(
                        imageVector = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = "Flash Toggle",
                        tint = if (flashMode == ImageCapture.FLASH_MODE_ON) MaterialTheme.colorScheme.primary else Color.White
                    )
                }

                // DPI Quick Toggle
                TextButton(onClick = { 
                    dpiMode = when (dpiMode) {
                        "Low" -> "Medium"
                        "Medium" -> "High"
                        else -> "Low"
                    }
                }) {
                    Text(dpiMode, color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                
                // Auto-Crop Toggle
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Auto Mode", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = autoCropEnabled,
                        onCheckedChange = { autoCropEnabled = it },
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
            }

            // Mock edge detection box if auto crop is on
            if (autoCropEnabled) {
                val boxColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val padding = 50.dp.toPx()
                    
                    drawRoundRect(
                        color = boxColor,
                        topLeft = Offset(padding, padding),
                        size = androidx.compose.ui.geometry.Size(width - 2 * padding, height - 2 * padding),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
                    )
                }
            }

            // Bottom Control Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .fillMaxWidth()
                    .background(Color.DarkGray.copy(alpha = 0.6f), androidx.compose.foundation.shape.RoundedCornerShape(40.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last Image Preview Thumb with Badge
                    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        if (capturedImages.isNotEmpty()) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(capturedImages.last()),
                                contentDescription = "Last captured",
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.Gray, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp)
                            ) { Text("${capturedImages.size}") }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.DarkGray, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    // Shutter Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White, CircleShape)
                            .border(4.dp, Color.LightGray, CircleShape)
                            .clickable {
                                previewViewRef?.display?.rotation?.let { currentRotation ->
                                    imageCapture.targetRotation = currentRotation
                                }
                                takePhoto(context, imageCapture, cameraExecutor, autoCropEnabled, filterMode) { uri ->
                                    capturedImages.add(uri)
                                }
                            }
                    )

                    // Next Process Button
                    IconButton(
                        onClick = { 
                            if (capturedImages.isNotEmpty()) {
                                onImagesCaptured(capturedImages, autoCropEnabled)
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        enabled = capturedImages.isNotEmpty()
                    ) {
                        Icon(if (autoCropEnabled) Icons.Default.Check else Icons.Default.ArrowForward, contentDescription = "Next", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

private fun takePhoto(context: Context, imageCapture: ImageCapture, executor: ExecutorService, autoCrop: Boolean, filterMode: String, onImageCaptured: (Uri) -> Unit) {
    val draftsDir = File(context.cacheDir, "scanner_drafts")
    if (!draftsDir.exists()) draftsDir.mkdirs()
    
    val timestamp = System.currentTimeMillis()
    val originalFile = File(draftsDir, "raw_img_$timestamp.jpg")
    val editedFile = File(draftsDir, "edited_img_$timestamp.jpg")

    val outputOptions = ImageCapture.OutputFileOptions.Builder(originalFile).build()

    imageCapture.takePicture(
        outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraScanner", "Photo capture failed: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                var finalFile = originalFile
                if (autoCrop || filterMode != "Original") {
                    try {
                        var bitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
                        
                        // 1. Crop
                        if (autoCrop) {
                            val cropX = (bitmap.width * 0.1).toInt()
                            val cropY = (bitmap.height * 0.1).toInt()
                            val cropWidth = bitmap.width - 2 * cropX
                            val cropHeight = bitmap.height - 2 * cropY
                            val croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
                            bitmap.recycle()
                            bitmap = croppedBitmap
                        }

                        // 2. Filter
                        if (filterMode != "Original") {
                            val filteredBitmap = FilterEngine.applyFilter(bitmap, filterMode)
                            if (filteredBitmap != bitmap) {
                                bitmap.recycle()
                                bitmap = filteredBitmap
                            }
                        }

                        val out = java.io.FileOutputStream(editedFile)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        out.flush()
                        out.close()
                        bitmap.recycle()
                        
                        // Save the association between edited and original
                        val prefs = context.getSharedPreferences("scanner_draft", Context.MODE_PRIVATE)
                        prefs.edit().putString("original_${editedFile.absolutePath}", originalFile.absolutePath).apply()
                        
                        finalFile = editedFile
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    val prefs = context.getSharedPreferences("scanner_draft", Context.MODE_PRIVATE)
                    prefs.edit().putString("original_${originalFile.absolutePath}", originalFile.absolutePath).apply()
                }
                
                val savedUri = Uri.fromFile(finalFile)
                ContextCompat.getMainExecutor(context).execute {
                    onImageCaptured(savedUri)
                    Toast.makeText(context, "Captured", Toast.LENGTH_SHORT).show()
                }
            }
        })
}
