package com.scholarvault.ui.tools.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.draw.rotate
import kotlin.math.hypot
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedPageMultiEditorScreen(
    initialUris: List<Uri>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onSaveAll: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var uris by remember { mutableStateOf(initialUris.toMutableList()) }
    var currentIndex by remember { mutableStateOf(initialIndex) }
    
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Transform State
    var rotationAngle by remember { mutableStateOf(0f) }
    var tiltAngle by remember { mutableStateOf(0f) }
    // Points are stored in relative coordinates (0..1)
    var points by remember { mutableStateOf(listOf(
        Offset(0f, 0f), Offset(1f, 0f),
        Offset(0f, 1f), Offset(1f, 1f)
    )) }
    var isRectCrop by remember { mutableStateOf(false) }
    
    // Undo/Redo Stacks
    // We will save the state (Bitmap) in the stack
    data class EditState(val bitmap: Bitmap)
    var undoStack by remember { mutableStateOf(listOf<EditState>()) }
    var redoStack by remember { mutableStateOf(listOf<EditState>()) }
    
    fun saveState() {
        currentBitmap?.let { 
            undoStack = undoStack + EditState(it.copy(it.config ?: Bitmap.Config.ARGB_8888, true))
            redoStack = emptyList() // clear redo on new action
        }
    }
    
    fun undo() {
        if (undoStack.isNotEmpty()) {
            val lastState = undoStack.last()
            undoStack = undoStack.dropLast(1)
            currentBitmap?.let { redoStack = redoStack + EditState(it.copy(it.config ?: Bitmap.Config.ARGB_8888, true)) }
            currentBitmap = lastState.bitmap
        }
    }
    
    fun redo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.last()
            redoStack = redoStack.dropLast(1)
            currentBitmap?.let { undoStack = undoStack + EditState(it.copy(it.config ?: Bitmap.Config.ARGB_8888, true)) }
            currentBitmap = nextState.bitmap
        }
    }
    
    // Load bitmap when page changes
    LaunchedEffect(currentIndex, uris) {
        if (currentIndex in uris.indices) {
            isProcessing = true
            withContext(Dispatchers.IO) {
                val uri = uris[currentIndex]
                
                // Get edited vs original if any
                val prefs = context.getSharedPreferences("scanner_draft", Context.MODE_PRIVATE)
                val originalPath = prefs.getString("original_${uri.path}", null)
                val fileToLoad = if (originalPath != null) File(originalPath) else File(uri.path!!)
                
                if (fileToLoad.exists()) {
                    val opts = BitmapFactory.Options()
                    opts.inPreferredConfig = Bitmap.Config.RGB_565
                    originalBitmap = BitmapFactory.decodeFile(fileToLoad.absolutePath, opts)
                    currentBitmap = originalBitmap?.copy(Bitmap.Config.RGB_565, true)
                }
                
                undoStack = emptyList()
                redoStack = emptyList()
                tiltAngle = 0f
                rotationAngle = 0f
                points = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(0f, 1f), Offset(1f, 1f))
            }
            isProcessing = false
        }
    }
    
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Edit Page ${currentIndex + 1} of ${uris.size}") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cancel") }
                },
                actions = {
                    IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                    }
                    IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
                    }
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp))
                    } else {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                isProcessing = true
                                val newUri = saveEditedBitmap(context, currentBitmap)
                                isProcessing = false
                                if (newUri != null) {
                                    uris[currentIndex] = newUri
                                    if (currentIndex < uris.size - 1) {
                                        currentIndex++
                                    } else {
                                        onSaveAll(uris)
                                    }
                                }
                            }
                        }) {
                            Icon(if (currentIndex < uris.size - 1) Icons.Default.ArrowForward else Icons.Default.Check, "Next/Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha=0.6f), titleContentColor = Color.White, actionIconContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray.copy(alpha=0.6f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(16.dp)
            ) {
                // Filter selection
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("Original", "Magic Enhance", "Grayscale", "B&W Document")
                    items(filters.size) { index ->
                        val filterName = filters[index]
                        FilterChip(
                            selected = false,
                            onClick = { 
                                saveState()
                                if (filterName == "Original") {
                                    originalBitmap?.let { currentBitmap = it.copy(Bitmap.Config.RGB_565, true) }
                                    points = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(0f, 1f), Offset(1f, 1f))
                                } else {
                                    currentBitmap?.let { bmp ->
                                        currentBitmap = FilterEngine.applyFilter(bmp, filterName)
                                    }
                                }
                            },
                            label = { Text(filterName, color = Color.White) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = Color.Black.copy(alpha=0.4f))
                        )
                    }
                }

                // Tilting slider
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("-15°", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = tiltAngle,
                        onValueChange = { tiltAngle = it },
                        onValueChangeFinished = {
                            if (tiltAngle != 0f) {
                                saveState()
                                currentBitmap?.let { bmp ->
                                    val matrix = Matrix()
                                    matrix.postRotate(tiltAngle)
                                    currentBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                }
                                tiltAngle = 0f
                            }
                        },
                        valueRange = -15f..15f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("+15°", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        saveState()
                        currentBitmap?.let { bmp ->
                            val matrix = Matrix()
                            matrix.postRotate(-90f)
                            currentBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                        }
                    }) {
                        Icon(Icons.Default.RotateLeft, "Rotate Left", tint = Color.White)
                    }
                    
                    IconButton(onClick = { isRectCrop = !isRectCrop }) {
                        Icon(if (isRectCrop) Icons.Default.CropPortrait else Icons.Default.Transform, "Toggle Crop Mode", tint = Color.White)
                    }
                    
                    IconButton(onClick = { 
                        saveState()
                        currentBitmap?.let { bmp ->
                            currentBitmap = if (isRectCrop) {
                                // For rectangular crop, just use points 0 (TL) and 3 (BR)
                                val x0 = (points[0].x * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                                val y0 = (points[0].y * bmp.height).toInt().coerceIn(0, bmp.height - 1)
                                val x1 = (points[3].x * bmp.width).toInt().coerceIn(0, bmp.width)
                                val y1 = (points[3].y * bmp.height).toInt().coerceIn(0, bmp.height)
                                val w = (x1 - x0).coerceAtLeast(1)
                                val h = (y1 - y0).coerceAtLeast(1)
                                Bitmap.createBitmap(bmp, x0, y0, minOf(w, bmp.width - x0), minOf(h, bmp.height - y0))
                            } else {
                                applyPerspectiveCropInternal(bmp, points)
                            }
                            points = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(0f, 1f), Offset(1f, 1f))
                        }
                    }) {
                        Icon(Icons.Default.Crop, "Apply Crop", tint = Color.White)
                    }
                    
                    IconButton(onClick = { 
                        saveState()
                        currentBitmap?.let { bmp ->
                            val matrix = Matrix()
                            matrix.postRotate(90f)
                            currentBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                        }
                    }) {
                        Icon(Icons.Default.RotateRight, "Rotate Right", tint = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            var boxSize by remember { mutableStateOf(IntSize.Zero) }
            
            fun getScaledBitmapDimensions(bmp: Bitmap, w: Int, h: Int): Pair<Float, Float> {
                val scale = kotlin.math.min(w.toFloat() / bmp.width, h.toFloat() / bmp.height)
                return Pair(bmp.width * scale, bmp.height * scale)
            }
            
            if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap!!.asImageBitmap(),
                    contentDescription = "Editing Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(tiltAngle)
                        .onSizeChanged { boxSize = it }
                )
                
                // Crop overlay
                if (boxSize != IntSize.Zero && currentBitmap != null) {
                    val (imgW, imgH) = getScaledBitmapDimensions(currentBitmap!!, boxSize.width, boxSize.height)
                    val offsetX = (boxSize.width - imgW) / 2f
                    val offsetY = (boxSize.height - imgH) / 2f
                    
                    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val pos = change.position
                            // find nearest point
                            val px = (pos.x - offsetX) / imgW
                            val py = (pos.y - offsetY) / imgH
                            
                            var minDistance = Float.MAX_VALUE
                            var nearestIdx = -1
                            
                            points.forEachIndexed { idx, p ->
                                val dist = hypot(p.x - px, p.y - py)
                                if (dist < minDistance) {
                                    minDistance = dist
                                    nearestIdx = idx
                                }
                            }
                            
                            if (nearestIdx != -1) {
                                val nx = (points[nearestIdx].x + dragAmount.x / imgW).coerceIn(0f, 1f)
                                val ny = (points[nearestIdx].y + dragAmount.y / imgH).coerceIn(0f, 1f)
                                val newPoints = points.toMutableList()
                                newPoints[nearestIdx] = Offset(nx, ny)
                                if (isRectCrop) {
                                    when (nearestIdx) {
                                        0 -> { newPoints[1] = Offset(newPoints[1].x, ny); newPoints[2] = Offset(nx, newPoints[2].y) }
                                        1 -> { newPoints[0] = Offset(newPoints[0].x, ny); newPoints[3] = Offset(nx, newPoints[3].y) }
                                        2 -> { newPoints[3] = Offset(newPoints[3].x, ny); newPoints[0] = Offset(nx, newPoints[0].y) }
                                        3 -> { newPoints[2] = Offset(newPoints[2].x, ny); newPoints[1] = Offset(nx, newPoints[1].y) }
                                    }
                                }
                                points = newPoints
                            }
                        }
                    }) {
                        val screenPoints = points.map { 
                            Offset(offsetX + it.x * imgW, offsetY + it.y * imgH)
                        }
                        
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(screenPoints[0].x, screenPoints[0].y)
                            lineTo(screenPoints[1].x, screenPoints[1].y)
                            lineTo(screenPoints[3].x, screenPoints[3].y)
                            lineTo(screenPoints[2].x, screenPoints[2].y)
                            close()
                        }
                        
                        drawPath(
                            path = path,
                            color = Color.Cyan.copy(alpha = 0.3f),
                            style = androidx.compose.ui.graphics.drawscope.Fill
                        )
                        drawPath(
                            path = path,
                            color = Color.Cyan,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                        )
                        
                        screenPoints.forEach { pt ->
                            drawCircle(color = Color.White, radius = 24f, center = pt)
                            drawCircle(color = Color.Cyan, radius = 20f, center = pt)
                        }
                    }
                }
            }
            
            // Left / Right navigation overlays
            if (currentIndex > 0) {
                IconButton(
                    onClick = { currentIndex-- },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous", tint = Color.White)
                }
            }
            
            if (currentIndex < uris.size - 1) {
                IconButton(
                    onClick = { currentIndex++ },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", tint = Color.White)
                }
            }
        }
    }
}

