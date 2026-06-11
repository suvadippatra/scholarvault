package com.scholarvault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.theme.LocalThemeController
import com.scholarvault.ui.theme.MyApplicationTheme
import com.scholarvault.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: DashboardViewModel
) {
    val remnants by viewModel.reminders.collectAsState()
    val deletedReminders by viewModel.deletedReminders.collectAsState()
    val showDeletedItems by viewModel.showDeletedReminders.collectAsState()
    val selectedReminderIds by viewModel.selectedReminderIds.collectAsState()
    val mContext = androidx.compose.ui.platform.LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = showDeletedItems) {
        viewModel.toggleShowDeletedReminders()
    }

    Scaffold(
        topBar = {
            TopSearchBar(
                onOpenDrawer = {
                    if (showDeletedItems) {
                        viewModel.toggleShowDeletedReminders()
                    } else {
                        onBack()
                    }
                },
                isBackButton = true,
                title = if (showDeletedItems) "Dismissed Reminders" else "Reminders",
                showSearchBar = false,
                showProfileIcon = false,
                onNavigate = onNavigate,
                actions = {
                    Box {
                        val themeController = LocalThemeController.current
                        val isDark = themeController.isDarkTheme
                        IconButton(onClick = { showMenu = true }) {
                            val tint = if (isDark) Color.White else Color.Black
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = tint)
                        }
                        if (showMenu) {
                            val themeMode = themeController.themeMode
                            androidx.compose.ui.window.Popup(
                                alignment = androidx.compose.ui.Alignment.TopEnd,
                                offset = androidx.compose.ui.unit.IntOffset(0, 0),
                                onDismissRequest = { showMenu = false },
                                properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                            ) {
                                MyApplicationTheme(themeMode = themeMode) {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                        color = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD0D0D0),
                                        shadowElevation = 8.dp,
                                        modifier = Modifier.width(200.dp).padding(8.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
                                            val btnBg = if (isDark) Color(0xFF333333) else Color(0xFFF2F2F2)
                                            val tc = if (isDark) Color.White else Color.Black

                                            if (!showDeletedItems) {
                                                if (selectedReminderIds.isNotEmpty()) {
                                                    Surface(
                                                        color = btnBg,
                                                        shape = RoundedCornerShape(50),
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(50)).clickable {
                                                            viewModel.deleteSelectedReminders()
                                                            showMenu = false
                                                        }
                                                    ) {
                                                        Text("Dismiss Selected", modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Normal, color = tc, fontSize = 14.sp)
                                                    }
                                                }
                                                Surface(
                                                    color = btnBg,
                                                    shape = RoundedCornerShape(50),
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(50)).clickable {
                                                        viewModel.toggleShowDeletedReminders()
                                                        showMenu = false
                                                    }
                                                ) {
                                                    Text("View Dismissed", modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Normal, color = tc, fontSize = 14.sp)
                                                }
                                            } else {
                                                if (selectedReminderIds.isNotEmpty()) {
                                                    Surface(
                                                        color = btnBg,
                                                        shape = RoundedCornerShape(50),
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(50)).clickable {
                                                            viewModel.restoreSelectedReminders()
                                                            showMenu = false
                                                        }
                                                    ) {
                                                        Text("Restore Selected", modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Normal, color = tc, fontSize = 14.sp)
                                                    }
                                                    Surface(
                                                        color = btnBg,
                                                        shape = RoundedCornerShape(50),
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(50)).clickable {
                                                            viewModel.completelyDeleteSelectedReminders()
                                                            showMenu = false
                                                        }
                                                    ) {
                                                        Text("Delete Forever", modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Normal, color = Color.Red, fontSize = 14.sp)
                                                    }
                                                }
                                                Surface(
                                                    color = btnBg,
                                                    shape = RoundedCornerShape(50),
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(50)).clickable {
                                                        viewModel.toggleShowDeletedReminders()
                                                        showMenu = false
                                                    }
                                                ) {
                                                    Text("Exit Dismissed", modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Normal, color = tc, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        val theme = LocalThemeController.current
        val isDark = theme.isDarkTheme
        val bg = if (isDark) Color.Black else Color.White
        val cardBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
        val textColor = if (isDark) Color.White else Color.Black
        
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(bg)) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    RemindersSection(
                        reminders = remnants,
                        deletedReminders = deletedReminders,
                        showDeletedItems = showDeletedItems,
                        onViewAllClick = null,
                        hideAddButton = true,
                        hideDeleteButton = true,
                        selectedIds = selectedReminderIds,
                        onSelectReminder = viewModel::toggleReminderSelection,
                        onAddReminder = viewModel::addReminder,
                        onDeleteSelected = viewModel::deleteSelectedReminders,
                        onRestoreSelected = viewModel::restoreSelectedReminders,
                        onCompletelyDeleteSelected = viewModel::completelyDeleteSelectedReminders
                    )
                }
            }

            if (!showDeletedItems) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp).height(50.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = cardBg, contentColor = textColor)
                ) {
                    Text("+ Add Reminder", fontSize = 16.sp)
                }
            }
        }
        
        if (showAddDialog) {
            com.scholarvault.ui.components.ReminderAddDialog(
                onDismissRequest = { showAddDialog = false },
                onAdd = { title, dateStr, timeMillis ->
                    val alarmManager = mContext.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        mContext.startActivity(intent)
                        android.widget.Toast.makeText(mContext, "Please grant exact alarm permissions and try again", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.addReminder(title, dateStr, timeMillis)
                        showAddDialog = false
                    }
                }
            )
        }
    }
}
