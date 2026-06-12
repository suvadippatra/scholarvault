package com.scholarvault.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FullScreenClockScreen(onBack: () -> Unit) {
    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }

    val timeStr = timeFormatter.format(currentTime)
    val dateStr = dateFormatter.format(currentTime)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
        }

        if (isPortrait) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnalogClock(time = currentTime)
                Spacer(modifier = Modifier.height(48.dp))
                DigitalSegmentClock(timeStr = timeStr)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = dateStr,
                    color = Color.DarkGray,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnalogClock(time = currentTime)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DigitalSegmentClock(timeStr = timeStr)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = dateStr,
                        color = Color.DarkGray,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AnalogClock(time: Date) {
    val cal = Calendar.getInstance().apply { timeInMillis = time.time }
    val hour = cal.get(Calendar.HOUR)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)

    val hourAngle = (hour + minute / 60f) * 30f
    val minuteAngle = (minute + second / 60f) * 6f
    val secondAngle = second * 6f

    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Clock Face
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.width / 2
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            
            drawCircle(
                color = Color(0xFF1E1E1E),
                radius = radius,
                center = center
            )
            
            // Draw ticks
            for (i in 0..59) {
                val angleRad = Math.toRadians((i * 6 - 90).toDouble())
                val isHour = i % 5 == 0
                val tickLength = if (isHour) 16.dp.toPx() else 8.dp.toPx()
                val strokeWidth = if (isHour) 4.dp.toPx() else 2.dp.toPx()
                val color = if (isHour) Color.White else Color.Gray

                val start = androidx.compose.ui.geometry.Offset(
                    x = (center.x + (radius - tickLength) * Math.cos(angleRad)).toFloat(),
                    y = (center.y + (radius - tickLength) * Math.sin(angleRad)).toFloat()
                )
                val end = androidx.compose.ui.geometry.Offset(
                    x = (center.x + radius * Math.cos(angleRad)).toFloat(),
                    y = (center.y + radius * Math.sin(angleRad)).toFloat()
                )
                
                drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            
            // Draw Hands
            fun drawHand(angle: Float, lengthRatio: Float, color: Color, width: Float) {
                val angleRad = Math.toRadians((angle - 90).toDouble())
                val end = androidx.compose.ui.geometry.Offset(
                    x = (center.x + radius * lengthRatio * Math.cos(angleRad)).toFloat(),
                    y = (center.y + radius * lengthRatio * Math.sin(angleRad)).toFloat()
                )
                drawLine(
                    color = color,
                    start = center,
                    end = end,
                    strokeWidth = width,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            
            drawHand(hourAngle, 0.5f, Color.White, 8.dp.toPx())
            drawHand(minuteAngle, 0.7f, Color.LightGray, 6.dp.toPx())
            drawHand(secondAngle, 0.8f, Color.Red, 2.dp.toPx())
            
            // Center pin
            drawCircle(color = Color.Red, radius = 4.dp.toPx(), center = center)
        }
    }
}

@Composable
fun DigitalSegmentClock(timeStr: String) {
    // Monospaced bold text can act as a simple substitute for a 7 segment for now
    Text(
        text = timeStr,
        fontSize = 72.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFFE53935), // Digital Red
        letterSpacing = 4.sp
    )
}
