package com.scholarvault.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.launch

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.theme.LocalThemeController
import com.scholarvault.ui.viewmodel.AcademicViewModel

import java.text.SimpleDateFormat
import java.util.Locale
import com.scholarvault.data.model.AcademicItem
import org.json.JSONArray
import org.json.JSONObject

import com.scholarvault.ui.components.DocumentAttachment
import com.scholarvault.ui.components.DocumentPreviewDialog

fun parseAttachments(jsonString: String?): List<DocumentAttachment> {
    if (jsonString.isNullOrEmpty()) return emptyList()
    val list = mutableListOf<DocumentAttachment>()
    try {
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(DocumentAttachment(obj.getString("name"), obj.getString("uri")))
        }
    } catch (e: Exception) {
        // Migration from old format
        list.add(DocumentAttachment("Document", jsonString))
    }
    return list
}

fun serializeAttachments(list: List<DocumentAttachment>): String {
    val array = JSONArray()
    for (item in list) {
        val obj = JSONObject()
        obj.put("name", item.name)
        obj.put("uri", item.uri)
        array.put(obj)
    }
    return array.toString()
}

class SecureIdVault(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context, "your_ids_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    fun getIds(): Map<String, String> = prefs.all.mapValues { it.value.toString() }
    fun saveId(name: String, value: String) = prefs.edit().putString(name, value).apply()
    fun removeId(name: String) = prefs.edit().remove(name).apply()
}

