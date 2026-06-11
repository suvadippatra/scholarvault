package com.scholarvault.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun PatternLock(
    modifier: Modifier = Modifier,
    onPatternComplete: (String) -> Unit
) {
    var selectedDots by remember { mutableStateOf(listOf<Int>()) }
    var currentDragPosition by remember { mutableStateOf<Offset?>(null) }
    
    val dotColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    
    val dotCount = 3
    val containerSize = 300.dp 
    
    Box(
        modifier = modifier
            .size(containerSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selectedDots = emptyList()
                        currentDragPosition = offset
                    },
                    onDragEnd = {
                        onPatternComplete(selectedDots.joinToString(""))
                        currentDragPosition = null
                        // Clear dots on end to allow retry, visual feedback is brief
                        selectedDots = emptyList()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentDragPosition = change.position
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val dotRadius = 12.dp.toPx()
            
            val cellWidth = canvasWidth / dotCount
            val cellHeight = canvasHeight / dotCount
            
            // Draw lines 
            if (selectedDots.isNotEmpty()) {
                val pathPoints = selectedDots.map { dotIndex ->
                    val row = dotIndex / dotCount
                    val col = dotIndex % dotCount
                    val x = col * cellWidth + cellWidth / 2
                    val y = row * cellHeight + cellHeight / 2
                    Offset(x, y)
                }
                
                for (i in 0 until pathPoints.size - 1) {
                    drawLine(
                        color = lineColor,
                        start = pathPoints[i],
                        end = pathPoints[i + 1],
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                
                currentDragPosition?.let { currentPos ->
                    drawLine(
                        color = lineColor,
                        start = pathPoints.last(),
                        end = currentPos,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            
            // Draw dots & Check intersections
            for (row in 0 until dotCount) {
                for (col in 0 until dotCount) {
                    val dotIndex = row * dotCount + col
                    val x = col * cellWidth + cellWidth / 2
                    val y = row * cellHeight + cellHeight / 2
                    val center = Offset(x, y)
                    
                    val isSelected = selectedDots.contains(dotIndex)
                    
                    // Interaction check
                    currentDragPosition?.let { dragPos ->
                        val distance = (dragPos - center).getDistance()
                        if (distance < dotRadius * 3 && !isSelected) {
                            selectedDots = selectedDots + dotIndex
                        }
                    }
                    
                    // Draw outer ring if selected
                    if (isSelected) {
                        drawCircle(
                            color = lineColor,
                            radius = dotRadius * 2,
                            center = center
                        )
                    }
                    
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = center
                    )
                }
            }
        }
    }
}
