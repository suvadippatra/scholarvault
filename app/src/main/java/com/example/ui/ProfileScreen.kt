package com.scholarvault.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.scaleIn
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Canvas
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.Path
import kotlinx.coroutines.launch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scholarvault.data.model.ProfileExperience
import com.scholarvault.data.model.ProfileWork
import com.scholarvault.data.model.UserProfile
import com.scholarvault.ui.theme.LocalThemeController
import com.scholarvault.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

data class SocialLink(val platform: String, val url: String)

data class CustomField(val category: String, val paramName: String, val paramValue: String)

fun parseCustomFields(json: String): List<CustomField> {
    val list = mutableListOf<CustomField>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(CustomField(obj.getString("category"), obj.getString("paramName"), obj.getString("paramValue")))
        }
    } catch(e: Exception) {}
    return list
}

fun saveCustomFields(fields: List<CustomField>): String {
    val array = JSONArray()
    fields.forEach {
        val obj = JSONObject()
        obj.put("category", it.category)
        obj.put("paramName", it.paramName)
        obj.put("paramValue", it.paramValue)
        array.put(obj)
    }
    return array.toString()
}

fun parseSocialLinks(json: String): List<SocialLink> {
    val list = mutableListOf<SocialLink>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(SocialLink(obj.getString("platform"), obj.getString("url")))
        }
    } catch (e: Exception) {}
    return list
}

