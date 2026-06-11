package com.scholarvault.ui.tools.flashcards

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.MainApplication
import com.scholarvault.data.model.FlashcardDeckEntity
import com.scholarvault.data.model.FlashcardEntity
import com.scholarvault.data.repository.FlashcardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FlashcardsDecksScreen(
    onBack: () -> Unit,
    onNavigateToDeck: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MainApplication
    val repository = remember { FlashcardRepository(app.database.flashcardDao()) }
    val scope = rememberCoroutineScope()

    val decks by repository.getAllDecks().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    var selectedDeckIds by remember { mutableStateOf(emptySet<String>()) }
    val isSelectionMode = selectedDeckIds.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("${selectedDeckIds.size} Selected")
                    } else {
                        Text("Flashcard Decks")
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedDeckIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                selectedDeckIds.forEach { id ->
                                    decks.find { it.id == id }?.let { repository.deleteDeck(it) }
                                }
                                withContext(Dispatchers.Main) {
                                    selectedDeckIds = emptySet()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected Decks", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Deck")
                }
            }
        }
    ) { padding ->
        if (decks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No decks yet. Tap + to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(decks, key = { it.id }) { deck ->
                    val isSelected = selectedDeckIds.contains(deck.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedDeckIds = if (isSelected) {
                                            selectedDeckIds - deck.id
                                        } else {
                                            selectedDeckIds + deck.id
                                        }
                                    } else {
                                        onNavigateToDeck(deck.id)
                                    }
                                },
                                onLongClick = {
                                    selectedDeckIds = selectedDeckIds + deck.id
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = deck.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedDeckIds = if (checked == true) {
                                                selectedDeckIds + deck.id
                                            } else {
                                                selectedDeckIds - deck.id
                                            }
                                        }
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "Open Deck",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (deck.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = deck.description,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            var deckName by remember { mutableStateOf("") }
            var deckDesc by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("New Deck") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = deckName,
                            onValueChange = { deckName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = deckDesc,
                            onValueChange = { deckDesc = it },
                            label = { Text("Description (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (deckName.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                repository.insertDeck(FlashcardDeckEntity(name = deckName.trim(), description = deckDesc.trim()))
                                showAddDialog = false
                            }
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FlashcardDeckDetailScreen(
    deckId: String,
    onBack: () -> Unit,
    onNavigateToStudy: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MainApplication
    val repository = remember { FlashcardRepository(app.database.flashcardDao()) }
    val scope = rememberCoroutineScope()

    var deck by remember { mutableStateOf<FlashcardDeckEntity?>(null) }
    val cards by repository.getCardsForDeck(deckId).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    var selectedCardIds by remember { mutableStateOf(emptySet<String>()) }
    val isSelectionMode = selectedCardIds.isNotEmpty()

    LaunchedEffect(deckId) {
        withContext(Dispatchers.IO) {
            deck = repository.getDeckById(deckId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("${selectedCardIds.size} Selected")
                    } else {
                        Text(deck?.name ?: "Deck")
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedCardIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                selectedCardIds.forEach { id ->
                                    cards.find { it.id == id }?.let { repository.deleteCard(it) }
                                }
                                withContext(Dispatchers.Main) {
                                    selectedCardIds = emptySet()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected Cards", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        if (cards.isNotEmpty()) {
                            IconButton(onClick = { onNavigateToStudy(deckId) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Study")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Card")
                }
            }
        }
    ) { padding ->
        if (cards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No cards in this deck. Tap + to add.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cards, key = { it.id }) { card ->
                    val isSelected = selectedCardIds.contains(card.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedCardIds = if (isSelected) {
                                            selectedCardIds - card.id
                                        } else {
                                            selectedCardIds + card.id
                                        }
                                    }
                                },
                                onLongClick = {
                                    selectedCardIds = selectedCardIds + card.id
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = card.frontText,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = card.backText,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val masteryLabel = when (card.masteryLevel) {
                                    1 -> "Familiar"
                                    2 -> "Mastered"
                                    else -> "Learning"
                                }
                                val color = when (card.masteryLevel) {
                                    1 -> Color(0xFFFFA000)
                                    2 -> Color(0xFF4CAF50)
                                    else -> MaterialTheme.colorScheme.error
                                }
                                Text("Status: $masteryLabel", color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            if (isSelectionMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedCardIds = if (checked == true) {
                                            selectedCardIds + card.id
                                        } else {
                                            selectedCardIds - card.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            var front by remember { mutableStateOf("") }
            var back by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("New Flashcard") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = front,
                            onValueChange = { front = it },
                            label = { Text("Front (Question/Term)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = back,
                            onValueChange = { back = it },
                            label = { Text("Back (Answer/Definition)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (front.isNotBlank() && back.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                repository.insertCard(FlashcardEntity(deckId = deckId, frontText = front.trim(), backText = back.trim()))
                                showAddDialog = false
                            }
                        }
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardStudyScreen(
    deckId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MainApplication
    val repository = remember { FlashcardRepository(app.database.flashcardDao()) }
    val scope = rememberCoroutineScope()

    var deck by remember { mutableStateOf<FlashcardDeckEntity?>(null) }
    var cards by remember { mutableStateOf(emptyList<FlashcardEntity>()) }
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    LaunchedEffect(deckId) {
        withContext(Dispatchers.IO) {
            deck = repository.getDeckById(deckId)
            repository.getCardsForDeck(deckId).collect {
                // Shuffle cards initially
                cards = it.shuffled()
                currentCardIndex = 0
                isFlipped = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study: ${deck?.name ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        cards = cards.shuffled()
                        currentCardIndex = 0
                        isFlipped = false
                    }) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
                    }
                }
            )
        }
    ) { padding ->
        if (cards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No cards to study.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        if (currentCardIndex >= cards.size) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Deck Completed!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        cards = cards.shuffled()
                        currentCardIndex = 0
                        isFlipped = false
                    }) {
                        Text("Study Again")
                    }
                }
            }
            return@Scaffold
        }

        val currentCard = cards[currentCardIndex]
        val rotation by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isFlipped) 180f else 0f,
            animationSpec = tween(500),
            label = "flip"
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Card ${currentCardIndex + 1} of ${cards.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            
            val mastery = when(currentCard.masteryLevel) {
                1 -> "Familiar"
                2 -> "Mastered"
                else -> "Learning"
            }
            Text("Current Mastery: $mastery", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clickable { isFlipped = !isFlipped }
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (rotation <= 90f) {
                        Text(
                            text = currentCard.frontText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Text(
                            text = currentCard.backText,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(16.dp)
                                .graphicsLayer { rotationY = 180f }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isFlipped) {
                Text("How well did you know this?", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        val updatedCard = currentCard.copy(masteryLevel = 0, lastReviewed = Date())
                        scope.launch(Dispatchers.IO) { repository.insertCard(updatedCard) }
                        currentCardIndex++
                        isFlipped = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Again")
                    }
                    Button(onClick = {
                        val updatedCard = currentCard.copy(masteryLevel = 1, lastReviewed = Date())
                        scope.launch(Dispatchers.IO) { repository.insertCard(updatedCard) }
                        currentCardIndex++
                        isFlipped = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))) {
                        Text("Hard")
                    }
                    Button(onClick = {
                        val updatedCard = currentCard.copy(masteryLevel = 2, lastReviewed = Date())
                        scope.launch(Dispatchers.IO) { repository.insertCard(updatedCard) }
                        currentCardIndex++
                        isFlipped = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                        Text("Easy")
                    }
                }
            } else {
                Text("Tap the card to reveal the answer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