private fun applyPerspectiveCropInternal(bitmap: Bitmap, points: List<Offset>): Bitmap {
    val src = FloatArray(8)
    src[0] = points[0].x * bitmap.width
    src[1] = points[0].y * bitmap.height
    src[2] = points[1].x * bitmap.width
    src[3] = points[1].y * bitmap.height
    src[4] = points[3].x * bitmap.width
    src[5] = points[3].y * bitmap.height
    src[6] = points[2].x * bitmap.width
    src[7] = points[2].y * bitmap.height

    val width1 = hypot(src[2] - src[0], src[3] - src[1])
    val width2 = hypot(src[4] - src[6], src[5] - src[7])
    val destWidth = max(width1, width2).toInt().coerceAtLeast(1)

    val height1 = hypot(src[6] - src[0], src[7] - src[1])
    val height2 = hypot(src[4] - src[2], src[5] - src[3])
    val destHeight = max(height1, height2).toInt().coerceAtLeast(1)

    val dest = FloatArray(8)
    dest[0] = 0f
    dest[1] = 0f
    dest[2] = destWidth.toFloat()
    dest[3] = 0f
    dest[4] = destWidth.toFloat()
    dest[5] = destHeight.toFloat()
    dest[6] = 0f
    dest[7] = destHeight.toFloat()

    val matrix = Matrix()
    matrix.setPolyToPoly(src, 0, dest, 0, 4)

    val destBmp = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(destBmp)
    val paint = android.graphics.Paint()
    paint.isAntiAlias = true
    canvas.drawBitmap(bitmap, matrix, paint)
    return destBmp
}

private suspend fun saveEditedBitmap(context: android.content.Context, bitmap: Bitmap?): Uri? = withContext(Dispatchers.IO) {
    if (bitmap == null) return@withContext null
    try {
        val file = File(context.cacheDir, "edited_scan_${System.currentTimeMillis()}.jpg")
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
        fos.flush()
        fos.close()
        return@withContext Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}
