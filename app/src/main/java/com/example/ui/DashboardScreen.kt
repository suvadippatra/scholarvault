package com.scholarvault.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Badge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scholarvault.ui.theme.LocalThemeController
import com.scholarvault.ui.viewmodel.DashboardViewModel
import com.scholarvault.ui.viewmodel.Quote
import com.scholarvault.data.model.TaskEntity
import com.scholarvault.data.model.ReminderEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenDrawer: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
    profileViewModel: com.scholarvault.ui.viewmodel.ProfileViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
    
    var permissionGranted by remember { 
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    
    var exactAlarmGranted by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                alarmManager?.canScheduleExactAlarms() == true
            } else true
        )
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { granted -> permissionGranted = granted }
    )

    LaunchedEffect(Unit) {
        if (!permissionGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (!exactAlarmGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.data = android.net.Uri.parse("package:" + context.packageName)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val theme = LocalThemeController.current
    val isDark = theme.isDarkTheme

    val quote by viewModel.quote.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val academicCount by viewModel.academicCount.collectAsState()
    val walletCount by viewModel.walletDocCount.collectAsState()
    val nextReminder by viewModel.nextReminder.collectAsState()
    val recentDocuments by viewModel.recentDocuments.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState()
    
    val weeklyActivity by viewModel.weeklyStudyActivity.collectAsState()
    
    val selectedTaskIds by viewModel.selectedTaskIds.collectAsState()
    val selectedReminderIds by viewModel.selectedReminderIds.collectAsState()

    val profileState by profileViewModel.profileStream.collectAsState()
    val searchQuery by profileViewModel.searchQuery.collectAsState()
    val allScans by viewModel.allScans.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()

    val filteredScans = remember(searchQuery, allScans) {
        if (searchQuery.isBlank()) emptyList()
        else allScans.filter { it.name.contains(searchQuery, true) || it.tags.any { t -> t.contains(searchQuery, true) } }
    }

    val filteredNotes = remember(searchQuery, allNotes) {
        if (searchQuery.isBlank()) emptyList()
        else allNotes.filter { it.content.contains(searchQuery, true) || it.folder?.contains(searchQuery, true) == true || it.tags.any { t -> t.contains(searchQuery, true) } }
    }
    
    val firstNameRaw = profileState?.profile?.firstName?.takeIf { it.isNotBlank() } ?: "Student"
    val pureFirstName = firstNameRaw.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "Student"

    if (selectedTaskIds.isNotEmpty()) {
        androidx.activity.compose.BackHandler { viewModel.clearTaskSelection() }
    }
    if (selectedReminderIds.isNotEmpty()) {
        androidx.activity.compose.BackHandler { viewModel.clearReminderSelection() }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 840

    var isSimulatedLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600)
        isSimulatedLoading = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = onOpenDrawer,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (searchQuery.isNotBlank()) {
                item {
                    if (filteredScans.isEmpty() && filteredNotes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No results found for \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        GlobalSearchResultsWidget(filteredScans, filteredNotes, onNavigate)
                    }
                }
            } else {
                item {
                    GreetingSection(pureFirstName, nextReminder)
                }
                item {
                    QuoteCard(quote)
                }
                item {
                    AcademicSummaryWidget(academicCount, walletCount)
                }
                if (weeklyActivity.isNotEmpty()) {
                    item {
                        WeeklyActivityWidget(weeklyActivity)
                    }
                }
                if (isSimulatedLoading || recentDocuments.isNotEmpty() || recentNotes.isNotEmpty()) {
                    item {
                        RecentMaterialsWidget(recentDocuments, recentNotes, isSimulatedLoading, onNavigate)
                    }
                }
                if (isTablet) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TasksSection(
                                    tasks = tasks.take(10), 
                                    deletedTasks = emptyList(),
                                    showDeletedItems = false,
                                    onViewAllClick = { onNavigate("tasks") },
                                    selectedIds = selectedTaskIds,
                                    isLoading = isSimulatedLoading,
                                    onToggleTask = { viewModel.toggleTask(it) },
                                    onSelectTask = viewModel::toggleTaskSelection,
                                    onAddTask = viewModel::addTask,
                                    onDeleteSelected = viewModel::deleteSelectedTasks
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                RemindersSection(
                                    reminders = reminders.take(10), 
                                    deletedReminders = emptyList(),
                                    showDeletedItems = false,
                                    onViewAllClick = { onNavigate("reminders") },
                                    selectedIds = selectedReminderIds,
                                    isLoading = isSimulatedLoading,
                                    onSelectReminder = viewModel::toggleReminderSelection,
                                    onAddReminder = viewModel::addReminder,
                                    onDeleteSelected = viewModel::deleteSelectedReminders
                                )
                            }
                        }
                    }
                } else {
                    item {
                        TasksSection(
                            tasks = tasks.take(10), 
                            deletedTasks = emptyList(),
                            showDeletedItems = false,
                            onViewAllClick = { onNavigate("tasks") },
                            selectedIds = selectedTaskIds,
                            isLoading = isSimulatedLoading,
                            onToggleTask = { viewModel.toggleTask(it) },
                            onSelectTask = viewModel::toggleTaskSelection,
                            onAddTask = viewModel::addTask,
                            onDeleteSelected = viewModel::deleteSelectedTasks
                        )
                    }
                    item {
                        RemindersSection(
                            reminders = reminders.take(5), 
                            deletedReminders = emptyList(),
                            showDeletedItems = false,
                            onViewAllClick = { onNavigate("reminders") },
                            selectedIds = selectedReminderIds,
                            isLoading = isSimulatedLoading,
                            onSelectReminder = viewModel::toggleReminderSelection,
                            onAddReminder = viewModel::addReminder,
                            onDeleteSelected = viewModel::deleteSelectedReminders
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GreetingSection(firstName: String, nextReminder: ReminderEntity? = null) {
    val theme = LocalThemeController.current
    val isDark = theme.isDarkTheme
    val textColor = if (isDark) Color.White else Color.Black
    val clockColor = if (isDark) Color(0xFFFF8A65) else Color(0xFFFF7043)
    val dateColor = if (isDark) Color(0xFFFFCCBC) else Color(0xFFFFAB91)
    
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    
    var currentTime by remember { mutableStateOf(Date()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }
    
    val dateStr = dateFormatter.format(currentTime)
    val timeStr = timeFormatter.format(currentTime)
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 840

    if (isTablet) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                if (nextReminder != null) {
                    NextAlarmTicker(nextReminder)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = "Hi $firstName!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColor
                )
                Text(
                    text = "Good ${getGreetingTime()}",
                    fontSize = 30.sp,
                    fontFamily = FontFamily.Cursive,
                    color = if (isDark) Color(0xFFAAAAAA) else Color.DarkGray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeStr,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = clockColor
                )
                Text(
                    text = dateStr,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = dateColor
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (nextReminder != null) {
                NextAlarmTicker(nextReminder)
                Spacer(modifier = Modifier.height(6.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Hi $firstName!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Good ${getGreetingTime()}",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Cursive,
                        color = if (isDark) Color(0xFFAAAAAA) else Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = clockColor,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = dateColor,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun NextAlarmTicker(reminder: ReminderEntity) {
    val isDark = LocalThemeController.current.isDarkTheme
    val containerColor = if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Next: ${reminder.title} at ${reminder.dateTimeStr.split(" ").last()}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun QuickToolsSection(onNavigate: (String) -> Unit) {
    val isDark = LocalThemeController.current.isDarkTheme
    val tc = if (isDark) Color.White else Color.Black
    val bg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD0D9E0) // Prominent bg
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickToolButton(Icons.Default.CameraAlt, "Scanner", bg, tc, onClick = { })
        QuickToolButton(Icons.Default.Calculate, "Calculator", bg, tc, onClick = { onNavigate(Screen.CgpaCalculator.route) })
        QuickToolButton(Icons.AutoMirrored.Filled.NoteAdd, "Notes", bg, tc, onClick = { onNavigate(Screen.NotesList.route) })
        QuickToolButton(Icons.Default.Mic, "Recorder", bg, tc, onClick = { onNavigate("sound_recorder") })
    }
}

@Composable
fun QuickToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, bg: Color, tc: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(color = bg, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = tc)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = tc, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun QuoteCard(quote: Quote) {
    val isDark = LocalThemeController.current.isDarkTheme
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "“${quote.text}”",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    color = textColor,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "— ${quote.author}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksSection(
    tasks: List<TaskEntity>,
    deletedTasks: List<TaskEntity>,
    showDeletedItems: Boolean,
    onViewAllClick: (() -> Unit)? = null,
    hideAddButton: Boolean = false,
    isExpandedMode: Boolean = false,
    hideDeleteButton: Boolean = false,
    selectedIds: Set<String>,
    isLoading: Boolean = false,
    onToggleTask: (TaskEntity) -> Unit,
    onSelectTask: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onRestoreSelected: (() -> Unit)? = null,
    onCompletelyDeleteSelected: (() -> Unit)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val isDark = LocalThemeController.current.isDarkTheme
    val tc = if (isDark) Color.White else Color.Black
    val bg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F7)
    val borderColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
    
    val completedCount = tasks.count { it.completed }
    val progress = if (tasks.isEmpty()) 0f else completedCount.toFloat() / tasks.size

    val displayedTasks = if (showDeletedItems) deletedTasks else tasks

    if (showAddDialog) {
        var newTaskText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Task") },
            text = {
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    label = { Text("Task Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    onAddTask(newTaskText)
                    showAddDialog = false 
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showDeletedItems) "Tasks History" else "Tasks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = tc
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onViewAllClick != null) {
                        TextButton(onClick = onViewAllClick) {
                            Text(
                                text = "View All",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (showDeletedItems) {
                        if (selectedIds.isNotEmpty() && onRestoreSelected != null && onCompletelyDeleteSelected != null && !hideDeleteButton) {
                            IconButton(onClick = onRestoreSelected, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = tc)
                            }
                            IconButton(onClick = onCompletelyDeleteSelected, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever", tint = Color.Red)
                            }
                        }
                    } else {
                        if (selectedIds.isNotEmpty() && !hideDeleteButton) {
                            IconButton(onClick = { onDeleteSelected() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        } else if (!hideAddButton && !isExpandedMode) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = if (isDark) Color(0xFF333333) else Color(0xFFDCDCDC),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.clickable { showAddDialog = true }
                            ) {
                                Text("+Add", fontSize = 14.sp, color = tc, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
            }

            if (!showDeletedItems) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(50)),
                        color = tc,
                        trackColor = if (isDark) Color(0xFF444444) else Color(0xFFDCDCDC)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$completedCount/${tasks.size}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = tc
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                if (isLoading) {
                    repeat(3) {
                        com.scholarvault.ui.components.TaskSkeletonLine()
                    }
                } else if (displayedTasks.isEmpty() && !isExpandedMode) {
                    Text(if (showDeletedItems) "No dismissed tasks." else "No tasks added yet.", color = Color.Gray, fontSize = 14.sp)
                } else {
                    displayedTasks.forEach { task ->
                    val isSelected = selectedIds.contains(task.id)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.Gray.copy(alpha=0.3f) else Color.Transparent)
                            .combinedClickable(
                                onClick = {
                                    if (!showDeletedItems) {
                                        if (selectedIds.isNotEmpty()) onSelectTask(task.id)
                                        else onToggleTask(task)
                                    } else {
                                        onSelectTask(task.id)
                                    }
                                },
                                onLongClick = { onSelectTask(task.id) }
                            )
                            .padding(vertical = 0.dp, horizontal = 4.dp)
                    ) {
                        if (!showDeletedItems) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(if (task.completed) tc else Color.Transparent, RoundedCornerShape(2.dp))
                                    .border(if (task.completed) 0.dp else 1.5.dp, tc, RoundedCornerShape(2.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (task.completed) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isDark) Color.Black else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        val strikeThroughProgress by animateFloatAsState(
                            targetValue = if (task.completed) 1f else 0f,
                            animationSpec = tween(durationMillis = 300),
                            label = "StrikeThrough"
                        )

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = if (showDeletedItems) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            ),
                            color = tc.copy(alpha = if (task.completed) 0.5f else 0.87f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .drawWithContent {
                                    drawContent()
                                    if (strikeThroughProgress > 0f && !showDeletedItems) {
                                        val y = size.height / 2f
                                        drawLine(
                                            color = tc.copy(alpha = 0.5f),
                                            start = androidx.compose.ui.geometry.Offset(0f, y),
                                            end = androidx.compose.ui.geometry.Offset(size.width * strikeThroughProgress, y),
                                            strokeWidth = 1.5.dp.toPx()
                                        )
                                    }
                                }
                        )
                    }
                }
                
                if (isExpandedMode && !showDeletedItems) {
                    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                    var newTaskText by remember { mutableStateOf("") }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 0.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(1.5.dp, tc, RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = newTaskText,
                            onValueChange = { newTaskText = it },
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp).focusRequester(focusRequester),
                            textStyle = androidx.compose.ui.text.TextStyle(color = tc, fontSize = 16.sp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = {
                                    if (newTaskText.isNotBlank()) {
                                        onAddTask(newTaskText)
                                        newTaskText = ""
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (newTaskText.isEmpty()) {
                                    Text("List item", color = tc.copy(alpha = 0.5f), fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            } // close else
            } // close inner column
        } // close outer column
    } // close Surface
} // close fun TasksSection

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RemindersSection(
    reminders: List<ReminderEntity>,
    deletedReminders: List<ReminderEntity>,
    showDeletedItems: Boolean,
    onViewAllClick: (() -> Unit)? = null,
    hideAddButton: Boolean = false,
    hideDeleteButton: Boolean = false,
    selectedIds: Set<String>,
    isLoading: Boolean = false,
    onSelectReminder: (String) -> Unit,
    onAddReminder: (String, String, Long) -> Unit,
    onDeleteSelected: () -> Unit,
    onRestoreSelected: (() -> Unit)? = null,
    onCompletelyDeleteSelected: (() -> Unit)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val isDark = LocalThemeController.current.isDarkTheme
    val tc = if (isDark) Color.White else Color.Black
    val bg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F7)
    val borderColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
    val context = androidx.compose.ui.platform.LocalContext.current
    val displayedReminders = if (showDeletedItems) deletedReminders else reminders

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (showAddDialog) {
        com.scholarvault.ui.components.ReminderAddDialog(
            onDismissRequest = { showAddDialog = false },
            onAdd = { title, dateStr, timeMillis ->
                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    context.startActivity(intent)
                    android.widget.Toast.makeText(context, "Please grant exact alarm permissions and try again", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    onAddReminder(title, dateStr, timeMillis)
                    showAddDialog = false
                }
            }
        )
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showDeletedItems) "Reminders History" else "Reminders",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tc
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onViewAllClick != null) {
                    TextButton(onClick = onViewAllClick) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (showDeletedItems) {
                    if (selectedIds.isNotEmpty() && onRestoreSelected != null && onCompletelyDeleteSelected != null && !hideDeleteButton) {
                        IconButton(onClick = onRestoreSelected, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore", tint = tc)
                        }
                        IconButton(onClick = onCompletelyDeleteSelected, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever", tint = Color.Red)
                        }
                    }
                } else {
                    if (selectedIds.isNotEmpty() && !hideDeleteButton) {
                        IconButton(onClick = { onDeleteSelected() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    } else if (!hideAddButton) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = if (isDark) Color(0xFF333333) else Color(0xFFDCDCDC),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable { showAddDialog = true }
                        ) {
                            Text("+Add", fontSize = 14.sp, color = tc, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Surface(
                        color = bg,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                    ) {
                        com.scholarvault.ui.components.TaskSkeletonLine()
                    }
                }
            }
        } else if (displayedReminders.isEmpty()) {
            Surface(
                color = bg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (showDeletedItems) "No dismissed reminders." else "No reminders.",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                displayedReminders.forEach { reminder ->
                val isSelected = selectedIds.contains(reminder.id)
                Surface(
                    color = if (isSelected) Color.Gray.copy(alpha=0.3f) else bg,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .combinedClickable(
                            onClick = {
                                if (!showDeletedItems) {
                                    if (selectedIds.isNotEmpty()) onSelectReminder(reminder.id)
                                }
                            },
                            onLongClick = { if (!showDeletedItems) onSelectReminder(reminder.id) }
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!showDeletedItems) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = tc, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Column {
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (showDeletedItems) Color.Gray else tc,
                                textDecoration = if (showDeletedItems) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reminder.dateTimeStr,
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = if (showDeletedItems) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                        }
                    }
                } // close Surface
            } // close forEach
            } // close inner Column
        } // close else
    } // close outer Column
} // close fun

fun getGreetingTime(): String {
    val c = java.util.Calendar.getInstance()
    return when (c.get(java.util.Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Morning.."
        in 12..15 -> "Afternoon.."
        in 16..20 -> "Evening.."
        else -> "Night.."
    }
}

@Composable
fun AcademicSummaryWidget(academicCount: Int, walletCount: Int) {
    val isDark = LocalThemeController.current.isDarkTheme
    val bg = if (isDark) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) else Color(0xFFF0F4F8)
    val textColor = if (isDark) Color.White else Color.Black
    
    // Simple compliance score calculation: 
    // Each course contributes, each document contributes.
    // Let's say 5 courses and 5 docs = 100%. Max 10 items.
    val complianceScore = remember(academicCount, walletCount) {
        val total = academicCount + walletCount
        (total / 10f).coerceIn(0f, 1f)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$academicCount", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Black, 
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Courses", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = textColor.copy(alpha = 0.6f)
                )
            }
            
            VerticalDivider(modifier = Modifier.height(40.dp), thickness = 1.dp, color = textColor.copy(alpha = 0.1f))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { complianceScore },
                        modifier = Modifier.size(52.dp),
                        color = if (complianceScore > 0.8f) Color(0xFF4CAF50) else if (complianceScore > 0.4f) Color(0xFFFFC107) else Color(0xFFFF5722),
                        strokeWidth = 4.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Icon(
                        imageVector = if (complianceScore > 0.8f) Icons.Default.Shield else Icons.Default.Badge, 
                        contentDescription = null, 
                        modifier = Modifier.size(20.dp),
                        tint = if (complianceScore > 0.8f) Color(0xFF4CAF50) else if (complianceScore > 0.4f) Color(0xFFFFC107) else Color(0xFFFF5722)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Safe Status: ${(complianceScore * 100).toInt()}%", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold, 
                    color = if (complianceScore > 0.8f) Color(0xFF4CAF50) else textColor.copy(alpha = 0.6f)
                )
            }
            
            VerticalDivider(modifier = Modifier.height(40.dp), thickness = 1.dp, color = textColor.copy(alpha = 0.1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$walletCount", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Black, 
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Documents", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = textColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun RecentMaterialsWidget(
    documents: List<com.scholarvault.data.model.DocumentFile>,
    notes: List<com.scholarvault.data.model.QuickNoteEntity>,
    isLoading: Boolean = false,
    onNavigate: (String) -> Unit
) {
    val isDark = LocalThemeController.current.isDarkTheme
    val bg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F7)
    val borderColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
    val tc = if (isDark) Color.White else Color.Black

    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Materials",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = tc
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isLoading) {
                    repeat(3) {
                        com.scholarvault.ui.components.TaskSkeletonLine()
                    }
                } else {
                    documents.take(3).forEach { doc ->
                        RecentMaterialItem(
                            icon = Icons.Default.InsertDriveFile,
                            title = doc.name,
                            subtitle = "Document",
                            onClick = { onNavigate("documents") }
                        )
                    }
                    notes.take(3).forEach { note ->
                        RecentMaterialItem(
                            icon = Icons.Default.ChatBubbleOutline,
                            title = "Take Notes",
                            subtitle = note.content.take(30).replace("\n", " ") + "...",
                            onClick = { onNavigate("quick_note_full") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentMaterialItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val isDark = LocalThemeController.current.isDarkTheme
    val tc = if (isDark) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = tc, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, fontSize = 12.sp, color = if (isDark) Color.LightGray else Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun WeeklyActivityWidget(activity: List<com.scholarvault.ui.viewmodel.DashboardViewModel.DailyActivity>) {
    val isDark = LocalThemeController.current.isDarkTheme
    val bg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F7)
    val borderColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
    val tc = if (isDark) Color.White else Color.Black

    val maxVal = activity.maxOfOrNull { maxOf(it.tasksCompleted, it.notesCreated) } ?: 1
    val yMax = if (maxVal < 5) 5 else maxVal + (maxVal / 5)

    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tc
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                activity.forEach { day ->
                    val taskFrac = (day.tasksCompleted.toFloat() / yMax).coerceIn(0f, 1f)
                    val noteFrac = (day.notesCreated.toFloat() / yMax).coerceIn(0f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .fillMaxHeight(taskFrac)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .fillMaxHeight(noteFrac)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = day.dayName.take(2),
                            fontSize = 11.sp,
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tasks", fontSize = 12.sp, color = tc)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.tertiary))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Notes", fontSize = 12.sp, color = tc)
                }
            }
        }
    }
}

@Composable
fun GlobalSearchResultsWidget(
    scans: List<com.scholarvault.data.model.ScannedDocumentEntity>,
    notes: List<com.scholarvault.data.model.QuickNoteEntity>,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (scans.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PDF Scans",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                scans.forEach { scan ->
                    com.scholarvault.ui.tools.scanner.RecentScanCard(
                        scan = scan,
                        isGrid = false,
                        onDelete = {},
                        onShare = {},
                        onClick = { onNavigate("document_scanner") },
                        onUpdateScan = {},
                        onDownload = {},
                        onSaveToVault = {}
                    )
                }
            }
        }
        if (notes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Take Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                notes.forEach { note ->
                    com.scholarvault.ui.tools.quick_note.QuickNoteCardFullScreen(
                        note = note,
                        isGrid = false,
                        onClick = { onNavigate("quick_note_full") },
                        onEdit = {},
                        onDelete = {},
                        context = androidx.compose.ui.platform.LocalContext.current
                    )
                }
            }
        }
    }
}
