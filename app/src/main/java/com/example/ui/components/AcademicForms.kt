package com.scholarvault.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.scholarvault.data.model.AcademicItem
import java.util.Date
import com.scholarvault.ui.viewmodel.DocumentViewModel
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import com.scholarvault.data.model.SubjectEntryState
import com.scholarvault.data.model.SubjectSerializer

data class EditableSemesterState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val semesterNumber: Int,
    val semesterLabel: String = "Semester $semesterNumber",
    val rollNumber: String = "",
    val overrideRollNumber: Boolean = false,
    val subjects: List<SubjectEntryState> = listOf(SubjectEntryState()),
    val evaluationSystem: String = "Percentage",
    val totalCredits: Double = 0.0,
    val earnedSgpa: Double? = null,
    val attachedDocumentId: String? = null,
    val attachedDocumentName: String? = null
)

@Composable
fun InfoTooltipIcon(title: String, body: String) {
    var show by remember { mutableStateOf(false) }
    IconButton(onClick = { show = true }, modifier = Modifier.size(20.dp)) {
        Icon(Icons.Default.Info, contentDescription = "Help", 
             tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
    }
    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(body) },
            confirmButton = { TextButton(onClick = { show = false }) { Text("Got it") } }
        )
    }
}

@Composable
fun CategorySelectionDialog(existingCategories: List<String> = emptyList(), onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val categories = listOf(
        "Pre-Matric", "Class 10", "Class 12", "College Degree",
        "Entrance Examination", "University Degree", "Skill-Based"
    ).filterNot { cat -> 
        cat in listOf("Pre-Matric", "Class 10", "Class 12") && existingCategories.contains(cat) 
    }

    val isDark = com.scholarvault.ui.theme.LocalThemeController.current.isDarkTheme
    val textColorPrimary = if (isDark) Color.White else Color.Black

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select Category", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColorPrimary)
                Spacer(modifier = Modifier.height(20.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        ElevatedCard(
                            onClick = { onSelect(cat) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = textColorPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddAcademicItemScreen(
    category: String,
    docViewModel: DocumentViewModel,
    onBack: () -> Unit,
    onSave: (AcademicItem, List<com.scholarvault.data.model.Semester>, android.net.Uri?, String?, Int?) -> Unit
) {
    val scrollState = rememberScrollState()
    val isDark = com.scholarvault.ui.theme.LocalThemeController.current.isDarkTheme
    val scaffoldBgColor = if (isDark) Color(0xFF121212) else Color(0xFFFCFCFC)
    val textColorPrimary = if (isDark) Color.White else Color.Black

    Scaffold(
        containerColor = scaffoldBgColor,
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(onOpenDrawer = onBack, isBackButton = true)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Add $category Details", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColorPrimary)
            Spacer(modifier = Modifier.height(24.dp))
            
            DynamicAcademicForm(
                category = category,
                docViewModel = docViewModel,
                existingItem = null,
                existingSemesters = emptyList(),
                isDark = isDark,
                textColorPrimary = textColorPrimary,
                onSave = onSave
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun EditAcademicItemScreen(
    item: AcademicItem,
    semesters: List<com.scholarvault.data.model.Semester>,
    docViewModel: DocumentViewModel,
    onBack: () -> Unit,
    onSave: (AcademicItem, List<com.scholarvault.data.model.Semester>, android.net.Uri?, String?, Int?) -> Unit
) {
    val scrollState = rememberScrollState()
    val isDark = com.scholarvault.ui.theme.LocalThemeController.current.isDarkTheme
    val scaffoldBgColor = if (isDark) Color(0xFF121212) else Color(0xFFFCFCFC)
    val textColorPrimary = if (isDark) Color.White else Color.Black

    Scaffold(
        containerColor = scaffoldBgColor,
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(onOpenDrawer = onBack, isBackButton = true)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Edit ${item.type} Details", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColorPrimary)
            Spacer(modifier = Modifier.height(24.dp))
            
            DynamicAcademicForm(
                category = item.type,
                docViewModel = docViewModel,
                existingItem = item,
                existingSemesters = semesters,
                isDark = isDark,
                textColorPrimary = textColorPrimary,
                onSave = onSave
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicAcademicForm(
    category: String,
    docViewModel: DocumentViewModel,
    existingItem: AcademicItem? = null,
    existingSemesters: List<com.scholarvault.data.model.Semester> = emptyList(),
    isDark: Boolean = false,
    textColorPrimary: Color = Color.Unspecified,
    onSave: (AcademicItem, List<com.scholarvault.data.model.Semester>, android.net.Uri?, String?, Int?) -> Unit
) {
    // CRITICAL: Solve Screen Flickering & Cursor Jumps
    // Key the form memory on the existingItem.id instead of the full changing object.
    val keyId = existingItem?.id ?: "new_item"

    // Universal Form States
    var institutionName by remember(keyId) { mutableStateOf(existingItem?.institutionName ?: "") }
    var evaluationSystem by remember(keyId) { mutableStateOf(existingItem?.evaluationSystem ?: "Percentage") }
    var structureType by remember(keyId) { mutableStateOf(existingItem?.structureType ?: "Yearly") }
    
    // Class 10 / 12 specific
    var conductingBody by remember(keyId) { mutableStateOf(existingItem?.conductingBody ?: "") }
    var conductingYear by remember(keyId) { mutableStateOf(existingItem?.conductingYear ?: "") }
    
    // NEW identification fields
    var useBestOfFive by remember(keyId) { mutableStateOf(existingItem?.useBestOfFive ?: false) }
    var bestOfNText by remember(keyId) { mutableStateOf(existingItem?.bestOfN?.toString() ?: "") }
    var gradeConversionFormula by remember(keyId) { mutableStateOf(existingItem?.gradeConversionFormula ?: "g * 9.5") }
    
    var boardRollNumber by remember(keyId) { mutableStateOf(existingItem?.boardRollNumber ?: "") }
    var boardRollCode by remember(keyId) { mutableStateOf(existingItem?.boardRollCode ?: "") }
    var marksheetIndexNumber by remember(keyId) { mutableStateOf(existingItem?.marksheetIndexNumber ?: "") }
    
    // Entrance Exam specific
    var ntaPercentile by remember(keyId) { mutableStateOf(existingItem?.ntaPercentile?.toString() ?: "") }
    var allIndiaRank by remember(keyId) { mutableStateOf(existingItem?.allIndiaRank?.toString() ?: "") }
    var categoryRank by remember(keyId) { mutableStateOf(existingItem?.categoryRank?.toString() ?: "") }
    
    // Pre-Matric specific
    var classNumber by remember(keyId) { mutableStateOf(existingItem?.classNumber ?: "") }
    
    // College specific
    var degreeType by remember(keyId) { mutableStateOf(existingItem?.degreeType ?: "") }
    
    // Auto-fill readiness (Part 5 Gaps A-C)
    var domicileState by remember(keyId) { mutableStateOf(existingItem?.domicileState ?: "") }
    var institutionDistrict by remember(keyId) { mutableStateOf(existingItem?.institutionDistrict ?: "") }
    var stream by remember(keyId) { mutableStateOf(existingItem?.stream ?: "") }

    var marksSubjects by remember(keyId) { 
        mutableStateOf(
            if (!existingItem?.subjectsJson.isNullOrEmpty()) SubjectSerializer.deserialize(existingItem!!.subjectsJson!!)
            else listOf(SubjectEntryState())
        ) 
    }

    // Dynamic Semester Duration and Generation States
    var durationYearsStr by remember(keyId) {
        val initialYears = if (existingSemesters.isNotEmpty()) (existingSemesters.size + 1) / 2 else 3
        mutableStateOf(initialYears.toString())
    }
    var semesterCountStr by remember(keyId) {
        val initialSems = if (existingSemesters.isNotEmpty()) existingSemesters.size else 6
        mutableStateOf(initialSems.toString())
    }

    // Track semesters UI state reactively
    var editableSemesters by remember(keyId, semesterCountStr) {
        val count = semesterCountStr.toIntOrNull() ?: 6
        val list = mutableListOf<EditableSemesterState>()
        for (i in 1..count) {
            val existing = existingSemesters.find { it.semesterNumber == i }
            list.add(
                EditableSemesterState(
                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                    semesterNumber = i,
                    semesterLabel = existing?.semesterLabel ?: "Semester $i",
                    rollNumber = existing?.rollNumber ?: "",
                    overrideRollNumber = existing?.overrideRollNumber ?: false,
                    subjects = if (existing?.subjectsJson != null) SubjectSerializer.deserialize(existing.subjectsJson) else listOf(SubjectEntryState()),
                    evaluationSystem = existing?.data?.substringBefore("|") ?: "Percentage",
                    totalCredits = existing?.totalCredits ?: 0.0,
                    earnedSgpa = existing?.earnedSgpa,
                    attachedDocumentId = existing?.attachedDocumentId,
                    attachedDocumentName = existing?.attachedDocumentName
                )
            )
        }
        mutableStateOf(list)
    }

    // Live propagation of first semester roll number
    fun updateSemesterRollNumber(index: Int, rawRoll: String) {
        val updated = editableSemesters.toMutableList()
        val sem = updated[index]
        val isOverridden = if (index > 0) true else sem.overrideRollNumber

        updated[index] = sem.copy(
            rollNumber = rawRoll,
            overrideRollNumber = isOverridden
        )

        // Propagation
        if (index == 0) {
            for (i in 1 until updated.size) {
                if (!updated[i].overrideRollNumber) {
                    updated[i] = updated[i].copy(rollNumber = rawRoll)
                }
            }
        }
        editableSemesters = updated
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        
        if (category == "Pre-Matric") {
            OutlinedTextField(
                value = classNumber,
                onValueChange = { classNumber = it },
                label = { Text("Class / Grade") },
                placeholder = { Text("e.g. Class 8, Grade 9") },
                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (category != "Entrance Examination") {
            OutlinedTextField(
                value = institutionName,
                onValueChange = { institutionName = it },
                label = { Text(if (category in listOf("Class 10", "Class 12", "Pre-Matric")) "School Name" else "Institution Name") },
                placeholder = { Text("e.g. St. Xavier's School, IIT Bombay") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
            
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = domicileState,
                    onValueChange = { domicileState = it },
                    label = { Text("State / Domicile") },
                    placeholder = { Text("e.g. Maharashtra") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = institutionDistrict,
                    onValueChange = { institutionDistrict = it },
                    label = { Text("District") },
                    placeholder = { Text("e.g. Mumbai") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (category in listOf("Class 10", "Class 12", "Entrance Examination")) {
            if (category == "Class 12") {
                OutlinedTextField(
                    value = stream,
                    onValueChange = { stream = it },
                    label = { Text("Stream / Subject Group") },
                    placeholder = { Text("e.g. Science (PCM), Commerce, Arts") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            val knownBoards = listOf(
                "CBSE", "CISCE (ICSE/ISC)", "NIOS", "IB (International Baccalaureate)",
                "WBCHSE", "BSEB (Bihar Board)", "MPBSE", "RBSE", "UPMSP", "MSBSHSE",
                "GSEB", "PSEB", "HPBOSE", "JKBOSE", "TBSE", "SEBA", "CHSE Odisha",
                "BIEAP", "BIETELANGANA", "KSEEB", "DGE Tamil Nadu", "DHSE Kerala",
                "JEE Main (NTA)", "JEE Advanced (IIT)", "NEET UG (NTA)", "GATE (IIT/IISc)",
                "CUET UG (NTA)", "CLAT", "CAT (IIM)", "Custom / Other"
            )

            var boardDropdownExpanded by remember { mutableStateOf(false) }
            var isCustomBoard by remember { mutableStateOf(conductingBody.isNotBlank() && !knownBoards.contains(conductingBody)) }

            ExposedDropdownMenuBox(
                expanded = boardDropdownExpanded,
                onExpandedChange = { boardDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                OutlinedTextField(
                    value = if (isCustomBoard) conductingBody else conductingBody,
                    onValueChange = { if (isCustomBoard) conductingBody = it },
                    readOnly = !isCustomBoard,
                    label = { Text(if (category == "Entrance Examination") "Exam Authority (e.g. NTA)" else "Board / Conducting Authority") },
                    leadingIcon = { Icon(Icons.Default.Verified, contentDescription = null) },
                    trailingIcon = { if (!isCustomBoard) ExposedDropdownMenuDefaults.TrailingIcon(boardDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                if (!isCustomBoard) {
                    ExposedDropdownMenu(expanded = boardDropdownExpanded, onDismissRequest = { boardDropdownExpanded = false }) {
                        knownBoards.forEach { board ->
                            DropdownMenuItem(
                                text = { Text(board) },
                                onClick = {
                                    if (board == "Custom / Other") {
                                        isCustomBoard = true
                                        conductingBody = ""
                                    } else {
                                        conductingBody = board
                                    }
                                    boardDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val yearRange = (currentYear downTo 1990).map { it.toString() }
            val yearError = conductingYear.isNotBlank() && (conductingYear.toIntOrNull() == null || conductingYear.toInt() < 1990 || conductingYear.toInt() > currentYear + 1)
            
            var yearDropdownExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = yearDropdownExpanded,
                onExpandedChange = { yearDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                OutlinedTextField(
                    value = conductingYear,
                    onValueChange = { conductingYear = it },
                    label = { Text("Passing Year") },
                    placeholder = { Text("e.g. 2024, 2025") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(yearDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    isError = yearError,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = yearDropdownExpanded, onDismissRequest = { yearDropdownExpanded = false }) {
                    yearRange.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year) },
                            onClick = {
                                conductingYear = year
                                yearDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
        
        if (category in listOf("Class 10", "Class 12")) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Registration & Identification", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColorPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = boardRollNumber,
                    onValueChange = { boardRollNumber = it },
                    label = { Text("Board Roll No") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = boardRollCode,
                        onValueChange = { boardRollCode = it },
                        label = { Text("Roll Code") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    InfoTooltipIcon(
                        title = "Roll Code vs Roll Number",
                        body = "State boards like WBCHSE, BSEB, and MPBSE use a two-part system. The Roll Code is your school's regional registration code (usually 6 digits). The Roll Number is your personal exam number. Both are required by state university portals."
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = marksheetIndexNumber,
                    onValueChange = { marksheetIndexNumber = it },
                    label = { Text("Marksheet Index Number") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                InfoTooltipIcon(
                    title = "Where is this number?",
                    body = "This is printed on your official marksheet, usually at the top-right corner (CBSE) or top-centre (ICSE/ISC). It is a 5–12 digit code. For state boards like WBCHSE it appears on the left edge as 'Index No.'."
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (category in listOf("College Degree", "University Degree")) {
            OutlinedTextField(
                value = degreeType,
                onValueChange = { degreeType = it },
                label = { Text("Degree / Course Title") },
                placeholder = { Text("e.g. B.Tech Computer Science, B.Sc Honours") },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (category == "Entrance Examination") {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Entrance Exam Results", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColorPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ntaPercentile,
                    onValueChange = { ntaPercentile = it },
                    label = { Text("NTA Percentile Score") },
                    placeholder = { Text("e.g. 99.12") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
                InfoTooltipIcon(
                    title = "What is a percentile?",
                    body = "Your NTA Percentile (e.g. 99.12) shows what percentage of students scored less than you. It is not your marks percentage. Your JEE Main scorecard shows this number prominently on the first page."
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = allIndiaRank,
                    onValueChange = { allIndiaRank = it },
                    label = { Text("All India Rank (CRL)") },
                    placeholder = { Text("e.g. 4231") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = categoryRank,
                    onValueChange = { categoryRank = it },
                    label = { Text("Category Rank") },
                    placeholder = { Text("e.g. 402") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = boardRollNumber,
                onValueChange = { boardRollNumber = it },
                label = { Text("Exam Application / Roll Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Text("Academic Structure Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColorPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = structureType == "Yearly",
                    onClick = { structureType = "Yearly" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Yearly Pattern")
                }
                SegmentedButton(
                    selected = structureType == "Semester",
                    onClick = { structureType = "Semester" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Semester Pattern")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Evaluation Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColorPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = evaluationSystem == "Percentage",
                    onClick = { evaluationSystem = "Percentage" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Percentage")
                }
                SegmentedButton(
                    selected = evaluationSystem == "Grade",
                    onClick = { evaluationSystem = "Grade" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Grade (GPA)")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            BoardMarksTable(
                evaluationSystem = evaluationSystem,
                subjects = marksSubjects,
                onSubjectsChange = { marksSubjects = it },
                showBestOfFiveToggle = category in listOf("Class 10", "Class 12"),
                useBestOfFive = useBestOfFive,
                onUseBestOfFiveChange = { useBestOfFive = it }
            )

            if (useBestOfFive && category in listOf("Class 10", "Class 12")) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = bestOfNText,
                        onValueChange = { bestOfNText = it },
                        label = { Text("Calculate Best of N Subjects") },
                        placeholder = { Text("e.g. 5 or 6") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                    InfoTooltipIcon(
                        title = "Best of N",
                        body = "Enter how many top-scoring subjects should be included in the total percentage calculation. Usually 5 for CBSE/ICSE."
                    )
                }
            }

            if (category in listOf("Class 10", "Class 12")) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Custom Percentage Formula", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    InfoTooltipIcon(
                        title = "Conversion Logic",
                        body = "CBSE uses (CGPA * 9.5) to find percentage. Some state boards use (Grade * 10). Use 'g' for your grade/CGPA in the formula. Example: g * 9.5"
                    )
                }
                OutlinedTextField(
                    value = gradeConversionFormula,
                    onValueChange = { gradeConversionFormula = it },
                    placeholder = { Text("e.g. g * 9.5") },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            if (structureType == "Semester") {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Course Duration & Semesters", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColorPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationYearsStr,
                        onValueChange = { 
                            durationYearsStr = it
                            val years = it.toIntOrNull() ?: 3
                            semesterCountStr = (years * 2).toString()
                        },
                        label = { Text("Duration (Years)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = semesterCountStr,
                        onValueChange = { semesterCountStr = it },
                        label = { Text("Total Semesters") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                editableSemesters.forEachIndexed { index, semester ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(semester.semesterLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (index == 0) {
                                    Text("(Primary Roll No Source)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = semester.rollNumber,
                                onValueChange = { updateSemesterRollNumber(index, it) },
                                label = { Text("Exam Roll Number") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                trailingIcon = {
                                    if (index > 0) {
                                        IconButton(onClick = {
                                            val updated = editableSemesters.toMutableList()
                                            updated[index] = updated[index].copy(
                                                overrideRollNumber = !updated[index].overrideRollNumber,
                                                rollNumber = if (updated[index].overrideRollNumber) editableSemesters[0].rollNumber else updated[index].rollNumber
                                            )
                                            editableSemesters = updated
                                        }) {
                                            Icon(
                                                imageVector = if (semester.overrideRollNumber) Icons.Default.LinkOff else Icons.Default.Link,
                                                contentDescription = "Toggle Link",
                                                tint = if (semester.overrideRollNumber) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            BoardMarksTable(
                                evaluationSystem = evaluationSystem,
                                subjects = semester.subjects,
                                onSubjectsChange = { newSubs ->
                                    val updated = editableSemesters.toMutableList()
                                    updated[index] = updated[index].copy(subjects = newSubs)
                                    editableSemesters = updated
                                }
                            )
                            
                            if (evaluationSystem == "Grade") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    val weightedSum = semester.subjects.sumOf { (it.obtainedMarks.toDoubleOrNull() ?: 0.0) * (it.creditPoints.toDoubleOrNull() ?: 1.0) }
                                    val totalCredits = semester.subjects.sumOf { if ((it.obtainedMarks.toDoubleOrNull() ?: 0.0) > 0) (it.creditPoints.toDoubleOrNull() ?: 1.0) else 0.0 }
                                    val sgpa = if (totalCredits > 0) weightedSum / totalCredits else 0.0
                                    Text("Est. SGPA: ", fontSize = 14.sp)
                                    Text(String.format("%.2f", sgpa), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
        
        val context = LocalContext.current
        var showAttachmentMenu by remember { mutableStateOf(false) }
        var showWalletSelector by remember { mutableStateOf(false) }
        
        var selectedExternalUri by remember { mutableStateOf<android.net.Uri?>(null) }
        var selectedExternalName by remember { mutableStateOf<String?>(null) }
        
        var selectedWalletDocId by remember { mutableStateOf<Int?>(null) }
        var selectedWalletDocName by remember { mutableStateOf<String?>(null) }

        val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                selectedExternalUri = uri
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            selectedExternalName = it.getString(nameIndex)
                        }
                    }
                }
            }
        }

        if (showWalletSelector) {
            val securityManager = remember { com.scholarvault.security.WalletSecurityManager(context) }
            com.scholarvault.security.WalletAuthenticationWrapper(
                securityManager = securityManager,
                onUnlockSuccess = {},
                onCancel = { showWalletSelector = false }
            ) {
                WalletDocumentSelectorDialog(
                    docViewModel = docViewModel,
                    onDismiss = { showWalletSelector = false },
                    onSelect = { docFile ->
                        selectedWalletDocId = docFile.id
                        selectedWalletDocName = docFile.name
                        selectedExternalUri = null
                        selectedExternalName = null
                        showWalletSelector = false
                    }
                )
            }
        }

        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            OutlinedButton(
                onClick = { showAttachmentMenu = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                if (selectedWalletDocName != null) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Linked: $selectedWalletDocName", fontWeight = FontWeight.Medium)
                } else if (selectedExternalName != null) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Selected: $selectedExternalName", fontWeight = FontWeight.Medium)
                } else {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Attach Document / Marksheet", fontWeight = FontWeight.Medium)
                }
            }
            
            if (showAttachmentMenu) {
                com.scholarvault.ui.components.UniversalAttachmentSelector(
                    onDismiss = { showAttachmentMenu = false },
                    onSelectFromDevice = {
                        showAttachmentMenu = false
                        filePickerLauncher.launch(arrayOf("*/*"))
                    },
                    onSelectFromWallet = {
                        showAttachmentMenu = false
                        showWalletSelector = true
                    },
                    onDragAndDrop = {
                        showAttachmentMenu = false
                        android.widget.Toast.makeText(context, "Drag & Drop zone opened (Simulation)", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                val titleString = when (category) {
                    "Pre-Matric" -> "Class $classNumber - $institutionName"
                    "Class 10", "Class 12" -> "$category Passing - $conductingBody"
                    "College Degree", "University Degree" -> "$degreeType - $institutionName"
                    "Entrance Examination" -> "$conductingBody Exam"
                    else -> institutionName.takeIf { it.isNotBlank() } ?: "Unknown Title"
                }

                val finalItem = AcademicItem(
                    id = existingItem?.id ?: java.util.UUID.randomUUID().toString(),
                    title = titleString,
                    type = category,
                    timelineDate = existingItem?.timelineDate ?: Date(),
                    institutionName = institutionName,
                    conductingBody = conductingBody,
                    conductingYear = conductingYear,
                    classNumber = classNumber,
                    degreeType = degreeType,
                    structureType = structureType,
                    evaluationSystem = evaluationSystem,
                    subjectsJson = SubjectSerializer.serialize(marksSubjects),
                    boardRollNumber = boardRollNumber,
                    boardRollCode = boardRollCode,
                    marksheetIndexNumber = marksheetIndexNumber,
                    useBestOfFive = useBestOfFive,
                    bestOfN = bestOfNText.toIntOrNull() ?: 5,
                    gradeConversionFormula = gradeConversionFormula,
                    ntaPercentile = ntaPercentile.toDoubleOrNull(),
                    allIndiaRank = allIndiaRank.toIntOrNull(),
                    categoryRank = categoryRank.toIntOrNull(),
                    domicileState = domicileState,
                    institutionDistrict = institutionDistrict,
                    stream = stream
                )

                val mappedSemesters = if (structureType == "Semester") {
                    editableSemesters.map { es ->
                        val weightedSum = es.subjects.sumOf { (it.obtainedMarks.toDoubleOrNull() ?: 0.0) * (it.creditPoints.toDoubleOrNull() ?: 1.0) }
                        val totalCredits = es.subjects.sumOf { if ((it.obtainedMarks.toDoubleOrNull() ?: 0.0) > 0) (it.creditPoints.toDoubleOrNull() ?: 1.0) else 0.0 }
                        val sgpa = if (totalCredits > 0) weightedSum / totalCredits else 0.0
                        
                        val dataStr = if (evaluationSystem == "Grade") String.format("%.2f SGPA", sgpa)
                        else {
                            val totalMax = es.subjects.sumOf { it.maxMarks.toDoubleOrNull() ?: 0.0 }
                            val totalObt = es.subjects.sumOf { it.obtainedMarks.toDoubleOrNull() ?: 0.0 }
                            val perc = if (totalMax > 0) (totalObt / totalMax) * 100 else 0.0
                            String.format("%.2f%%", perc)
                        }

                        com.scholarvault.data.model.Semester(
                            id = es.id,
                            courseId = finalItem.id,
                            semesterNumber = es.semesterNumber,
                            semesterLabel = es.semesterLabel,
                            rollNumber = es.rollNumber,
                            overrideRollNumber = es.overrideRollNumber,
                            subjectsJson = SubjectSerializer.serialize(es.subjects),
                            totalCredits = totalCredits,
                            earnedSgpa = if (evaluationSystem == "Grade") sgpa else null,
                            data = dataStr,
                            attachedDocumentId = es.attachedDocumentId,
                            attachedDocumentName = es.attachedDocumentName
                        )
                    }
                } else emptyList()

                onSave(finalItem, mappedSemesters, selectedExternalUri, selectedExternalName, selectedWalletDocId)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Save details", modifier = Modifier.padding(vertical = 4.dp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun WalletDocumentSelectorDialog(
    docViewModel: DocumentViewModel,
    onDismiss: () -> Unit,
    onSelect: (com.scholarvault.data.model.DocumentFile) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val securityManager = remember { com.scholarvault.security.WalletSecurityManager(context) }
    
    val allWalletFiles by docViewModel.rootFiles.collectAsState()
    val walletFiles = allWalletFiles.filter { it.tags.contains("wallet") || it.isEncrypted }

    com.scholarvault.security.WalletAuthenticationWrapper(
        securityManager = securityManager,
        onUnlockSuccess = {},
        onCancel = onDismiss
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Wallet Document") },
            text = {
                if (walletFiles.isEmpty()) {
                    Text("No wallet documents found.")
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                    ) {
                        items(walletFiles.size) { index ->
                            val file = walletFiles[index]
                            androidx.compose.material3.ListItem(
                                headlineContent = { Text(file.name) },
                                supportingContent = { Text("Stored in secure wallet") },
                                leadingContent = { Icon(Icons.Default.AttachFile, contentDescription = null) },
                                modifier = Modifier.clickable { onSelect(file) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}
