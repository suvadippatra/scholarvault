package com.scholarvault.ui.tools.image_resizer

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AdcancedCropDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit
) {
    var cropRect by remember { mutableStateOf(Rect.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        var startOffset by remember { mutableStateOf(Offset.Zero) }
        var endOffset by remember { mutableStateOf(Offset.Zero) }
        
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)) {
            Text(
                "Crop pic so face covers 80% & background is uniform colour.",
                color = Color.White, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                "Avoid spikes in hair or cloth.",
                color = Color.White, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
                .onGloballyPositioned { containerSize = Size(it.size.width.toFloat(), it.size.height.toFloat()) }
                .pointerInput(containerSize) {
                    detectDragGestures(
                        onDragStart = { 
                            startOffset = it
                            endOffset = it 
                            cropRect = Rect(it.x, it.y, it.x, it.y)
                        },
                        onDrag = { change, _ -> 
                            endOffset = change.position
                            val left = minOf(startOffset.x, endOffset.x).coerceIn(0f, containerSize.width)
                            val top = minOf(startOffset.y, endOffset.y).coerceIn(0f, containerSize.height)
                            val right = maxOf(startOffset.x, endOffset.x).coerceIn(0f, containerSize.width)
                            val bottom = maxOf(startOffset.y, endOffset.y).coerceIn(0f, containerSize.height)
                            cropRect = Rect(left, top, right, bottom)
                            change.consume()
                        },
                        onDragEnd = {
                            val left = minOf(startOffset.x, endOffset.x).coerceIn(0f, containerSize.width)
                            val top = minOf(startOffset.y, endOffset.y).coerceIn(0f, containerSize.height)
                            val right = maxOf(startOffset.x, endOffset.x).coerceIn(0f, containerSize.width)
                            val bottom = maxOf(startOffset.y, endOffset.y).coerceIn(0f, containerSize.height)
                            cropRect = Rect(left, top, right, bottom)
                        }
                    )
                },
            contentScale = ContentScale.Fit
        )
        
        val displayRatio = if (bitmap.width > 0 && bitmap.height > 0 && containerSize.width > 0 && containerSize.height > 0) {
            minOf(containerSize.width / bitmap.width, containerSize.height / bitmap.height)
        } else 1f
        val displWidth = bitmap.width * displayRatio
        val displHeight = bitmap.height * displayRatio
        val padX = (containerSize.width - displWidth) / 2f
        val padY = (containerSize.height - displHeight) / 2f
            
        var actW = 0
        var actH = 0
        if (cropRect.width > 0 && cropRect.height > 0) {
            val bmpLeft = ((cropRect.left - padX) / displayRatio).toInt().coerceIn(0, bitmap.width)
            val bmpTop = ((cropRect.top - padY) / displayRatio).toInt().coerceIn(0, bitmap.height)
            val bmpRight = ((cropRect.right - padX) / displayRatio).toInt().coerceIn(0, bitmap.width)
            val bmpBottom = ((cropRect.bottom - padY) / displayRatio).toInt().coerceIn(0, bitmap.height)
            actW = maxOf(0, bmpRight - bmpLeft)
            actH = maxOf(0, bmpBottom - bmpTop)
            
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw dimmed overlay outside crop rect
                // Top
                drawRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, cropRect.top)
                )
                // Bottom
                drawRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    topLeft = Offset(0f, cropRect.bottom),
                    size = Size(w, h - cropRect.bottom)
                )
                // Left
                drawRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    topLeft = Offset(0f, cropRect.top),
                    size = Size(cropRect.left, cropRect.height)
                )
                // Right
                drawRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    topLeft = Offset(cropRect.right, cropRect.top),
                    size = Size(w - cropRect.right, cropRect.height)
                )

                // Draw bounding box
                drawRect(
                    color = Color.White,
                    topLeft = Offset(cropRect.left, cropRect.top),
                    size = Size(cropRect.width, cropRect.height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )

                // Rule-of-Thirds inside grid lines
                if (cropRect.width > 30f && cropRect.height > 30f) {
                    val dx = cropRect.width / 3f
                    val dy = cropRect.height / 3f
                    val stroke = 1.dp.toPx()
                    val gridColor = Color.White.copy(alpha = 0.5f)

                    // Verticals
                    drawLine(gridColor, Offset(cropRect.left + dx, cropRect.top), Offset(cropRect.left + dx, cropRect.bottom), stroke)
                    drawLine(gridColor, Offset(cropRect.left + dx * 2, cropRect.top), Offset(cropRect.left + dx * 2, cropRect.bottom), stroke)
                    // Horizontals
                    drawLine(gridColor, Offset(cropRect.left, cropRect.top + dy), Offset(cropRect.right, cropRect.top + dy), stroke)
                    drawLine(gridColor, Offset(cropRect.left, cropRect.top + dy * 2), Offset(cropRect.right, cropRect.top + dy * 2), stroke)
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.BottomCenter).padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cancel", tint = Color.White) }
            Text("${actW} x ${actH} px", color = Color.White, modifier = Modifier.align(Alignment.CenterVertically))
            IconButton(onClick = {
                if (actW > 0 && actH > 0) {
                    val bmpLeft = ((cropRect.left - padX) / displayRatio).toInt().coerceIn(0, bitmap.width)
                    val bmpTop = ((cropRect.top - padY) / displayRatio).toInt().coerceIn(0, bitmap.height)
                    val newBmp = Bitmap.createBitmap(bitmap, bmpLeft, bmpTop, actW, actH)
                    onCropped(newBmp)
                }
            }) { Icon(Icons.Default.Check, "Done", tint = Color.White) }
        }
    }
}
