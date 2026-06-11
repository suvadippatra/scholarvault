package com.scholarvault.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.clickable
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.theme.LocalThemeController
import com.scholarvault.ui.viewmodel.DocumentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewWalletScreen(
    docViewModel: DocumentViewModel,
    onBack: () -> Unit,
    onNavigateToCategory: (Int) -> Unit,
    onNavigateToCard: (Int) -> Unit
) {
    val categories by docViewModel.allCategories.collectAsState()
    val allCards by docViewModel.allCards.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryTitle by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopSearchBar(
            onOpenDrawer = onBack,
            isBackButton = true
        )

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 120.dp) 
            ) {
                // Group Cards by Category ID
                val groupedCards = allCards.groupBy { it.categoryId }

                items(categories.size) { index ->
                    val category = categories[index]
                    val cards = groupedCards[category.id] ?: emptyList()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Category Header
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToCategory(category.id) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = category.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { onNavigateToCategory(category.id) }) {
                                    Text("View")
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${cards.size} Cards", 
                                fontSize = 16.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (cards.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val cardNames = cards.take(3).joinToString(", ") { it.title }
                                Text(
                                    text = "Includes: $cardNames",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            Button(
                onClick = { showAddCategoryDialog = true },
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Category")
            }
        }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryTitle,
                    onValueChange = { newCategoryTitle = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryTitle.isNotBlank()) {
                        docViewModel.insertCategory(com.scholarvault.data.model.WalletCategory(title = newCategoryTitle.trim(), iconName = "label"))
                        newCategoryTitle = ""
                        showAddCategoryDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WalletCategoryScreen(
    categoryId: Int,
    docViewModel: DocumentViewModel,
    onBack: () -> Unit,
    onNavigateToCard: (Int) -> Unit,
    onAddCard: (Int) -> Unit
) {
    val categories by docViewModel.allCategories.collectAsState()
    val allCards by docViewModel.allCards.collectAsState()
    
    val category = categories.find { it.id == categoryId }
    val cards = allCards.filter { it.categoryId == categoryId }
    
    var categoryToDelete by remember { mutableStateOf<com.scholarvault.data.model.WalletCategory?>(null) }
    var categoryToEdit by remember { mutableStateOf<com.scholarvault.data.model.WalletCategory?>(null) }
    var editCategoryTitle by remember { mutableStateOf("") }
    
    val animationsEnabled = LocalThemeController.current.animationsEnabled
    var showCards by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) {
            kotlinx.coroutines.delay(100)
            showCards = true
        } else {
            showCards = true
        }
    }

    if (category == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Category not found")
        }
        return
    }

    if (categoryToEdit != null) {
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("Edit Category") },
            text = {
                OutlinedTextField(
                    value = editCategoryTitle,
                    onValueChange = { editCategoryTitle = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editCategoryTitle.isNotBlank()) {
                            docViewModel.insertCategory(categoryToEdit!!.copy(title = editCategoryTitle))
                            categoryToEdit = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category?") },
            text = { Text("Are you sure you want to delete '${categoryToDelete!!.title}' and all its cards? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        docViewModel.deleteCategory(categoryToDelete!!)
                        categoryToDelete = null
                        onBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        com.scholarvault.ui.components.TopSearchBar(
            onOpenDrawer = onBack,
            isBackButton = true,
            title = category.title,
            showProfileIcon = false,
            actions = {
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isTabletScreen = configuration.screenWidthDp >= 600

                var showHelpDialog by remember { mutableStateOf(false) }

                // "?" Button to teach how to use
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Help")
                }

                // Add card button directly next to option actions
                IconButton(onClick = { onAddCard(category.id) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add New Card", tint = MaterialTheme.colorScheme.primary)
                }

                if (isTabletScreen) {
                    // Show edit/delete icon buttons directly AND keep the triple dot MoreOptions button visible so both pathways are present for tablets
                    IconButton(onClick = {
                        editCategoryTitle = category.title
                        categoryToEdit = category
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Category")
                    }
                    IconButton(onClick = {
                        categoryToDelete = category
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                    }
                }

                var showCatMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showCatMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    com.scholarvault.ui.components.CustomPopupMenu(
                        expanded = showCatMenu,
                        onDismissRequest = { showCatMenu = false },
                        items = listOf(
                            com.scholarvault.ui.components.MenuItem("Edit Category", Icons.Default.Edit) {
                                showCatMenu = false
                                editCategoryTitle = category.title
                                categoryToEdit = category
                            },
                            com.scholarvault.ui.components.MenuItem("Delete Category", Icons.Default.Delete, isDestructive = true) {
                                showCatMenu = false
                                categoryToDelete = category
                            }
                        )
                    )
                }

                if (showHelpDialog) {
                    AlertDialog(
                        onDismissRequest = { showHelpDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Student Wallet Help")
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Welcome to your secure Student Wallet! This section acts as an organizer for all your academic and personal identity documents.",
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    "✨ Key Features & Instructions:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "• 💳 Category Storage: Keep documents classified neatly. You can create custom categories or use defaults like receipts, Marksheets, admits, and castes.\n" +
                                    "• 🔄 Interactive Flip: Tap any card inside the details view to flip it and see the backside image on-the-fly!\n" +
                                    "• 📎 Document Attachments: Link external files (PDFs, images) directly to any card for fast access.\n" +
                                    "• 📋 Quick Copy Fields: Click the copy icon next to any field (like Certificate Number or Date of Issue) to instantly copy it to your clipboard.",
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = { showHelpDialog = false }) {
                                Text("Got it!")
                            }
                        }
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.animation.AnimatedVisibility(
            visible = showCards,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { 20 })
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Button(
                        onClick = { onAddCard(category.id) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .navigationBarsPadding(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add New Card")
                    }
                }
                
                if (cards.isEmpty()) {
                    item {
                        Text("No cards in this category.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    cards.chunked(2).forEach { rowCards ->
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowCards.forEach { card ->
                                    WalletCardItem(
                                        card = card,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onNavigateToCard(card.id) }
                                    )
                                }
                                if (rowCards.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete '${cat.title}'? All cards inside will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    docViewModel.deleteCategory(cat)
                    categoryToDelete = null
                    onBack() // go back after delete
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WalletCardItem(
    card: com.scholarvault.data.model.WalletCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = card.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WalletCardDetailScreen(
    cardId: Int,
    docViewModel: DocumentViewModel,
    onBack: () -> Unit,
    onEdit: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToViewer: (String) -> Unit = {}
) {
    val allCards by docViewModel.allCards.collectAsState()
    val card = allCards.find { it.id == cardId }
    val fields = remember(card) { docViewModel.parseFieldsOfCard(card) }
    var expandedMenu by remember { mutableStateOf(false) }

    if (card == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading or Card not found...")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        com.scholarvault.ui.components.TopSearchBar(
            onOpenDrawer = onBack,
            isBackButton = true,
            title = card.title,
            showProfileIcon = false,
            actions = {
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isTabletScreen = configuration.screenWidthDp >= 600

                if (isTabletScreen) {
                    IconButton(onClick = { onEdit(card.id, card.categoryId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { expandedMenu = false /* Share placeholder */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { expandedMenu = false /* Print placeholder */ }) {
                        Icon(Icons.Default.Print, contentDescription = "Print")
                    }
                }

                Box {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    com.scholarvault.ui.components.CustomPopupMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        items = listOf(
                            com.scholarvault.ui.components.MenuItem("Edit", Icons.Default.Edit) {
                                expandedMenu = false 
                                onEdit(card.id, card.categoryId)
                            },
                            com.scholarvault.ui.components.MenuItem("Share", Icons.Default.Share) { expandedMenu = false },
                            com.scholarvault.ui.components.MenuItem("Print", Icons.Default.Print) { expandedMenu = false }
                        )
                    )
                }
            }
        )
        
        var cardAspectRatio by remember { mutableStateOf(1.586f) }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Animated Card (Placeholder for now)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                var isFlipped by remember { mutableStateOf(false) }
                val rotation by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 500)
                )
                val currentDensity = LocalDensity.current.density

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(cardAspectRatio)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { isFlipped = !isFlipped }
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * currentDensity
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        if (card.frontImagePath != null) {
                            coil.compose.SubcomposeAsyncImage(
                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(android.net.Uri.parse(card.frontImagePath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Front Image",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                                onSuccess = { state ->
                                    val size = state.painter.intrinsicSize
                                    if (size.width > 0 && size.height > 0) {
                                        cardAspectRatio = size.width / size.height
                                    }
                                }
                            )
                        } else {
                            Text(
                                "Front of ${card.title}\n(Tap to Flip)", 
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.tertiaryContainer).graphicsLayer { rotationY = 180f }) {
                            if (card.backImagePath != null) {
                                coil.compose.SubcomposeAsyncImage(
                                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                        .data(android.net.Uri.parse(card.backImagePath))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Back Image",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
                                )
                            } else {
                                Text(
                                    "Back of ${card.title}", 
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Custom Parameters (Fields)
            items(fields.size) { index ->
                val field = fields[index]
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = field.label,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = field.value,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        IconButton(onClick = { 
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(field.value))
                            android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }
                }
            }
            
            // Notes area
            item {
                if (!card.notes.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Short Notes",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = card.notes,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Attached Document area
            item {
                if (card.linkedDocumentId != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val rootFiles by docViewModel.rootFiles.collectAsState()
                    val linkedFile = rootFiles.find { it.id == card.linkedDocumentId } // Or get from repository directly if not in rootFiles
                    
                    ElevatedCard(
                        onClick = { 
                            if (linkedFile != null) {
                                val mappedType = when (linkedFile.extension?.lowercase()) {
                                    "pdf" -> "pdf"
                                    "mp3", "wav", "m4a" -> "audio"
                                    "jpg", "png", "jpeg" -> "image"
                                    else -> "text"
                                }
                                onNavigateToViewer(com.scholarvault.ui.Screen.Viewer.createRoute(mappedType, linkedFile.filePath, linkedFile.name))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Attached Document", 
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 12.sp
                                )
                                Text(
                                    linkedFile?.name ?: "Document #${card.linkedDocumentId}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWalletCardScreen(
    categoryId: Int,
    cardId: Int,
    docViewModel: DocumentViewModel,
    onBack: () -> Unit
) {
    val allCards by docViewModel.allCards.collectAsState()
    val allCategories by docViewModel.allCategories.collectAsState()
    val existingCard = allCards.find { it.id == cardId }
    
    val initialFields = remember(existingCard) { 
        if (existingCard != null) {
            docViewModel.parseFieldsOfCard(existingCard)
        } else {
            val categoryName = allCategories.find { it.id == categoryId }?.title
            if (categoryName?.contains("Certificate", ignoreCase = true) == true || 
                categoryName?.contains("Reservation", ignoreCase = true) == true ||
                categoryName?.contains("Caste", ignoreCase = true) == true) {
                listOf(
                    com.scholarvault.data.model.WalletCardCustomField(label = "Category (OBC-NCL/SC/ST/EWS)", value = "", orderIndex = 0),
                    com.scholarvault.data.model.WalletCardCustomField(label = "Certificate Number", value = "", orderIndex = 1),
                    com.scholarvault.data.model.WalletCardCustomField(label = "Issuing Authority", value = "", orderIndex = 2),
                    com.scholarvault.data.model.WalletCardCustomField(label = "Date of Issue (DD/MM/YYYY)", value = "", orderIndex = 3)
                )
            } else if (categoryName?.contains("Marksheet", ignoreCase = true) == true) {
                listOf(
                    com.scholarvault.data.model.WalletCardCustomField(label = "Board / University", value = "", orderIndex = 0),
                    com.scholarvault.data.model.WalletCardCustomField(label = "Year of Passing", value = "", orderIndex = 1),
                    com.scholarvault.data.model.WalletCardCustomField(label = "Roll Number", value = "", orderIndex = 2),
                    com.scholarvault.data.model.WalletCardCustomField(label = "Total Marks / CGPA", value = "", orderIndex = 3)
                )
            } else if (categoryName?.contains("admit", ignoreCase = true) == true) {
                listOf(
                    com.scholarvault.data.model.WalletCardCustomField(label = "Exam Name", value = "", orderIndex = 0),
                    com.scholarvault.data.model.WalletCardCustomField(label = "Roll Number/ID", value = "", orderIndex = 1),
                    com.scholarvault.data.model.WalletCardCustomField(label = "Exam Date (DD/MM/YYYY)", value = "", orderIndex = 2),
                    com.scholarvault.data.model.WalletCardCustomField(label = "Center Name", value = "", orderIndex = 3)
                )
            } else if (categoryName?.contains("receipt", ignoreCase = true) == true) {
                listOf(
                    com.scholarvault.data.model.WalletCardCustomField(label = "Merchant / Organization", value = "", orderIndex = 0),
                    com.scholarvault.data.model.WalletCardCustomField(label = "Amount Paid", value = "", orderIndex = 1),
                    com.scholarvault.data.model.WalletCardCustomField(label = "Payment Date (DD/MM/YYYY)", value = "", orderIndex = 2),
                    com.scholarvault.data.model.WalletCardCustomField(label = "Transaction ID", value = "", orderIndex = 3)
                )
            } else {
                emptyList()
            }
        }
    }
    
    var title by remember { mutableStateOf(existingCard?.title ?: "") }
    var notes by remember { mutableStateOf(existingCard?.notes ?: "") }
    var fields by remember { mutableStateOf(initialFields) }

    var frontImageUri by remember { mutableStateOf(existingCard?.frontImagePath) }
    var backImageUri by remember { mutableStateOf(existingCard?.backImagePath) }
    var currentImageTarget by remember { mutableStateOf("") }
    var documentUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var documentName by remember { mutableStateOf(if (existingCard?.linkedDocumentId != null) "Existing Document Attached" else "") }
    var selectedWalletDocId by remember { mutableStateOf<Int?>(existingCard?.linkedDocumentId) }
    
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showWalletSelector by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            if (currentImageTarget == "front") {
                val copied = copyUriToInternalStorage(context, uri, "wallet_front")
                if (copied != null) {
                    frontImageUri = copied
                }
            } else if (currentImageTarget == "back") {
                val copied = copyUriToInternalStorage(context, uri, "wallet_back")
                if (copied != null) {
                    backImageUri = copied
                }
            } else if (currentImageTarget == "doc") {
                documentUri = uri
                
                // Extract file name
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            documentName = it.getString(nameIndex)
                        }
                    }
                }
            }
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

    if (showWalletSelector) {
        val securityManager = remember { com.scholarvault.security.WalletSecurityManager(context) }
        com.scholarvault.security.WalletAuthenticationWrapper(
            securityManager = securityManager,
            onUnlockSuccess = {},
            onCancel = { showWalletSelector = false }
        ) {
            com.scholarvault.ui.components.WalletDocumentSelectorDialog(
                docViewModel = docViewModel,
                onDismiss = { showWalletSelector = false },
                onSelect = { docFile ->
                    selectedWalletDocId = docFile.id
                    documentName = docFile.name
                    documentUri = null
                    showWalletSelector = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = if (cardId == -1) "New Card" else "Edit Card",
                showProfileIcon = false
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Card Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Image Front & Back (Placeholders)
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedCard(
                        onClick = { 
                            currentImageTarget = "front"
                            filePickerLauncher.launch(arrayOf("image/*")) 
                        },
                        modifier = Modifier.weight(1f).height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (frontImageUri != null) {
                            CachedImage(uri = android.net.Uri.parse(frontImageUri!!), modifier = Modifier.fillMaxSize())
                        } else {
                            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Text("Front Image")
                            }
                        }
                    }
                    ElevatedCard(
                        onClick = { 
                            currentImageTarget = "back"
                            filePickerLauncher.launch(arrayOf("image/*")) 
                        },
                        modifier = Modifier.weight(1f).height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (backImageUri != null) {
                            CachedImage(uri = android.net.Uri.parse(backImageUri!!), modifier = Modifier.fillMaxSize())
                        } else {
                            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Text("Back Image")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                ElevatedCard(
                    onClick = { 
                        currentImageTarget = "doc"
                        showAttachmentMenu = true
                    },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (documentName.isNotEmpty()) documentName else "Attach External Document (PDF, etc.)", color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Custom Fields
            item {
                Text("Custom Details", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(fields.size) { index ->
                val field = fields[index]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = field.label,
                        onValueChange = { newLabel -> 
                            fields = fields.toMutableList().apply { 
                                set(index, field.copy(label = newLabel)) 
                            }
                        },
                        label = { Text("Label (e.g. Expiry Date)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = field.value,
                        onValueChange = { newValue -> 
                            fields = fields.toMutableList().apply { 
                                set(index, field.copy(value = newValue)) 
                            }
                        },
                        label = { Text("Value") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { 
                        fields = fields.toMutableList().apply { removeAt(index) } 
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Field", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { 
                        if (fields.size < 20) {
                            fields = fields + com.scholarvault.data.model.WalletCardCustomField(label = "", value = "", orderIndex = fields.size)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = fields.size < 20,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (fields.size < 20) "Add another detail..." else "Max fields reached (20)")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            coroutineScope.launch {
                                var finalDocId = selectedWalletDocId
                                if (documentUri != null && documentUri.toString() != "existing") {
                                    val docFileToSave = com.scholarvault.data.model.DocumentFile(
                                        name = documentName,
                                        isFolder = false,
                                        parentFolderId = null,
                                        filePath = "",
                                        extension = documentName.substringAfterLast(".", ""),
                                        sizeBytes = 0L,
                                        isEncrypted = true,
                                        tags = listOf("wallet")
                                    )
                                    val newDocId = docViewModel.insertAttachmentFile(context, docFileToSave, documentUri)
                                    if (newDocId != -1L) {
                                        finalDocId = newDocId.toInt()
                                    }
                                }
                                
                                val cardToSave = existingCard?.copy(
                                    title = title,
                                    notes = notes,
                                    frontImagePath = frontImageUri,
                                    backImagePath = backImageUri,
                                    linkedDocumentId = finalDocId
                                ) ?: com.scholarvault.data.model.WalletCard(
                                    categoryId = categoryId,
                                    title = title,
                                    notes = notes,
                                    frontImagePath = frontImageUri,
                                    backImagePath = backImageUri,
                                    linkedDocumentId = finalDocId
                                )
                                docViewModel.insertCardWithFields(cardToSave, fields)
                                onBack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(if (cardId == -1) "Save Card" else "Update Card", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                if (cardId != -1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { 
                            existingCard?.let { docViewModel.deleteCard(it) }
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Card", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
