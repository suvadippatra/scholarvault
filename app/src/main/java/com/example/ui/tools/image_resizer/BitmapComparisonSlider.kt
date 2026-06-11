package com.scholarvault.ui.tools.image_resizer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun BitmapComparisonSlider(
    leftBitmap: Bitmap,
    rightBitmap: Bitmap
) {
    var slidePos by remember { mutableStateOf(0.5f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val density = LocalDensity.current
        val totalWidthPx = constraints.maxWidth.toFloat()
        val totalHeightPx = constraints.maxHeight.toFloat()

        if (totalWidthPx > 0 && totalHeightPx > 0) {
            val totalWidthDp = with(density) { totalWidthPx.toDp() }
            val totalHeightDp = with(density) { totalHeightPx.toDp() }

            // Aspect ratio of original (left) bitmap
            val leftWidth = leftBitmap.width.toFloat()
            val leftHeight = leftBitmap.height.toFloat()
            val imageAspectRatio = if (leftHeight > 0f) leftWidth / leftHeight else 1f
            val containerAspectRatio = totalWidthPx / totalHeightPx

            val (fitWidthDp, fitHeightDp) = if (containerAspectRatio > imageAspectRatio) {
                Pair(totalHeightDp * imageAspectRatio, totalHeightDp)
            } else {
                Pair(totalWidthDp, totalWidthDp / imageAspectRatio)
            }

            Box(
                modifier = Modifier
                    .size(fitWidthDp, fitHeightDp)
                    .align(Alignment.Center)
                    .clipToBounds()
                    .pointerInput(fitWidthDp) {
                        detectDragGestures { change, dragAmount ->
                            val widthPx = with(density) { fitWidthDp.toPx() }
                            if (widthPx > 0) {
                                slidePos = (slidePos + dragAmount.x / widthPx).coerceIn(0f, 1f)
                                change.consume()
                            }
                        }
                    }
                    .pointerInput(fitWidthDp) {
                        detectTapGestures { offset ->
                            val widthPx = with(density) { fitWidthDp.toPx() }
                            if (widthPx > 0) {
                                slidePos = (offset.x / widthPx).coerceIn(0f, 1f)
                            }
                        }
                    }
            ) {
                val fitWidthPx = with(density) { fitWidthDp.toPx() }

                // Right image (Bottom layer)
                Image(
                    bitmap = rightBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                // Left image (Top layer) clipped by slider position
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(slidePos)
                        .clipToBounds()
                ) {
                    Image(
                        bitmap = leftBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(fitWidthDp),
                        contentScale = ContentScale.FillBounds
                    )
                }

                // Slider Handle
                val handleX = fitWidthPx * slidePos
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .offset { IntOffset(handleX.roundToInt() - 2.dp.toPx().toInt(), 0) }
                        .background(Color.White)
                )
                
                // Handle Knob
                Box(
                    modifier = Modifier
                        .offset { IntOffset(handleX.roundToInt() - 16.dp.toPx().toInt(), 0) }
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(Color.White, shape = androidx.compose.foundation.shape.CircleShape)
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(modifier = Modifier.size(4.dp, 12.dp).background(Color.Gray))
                        Spacer(Modifier.width(4.dp))
                        Box(modifier = Modifier.size(4.dp, 12.dp).background(Color.Gray))
                    }
                }
            }
        }
    }
}