fun saveSocialLinks(links: List<SocialLink>): String {
    val array = JSONArray()
    links.forEach {
        val obj = JSONObject()
        obj.put("platform", it.platform)
        obj.put("url", it.url)
        array.put(obj)
    }
    return array.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val theme = LocalThemeController.current
    val isDark = theme.isDarkTheme
    val animationsEnabled = theme.animationsEnabled
    
    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFBFCFD)
    val cardBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFEEEEEE)
    val textColor = if (isDark) Color.White else Color.Black
    
    val profileFlow by viewModel.profileStream.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val dbProfile = profileFlow?.profile ?: UserProfile()
    val experiences = profileFlow?.experiences ?: emptyList()
    val works = profileFlow?.works ?: emptyList()

    var isEditingMode by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf(dbProfile) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val hasChanges = remember(profile, dbProfile) { profile != dbProfile }

    LaunchedEffect(dbProfile, isEditingMode) {
        if (!isEditingMode) {
            profile = dbProfile
        }
    }

    var showMoreDetails by remember { mutableStateOf(false) }
    var listVisible by remember { mutableStateOf(!animationsEnabled) }

    BackHandler(enabled = isEditingMode) {
        if (hasChanges) {
            showDiscardDialog = true
        } else {
            isEditingMode = false
        }
    }

    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) {
            delay(100)
            listVisible = true
        } else {
            listVisible = true
        }
    }

    val context = LocalContext.current

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("Are you sure you want to discard your unsaved changes?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    isEditingMode = false
                    profile = dbProfile
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showHelpDialog) {
        ProfileInstructionsSheet(
            onDismiss = { showHelpDialog = false },
            isDark = isDark
        )
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = { 
                    if (isEditingMode) {
                        if (hasChanges) {
                            showDiscardDialog = true
                        } else {
                            isEditingMode = false
                            profile = dbProfile
                        }
                    } else {
                        onBack() 
                    }
                },
                isBackButton = true,
                searchQuery = searchQuery,
                onSearchChange = { viewModel.updateSearchQuery(it) },
                showProfileIcon = false,
                showSearchBar = false
            ) {
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(
                        Icons.Default.HelpOutline, 
                        contentDescription = "Help",
                        tint = if (isDark) Color.White else Color.Black
                    )
                }
                IconButton(onClick = { 
                    if (isEditingMode) {
                        viewModel.updateProfile(profile)
                        isEditingMode = false
                    } else {
                        profile = dbProfile
                        isEditingMode = true
                    }
                }) {
                    Icon(
                        if (isEditingMode) Icons.Default.Check else Icons.Default.Edit, 
                        contentDescription = if(isEditingMode) "Save Profile" else "Edit Profile",
                        tint = if (isEditingMode) MaterialTheme.colorScheme.primary else (if (isDark) Color.White else Color.Black)
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedVisibility(
            visible = listVisible,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(400))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                if (!isEditingMode) {
                    item {
                        ProfileViewDashboard(
                            profile = dbProfile,
                            experiences = experiences,
                            works = works,
                            isDark = isDark,
                            textColor = textColor
                        )
                    }
                } else {
                    item {
                        HeaderCard(
                            profile = profile, 
                            isEditingMode = true, 
                            onProfileUpdate = { updatedProfile ->
                                profile = updatedProfile
                            },
                            textColor = textColor
                        )
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        ProfileSectionCard(title = "1. Core Identity & Demographics", isExpandedDefault = true, forceExpand = true) {
                            ProfileField("First Name", profile.firstName, true, icon = Icons.Default.Person) { profile = profile.copy(firstName = it) }
                            ProfileField("Middle Name", profile.middleName, true, icon = Icons.Default.Person) { profile = profile.copy(middleName = it) }
                            ProfileField("Last Name", profile.lastName, true, icon = Icons.Default.Person) { profile = profile.copy(lastName = it) }
                            ProfileDateField("Date of Birth", profile.dateOfBirth, true, icon = Icons.Default.DateRange) { profile = profile.copy(dateOfBirth = it) }
                            ProfileDropdownField("Gender", profile.gender, listOf("Male", "Female", "Other"), true, icon = Icons.Default.Person) { profile = profile.copy(gender = it) }
                            ProfileField("Mother Tongue", profile.motherTongue, true, icon = Icons.Default.MenuBook) { profile = profile.copy(motherTongue = it) }
                            ProfileDropdownField("Marital Status", profile.maritalStatus, listOf("Single", "Married", "Divorced", "Widowed"), true, icon = Icons.Default.Favorite) { profile = profile.copy(maritalStatus = it) }
                            ProfileDropdownField("Caste", profile.caste, listOf("General", "OBC", "SC", "ST", "Other"), true, icon = Icons.Default.Info) { profile = profile.copy(caste = it) }
                            ProfileDropdownField("Religion", profile.religion, listOf("Hinduism", "Islam", "Christianity", "Sikhism", "Buddhism", "Jainism", "Other"), true, icon = Icons.Default.Info) { profile = profile.copy(religion = it) }
                            CustomFieldsSection("Identity", profile, true) { profile = it }
                        }
                    }

                    item {
                        ProfileSectionCard(title = "2. Contact & Domicile Information", isExpandedDefault = false, forceExpand = true) {
                            ProfileField("Mobile Number", profile.mobileNumber, true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), icon = Icons.Default.Phone) { profile = profile.copy(mobileNumber = it) }
                            ProfileField("WhatsApp Number", profile.whatsappNumber, true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), icon = Icons.Default.Phone) { profile = profile.copy(whatsappNumber = it) }
                            ProfileField("Email", profile.email, true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), icon = Icons.Default.Email) { profile = profile.copy(email = it) }
                            ProfileField("Present Address", profile.presentAddress, true, singleLine = false, icon = Icons.Default.Home) { profile = profile.copy(presentAddress = it) }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Checkbox(
                                    checked = profile.isPermanentSameAsPresent,
                                    onCheckedChange = { profile = profile.copy(isPermanentSameAsPresent = it) }
                                )
                                Text("Permanent address is same as present")
                            }
                            if (!profile.isPermanentSameAsPresent) {
                                ProfileField("Permanent Address", profile.permanentAddress, true, singleLine = false, icon = Icons.Default.Home) { profile = profile.copy(permanentAddress = it) }
                            }
                            CustomFieldsSection("Contact", profile, true) { profile = it }
                        }
                    }

                    item {
                        ProfileSectionCard(title = "3. Guardianship & Emergency", isExpandedDefault = false, forceExpand = true) {
                            ProfileField("Primary Guardian Name", profile.guardianName, true, icon = Icons.Default.Person) { profile = profile.copy(guardianName = it) }
                            ProfileField("Relationship to Student", profile.guardianRelationship, true, icon = Icons.Default.Info) { profile = profile.copy(guardianRelationship = it) }
                            ProfileField("Guardian's Contact", profile.guardianNumber, true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), icon = Icons.Default.Phone) { profile = profile.copy(guardianNumber = it) }
                            ProfileField("Father's Name", profile.fatherName, true, icon = Icons.Default.Person) { profile = profile.copy(fatherName = it) }
                            ProfileField("Mother's Name", profile.motherName, true, icon = Icons.Default.Person) { profile = profile.copy(motherName = it) }
                            ProfileField("Family Income (Monthly)", profile.familyIncome, true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), icon = Icons.Default.Info) { profile = profile.copy(familyIncome = it) }
                            ProfileField("Emergency Contact", profile.emergencyContact, true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), icon = Icons.Default.Phone) { profile = profile.copy(emergencyContact = it) }
                            CustomFieldsSection("Guardianship", profile, true) { profile = it }
                        }
                    }

                    item {
                        ProfileSectionCard(title = "4. Academic & Research Footprint", isExpandedDefault = false, forceExpand = true) {
                            ProfileField("Professional Summary", profile.professionalSummary, true, singleLine = false, icon = Icons.Default.Info) { profile = profile.copy(professionalSummary = it) }
                            
                            val links = remember(profile.socialLinksJson) { parseSocialLinks(profile.socialLinksJson) }
                            Text("Social Links", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                            links.forEach { link ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(link.platform, fontWeight = FontWeight.Bold)
                                        Text(link.url, fontSize = 12.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = {
                                        val newLinks = links.filter { it != link }
                                        profile = profile.copy(socialLinksJson = saveSocialLinks(newLinks))
                                    }) {
                                        Icon(Icons.Default.Delete, tint = Color.Red, contentDescription = "Delete Link")
                                    }
                                }
                            }
                            var showAddLinkDialog by remember { mutableStateOf(false) }
                            if (showAddLinkDialog) {
                                var platform by remember { mutableStateOf("") }
                                var url by remember { mutableStateOf("") }
                                AlertDialog(
                                    onDismissRequest = { showAddLinkDialog = false },
                                    title = { Text("Add Link") },
                                    text = {
                                        Column {
                                            OutlinedTextField(value = platform, onValueChange = { platform = it }, label = { Text("Platform (e.g. LinkedIn, GitHub)") }, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Link URL") }, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth())
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            val newLinks = links.toMutableList()
                                            newLinks.add(SocialLink(platform, url))
                                            profile = profile.copy(socialLinksJson = saveSocialLinks(newLinks))
                                            showAddLinkDialog = false
                                        }) { Text("Add") }
                                    }
                                )
                            }
                            TextButton(onClick = { showAddLinkDialog = true }) {
                                Text("+ Add Link")
                            }
                            CustomFieldsSection("Academic", profile, true) { profile = it }
                        }
                    }

                    item {
                        ProfileSectionCard(title = "5. Edit Experiences & Works", isExpandedDefault = false, forceExpand = true) {
                            Text("Experiences", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                            experiences.forEach { exp ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(exp.role, fontWeight = FontWeight.Bold)
                                        Text("at ${exp.location} (${exp.duration})", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = { viewModel.removeExperience(exp) }) {
                                        Icon(Icons.Default.Delete, tint = Color.Red, contentDescription = "Delete")
                                    }
                                }
                            }
                            AddExperienceButton(onAdd = { r, d, l -> viewModel.addExperience(r, d, l) })
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                            
                            Text("Works", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                            works.forEach { work ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(work.title, fontWeight = FontWeight.Bold)
                                        Text(work.date, fontSize = 12.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = { viewModel.removeWork(work) }) {
                                        Icon(Icons.Default.Delete, tint = Color.Red, contentDescription = "Delete")
                                    }
                                }
                            }
                            AddWorkButton(onAdd = { t, d, w -> viewModel.addWork(t, d, w) })
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileViewDashboard(
    profile: UserProfile,
    experiences: List<ProfileExperience>,
    works: List<ProfileWork>,
    isDark: Boolean,
    textColor: Color
) {
    val cardBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF)
    val dividerColor = if (isDark) Color(0xFF444444) else Color(0xFFE0E0E0)
    val animationsEnabled = LocalThemeController.current.animationsEnabled
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(100.dp)
                ) {
                    if (profile.profilePicUri != null) {
                        CachedImage(
                            uri = Uri.parse(profile.profilePicUri),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                val fullName = listOf(profile.firstName, profile.middleName, profile.lastName).filter { it.isNotBlank() }.joinToString(" ")
                
                if (fullName.isNotBlank()) {
                    Text(
                        text = fullName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Student",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }

                val metaItems = listOf(profile.gender, profile.dateOfBirth).filter { it.isNotBlank() }
                if (metaItems.isNotEmpty()) {
                    Text(
                        text = metaItems.joinToString(" • "),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        // Responsive Grid
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            maxItemsInEachRow = 2
        ) {
            val cellModifier = Modifier.weight(1f).fillMaxWidth()

            // 1. Identity
            val identityContent = listOf(
                "Mother Tongue" to profile.motherTongue,
                "Marital Status" to profile.maritalStatus,
                "Caste" to profile.caste,
                "Religion" to profile.religion
            ).filter { it.second.isNotBlank() }

            if (identityContent.isNotEmpty()) {
                ViewModeCard("Core Identity", cardBg, textColor, cellModifier, animationsEnabled, 0) {
                    identityContent.forEach { (lbl, currVal) ->
                        ViewModeField(lbl, currVal, textColor)
                    }
                }
            }

            // 2. Contact & Address
            val address = if (profile.presentAddress.isNotBlank()) profile.presentAddress else if (profile.permanentAddress.isNotBlank()) profile.permanentAddress else ""
            val contactContent = listOf(
                "Mobile" to profile.mobileNumber,
                "WhatsApp" to profile.whatsappNumber,
                "Email" to profile.email,
                "Address" to address
            ).filter { it.second.isNotBlank() }

            if (contactContent.isNotEmpty()) {
                ViewModeCard("Contact & Domicile", cardBg, textColor, cellModifier, animationsEnabled, 1) {
                    contactContent.forEach { (lbl, currVal) ->
                        ViewModeField(lbl, currVal, textColor)
                    }
                }
            }
            
            // 3. Guardianship
            val guardianshipContent = listOf(
                "Guardian" to (if (profile.guardianName.isNotBlank() && profile.guardianRelationship.isNotBlank()) "${profile.guardianName} (${profile.guardianRelationship})" else profile.guardianName),
                "Contact" to profile.guardianNumber,
                "Father" to profile.fatherName,
                "Mother" to profile.motherName,
                "Family Income" to if (profile.familyIncome.isNotBlank()) "₹${profile.familyIncome}" else "",
                "Emergency" to profile.emergencyContact
            ).filter { it.second.isNotBlank() }

            if (guardianshipContent.isNotEmpty()) {
                ViewModeCard("Guardianship", cardBg, textColor, cellModifier, animationsEnabled, 2) {
                    guardianshipContent.forEach { (lbl, currVal) ->
                        ViewModeField(lbl, currVal, textColor)
                    }
                }
            }

            // 4. Custom Fields (if any)
            val customFields = parseCustomFields(profile.customFieldsJson)
            if (customFields.isNotEmpty()) {
                ViewModeCard("Additional Details", cardBg, textColor, Modifier.fillMaxWidth(), animationsEnabled, 3) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 2
                    ) {
                        customFields.forEach {
                            ViewModeField(it.paramName, it.paramValue, textColor, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        val links = remember(profile.socialLinksJson) { parseSocialLinks(profile.socialLinksJson) }
        val profSummary = profile.professionalSummary.trim()
        
        if (profSummary.isNotBlank() || links.isNotEmpty()) {
            ViewModeCard("Academic & Professional Summary", cardBg, textColor, Modifier.fillMaxWidth(), animationsEnabled, 4) {
                if (profSummary.isNotBlank()) {
                    Text(
                        text = profSummary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (links.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        links.forEach { link ->
                             val platLower = link.platform.lowercase()
                             val col = when {
                                 platLower.contains("linkedin") -> Color(0xFF0077B5)
                                 platLower.contains("github") -> Color(0xFF333333)
                                 platLower.contains("orcid") -> Color(0xFFA6CE39)
                                 platLower.contains("scholar") -> Color(0xFF4285F4)
                                 platLower.contains("instagram") -> Color(0xFFC13584)
                                 platLower.contains("twit") || platLower.contains("x") -> Color(0xFF1DA1F2)
                                 platLower.contains("face") -> Color(0xFF1877F2)
                                 platLower.contains("you") -> Color(0xFFFF0000)
                                 else -> Color.Gray
                             }
                             val context = LocalContext.current
                             val handler = androidx.compose.ui.platform.LocalUriHandler.current
                             Box(
                                 modifier = Modifier
                                     .size(40.dp)
                                     .clip(CircleShape)
                                     .background(col)
                                     .clickable { try { handler.openUri(link.url) } catch(e: Exception){} },
                                 contentAlignment = Alignment.Center
                             ) {
                                 Icon(Icons.Default.Link, contentDescription = link.platform, tint = Color.White, modifier = Modifier.size(20.dp))
                             }
                        }
                    }
                }
            }
        }
        
        if (experiences.isNotEmpty()) {
            ViewModeCard("Experience", cardBg, textColor, Modifier.fillMaxWidth(), animationsEnabled, 5) {
                experiences.forEachIndexed { index, exp ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(exp.role, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        Text("${exp.location} • ${exp.duration}", fontSize = 14.sp, color = Color.Gray)
                    }
                    if (index < experiences.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = dividerColor)
                    }
                }
            }
        }

        if (works.isNotEmpty()) {
            ViewModeCard("Works & Projects", cardBg, textColor, Modifier.fillMaxWidth(), animationsEnabled, 6) {
                works.forEachIndexed { index, work ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(work.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                            Text(work.date, fontSize = 14.sp, color = Color.Gray)
                        }
                        Icon(if (work.isWebLink) Icons.Default.Language else Icons.Default.Image, contentDescription = "Work Type", tint = Color.Gray, modifier = Modifier.size(24.dp))
                    }
                    if (index < works.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = dividerColor)
                    }
                }
            }
        }
    }
}

@Composable
fun ViewModeCard(
    title: String,
    cardBg: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = false,
    index: Int = 0,
    content: @Composable ColumnScope.() -> Unit
) {
    var visible by remember { mutableStateOf(!animationsEnabled) }
    
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) {
            delay(100 + index * 100L)
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + scaleIn(
            initialScale = 0.95f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        modifier = modifier
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                content()
            }
        }
    }
}

@Composable
fun ViewModeField(label: String, value: String, textColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 15.sp, color = textColor, modifier = Modifier.padding(top = 2.dp))
    }
}

fun copyUriToInternalStorage(context: android.content.Context, uri: Uri, prefix: String): String? {
    return try {
        val dir = context.filesDir
        try {
            dir.listFiles()?.forEach { file ->
                if (file.name.startsWith(prefix + "_")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val extension = try {
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType != null) {
                android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "png"
            } else {
                val path = uri.path
                if (path != null && path.contains(".")) {
                    path.substringAfterLast(".", "png")
                } else {
                    "png"
                }
            }
        } catch (e: Exception) {
            "png"
        }
        
        val filename = "${prefix}_${System.currentTimeMillis()}.$extension"
        val destinationFile = java.io.File(dir, filename)
        
        var copySuccess = false
        val inputStream = if (uri.scheme == "file" && uri.path != null) {
            java.io.FileInputStream(java.io.File(uri.path!!))
        } else {
            context.contentResolver.openInputStream(uri)
        }
        inputStream?.use { stream ->
            java.io.FileOutputStream(destinationFile).use { outputStream ->
                stream.copyTo(outputStream)
                copySuccess = true
            }
        }
        if (copySuccess && destinationFile.exists() && destinationFile.length() > 0) {
            Uri.fromFile(destinationFile).toString()
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

enum class CropState {
    IDLE,
    CROPPING,
    COMPRESSING
}

fun performBitmapCrop(
    bmp: android.graphics.Bitmap,
    scale: Float,
    offset: androidx.compose.ui.geometry.Offset,
    rotation: Float,
    screenSize: androidx.compose.ui.geometry.Size,
    viewportSize: androidx.compose.ui.geometry.Size
): android.graphics.Bitmap {
    val canvasWidth = screenSize.width
    val canvasHeight = screenSize.height

    val viewportWidth = viewportSize.width.toInt().coerceAtLeast(1)
    val viewportHeight = viewportSize.height.toInt().coerceAtLeast(1)

    val csX = canvasWidth / 2f
    val csY = canvasHeight / 2f

    val imageWidth = bmp.width.toFloat()
    val imageHeight = bmp.height.toFloat()
    val scaleFitX = canvasWidth / imageWidth
    val scaleFitY = canvasHeight / imageHeight
    val initialScale = minOf(scaleFitX, scaleFitY)

    val startX = (canvasWidth - imageWidth * initialScale) / 2f
    val startY = (canvasHeight - imageHeight * initialScale) / 2f

    val resultBitmap = android.graphics.Bitmap.createBitmap(viewportWidth, viewportHeight, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(resultBitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    canvas.translate(-(canvasWidth - viewportWidth) / 2f, -(canvasHeight - viewportHeight) / 2f)
    canvas.translate(offset.x, offset.y)
    canvas.scale(scale, scale, csX, csY)
    canvas.rotate(rotation, csX, csY)

    canvas.translate(startX, startY)
    canvas.scale(initialScale, initialScale)

    canvas.drawBitmap(bmp, 0f, 0f, android.graphics.Paint().apply {
        isFilterBitmap = true
    })

    return resultBitmap
}

suspend fun compressBitmapToTargetRange(
    bitmap: android.graphics.Bitmap,
    minKB: Int,
    maxKB: Int
): ByteArray {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var quality = 95
        var bestBytes: ByteArray? = null
        var bestSizeKB = Int.MAX_VALUE
        
        val outputBytes = java.io.ByteArrayOutputStream()
        
        while (quality >= 5) {
            outputBytes.reset()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputBytes)
            val bytes = outputBytes.toByteArray()
            val kb = bytes.size / 1024
            
            if (kb in minKB..maxKB) {
                return@withContext bytes
            }
            
            if (kb < minKB) {
                if (kb < bestSizeKB) {
                    bestBytes = bytes
                    bestSizeKB = kb
                }
                break
            }
            
            if (kb < bestSizeKB) {
                bestBytes = bytes
                bestSizeKB = kb
            }
            
            quality -= 5
        }
        
        var resultBytes = bestBytes ?: outputBytes.toByteArray()
        var currentScale = 0.95f
        
        while (resultBytes.size / 1024 > maxKB && currentScale >= 0.3f) {
            val nextWidth = (bitmap.width * currentScale).toInt().coerceAtLeast(10)
            val nextHeight = (bitmap.height * currentScale).toInt().coerceAtLeast(10)
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, nextWidth, nextHeight, true)
            
            var scaledQuality = 80
            while (scaledQuality >= 10) {
                outputBytes.reset()
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, scaledQuality, outputBytes)
                val tempBytes = outputBytes.toByteArray()
                val tempKb = tempBytes.size / 1024
                if (tempKb <= maxKB) {
                    resultBytes = tempBytes
                    break
                }
                scaledQuality -= 10
            }
            
            if (outputBytes.size() / 1024 <= maxKB) {
                resultBytes = outputBytes.toByteArray()
                break
            }
            
            currentScale -= 0.05f
        }
        
        resultBytes
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HeaderCard(
    profile: UserProfile,
    isEditingMode: Boolean,
    onProfileUpdate: (UserProfile) -> Unit,
    textColor: Color
) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val isDark = LocalThemeController.current.isDarkTheme

    var activeCropUri by remember { mutableStateOf<Uri?>(null) }
    var cropType by remember { mutableStateOf("profile_pic") } // "profile_pic" or "digital_sign"
    var cropState by remember { mutableStateOf(CropState.IDLE) }; var showZoomedProfilePic by remember { mutableStateOf(false) }

    val picLauncher = rememberLauncherForActivityResult(object : ActivityResultContracts.GetContent() {
        override fun createIntent(context: android.content.Context, input: String): android.content.Intent {
            return super.createIntent(context, input).apply {
                putExtra(android.content.Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
            }
        }
    }) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri)
            val isValid = mimeType == "image/jpeg" || mimeType == "image/png" || mimeType == "image/jpg"
            if (isValid) {
                activeCropUri = uri
                cropType = "profile_pic"
                cropState = CropState.CROPPING
            } else {
                android.widget.Toast.makeText(context, "Only JPG, JPEG, and PNG formats are allowed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val signLauncher = rememberLauncherForActivityResult(object : ActivityResultContracts.GetContent() {
        override fun createIntent(context: android.content.Context, input: String): android.content.Intent {
            return super.createIntent(context, input).apply {
                putExtra(android.content.Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
            }
        }
    }) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri)
            val isValid = mimeType == "image/jpeg" || mimeType == "image/png" || mimeType == "image/jpg"
            if (isValid) {
                activeCropUri = uri
                cropType = "digital_sign"
                cropState = CropState.CROPPING
            } else {
                android.widget.Toast.makeText(context, "Only JPG, JPEG, and PNG formats are allowed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (activeCropUri != null) {
        Dialog(
            onDismissRequest = {
                activeCropUri = null
                cropState = CropState.IDLE
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val screenWidth = maxWidth
                val screenHeight = maxHeight
                
                var sourceBitmap by remember(activeCropUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                val scope = rememberCoroutineScope()
                
                LaunchedEffect(activeCropUri) {
                    if (activeCropUri != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                context.contentResolver.openInputStream(activeCropUri!!)?.use { stream ->
                                    sourceBitmap = android.graphics.BitmapFactory.decodeStream(stream)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                
                if (sourceBitmap == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    var scale by remember { mutableStateOf(1.0f) }
                    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    var rotationState by remember { mutableStateOf(0f) }
                    val density = LocalDensity.current
                    
                    AnimatedVisibility(
                        visible = cropState == CropState.CROPPING,
                        enter = fadeIn(tween(300)),
                        exit = fadeOut(tween(300))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                                .statusBarsPadding()
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = {
                                activeCropUri = null
                                cropState = CropState.IDLE
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                            }
                            Text(
                                text = if (cropType == "profile_pic") "Crop Profile Photo" else "Crop Digital Signature",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clipToBounds()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, rotationChange ->
                                        scale = (scale * zoom).coerceIn(0.5f, 10.0f)
                                        offset = offset + pan
                                        rotationState += rotationChange
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val bmp = sourceBitmap ?: return@Canvas
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                
                                val imageWidth = bmp.width.toFloat()
                                val imageHeight = bmp.height.toFloat()
                                
                                val scaleFitX = canvasWidth / imageWidth
                                val scaleFitY = canvasHeight / imageHeight
                                val initialScale = minOf(scaleFitX, scaleFitY)
                                
                                val drawW = imageWidth * initialScale
                                val drawH = imageHeight * initialScale
                                
                                val startX = (canvasWidth - drawW) / 2f
                                val startY = (canvasHeight - drawH) / 2f
                                
                                val csX = canvasWidth / 2f
                                val csY = canvasHeight / 2f
                                
                                withTransform({
                                    translate(offset.x, offset.y)
                                    scale(scale, scale, pivot = androidx.compose.ui.geometry.Offset(csX, csY))
                                    rotate(rotationState, pivot = androidx.compose.ui.geometry.Offset(csX, csY))
                                }) {
                                    drawImage(
                                        image = bmp.asImageBitmap(),
                                        dstOffset = androidx.compose.ui.unit.IntOffset(startX.toInt(), startY.toInt()),
                                        dstSize = androidx.compose.ui.unit.IntSize(drawW.toInt(), drawH.toInt())
                                    )
                                }
                                
                                val viewportWidth = canvasWidth * 0.85f
                                val viewportHeight = if (cropType == "profile_pic") {
                                    viewportWidth
                                } else {
                                    viewportWidth / 3f
                                }
                                
                                val left = (canvasWidth - viewportWidth) / 2f
                                val top = (canvasHeight - viewportHeight) / 2f
                                val right = left + viewportWidth
                                val bottom = top + viewportHeight
                                
                                val path = Path().apply {
                                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, canvasWidth, canvasHeight))
                                }
                                val viewportPath = Path().apply {
                                    addRect(androidx.compose.ui.geometry.Rect(left, top, right, bottom))
                                }
                                
                                val differencePath = Path.combine(
                                    PathOperation.Difference,
                                    path,
                                    viewportPath
                                )
                                
                                drawPath(
                                    path = differencePath,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                                
                                drawRect(
                                    color = Color.White,
                                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                    size = androidx.compose.ui.geometry.Size(viewportWidth, viewportHeight),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF121212)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Tilt", tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    androidx.compose.material3.Slider(
                                        value = rotationState,
                                        onValueChange = { rotationState = it },
                                        valueRange = -180f..180f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 24.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            scale = 1.0f
                                            offset = androidx.compose.ui.geometry.Offset.Zero
                                            rotationState = 0f
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Reset")
                                    }
                                
                                val view = androidx.compose.ui.platform.LocalView.current
                                Button(
                                    onClick = {
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                        cropState = CropState.COMPRESSING
                                        scope.launch {
                                            try {
                                                val sizePx = density.run {
                                                    val w = (screenWidth * 0.85f).toPx()
                                                    val h = if (cropType == "profile_pic") w else w / 3f
                                                    androidx.compose.ui.geometry.Size(w, h)
                                                }
                                                val screenPx = density.run {
                                                    androidx.compose.ui.geometry.Size(screenWidth.toPx(), screenHeight.toPx())
                                                }
                                                
                                                val sourceB = sourceBitmap
                                                if (sourceB != null) {
                                                    val cropped = withContext(Dispatchers.Default) {
                                                        performBitmapCrop(
                                                            bmp = sourceB,
                                                            scale = scale,
                                                            offset = offset,
                                                            rotation = rotationState,
                                                            screenSize = screenPx,
                                                            viewportSize = sizePx
                                                        )
                                                    }
                                                    
                                                    val scaled = withContext(Dispatchers.Default) {
                                                        if (cropType == "profile_pic") {
                                                            android.graphics.Bitmap.createScaledBitmap(cropped, 400, 400, true)
                                                        } else {
                                                            android.graphics.Bitmap.createScaledBitmap(cropped, 300, 100, true)
                                                        }
                                                    }
                                                    
                                                    val minKB = if (cropType == "profile_pic") 10 else 5
                                                    val maxKB = if (cropType == "profile_pic") 50 else 15
                                                    
                                                    val compressedBytes = compressBitmapToTargetRange(scaled, minKB, maxKB)
                                                    
                                                    val internalPath = withContext(Dispatchers.IO) {
                                                        val dir = context.filesDir
                                                        try {
                                                            dir.listFiles()?.forEach { file ->
                                                                if (file.name.startsWith(cropType + "_")) {
                                                                    file.delete()
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                        
                                                        val filename = "${cropType}_${System.currentTimeMillis()}.jpg"
                                                        val destFile = java.io.File(dir, filename)
                                                        java.io.FileOutputStream(destFile).use { outStream ->
                                                            outStream.write(compressedBytes)
                                                        }
                                                        Uri.fromFile(destFile).toString()
                                                    }
                                                    
                                                    if (internalPath != null) {
                                                        onProfileUpdate(
                                                            if (cropType == "profile_pic") {
                                                                profile.copy(profilePicUri = internalPath)
                                                             } else {
                                                                profile.copy(digitalSignUri = internalPath)
                                                             }
                                                        )
                                                    }
                                                }
                                                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                                activeCropUri = null
                                                cropState = CropState.IDLE
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                android.widget.Toast.makeText(context, "Error saving cropped image: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                activeCropUri = null
                                                cropState = CropState.IDLE
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Next")
                                }
                            }
                        }
                    }
                    }
                }
                
                if (cropState == CropState.COMPRESSING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Compressing and optimizing image assets...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

val contentBox = @Composable {
        Box(modifier = Modifier.fillMaxWidth().padding(if (isEditingMode) 0.dp else 16.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(if (profile.profilePicUri != null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer)
                        .clickable { if (isEditingMode) picLauncher.launch("image/*") else showZoomedProfilePic = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.profilePicUri != null) {
                        CachedImage(uri = Uri.parse(profile.profilePicUri))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    
                    if (isEditingMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile Picture", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                if (profile.digitalSignUri != null) {
                    Box(
                        modifier = Modifier
                            .height(64.dp)
                            .clickable(enabled = isEditingMode) { signLauncher.launch("image/*") }
                    ) {
                        CachedImage(uri = Uri.parse(profile.digitalSignUri), contentScale = ContentScale.Fit)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (isEditingMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF333333) else Color(0xFFEAEAEA))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .clickable { signLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Edit, contentDescription = "Upload Signature", tint = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Upload Signature", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                val fullName = listOf(profile.firstName, profile.middleName, profile.lastName).filter { it.isNotBlank() }.joinToString(" ")
                
                if (fullName.isNotBlank() || isEditingMode) {
                    Text(
                        text = if (fullName.isBlank()) "Student" else fullName,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                
                val addressDetails = listOf(profile.presentAddress).filter { it.isNotBlank() }.joinToString(", ")
                if (addressDetails.isNotBlank()) {
                    Text(
                        text = addressDetails,
                        fontSize = 14.sp,
                        color = if (isDark) Color.LightGray else Color.DarkGray,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                } else if (isEditingMode) {
                    Text(
                        text = "No address provided",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                if (!isEditingMode) {
                    val links = remember(profile.socialLinksJson) { parseSocialLinks(profile.socialLinksJson) }
                    if (links.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            links.forEach { link ->
                                val platLower = link.platform.lowercase()
                                val col = when {
                                    platLower.contains("linkedin") -> Color(0xFF0077B5)
                                    platLower.contains("github") -> Color(0xFF333333)
                                    platLower.contains("orcid") -> Color(0xFFA6CE39)
                                    platLower.contains("scholar") -> Color(0xFF4285F4)
                                    platLower.contains("instagram") -> Color(0xFFC13584)
                                    platLower.contains("twit") || platLower.contains("x") -> Color(0xFF1DA1F2)
                                    platLower.contains("face") -> Color(0xFF1877F2)
                                    platLower.contains("you") -> Color(0xFFFF0000)
                                    else -> Color.Gray
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .combinedClickable(
                                            onClick = { try { uriHandler.openUri(link.url) } catch(e: Exception){} },
                                            onLongClick = { 
                                                clipboardManager.setText(AnnotatedString(link.url))
                                                android.widget.Toast.makeText(context, "Link copied", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = link.platform, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (!isEditingMode) {
        Surface(
            color = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            contentBox()
        }
    } else {
        contentBox()
    }

    if (showZoomedProfilePic) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showZoomedProfilePic = false }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.padding(16.dp).width(300.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showZoomedProfilePic = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(240.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (profile.profilePicUri != null) {
                                CachedImage(
                                    uri = Uri.parse(profile.profilePicUri),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(160.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileSectionCard(
    title: String,
    isExpandedDefault: Boolean = false,
    forceExpand: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(isExpandedDefault) }
    val isDark = LocalThemeController.current.isDarkTheme
    val isActuallyExpanded = expanded || forceExpand
    val rotation by animateFloatAsState(targetValue = if (isActuallyExpanded) 180f else 0f, label = "Chevron Rotation")

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !forceExpand) { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDark) Color.White else Color.Black
                )
                if (!forceExpand) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }
            AnimatedVisibility(visible = isActuallyExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    content()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun CustomFieldsSection(
    category: String,
    profile: UserProfile,
    isEditing: Boolean,
    onUpdate: (UserProfile) -> Unit
) {
    val fields = remember(profile.customFieldsJson) { parseCustomFields(profile.customFieldsJson) }
    val categoryFields = fields.filter { it.category == category }

    categoryFields.forEach { cf ->
        if (isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                ProfileField(cf.paramName, cf.paramValue, isEditing = true, modifier = Modifier.weight(1f), icon = Icons.Default.Label) { updatedVal -> 
                    val newFields = fields.map { if (it == cf) it.copy(paramValue = updatedVal) else it }
                    onUpdate(profile.copy(customFieldsJson = saveCustomFields(newFields)))
                }
                IconButton(onClick = {
                    val newFields = fields.filter { it != cf }
                    onUpdate(profile.copy(customFieldsJson = saveCustomFields(newFields)))
                }) {
                    Icon(Icons.Default.Delete, tint = Color.Red, contentDescription = "Delete Field")
                }
            }
        } else {
            ProfileField(cf.paramName, cf.paramValue, isEditing = false) {}
        }
    }

    if (isEditing) {
        var showDialog by remember { mutableStateOf(false) }
        if (showDialog) {
            var paramName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Add Custom Field") },
                text = {
                    OutlinedTextField(value = paramName, onValueChange = { paramName = it }, label = { Text("Parameter Name") }, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth())
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (paramName.isNotBlank()) {
                            val newFields = fields.toMutableList()
                            newFields.add(CustomField(category, paramName, ""))
                            onUpdate(profile.copy(customFieldsJson = saveCustomFields(newFields)))
                        }
                        showDialog = false
                    }) { Text("Add") }
                }
            )
        }
        TextButton(onClick = { showDialog = true }, modifier = Modifier.padding(top = 4.dp)) {
            Text("+ Add custom field")
        }
    }
}

@Composable
fun ProfileField(
    label: String, 
    value: String, 
    isEditing: Boolean, 
    singleLine: Boolean = true, 
    modifier: Modifier = Modifier, 
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onValueChange: (String) -> Unit
) {
    if (isEditing) {
        var text by remember(isEditing) { mutableStateOf(value) }
        
        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                onValueChange(it)
            },
            label = { Text(label) },
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = icon?.let { { Icon(it, contentDescription = null) } }
        )
    } else {
        if (value.isNotEmpty()) {
            Column(modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(label, fontSize = 12.sp, color = Color.Gray)
                Text(value, fontSize = 16.sp, color = if (LocalThemeController.current.isDarkTheme) Color.White else Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDropdownField(
    label: String,
    value: String,
    options: List<String>,
    isEditing: Boolean,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onValueChange: (String) -> Unit
) {
    if (isEditing) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                leadingIcon = icon?.let { { Icon(it, contentDescription = null) } },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    } else {
        if (value.isNotEmpty()) {
            Column(modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(label, fontSize = 12.sp, color = Color.Gray)
                Text(value, fontSize = 16.sp, color = if (LocalThemeController.current.isDarkTheme) Color.White else Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDateField(
    label: String, 
    value: String, 
    isEditing: Boolean, 
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onValueChange: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        onValueChange(sdf.format(Date(millis)))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (isEditing) {
        var text by remember(isEditing) { mutableStateOf(value) }
        
        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                onValueChange(it) 
            },
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = icon?.let { { Icon(it, contentDescription = null) } },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                }
            }
        )
    } else {
        if (value.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(label, fontSize = 12.sp, color = Color.Gray)
                Text(value, fontSize = 16.sp, color = if (LocalThemeController.current.isDarkTheme) Color.White else Color.Black)
            }
        }
    }
}

@Composable
fun ExperienceCard(exp: ProfileExperience, isDark: Boolean) {
    val bg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0E0)
    val textMain = if (isDark) Color.White else Color.Black
    
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(exp.role, fontSize = 16.sp, color = textMain)
                Text("at ${exp.location}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textMain)
            }
            Text(exp.duration, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun WorkCard(work: ProfileWork, isDark: Boolean) {
    val bg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0E0)
    val textMain = if (isDark) Color.White else Color.Black
    
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(work.title, fontSize = 16.sp, color = textMain)
                Text("On ${work.date}", fontSize = 14.sp, color = Color.Gray)
            }
            Icon(Icons.Default.Star, contentDescription = null, tint = textMain, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun AddExperienceButton(onAdd: (String, String, String) -> Unit) {
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        var role by remember { mutableStateOf("") }
        var dur by remember { mutableStateOf("") }
        var loc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text("Add Experience") },
            text = {
                Column {
                    OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role / Job") }, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Location / Status") }, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dur, onValueChange = { dur = it }, label = { Text("Duration (e.g. 2023-2025)") }, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = { onAdd(role, dur, loc); dialogOpen = false }) { Text("Add") }
            }
        )
    }

    OutlinedButton(onClick = { dialogOpen = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Icon(Icons.Default.Add, contentDescription = "Add Experience")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Add Experience")
    }
}

@Composable
fun AddWorkButton(onAdd: (String, String, Boolean) -> Unit) {
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        var title by remember { mutableStateOf("") }
        var date by remember { mutableStateOf("") }
        var isWeb by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text("Add Work") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Checkbox(checked = isWeb, onCheckedChange = { isWeb = it })
                        Text("Is Web/Code Project? (Uncheck for Graphic/Design)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onAdd(title, date, isWeb); dialogOpen = false }) { Text("Add") }
            }
        )
    }

    OutlinedButton(onClick = { dialogOpen = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Icon(Icons.Default.Add, contentDescription = "Add Work")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Add Work")
    }
}

@Composable
fun CachedImage(uri: Uri, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val theme = LocalThemeController.current
    val animationsEnabled = theme.animationsEnabled
    
    coil.compose.SubcomposeAsyncImage(
        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
            .data(uri)
            .crossfade(animationsEnabled)
            .build(),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = contentScale,
        loading = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
        },
        error = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Gray)
            }
        }
    )
}
