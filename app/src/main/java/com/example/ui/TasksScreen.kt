package com.scholarvault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.theme.LocalThemeController
import com.scholarvault.ui.theme.MyApplicationTheme
import com.scholarvault.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: DashboardViewModel
) {
    val tasks by viewModel.tasks.collectAsState()
    val deletedTasks by viewModel.deletedTasks.collectAsState()
    val showDeletedItems by viewModel.showDeletedTasks.collectAsState()
    val selectedTaskIds by viewModel.selectedTaskIds.collectAsState()
    
    var showMenu by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = showDeletedItems) {
        viewModel.toggleShowDeletedTasks()
    }

    Scaffold(
        topBar = {
            TopSearchBar(
                onOpenDrawer = {
                    if (showDeletedItems) {
                        viewModel.toggleShowDeletedTasks()
                    } else {
                        onBack()
                    }
                },
                isBackButton = true,
                title = if (showDeletedItems) "Dismissed Tasks" else "Tasks",
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
                                                if (selectedTaskIds.isNotEmpty()) {
                                                    Surface(
                                                        color = btnBg,
                                                        shape = RoundedCornerShape(50),
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(50)).clickable {
                                                            viewModel.deleteSelectedTasks()
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
                                                        viewModel.toggleShowDeletedTasks()
                                                        showMenu = false
                                                    }
                                                ) {
                                                    Text("View Dismissed", modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Normal, color = tc, fontSize = 14.sp)
                                                }
                                            } else {
                                                if (selectedTaskIds.isNotEmpty()) {
                                                    Surface(
                                                        color = btnBg,
                                                        shape = RoundedCornerShape(50),
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(50)).clickable {
                                                            viewModel.restoreSelectedTasks()
                                                            showMenu = false
                                                        }
                                                    ) {
                                                        Text("Restore Selected", modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Normal, color = tc, fontSize = 14.sp)
                                                    }
                                                    Surface(
                                                        color = btnBg,
                                                        shape = RoundedCornerShape(50),
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(50)).clickable {
                                                            viewModel.completelyDeleteSelectedTasks()
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
                                                        viewModel.toggleShowDeletedTasks()
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
        
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(bg)) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    TasksSection(
                        tasks = tasks,
                        deletedTasks = deletedTasks,
                        showDeletedItems = showDeletedItems,
                        onViewAllClick = null,
                        hideAddButton = true,
                        isExpandedMode = true,
                        hideDeleteButton = true,
                        selectedIds = selectedTaskIds,
                        onToggleTask = { viewModel.toggleTask(it) },
                        onSelectTask = viewModel::toggleTaskSelection,
                        onAddTask = viewModel::addTask,
                        onDeleteSelected = viewModel::deleteSelectedTasks,
                        onRestoreSelected = viewModel::restoreSelectedTasks,
                        onCompletelyDeleteSelected = viewModel::completelyDeleteSelectedTasks
                    )
                }
            }
        }
    }
}