@Composable
fun TimelineNode() {
    val isDark = LocalThemeController.current.isDarkTheme
    val nodeColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(nodeColor, shape = RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(64.dp)
                .background(nodeColor.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun TimelineNodeCard(
    item: com.scholarvault.data.model.CourseWithSemesters,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalThemeController.current.isDarkTheme
    val cardBg = if (isDark) Color(0xFF2A2A2A) else Color.White
    val textColorPrimary = if (isDark) Color.White else Color.Black
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val course = item.course
    val semesters = item.semesters
    
    val summaryData = remember(semesters) {
        if (semesters.isEmpty()) null
        else {
            val evaluationSystem = course.evaluationSystem ?: "Percentage"
            if (evaluationSystem == "Grade") {
                val totalCredits = semesters.sumOf { it.totalCredits }
                val weightedSgpa = semesters.sumOf { (it.earnedSgpa ?: 0.0) * it.totalCredits }
                val cgpa = if (totalCredits > 0) weightedSgpa / totalCredits else 0.0
                String.format("CGPA: %.2f", cgpa)
            } else {
                // If it's percentage, we look at the 'data' field or aggregate marks
                // For simplicity, let's look at the average of percentage strings if present, 
                // OR just show the count of semesters
                if (semesters.size == 1) semesters.first().data
                else "${semesters.size} Terms"
            }
        }
    }

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isDark) 0.dp else 4.dp),
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(course.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColorPrimary, modifier = Modifier.weight(1f))
                if (summaryData != null) {
                    Text(summaryData, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatter.format(course.timelineDate), fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("•", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(course.type, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            }
            if (!course.institutionName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(course.institutionName!!, fontSize = 13.sp, color = textColorPrimary.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun AcademicTimelineList(items: List<com.scholarvault.data.model.CourseWithSemesters>, onNavigateToDetail: (String) -> Unit) {
    val animationsEnabled = LocalThemeController.current.animationsEnabled
    var showTimeline by remember { mutableStateOf(!animationsEnabled) }
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 840

    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) {
            kotlinx.coroutines.delay(100)
            showTimeline = true
        } else {
            showTimeline = true
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = showTimeline,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { 40 })
    ) {
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(if (isTablet) 2 else 1),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items.size) { index ->
                val item = items[index]
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (!isTablet) TimelineNode()
                    TimelineNodeCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.course.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AcademicsScreen(
    viewModel: AcademicViewModel,
    onOpenDrawer: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToAddItem: (String) -> Unit = {},
    onNavigateToIdsList: () -> Unit = {}
) {
    val themeController = LocalThemeController.current
    val isDark = themeController.isDarkTheme
    val items by viewModel.coursesWithSemesters.collectAsState()
    var showCategorySheet by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 840

    var isSimulatedLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600)
        isSimulatedLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopSearchBar(onOpenDrawer = onOpenDrawer, onNavigate = onNavigate)
        
        YourIdsCard(isDark = isDark, onViewIdsClick = onNavigateToIdsList)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = if (isTablet) 32.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val textColorPrimary = if (isDark) Color.White else Color.Black
            Text("Academic Timeline", fontWeight = FontWeight.Bold, fontSize = if (isTablet) 36.sp else 28.sp, color = textColorPrimary)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isSimulatedLoading) {
            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                repeat(4) {
                    com.scholarvault.ui.components.AcademicCourseSkeletonLine()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Click here to add your courses.",
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.Gray,
                    modifier = Modifier.clickable { showCategorySheet = true }.padding(16.dp)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Box(modifier = Modifier.weight(1f)) {
                    AcademicTimelineList(items = items, onNavigateToDetail = onNavigateToDetail)
                }
                
                val cardBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                val textColor = if (isDark) Color.White else Color.Black
                Button(
                    onClick = { showCategorySheet = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp).height(50.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = cardBg, contentColor = textColor)
                ) {
                    Text("+ Add Academic Record", fontSize = 16.sp)
                }
            }
        }
    }

    if (showCategorySheet) {
        val existingCategories = items.map { it.course.type }
        com.scholarvault.ui.components.CategorySelectionDialog(
            existingCategories = existingCategories,
            onDismiss = { showCategorySheet = false },
            onSelect = { category ->
                showCategorySheet = false
                onNavigateToAddItem(category)
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun YourIdsCard(isDark: Boolean, onViewIdsClick: () -> Unit) {
    val context = LocalContext.current
    val vault = remember { SecureIdVault(context) }
    var ids by remember { mutableStateOf(vault.getIds()) }

    val cardBg = if (isDark) Color(0xFF2A2A2A) else Color.White
    val textColorPrimary = if (isDark) Color.White else Color.Black
    val containerColor = if (isDark) Color(0xFF424242) else Color(0xFFE0E0E0)

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your IDs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColorPrimary
                )
                TextButton(
                    onClick = onViewIdsClick,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                ) {
                    Text("View IDs", fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (ids.isEmpty()) {
                Text("No IDs saved.", fontSize = 13.sp, color = Color.Gray)
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayIds = ids.filterKeys { !it.endsWith("_attachment") && !it.endsWith("_attachments") }
                    items(displayIds.toList()) { (name, value) ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = { copyToClipboard(name, value) }
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.padding(end = 8.dp)) {
                                    Text(name, fontSize = 13.sp, color = Color.Gray, softWrap = true)
                                    Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColorPrimary, softWrap = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateInfoSection(title: String, desc: String, isDark: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFF9F9F9), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDark) Color.LightGray else Color.DarkGray,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun IdsListScreen(onBack: () -> Unit, docViewModel: com.scholarvault.ui.viewmodel.DocumentViewModel) {
    val context = LocalContext.current
    val vault = remember { SecureIdVault(context) }
    var ids by remember { mutableStateOf(vault.getIds()) }
    var showAddDialog by remember { mutableStateOf(false) }
    val themeController = LocalThemeController.current
    val isDark = themeController.isDarkTheme
    val scope = rememberCoroutineScope()

    val bg = if (isDark) Color.Black else Color.White
    val cardBg = if (isDark) Color(0xFF2A2A2A) else Color.White
    val textColorPrimary = if (isDark) Color.White else Color.Black
    val containerColor = if (isDark) Color(0xFF424242) else Color(0xFFE0E0E0)
    
    var editingKey by remember { mutableStateOf<String?>(null) }
    var viewingAttachmentsFor by remember { mutableStateOf<List<DocumentAttachment>?>(null) }
    var editingValue by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var deleteConfirmationKey by remember { mutableStateOf<String?>(null) }

    val displayIds = remember(ids) { ids.filterKeys { !it.endsWith("_attachment") && !it.endsWith("_attachments") } }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
    }

    if (deleteConfirmationKey != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmationKey = null },
            title = { Text("Delete ID") },
            text = { Text("Are you sure you want to delete this ID? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val name = deleteConfirmationKey!!
                    vault.removeId(name)
                    vault.removeId("${name}_attachment")
                    vault.removeId("${name}_attachments")
                    ids = vault.getIds()
                    deleteConfirmationKey = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmationKey = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        com.scholarvault.ui.components.TopSearchBar(onOpenDrawer = onBack, isBackButton = true)
        Text("All Saved IDs", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = textColorPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp),
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            if (displayIds.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = "No Saved IDs Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Saved IDs Yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColorPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Digitize and secure your essential academic credentials.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        EmptyStateInfoSection(
                            title = "What is the ID Wallet?",
                            desc = "A secure local vault to store student ID cards, library card details, registration numbers, and examination credentials without carrying physical cards.",
                            isDark = isDark
                        )
                        EmptyStateInfoSection(
                            title = "How to add and use?",
                            desc = "1. Tap '+ Add More IDs' below.\n2. Label your ID (e.g., 'Roll Number') and enter the value details.\n3. Save to access instantly offline. Click copy/edit buttons to use.",
                            isDark = isDark
                        )
                        EmptyStateInfoSection(
                            title = "Why is it helpful?",
                            desc = "Keeps your registration passes visual and secure completely offline. Perfect for quick access on campuses with no internet access.",
                            isDark = isDark
                        )
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayIds.toList()) { (name, value) ->
                        val hasAttachment = ids.containsKey("${name}_attachment") || ids.containsKey("${name}_attachments")
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(name, fontSize = 13.sp, color = Color.Gray)
                                    Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColorPrimary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (hasAttachment) {
                                        val parsedAttachments = parseAttachments(ids["${name}_attachments"] ?: ids["${name}_attachment"])
                                        IconButton(onClick = { viewingAttachmentsFor = parsedAttachments }) {
                                            Icon(
                                                Icons.Default.Visibility, 
                                                contentDescription = "View Documents", 
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { 
                                            editingKey = name
                                            editingValue = value
                                            isEditing = true
                                            showAddDialog = true 
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = { 
                                            deleteConfirmationKey = name
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = { copyToClipboard(name, value) }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { 
                editingKey = null
                editingValue = ""
                isEditing = false
                showAddDialog = true 
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
            shape = RoundedCornerShape(50)
        ) {
            Text("+ Add More IDs", fontSize = 14.sp)
        }
    }

    if (showAddDialog) {
        var newName by remember(editingKey) { mutableStateOf(editingKey ?: "") }
        var newValue by remember(editingValue) { mutableStateOf(editingValue) }
        
        var currentAttachments by remember(editingKey) { 
            mutableStateOf(parseAttachments(ids["${editingKey ?: ""}_attachments"] ?: ids["${editingKey ?: ""}_attachment"])) 
        }
        var showAttachmentNameDialog by remember { mutableStateOf(false) }
        var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
        var pendingAttachmentName by remember { mutableStateOf("") }
        
        val dialogLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                pendingUri = uri
                pendingAttachmentName = ""
                showAttachmentNameDialog = true
            }
        }
        
        // Storage permissions for attachments
        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                dialogLauncher.launch("*/*")
            } else {
                Toast.makeText(context, "Permission required to access files", Toast.LENGTH_SHORT).show()
            }
        }
        
        fun handleAddAttachment() {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                dialogLauncher.launch("*/*")
            } else {
                val permissionStatus = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
                if (permissionStatus == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    dialogLauncher.launch("*/*")
                } else {
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = cardBg,
            title = { Text(if (isEditing) "Edit ID" else "Add New ID", color = textColorPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("ID Name (e.g. ABC ID)", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = textColorPrimary),
                        enabled = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("ID Number", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = textColorPrimary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Linked Documents", fontWeight = FontWeight.Bold, color = textColorPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (currentAttachments.isEmpty()) {
                        Text("No documents linked.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        currentAttachments.forEachIndexed { index, attachment ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Icon(Icons.Default.Attachment, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(attachment.name, color = textColorPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { 
                                    val newList = currentAttachments.toMutableList()
                                    newList.removeAt(index)
                                    currentAttachments = newList
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { handleAddAttachment() }) {
                        Text("+ Add Attachment", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank() && newValue.isNotBlank()) {
                        if (isEditing && newName != editingKey) {
                            vault.removeId(editingKey!!)
                            vault.removeId("${editingKey}_attachment")
                            vault.removeId("${editingKey}_attachments")
                        }
                        vault.saveId(newName, newValue)
                        if (currentAttachments.isNotEmpty()) {
                            vault.saveId("${newName}_attachments", serializeAttachments(currentAttachments))
                        } else {
                            vault.removeId("${newName}_attachments")
                        }
                        vault.removeId("${newName}_attachment") // cleanup old
                        ids = vault.getIds()
                        showAddDialog = false
                    }
                }) { Text("Save", fontSize = 14.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", fontSize = 14.sp) }
            }
        )
        
        if (showAttachmentNameDialog) {
            AlertDialog(
                onDismissRequest = { showAttachmentNameDialog = false },
                containerColor = cardBg,
                title = { Text("Attachment Name", color = textColorPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = pendingAttachmentName,
                        onValueChange = { pendingAttachmentName = it },
                        label = { Text("Document Name", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = textColorPrimary)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (pendingAttachmentName.isNotBlank() && pendingUri != null) {
                            scope.launch {
                                val uriToSave = pendingUri
                                var finalExt = "pdf"
                                val c = context.contentResolver.query(uriToSave!!, null, null, null, null)
                                c?.use {
                                    if (it.moveToFirst()) {
                                        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                        if (idx != -1) {
                                            val n = it.getString(idx)
                                            if (n.contains(".")) finalExt = n.substringAfterLast(".")
                                        }
                                    }
                                }
                                val internalUriStr = docViewModel.insertAttachmentFile(
                                    context,
                                    com.scholarvault.data.model.DocumentFile(
                                        name = "${pendingAttachmentName}.$finalExt",
                                        isFolder = false,
                                        parentFolderId = null,
                                        filePath = "",
                                        extension = finalExt,
                                        sizeBytes = 0L,
                                        isEncrypted = true,
                                        tags = listOf("wallet")
                                    ),
                                    uriToSave
                                )
                                currentAttachments = currentAttachments + DocumentAttachment(pendingAttachmentName, internalUriStr.toString())
                                pendingUri = null
                                showAttachmentNameDialog = false
                            }
                        } else {
                            showAttachmentNameDialog = false
                            pendingUri = null
                        }
                    }) { Text("Add", fontSize = 14.sp) }
                },
                dismissButton = {
                    TextButton(onClick = { showAttachmentNameDialog = false; pendingUri = null }) { Text("Cancel", fontSize = 14.sp) }
                }
            )
        }
        if (viewingAttachmentsFor != null) {
            if (viewingAttachmentsFor!!.size == 1) {
                DocumentPreviewDialog(attachment = viewingAttachmentsFor!!.first(), onDismiss = { viewingAttachmentsFor = null }, cardBg = cardBg, textColor = textColorPrimary)
            } else {
                AlertDialog(
                    onDismissRequest = { viewingAttachmentsFor = null },
                    title = { Text("Select Document", color = textColorPrimary, fontWeight = FontWeight.Bold) },
                    containerColor = cardBg,
                    text = {
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(viewingAttachmentsFor!!.size) { idx ->
                                val attachment = viewingAttachmentsFor!![idx]
                                TextButton(
                                    onClick = { viewingAttachmentsFor = listOf(attachment) },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    Text(attachment.name, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewingAttachmentsFor = null }) { Text("Close") }
                    }
                )
            }
        }
    }
}

