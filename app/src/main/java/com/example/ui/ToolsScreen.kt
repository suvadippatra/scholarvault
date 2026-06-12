package com.scholarvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.theme.LocalThemeController

val AllToolsList = listOf(
    ToolItem("Transactions", "Track your finances securely.", Icons.Default.CurrencyExchange, "Utility", Screen.Transactions.route),
    ToolItem("CGPA Calculator", "Calculate and track your academic performance.", Icons.Default.Calculate, "Educational", Screen.CgpaCalculator.route),
    ToolItem("Unit Converter", "Convert different measurements easily.", androidx.compose.material.icons.Icons.Default.SyncAlt, "Educational", Screen.UnitConverter.route),
    ToolItem("Citation Generator", "Generate APA, MLA, or Chicago citations.", Icons.Default.FormatQuote, "Educational", Screen.CitationGenerator.route),
    ToolItem("Calculator", "Perform general and scientific math operations.", Icons.Default.Calculate, "Educational", "calculator"),
    ToolItem("Sound Recorder", "Record and save audio easily.", Icons.Default.Mic, "Utility", "sound_recorder"),
    ToolItem("Password Generator", "Generate strong and secure passwords.", Icons.Default.Password, "Utility", Screen.PasswordGenerator.route),
    ToolItem("Focus Timer", "Concentrate on your study with Pomodoro technique.", Icons.Default.Timer, "Utility", Screen.FocusTimer.route),
    ToolItem("Tasks Manager", "Organize your daily study and life tasks.", Icons.Default.List, "Utility", Screen.Tasks.route),
    ToolItem("Flashcards", "Create study decks and master topics.", Icons.Default.Style, "Educational", Screen.FlashcardsDecks.route),
    ToolItem("Take Notes", "Full screen rich text Markdown editor.", Icons.Default.EditNote, "Utility", Screen.QuickNoteFull.route),
    ToolItem("Take Notes Bubble", "Floating widget for instant ideas.", Icons.Default.ChatBubble, "Utility", "quick_note_widget"),
    ToolItem("Reminders", "Never miss a deadline with scheduled alerts.", Icons.Default.Notifications, "Utility", Screen.Reminders.route),
    ToolItem("PDF Color Inverter", "Invert PDF colors for easier reading.", Icons.Default.PictureAsPdf, "Files", "pdf_color_inverter", true),
    ToolItem("Image Resizer", "Resize images with precision for exam forms.", Icons.Default.Image, "Image", Screen.ImageResizer.route, true),
    ToolItem("Compress File", "Reduce image size or flatten PDF files.", Icons.Default.Compress, "Files", Screen.CompressImage.route, true),
    ToolItem("Image to PDF", "Convert your photos into high-quality PDF files.", Icons.Default.PictureAsPdf, "Image", Screen.ImageToPdf.route, true),
    ToolItem("Recent Generated Media", "Review, share, and save recently generated or compressed files.", Icons.Default.History, "Image", Screen.RecentGeneratedMedia.route, true),
    ToolItem("PDF N-Up", "Combine multiple pages into a single sheet to save paper.", Icons.Default.ViewModule, "Utility", "pdf_nup", true),
    ToolItem("Pre-Printing Setup", "Print multiple pages on a single sheet, margins & borders.", Icons.Default.Print, "Utility", Screen.PrePrintingSetup.route, true),
    ToolItem("Secure Backup", "Safely backup your files with AES-256 encryption.", Icons.Default.Backup, "Files", Screen.SecureBackup.route, true),
    ToolItem("Settings", "Change your preferences.", Icons.Default.Settings, "Utility", "settings"),
    ToolItem("Document Scanner", "Scan study materials to PDF with 4-point crop and filters.", Icons.Default.DocumentScanner, "Files", Screen.DocumentScanner.route)
)

@Composable
fun ToolsScreen(onOpenDrawer: () -> Unit = {}, onNavigate: (String) -> Unit = {}) {
    val themeController = LocalThemeController.current
    val animationsEnabled = themeController.animationsEnabled
    
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Educational", "Image", "Files", "Utility")
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 840
    
    val filteredTools = remember(selectedCategory) {
        if (selectedCategory == "All") AllToolsList else AllToolsList.filter { it.category == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopSearchBar(onOpenDrawer = onOpenDrawer, onNavigate = onNavigate)
        
        val columns = if (isTablet) 3 else 2
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Smart Toolkit",
                        style = if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "A collection of productivity and utility tools designed to enhance your academic lifestyle and manage your data securely.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val prefs = remember { context.getSharedPreferences("scanner_draft", android.content.Context.MODE_PRIVATE) }
                val hasDraft = prefs.getString("draft_uris", null)?.let { org.json.JSONArray(it).length() > 0 } == true
                
                if (hasDraft) {
                    ElevatedCard(
                        onClick = { onNavigate(Screen.DocumentScanner.route) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Resume Document Scan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text("You have an unsaved draft session.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
            
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories.size) { index ->
                        val cat = categories[index]
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(24.dp)
                        )
                    }
                }
            }
            
            items(filteredTools.size) { index ->
                val tool = filteredTools[index]
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ToolCard(tool) {
                        if (tool.route == "quick_note_widget") {
                            com.scholarvault.ui.tools.SharedData.isQuickNoteWidgetVisible.value = true
                        } else if (tool.route.isNotEmpty()) {
                            onNavigate(tool.route)
                        }
                    }
                }
            }
        }
    }
}

data class ToolItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val category: String,
    val route: String,
    val isEnabled: Boolean = true
)

@Composable
fun ToolCard(
    tool: ToolItem,
    onClick: () -> Unit
) {
    val isDark = LocalThemeController.current.isDarkTheme
    val alpha = if (tool.isEnabled) 1f else 0.4f
    
    ElevatedCard(
        onClick = { if (tool.isEnabled) onClick() },
        modifier = Modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = alpha) else Color.White.copy(alpha = alpha)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f * alpha),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}
