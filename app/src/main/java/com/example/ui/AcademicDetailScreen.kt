// Path: app/src/main/java/com.scholarvault/ui/AcademicDetailScreen.kt
package com.scholarvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.data.model.Semester
import com.scholarvault.ui.theme.LocalThemeController
import com.scholarvault.ui.viewmodel.AcademicViewModel

import com.scholarvault.ui.components.DocumentAttachment
import com.scholarvault.ui.components.DocumentPreviewDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.scholarvault.data.model.SubjectEntryState
import com.scholarvault.data.model.SubjectSerializer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicDetailScreen(
    itemId: String, 
    viewModel: AcademicViewModel, 
    docViewModel: com.scholarvault.ui.viewmodel.DocumentViewModel,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToViewer: (String) -> Unit,
    onBack: () -> Unit
) {
    val theme = LocalThemeController.current
    val isDark = theme.isDarkTheme
    val animationsEnabled = theme.animationsEnabled
    val scaffoldBgColor = if (isDark) Color.Black else Color(0xFFF5F5F5)
    val textColorPrimary = if (isDark) Color.White else Color.Black
    val cardBg = if (isDark) Color(0xFF2A2A2A) else Color.White

    val courseWithSemesters by remember(itemId) { viewModel.getCourseWithSemesters(itemId) }.collectAsState()
    val documentLinks by remember(itemId) { viewModel.getLinksForAcademicItem(itemId) }.collectAsState()
    val allWalletFiles by docViewModel.rootFiles.collectAsState()
    
    var showAddSemesterDialog by remember { mutableStateOf(false) }
    var documentTargetSemesterId by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var attachedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var viewDocument by remember { mutableStateOf<DocumentAttachment?>(null) }
    var listVisible by remember { mutableStateOf(!animationsEnabled) }

    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) {
            delay(100)
            listVisible = true
        } else {
            listVisible = true
        }
    }
    
    val scope = rememberCoroutineScope()
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            scope.launch {
                var retrievedName = "Academic_Doc_${System.currentTimeMillis()}"
                var finalExt = ""
                var finalSize = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                     if (cursor.moveToFirst()) {
                         val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                         if (nameIndex != -1) {
                             retrievedName = cursor.getString(nameIndex)
                             if (retrievedName.contains('.')) {
                                 finalExt = retrievedName.substringAfterLast(".", "")
                             }
                         }
                         val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                         if (sizeIndex != -1) {
                             finalSize = cursor.getLong(sizeIndex)
                         }
                     }
                }
                
                val docFile = com.scholarvault.data.model.DocumentFile(
                    name = retrievedName,
                    isFolder = false,
                    isEncrypted = true,
                    extension = finalExt,
                    sizeBytes = finalSize,
                    filePath = "",
                    tags = listOf("academic")
                )
                val newId = docViewModel.insertAttachmentFile(context, docFile, uri)
                if (newId > 0) {
                    if (documentTargetSemesterId != null) {
                        // Semester-level attachment
                        val semesterId = documentTargetSemesterId!!
                        val semester = courseWithSemesters?.semesters?.find { it.id == semesterId }
                        if (semester != null) {
                            val updatedSemester = semester.copy(
                                attachedDocumentId = newId.toString(),
                                attachedDocumentName = retrievedName
                            )
                            viewModel.updateSemester(updatedSemester)
                            android.widget.Toast.makeText(context, "Semester Marksheet linked", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        documentTargetSemesterId = null
                    } else {
                        // Course-level attachment
                        val link = com.scholarvault.data.model.AcademicDocumentLink(
                            academicItemId = itemId,
                            walletDocumentId = newId.toString(),
                            linkLabel = "Attached Document"
                        )
                        viewModel.insertAcademicDocumentLink(link)
                        android.widget.Toast.makeText(context, "Attachment saved & linked", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.widget.Toast.makeText(context, "Failed to save attachment", android.widget.Toast.LENGTH_SHORT).show()
                    documentTargetSemesterId = null
                }
            }
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launcher.launch("*/*")
        } else {
            android.widget.Toast.makeText(context, "Permission required to access files", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun handleAttach() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            launcher.launch("*/*")
        } else {
            val permissionStatus = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            if (permissionStatus == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                launcher.launch("*/*")
            } else {
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    fun handleSemesterAttach(semesterId: String) {
        documentTargetSemesterId = semesterId
        handleAttach()
    }
    
    Scaffold(
        containerColor = scaffoldBgColor,
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = onBack, 
                isBackButton = true,
                actions = {
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = textColorPrimary)
                        }
                        com.scholarvault.ui.components.CustomPopupMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            items = listOf(
                                com.scholarvault.ui.components.MenuItem("Edit", Icons.Default.Edit) {
                                    showMenu = false 
                                    onNavigateToEdit(itemId)
                                }
                            )
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSemesterDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Semester") },
                text = { Text("Add Term/Semester") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { innerPadding ->
        if (courseWithSemesters == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val item = courseWithSemesters!!.course
            val semesters = courseWithSemesters!!.semesters
            val parsedSubjects = remember(item.subjectsJson) { SubjectSerializer.deserialize(item.subjectsJson) }

            AnimatedVisibility(
                visible = listVisible,
                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(400))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ElevatedCard(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                            modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = textColorPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Category: ${item.type}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = { handleAttach() }, 
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Attachment, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Link Document")
                                    }
                                }
                                
                                if (documentLinks.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Linked Certificates & Marksheets", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = textColorPrimary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    documentLinks.forEach { link ->
                                        val linkedFile = allWalletFiles.find { it.id.toString() == link.walletDocumentId }
                                        if (linkedFile != null) {
                                            ElevatedCard(
                                                onClick = {
                                                    val mappedType = when (linkedFile.extension?.lowercase()) {
                                                        "pdf" -> "pdf"
                                                        "jpg", "jpeg", "png", "gif", "webp" -> "image"
                                                        "mp3", "m4a", "wav", "ogg" -> "audio"
                                                        else -> "unknown"
                                                    }
                                                    onNavigateToViewer(com.scholarvault.ui.Screen.Viewer.createRoute(mappedType, linkedFile.filePath, linkedFile.name))
                                                },
                                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Attachment,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Text(
                                                            text = linkedFile.name,
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = 15.sp,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.Visibility,
                                                        contentDescription = "View Document",
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Group 1: General Information
                    if (!item.institutionName.isNullOrBlank() || !item.classNumber.isNullOrBlank() || !item.degreeType.isNullOrBlank() || !item.structureType.isNullOrBlank()) {
                        item {
                            ElevatedCard(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                                modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("General Information", fontWeight = FontWeight.Bold, color = textColorPrimary, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (!item.institutionName.isNullOrBlank()) DetailRow(label = "Institution / School", value = item.institutionName, icon = Icons.Default.Business)
                                    if (!item.degreeType.isNullOrBlank()) DetailRow(label = "Degree / Course Title", value = item.degreeType, icon = Icons.Default.School)
                                    if (!item.classNumber.isNullOrBlank()) DetailRow(label = "Class / Grade", value = item.classNumber, icon = Icons.Default.Class)
                                    if (!item.structureType.isNullOrBlank()) DetailRow(label = "Structure Type", value = item.structureType, icon = Icons.Default.ViewTimeline)
                                    if (!item.evaluationSystem.isNullOrBlank()) DetailRow(label = "Evaluation System", value = item.evaluationSystem, icon = Icons.Default.Leaderboard)
                                }
                            }
                        }
                    }

                    // Group 2: Examination Details & Registration
                    if (!item.conductingBody.isNullOrBlank() || !item.boardRollNumber.isNullOrBlank() || !item.boardRollCode.isNullOrBlank() || !item.marksheetIndexNumber.isNullOrBlank()) {
                        item {
                            ElevatedCard(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                                modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Registration & Identifiers", fontWeight = FontWeight.Bold, color = textColorPrimary, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (!item.conductingBody.isNullOrBlank()) DetailRow(label = "Exam Authority / Board", value = item.conductingBody, icon = Icons.Default.Verified)
                                    if (!item.conductingYear.isNullOrBlank()) DetailRow(label = "Passing Year", value = item.conductingYear, icon = Icons.Default.CalendarMonth)
                                    if (item.type == "Class 10" || item.type == "Class 12") {
                                        if (!item.boardRollCode.isNullOrBlank()) DetailRow(label = "Roll Code", value = item.boardRollCode, icon = Icons.Default.Pin)
                                        if (!item.boardRollNumber.isNullOrBlank()) DetailRow(label = "Roll Number", value = item.boardRollNumber, icon = Icons.Default.Numbers)
                                        if (!item.marksheetIndexNumber.isNullOrBlank()) DetailRow(label = "Marksheet Index No", value = item.marksheetIndexNumber, icon = Icons.Default.Tag)
                                    } else {
                                        if (!item.boardRollNumber.isNullOrBlank()) DetailRow(label = "Roll / Application No", value = item.boardRollNumber, icon = Icons.Default.Badge)
                                    }
                                }
                            }
                        }
                    }
                    
                    // Group 2.5: Gaps / Geo Info
                    if (!item.domicileState.isNullOrBlank() || !item.institutionDistrict.isNullOrBlank() || !item.stream.isNullOrBlank()) {
                        item {
                            ElevatedCard(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                                modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Academic Context", fontWeight = FontWeight.Bold, color = textColorPrimary, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (!item.stream.isNullOrBlank()) DetailRow(label = "Stream / Subject Group", value = item.stream, icon = Icons.Default.Category)
                                    if (!item.domicileState.isNullOrBlank()) DetailRow(label = "State / Domicile", value = item.domicileState, icon = Icons.Default.Map)
                                    if (!item.institutionDistrict.isNullOrBlank()) DetailRow(label = "District", value = item.institutionDistrict, icon = Icons.Default.MyLocation)
                                }
                            }
                        }
                    }
                    
                    // Group 3: Entrance Exam Info
                    if (item.ntaPercentile != null || item.allIndiaRank != null || item.categoryRank != null) {
                        item {
                            ElevatedCard(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                                modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Entrance Results", fontWeight = FontWeight.Bold, color = textColorPrimary, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (item.ntaPercentile != null) DetailRow(label = "NTA Percentile", value = item.ntaPercentile.toString(), icon = Icons.Default.Percent)
                                    if (item.allIndiaRank != null) DetailRow(label = "All India Rank (CRL)", value = item.allIndiaRank.toString(), icon = Icons.Default.Leaderboard)
                                    if (item.categoryRank != null) DetailRow(label = "Category Rank", value = item.categoryRank.toString(), icon = Icons.Default.Category)
                                }
                            }
                        }
                    }

                    // Render gorgeous Tabular view of subjects/marks if present
                    if (parsedSubjects.isNotEmpty()) {
                        item {
                            ViewMarksTable(
                                subjects = parsedSubjects, 
                                evaluationSystem = item.evaluationSystem ?: "Percentage", 
                                isDark = isDark,
                                useBestOfFive = item.useBestOfFive,
                                bestOfN = item.bestOfN ?: 5,
                                gradeConversionFormula = item.gradeConversionFormula ?: "g * 9.5"
                            )
                        }
                    }
                    
                    item {
                        Text(
                            text = if (item.structureType == "Semester") "Term / Semesters" else "Academic Periods / Years", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 20.sp, 
                            color = textColorPrimary
                        )
                    }

                    if (semesters.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.5f))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("No specific semester records added. Tap '+ Add Term/Semester' below.", fontSize = 14.sp, color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        items(semesters.sortedBy { it.semesterNumber }) { semester ->
                            var expanded by remember { mutableStateOf(false) }
                            
                            ElevatedCard(
                                onClick = { expanded = !expanded },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = semester.semesterLabel.ifEmpty { if (item.structureType == "Semester") "Semester ${semester.semesterNumber}" else "Year / Term ${semester.semesterNumber}" }, 
                                            fontWeight = FontWeight.SemiBold, 
                                            color = textColorPrimary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (item.evaluationSystem == "Grade") "SGPA: ${semester.earnedSgpa}" else "Marks: ${semester.data}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = "Toggle",
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                    
                                    AnimatedVisibility(visible = expanded) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            val semSubjects = SubjectSerializer.deserialize(semester.subjectsJson)
                                            if (semSubjects.isNotEmpty() && semSubjects.first().subjectName.isNotEmpty()) {
                                                Text("Semester Subjects", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                                                semSubjects.forEachIndexed { i, sub ->
                                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text(sub.subjectName, fontSize = 14.sp)
                                                        Text("${sub.obtainedMarks}/${sub.maxMarks}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            } else {
                                                Text("No subjects recorded for this term.", fontSize = 13.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                            }
                                            
                                            // Document Section for Semester
                                            if (semester.attachedDocumentId != null) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                val linkedFile = allWalletFiles.find { it.id.toString() == semester.attachedDocumentId }
                                                Text("Linked Marksheet", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Surface(
                                                    onClick = {
                                                        if (linkedFile != null) {
                                                            val mappedType = when (linkedFile.extension?.lowercase()) {
                                                                "pdf" -> "pdf"
                                                                "jpg", "jpeg", "png", "gif", "webp" -> "image"
                                                                "mp3", "m4a", "wav", "ogg" -> "audio"
                                                                else -> "unknown"
                                                            }
                                                            onNavigateToViewer(com.scholarvault.ui.Screen.Viewer.createRoute(mappedType, linkedFile.filePath, linkedFile.name))
                                                        } else {
                                                            android.widget.Toast.makeText(context, "Document not found in wallet", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            text = semester.attachedDocumentName ?: "View Document",
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedButton(
                                                    onClick = { handleSemesterAttach(semester.id) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(8.dp)
                                                ) {
                                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Attach Marksheet", fontSize = 13.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showAddSemesterDialog) {
        var semLabel by remember { mutableStateOf("") }
        var semNum by remember { mutableStateOf("") }
        var semNumError by remember { mutableStateOf(false) }
        var semesterSubjects by remember { mutableStateOf(listOf<SubjectEntryState>()) }

        AlertDialog(
            onDismissRequest = { showAddSemesterDialog = false },
            title = { Text("Add Term / Period") },
            text = {
                Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    OutlinedTextField(
                        value = semLabel, onValueChange = { semLabel = it },
                        label = { Text("Term Label (Optional)") },
                        placeholder = { Text("e.g. Year 2 / Sem 3") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = semNum, onValueChange = { semNum = it; semNumError = it.toIntOrNull() == null && it.isNotBlank() },
                        label = { Text("Term/Sem Number") },
                        placeholder = { Text("e.g. 1, 2, 3") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        isError = semNumError,
                        shape = RoundedCornerShape(10.dp),
                        supportingText = {
                            if (semNumError) Text("Please enter a number (e.g. 1, 2, 3)", color = MaterialTheme.colorScheme.error)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Subjects & Marks", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    com.scholarvault.ui.components.BoardMarksTable(
                        evaluationSystem = courseWithSemesters?.course?.evaluationSystem ?: "Percentage",
                        subjects = semesterSubjects,
                        onSubjectsChange = { semesterSubjects = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = semNum.toIntOrNull()
                        if (num == null) {
                            semNumError = true
                            return@Button
                        }
                        
                        val totalCredits = semesterSubjects.sumOf { it.creditPoints.toDoubleOrNull() ?: 0.0 }
                        val evaluationSystem = courseWithSemesters?.course?.evaluationSystem ?: "Percentage"
                        var sgpa: Double? = null
                        var dataStr = ""
                        
                        if (evaluationSystem == "Grade") {
                            val weightedSum = semesterSubjects.sumOf { s ->
                                val gp = s.obtainedMarks.toDoubleOrNull() ?: 0.0
                                val credits = s.creditPoints.toDoubleOrNull() ?: 1.0
                                gp * credits
                            }
                            val effectiveCredits = semesterSubjects.sumOf { s ->
                                if ((s.obtainedMarks.toDoubleOrNull() ?: 0.0) > 0)
                                    s.creditPoints.toDoubleOrNull() ?: 1.0
                                else 0.0
                            }
                            sgpa = if (effectiveCredits > 0) weightedSum / effectiveCredits else null
                            dataStr = sgpa?.let { String.format("%.2f", it) } ?: ""
                        } else {
                            val totalMax = semesterSubjects.sumOf { it.maxMarks.toDoubleOrNull() ?: 0.0 }
                            val totalObt = semesterSubjects.sumOf { it.obtainedMarks.toDoubleOrNull() ?: 0.0 }
                            val percentage = if (totalMax > 0) (totalObt / totalMax) * 100 else 0.0
                            dataStr = String.format("%.2f%%", percentage)
                        }

                        val serializedSubjects = SubjectSerializer.serialize(semesterSubjects)
                        
                        viewModel.insertSemester(
                            Semester(
                                courseId = itemId, 
                                semesterNumber = num, 
                                semesterLabel = semLabel,
                                subjectsJson = serializedSubjects,
                                totalCredits = totalCredits,
                                earnedSgpa = sgpa,
                                data = dataStr
                            )
                        )
                        showAddSemesterDialog = false
                    },
                    enabled = semNum.isNotBlank() && !semNumError
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSemesterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (viewDocument != null) {
        DocumentPreviewDialog(
            attachment = viewDocument!!,
            onDismiss = { viewDocument = null },
            cardBg = cardBg,
            textColor = textColorPrimary
        )
    }
}

@Composable
fun DetailRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ViewMarksTable(
    subjects: List<SubjectEntryState>, 
    evaluationSystem: String, 
    isDark: Boolean, 
    useBestOfFive: Boolean = false,
    bestOfN: Int = 5,
    gradeConversionFormula: String = "g * 9.5"
) {
    val tableBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFDFDFD)
    val dividerColor = if (isDark) Color(0xFF3C3C3C) else Color(0xFFE5E5E5)
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tableBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, dividerColor.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Academic Marks Board",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subject", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(2.0f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Code", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.0f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Max", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.8f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Obtained", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.0f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                if (evaluationSystem == "Grade") {
                    Text("Grade", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.8f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Rows
            subjects.forEachIndexed { idx, sub ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(sub.subjectName.ifEmpty { "Subject ${idx + 1}" }, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2.0f), color = MaterialTheme.colorScheme.onSurface)
                    Text(sub.subjectCode.ifEmpty { "-" }, fontSize = 13.sp, modifier = Modifier.weight(1.0f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sub.maxMarks.ifEmpty { "100" }, fontSize = 13.sp, modifier = Modifier.weight(0.8f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sub.obtainedMarks.ifEmpty { "0" }, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.0f), color = MaterialTheme.colorScheme.onSurface)
                    if (evaluationSystem == "Grade") {
                        Text(sub.grade.ifEmpty { "-" }, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(0.8f), color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                if (idx < subjects.size - 1) {
                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
            
            HorizontalDivider(color = dividerColor, thickness = 1.2.dp, modifier = Modifier.padding(vertical = 6.dp))
            
            // Footer Highlight Row
            val filteredSubjects = if (useBestOfFive && subjects.size > bestOfN) {
                subjects.sortedByDescending { it.obtainedMarks.toDoubleOrNull() ?: 0.0 }.take(bestOfN)
            } else {
                subjects
            }
            
            val totalMax = filteredSubjects.sumOf { it.maxMarks.toDoubleOrNull() ?: 0.0 }
            val totalObt = filteredSubjects.sumOf { it.obtainedMarks.toDoubleOrNull() ?: 0.0 }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (useBestOfFive && subjects.size > bestOfN) "Best of $bestOfN Summary" else "Total Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(2.0f), color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("-", modifier = Modifier.weight(1.0f), color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(totalMax.toInt().toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(0.8f), color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(totalObt.toInt().toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.0f), color = MaterialTheme.colorScheme.onSecondaryContainer)
                if (evaluationSystem == "Grade") {
                    val totalCredits = filteredSubjects.sumOf { it.creditPoints.toDoubleOrNull() ?: 0.0 }
                    val totalGradePoints = filteredSubjects.sumOf { 
                        val obt = it.obtainedMarks.toDoubleOrNull() ?: 0.0
                        val tc = it.creditPoints.toDoubleOrNull() ?: 0.0
                        obt * tc 
                    }
                    val gpaAvg = if (totalCredits > 0) totalGradePoints / totalCredits else 0.0
                    Text(String.format("%.2f", gpaAvg), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(0.8f), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            
            if (evaluationSystem == "Percentage" && totalMax > 0) {
                val percentage = (totalObt / totalMax) * 100
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Overall Marks percentage: ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format("%.2f%%", percentage), fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            } else if (evaluationSystem == "Grade") {
                val totalCredits = filteredSubjects.sumOf { it.creditPoints.toDoubleOrNull() ?: 0.0 }
                val totalGradePoints = filteredSubjects.sumOf { 
                    val obt = it.obtainedMarks.toDoubleOrNull() ?: 0.0
                    val tc = it.creditPoints.toDoubleOrNull() ?: 0.0
                    obt * tc 
                }
                val gpaAvg = if (totalCredits > 0) totalGradePoints / totalCredits else 0.0
                
                // Use the formula
                val convertedPercentage = try {
                    val formula = gradeConversionFormula.replace("g", gpaAvg.toString())
                    // Very simple parser for a * b
                    if (formula.contains("*")) {
                        val parts = formula.split("*")
                        parts[0].trim().toDouble() * parts[1].trim().toDouble()
                    } else if (formula.contains("/")) {
                        val parts = formula.split("/")
                        parts[0].trim().toDouble() / parts[1].trim().toDouble()
                    } else {
                        gpaAvg * 9.5 // fallback
                    }
                } catch (e: Exception) {
                    gpaAvg * 9.5
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Estimated Percentage: ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format("%.2f%%", convertedPercentage), fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
                Text("Based on formula: $gradeConversionFormula", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End).padding(horizontal = 8.dp))
            }
        }
    }
}
