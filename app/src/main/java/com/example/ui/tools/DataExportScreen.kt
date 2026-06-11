package com.scholarvault.ui.tools

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.theme.LocalThemeController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataExportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalThemeController.current.isDarkTheme

    // Checked parameters to export
    var exportAcademics by remember { mutableStateOf(true) }
    var exportTasks by remember { mutableStateOf(true) }
    var exportReminders by remember { mutableStateOf(true) }
    var exportWallet by remember { mutableStateOf(true) }
    var exportFormat by remember { mutableStateOf("JSON") } // "JSON" or "CSV"

    // Stats variables
    var courseCount by remember { mutableStateOf(0) }
    var taskCount by remember { mutableStateOf(0) }
    var reminderCount by remember { mutableStateOf(0) }
    var walletCount by remember { mutableStateOf(0) }

    var isExporting by remember { mutableStateOf(false) }
    var generatedExportFile by remember { mutableStateOf<File?>(null) }

    val db = remember { (context.applicationContext as com.scholarvault.MainApplication).database }

    // Fetch stats on launch
    LaunchedEffect(key1 = true) {
        scope.launch(Dispatchers.IO) {
            val courses = db.academicItemDao().getAllAcademicItems().firstOrNull() ?: emptyList()
            courseCount = courses.size

            val tasks = db.taskDao().getAllTasks().firstOrNull() ?: emptyList()
            taskCount = tasks.size

            val reminders = db.reminderDao().getAllReminders().firstOrNull() ?: emptyList()
            reminderCount = reminders.size

            val cards = db.walletDao().getAllCards().firstOrNull() ?: emptyList()
            walletCount = cards.size
        }
    }

    Scaffold(
        topBar = {
            TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Data Export",
                showProfileIcon = false,
                showSearchBar = false
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ImportExport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            "Export Academic Data",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Compile grades, notes, chores, and schedules into structured formats.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Database Statistics Dashboard
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Vault Statistics Database",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatItem(Modifier.weight(1f), "Courses", courseCount.toString())
                        StatItem(Modifier.weight(1f), "Tasks", taskCount.toString())
                        StatItem(Modifier.weight(1f), "Reminders", reminderCount.toString())
                        StatItem(Modifier.weight(1f), "Wallet", walletCount.toString())
                    }
                }
            }

            // Export Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Select Datasets to Include", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    ExportSelectorItem(
                        title = "Academic Track (GPA & Grades)",
                        checked = exportAcademics,
                        onCheckedChange = { exportAcademics = it }
                    )

                    ExportSelectorItem(
                        title = "Tasks Queue",
                        checked = exportTasks,
                        onCheckedChange = { exportTasks = it }
                    )

                    ExportSelectorItem(
                        title = "Deadlines & Reminders",
                        checked = exportReminders,
                        onCheckedChange = { exportReminders = it }
                    )

                    ExportSelectorItem(
                        title = "Wallet Documents Metadata",
                        checked = exportWallet,
                        onCheckedChange = { exportWallet = it }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("File Export Format", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = exportFormat == "JSON",
                            onClick = { exportFormat = "JSON" },
                            label = { Text("Structured JSON") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = exportFormat == "CSV",
                            onClick = {
                                exportFormat = "CSV"
                                // CSV can only contain structured rows like Academics
                                exportAcademics = true
                                exportTasks = false
                                exportReminders = false
                                exportWallet = false
                            },
                            label = { Text("Sheets CSV (Academics Only)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (exportFormat == "CSV") {
                        Text(
                            "* CSV format generates coarse tabular rows suitable for Excel / Google Sheets and only parses Cumulative Academic Grades.",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            isExporting = true
                            generatedExportFile = null
                            scope.launch {
                                compileDataExport(
                                    context = context,
                                    db = db,
                                    exportAcademics = exportAcademics,
                                    exportTasks = exportTasks,
                                    exportReminders = exportReminders,
                                    exportWallet = exportWallet,
                                    format = exportFormat
                                ) { file ->
                                    generatedExportFile = file
                                    isExporting = false
                                    Toast.makeText(context, "Export compiled successfully!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isExporting && (exportAcademics || exportTasks || exportReminders || exportWallet)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Text("Compiling Export Documents...")
                        } else {
                            Icon(Icons.Default.ImportExport, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compile Export File")
                        }
                    }
                }
            }

            if (generatedExportFile != null) {
                // Success block
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Export Compiled Successfully!",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "File: " + generatedExportFile!!.name + "\nSize: " + formatBytes(generatedExportFile!!.length()),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    saveExportToPublicFolder(context, generatedExportFile!!, exportFormat)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save to Downloads")
                            }

                            Button(
                                onClick = {
                                    shareExportFile(context, generatedExportFile!!, exportFormat)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share File")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(modifier: Modifier, title: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExportSelectorItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(title, fontSize = 14.sp)
    }
}

private suspend fun compileDataExport(
    context: Context,
    db: com.scholarvault.data.AppDatabase,
    exportAcademics: Boolean,
    exportTasks: Boolean,
    exportReminders: Boolean,
    exportWallet: Boolean,
    format: String,
    onComplete: (File) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val extension = if (format == "JSON") "json" else "csv"
        val exportFile = File(context.cacheDir, "ScholarVault_Data_Export_${System.currentTimeMillis()}.$extension")

        if (format == "JSON") {
            val root = JSONObject()

            if (exportAcademics) {
                val coursesArray = JSONArray()
                val courses = db.academicItemDao().getAllAcademicItems().firstOrNull() ?: emptyList()
                courses.forEach { course ->
                    val cObj = JSONObject()
                    cObj.put("course_id", course.id)
                    cObj.put("course_name", course.title)
                    cObj.put("board_roll_number", course.boardRollNumber)
                    cObj.put("passing_status", course.passingStatus)

                    // Fetch associated semesters
                    val semesters = db.semesterDao().getSemestersForCourse(course.id).firstOrNull() ?: emptyList()
                    val semArray = JSONArray()
                    semesters.forEach { sem ->
                        val sObj = JSONObject()
                        sObj.put("semester_number", sem.semesterNumber)
                        sObj.put("semester_label", sem.semesterLabel)
                        sObj.put("earned_sgpa", sem.earnedSgpa ?: 0.0)
                        sObj.put("subjects_json", sem.subjectsJson)
                        semArray.put(sObj)
                    }
                    cObj.put("semesters", semArray)
                    coursesArray.put(cObj)
                }
                root.put("academics", coursesArray)
            }

            if (exportTasks) {
                val tasksArray = JSONArray()
                val tasks = db.taskDao().getAllTasks().firstOrNull() ?: emptyList()
                tasks.forEach { task ->
                    val tObj = JSONObject()
                    tObj.put("title", task.title)
                    tObj.put("completed", task.completed)
                    tasksArray.put(tObj)
                }
                root.put("tasks", tasksArray)
            }

            if (exportReminders) {
                val remindersArray = JSONArray()
                val reminders = db.reminderDao().getAllReminders().firstOrNull() ?: emptyList()
                reminders.forEach { reminder ->
                    val rObj = JSONObject()
                    rObj.put("title", reminder.title)
                    rObj.put("notes", reminder.dateTimeStr)
                    rObj.put("triggerTime", reminder.timeInMillis)
                    remindersArray.put(rObj)
                }
                root.put("reminders", remindersArray)
            }

            if (exportWallet) {
                val walletArray = JSONArray()
                val cards = db.walletDao().getAllCards().firstOrNull() ?: emptyList()
                cards.forEach { card ->
                    val wObj = JSONObject()
                    wObj.put("title", card.title)
                    wObj.put("notes", card.notes)
                    wObj.put("createdAt", card.createdAt.time)
                    walletArray.put(wObj)
                }
                root.put("wallet_metadata", walletArray)
            }

            FileOutputStream(exportFile).use { fos ->
                fos.write(root.toString(4).toByteArray())
                fos.flush()
            }
        } else {
            // CSV generation (Academics only)
            val builder = StringBuilder()
            builder.append("CourseName,SemesterNumber,SemesterLabel,EarnedSgpa,SubjectCode,SubjectName,MaxMarks,ObtainedMarks,Grade,CreditPoints\n")

            val courses = db.academicItemDao().getAllAcademicItems().firstOrNull() ?: emptyList()
            courses.forEach { course ->
                val semesters = db.semesterDao().getSemestersForCourse(course.id).firstOrNull() ?: emptyList()
                semesters.forEach { sem ->
                    val subjectEntries = com.scholarvault.data.model.SubjectSerializer.deserialize(sem.subjectsJson)
                    if (subjectEntries.isEmpty()) {
                        builder.append("${escapeCsv(course.title)},${sem.semesterNumber},${escapeCsv(sem.semesterLabel)},${sem.earnedSgpa ?: ""},,,,,,\n")
                    } else {
                        subjectEntries.forEach { sub ->
                            builder.append("${escapeCsv(course.title)},${sem.semesterNumber},${escapeCsv(sem.semesterLabel)},${sem.earnedSgpa ?: ""},")
                            builder.append("${escapeCsv(sub.subjectCode)},${escapeCsv(sub.subjectName)},${escapeCsv(sub.maxMarks)},${escapeCsv(sub.obtainedMarks)},${escapeCsv(sub.grade)},${escapeCsv(sub.creditPoints)}\n")
                        }
                    }
                }
            }

            FileOutputStream(exportFile).use { fos ->
                fos.write(builder.toString().toByteArray())
                fos.flush()
            }
        }

        onComplete(exportFile)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun escapeCsv(value: String?): String {
    if (value == null) return ""
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
    return value
}

private fun saveExportToPublicFolder(context: Context, file: File, format: String) {
    try {
        val root = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val ext = if (format == "JSON") "json" else "csv"
        val destFile = File(root, "ScholarVault_Export_${System.currentTimeMillis()}.$ext")

        file.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        Toast.makeText(context, "Saved to Downloads folder successfully!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving to Downloads", Toast.LENGTH_SHORT).show()
    }
}

private fun shareExportFile(context: Context, file: File, format: String) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val ext = if (format == "JSON") "json" else "csv"
        val typeStr = if (format == "JSON") "application/json" else "text/csv"

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = typeStr
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share ScholarVault Data"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error sharing export file", Toast.LENGTH_SHORT).show()
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.toDouble())).toInt().coerceIn(0, units.size - 1)
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.toDouble(), digitGroups.toDouble()), units[digitGroups])
}
