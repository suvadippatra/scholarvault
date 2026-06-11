package com.scholarvault.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.data.model.DocumentFile
import com.scholarvault.ui.Screen
import com.scholarvault.ui.formatFileSize
import com.scholarvault.ui.viewmodel.DocumentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentRecordingsScreen(
    docViewModel: DocumentViewModel,
    onBack: () -> Unit,
    onNavigateToViewer: (String) -> Unit
) {
    val context = LocalContext.current
    val rootFiles by docViewModel.allNonFolderFiles.collectAsState()
    val previousRecordings = remember(rootFiles) {
        rootFiles.filter { it.extension?.lowercase() in listOf("m4a", "ogg", "wav", "mp3") }
            .sortedByDescending { it.createdAt }
    }
    
    var fileToTrash by remember { mutableStateOf<DocumentFile?>(null) }
    var fileToRename by remember { mutableStateOf<DocumentFile?>(null) }
    var renameText by remember { mutableStateOf("") }
    var renameTagsText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Recent Recordings",
                showProfileIcon = false,
                showSearchBar = false
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (previousRecordings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "No files",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No recordings found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(previousRecordings.size) { i ->
                        val file = previousRecordings[i]
                        RecentRecordingItem(
                            file = file,
                            onPlayClick = {
                                onNavigateToViewer(Screen.Viewer.createRoute("audio", file.filePath, file.name))
                            },
                            onEditClick = {
                                fileToRename = file
                                renameText = file.name
                                renameTagsText = file.tags.filter { !it.startsWith("linked-doc-") && it != "recording" }.joinToString(", ")
                            },
                            onTrashClick = { fileToTrash = file }
                        )
                    }
                }
            }
        }

        if (fileToTrash != null) {
            AlertDialog(
                onDismissRequest = { fileToTrash = null },
                title = { Text("Confirm Trash Move") },
                text = { Text("Are you sure you want to move typical recording '${fileToTrash?.name}' to the trash?") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            fileToTrash?.let { docViewModel.trashFile(it.id) }
                            fileToTrash = null
                        }
                    ) {
                        Text("Move to Trash")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fileToTrash = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (fileToRename != null) {
            AlertDialog(
                onDismissRequest = { fileToRename = null },
                title = { Text("Edit Recording Details") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            label = { Text("Recording Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = renameTagsText,
                            onValueChange = { renameTagsText = it },
                            label = { Text("Tags (comma separated)") },
                            placeholder = { Text("e.g. lecture, math, audio") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = fileToRename ?: return@Button
                            if (renameText.isNotBlank()) {
                                val userTags = renameTagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                val systemTags = target.tags.filter { it.startsWith("linked-doc-") || it == "recording" }
                                val mergedTags = (systemTags + userTags).distinct()
                                docViewModel.updateFile(target.copy(name = renameText, tags = mergedTags))
                                fileToRename = null
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fileToRename = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun RecentRecordingItem(
    file: DocumentFile,
    onPlayClick: () -> Unit,
    onEditClick: () -> Unit,
    onTrashClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Audiotrack,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(file.createdAt)
                Text(
                    text = "${file.extension?.uppercase()} • ${formatFileSize(file.sizeBytes)} • $dateStr",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                val linkedDocName = file.tags.find { it.startsWith("linked-doc-name:") }?.substringAfter("linked-doc-name:")
                if (linkedDocName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Linked document",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Linked to: $linkedDocName",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            var showMenu by remember { mutableStateOf(false) }
            val context = androidx.compose.ui.platform.LocalContext.current
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More actions")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play") },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                        onClick = {
                            showMenu = false
                            onPlayClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Details") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            showMenu = false
                            onEditClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = {
                            showMenu = false
                            // Simple share logic using an Intent
                            try {
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    java.io.File(file.filePath)
                                )
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Share Recording"))
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Error sharing file", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to Trash") },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onTrashClick()
                        }
                    )
                }
            }
        }
    }
}
