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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
    onImagesCaptured: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var dpiMode by remember { mutableStateOf("Medium") }
    var filterMode by remember { mutableStateOf("Original") }
    var autoCropEnabled by remember { mutableStateOf(false) }
    
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
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }

    Scaffold { paddingValues ->
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
            
            // Top Overlay Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Filter Dropdown
                var filterExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { filterExpanded = true }) {
                        Icon(Icons.Default.FilterBAndW, contentDescription = "Filter", tint = Color.White)
                    }
                    DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                        listOf("Original", "Magic Enhance", "Grayscale", "B&W Document").forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode) },
                                onClick = { filterMode = mode; filterExpanded = false }
                            )
                        }
                    }
                }

                // DPI Dropdown
                var dpiExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { dpiExpanded = true }) {
                        Icon(Icons.Default.HighQuality, contentDescription = "DPI Context", tint = Color.White)
                    }
                    DropdownMenu(expanded = dpiExpanded, onDismissRequest = { dpiExpanded = false }) {
                        listOf("Low", "Medium", "High").forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode) },
                                onClick = { dpiMode = mode; dpiExpanded = false }
                            )
                        }
                    }
                }
                
                // Auto-Crop Toggle
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Auto-Crop", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = autoCropEnabled,
                        onCheckedChange = { autoCropEnabled = it },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            // Mock edge detection box if auto crop is on
            if (autoCropEnabled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val padding = 50.dp.toPx()
                    
                    drawRect(
                        color = Color.Blue.copy(alpha = 0.5f),
                        topLeft = Offset(padding, padding),
                        size = androidx.compose.ui.geometry.Size(width - 2 * padding, height - 2 * padding),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                }
            }

            // Bottom controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Done button
                if (capturedImages.isNotEmpty()) {
                    Button(onClick = { onImagesCaptured(capturedImages) }) {
                        Text("Save (${capturedImages.size})")
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                // Capture Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White, CircleShape)
                        .border(4.dp, Color.LightGray, CircleShape)
                        .clickable {
                            takePhoto(context, imageCapture, cameraExecutor, autoCropEnabled, filterMode) { uri ->
                                capturedImages.add(uri)
                            }
                        }
                )

                Spacer(modifier = Modifier.width(80.dp))
            }
        }
    }
}

private fun takePhoto(context: Context, imageCapture: ImageCapture, executor: ExecutorService, autoCrop: Boolean, filterMode: String, onImageCaptured: (Uri) -> Unit) {
    val photoFile = File(
        context.cacheDir,
        "scanned_img_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraScanner", "Photo capture failed: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                if (autoCrop || filterMode != "Original") {
                    try {
                        var bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                        
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

                        val out = java.io.FileOutputStream(photoFile)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        out.flush()
                        out.close()
                        bitmap.recycle()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                val savedUri = Uri.fromFile(photoFile)
                ContextCompat.getMainExecutor(context).execute {
                    onImageCaptured(savedUri)
                    Toast.makeText(context, "Captured", Toast.LENGTH_SHORT).show()
                }
            }
        })
}
