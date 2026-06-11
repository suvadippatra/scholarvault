package com.scholarvault.ui.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ImageComparisonSlider(
    beforeImageUri: Any?,
    afterImageUri: Any?,
    beforeLabel: String = "Original",
    afterLabel: String = "Resized",
    modifier: Modifier = Modifier
) {
    var fraction by remember { mutableStateOf(0.5f) }
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.1f))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    fraction = (fraction + dragAmount.x / size.width).coerceIn(0f, 1f)
                }
            }
    ) {
        val width = maxWidth
        val height = maxHeight
        
        // After Image (Base Layer - visible when fraction is dragged away)
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = afterImageUri,
                contentDescription = afterLabel,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            // Label for after
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                Text(afterLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        // Before Image (Top Layer - cropped by fraction)
        Box(
            modifier = Modifier
                .width(width * fraction)
                .fillMaxHeight()
                .clip(androidx.compose.ui.graphics.RectangleShape)
        ) {
            AsyncImage(
                model = beforeImageUri,
                contentDescription = beforeLabel,
                contentScale = ContentScale.Fit, // Assuming both are the same ratio and size roughly inside the box
                modifier = Modifier.width(width).fillMaxHeight() // Must be full width to align identically
            )
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                Text(beforeLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        // Slider Handle
        Box(
            modifier = Modifier
                .offset(x = width * fraction - 2.dp)
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
