package com.scholarvault.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.ui.theme.LocalThemeController
import java.util.*
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    val theme = LocalThemeController.current
    val isDark = theme.isDarkTheme
    val bgColor = MaterialTheme.colorScheme.background
    val tc = MaterialTheme.colorScheme.onBackground

    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val formatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = tc,
                    navigationIconContentColor = tc
                )
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header (Month / Year and Arrows)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newMonth = currentMonth.clone() as Calendar
                    newMonth.add(Calendar.MONTH, -1)
                    currentMonth = newMonth
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                }

                Text(
                    text = formatter.format(currentMonth.time),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = tc
                )

                IconButton(onClick = {
                    val newMonth = currentMonth.clone() as Calendar
                    newMonth.add(Calendar.MONTH, 1)
                    currentMonth = newMonth
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Days of week row
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Calendar Grid
            val cal = currentMonth.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            // Calculate total weeks needed (usually 5 or 6)
            val totalCells = firstDayOfWeek + daysInMonth
            val totalRows = kotlin.math.ceil(totalCells / 7.0).toInt()
            
            val today = Calendar.getInstance()
            val isCurrentMonth = today.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) && 
                                 today.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)

            var dayCounter = 1
            for (row in 0 until totalRows) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..6) {
                        if (row == 0 && col < firstDayOfWeek) {
                            Box(modifier = Modifier.weight(1f))
                        } else if (dayCounter > daysInMonth) {
                            Box(modifier = Modifier.weight(1f))
                        } else {
                            val isToday = isCurrentMonth && dayCounter == today.get(Calendar.DAY_OF_MONTH)
                            val thisDay = dayCounter
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { /* Could open daily schedule later */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = thisDay.toString(),
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else tc
                                )
                            }
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}
