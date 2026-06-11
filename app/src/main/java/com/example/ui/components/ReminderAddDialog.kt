package com.scholarvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.ui.theme.LocalThemeController

@Composable
fun ReminderAddDialog(
    onDismissRequest: () -> Unit,
    onAdd: (String, String, Long) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var dateTimeStr by remember { mutableStateOf("") }
    var selectedTimeInMillis by remember { mutableStateOf<Long?>(null) }
    
    val calendar = remember { java.util.Calendar.getInstance() }
    val isDark = LocalThemeController.current.isDarkTheme
    val textColor = if (isDark) Color.White else Color.Black
    val dialogBg = if (isDark) Color(0xFF1E1E1E) else Color.White

    val timePickerDialog = remember {
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(java.util.Calendar.MINUTE, minute)
                calendar.set(java.util.Calendar.SECOND, 0)
                selectedTimeInMillis = calendar.timeInMillis
                
                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                dateTimeStr = sdf.format(calendar.time)
            },
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            false
        )
    }

    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(java.util.Calendar.YEAR, year)
                calendar.set(java.util.Calendar.MONTH, month)
                calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                timePickerDialog.show()
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier,
        containerColor = dialogBg,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                "Add Reminder",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Modern Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What to remind?", color = textColor.copy(alpha = 0.5f)) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = textColor.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )

                // Date & Time Picker field
                OutlinedTextField(
                    value = dateTimeStr,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Set Date & Time", color = textColor.copy(alpha = 0.5f)) },
                    label = { Text("Reminder Time") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Pick Date/Time",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        disabledTextColor = textColor,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                        disabledBorderColor = textColor.copy(alpha = 0.2f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = textColor.copy(alpha = 0.5f),
                        disabledLabelColor = textColor.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedTimeInMillis != null) {
                        onAdd(title, dateTimeStr, selectedTimeInMillis!!)
                    }
                },
                enabled = title.isNotBlank() && selectedTimeInMillis != null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
            ) {
                Text("Save Reminder")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Cancel", color = textColor.copy(alpha = 0.7f))
            }
        }
    )
}
