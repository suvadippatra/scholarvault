package com.scholarvault.ui.tools

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.formatFileSize
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentGeneratedMediaScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "PDF", "Compression", "Resized"

    var cacheFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var showHelpDialog by remember { mutableStateOf(false) }

    var selectedFileForExport by remember { mutableStateOf<File?>(null) }
    var showExportSheet by remember { mutableStateOf(false) }

    fun refreshFileList() {
        val cacheDir = context.cacheDir
        val allFiles = cacheDir.listFiles() ?: emptyArray()
        
        cacheFiles = allFiles.filter { file ->
            val name = file.name
            val matchesCompress = name.startsWith("compressed_") && name.endsWith(".jpg")
            val matchesPdf = name.startsWith("ImgToPdf_") && name.endsWith(".pdf")
            val matchesResize = name.contains("_final") && (name.endsWith(".jpg") || name.endsWith(".png"))
            
            matchesCompress || matchesPdf || matchesResize
        }.sortedByDescending { it.lastModified() }
    }

    LaunchedEffect(Unit) {
        refreshFileList()
    }

    val filteredFiles = remember(cacheFiles, searchQuery, selectedFilter) {
        cacheFiles.filter { file ->
            val nameMatches = file.name.contains(searchQuery, ignoreCase = true)
            val categoryMatches = when (selectedFilter) {
                "PDF" -> file.name.startsWith("ImgToPdf_")
                "Compression" -> file.name.startsWith("compressed_")
                "Resized" -> file.name.contains("_final")
                else -> true
            }
            nameMatches && categoryMatches
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Recent Media Guide") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "All compiled and reducated media files (such as compressed images, resized images, and generated PDFs) are stored temporarily in your device's cache folder for lightning speed performance.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚠️ CAUTION: Files listed here are temporary! Android may naturally purge this storage during system memory cleanups. To permanently lock your documents, click 'Export' and save them securely.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Got it") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Recent Generated Media",
                showProfileIcon = false,
                showSearchBar = false,
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, "Help")
                    }
                    if (cacheFiles.isNotEmpty()) {
                        IconButton(onClick = {
                            val cacheDir = context.cacheDir
                            val filesToDelete = cacheDir.listFiles()?.filter { f ->
                                val name = f.name
                                name.startsWith("compressed_") || name.startsWith("ImgToPdf_") || name.contains("_final")
                            } ?: emptyList()
                            filesToDelete.forEach { it.delete() }
                            refreshFileList()
                            Toast.makeText(context, "Cleared temporary media cache!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.DeleteForever, "Clear Cache", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search generated files...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "PDF", "Compression", "Resized").forEach { category ->
                    val isSelected = selectedFilter == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = category },
                        label = { Text(category) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (cacheFiles.isEmpty()) "No generated media yet." else "No files match your search.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredFiles.size) { index ->
                        val file = filteredFiles[index]
                        val extension = file.extension.lowercase()
                        val isPdf = extension == "pdf"
                        
                        val icon = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image
                        val iconColor = if (isPdf) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        
                        val formattedSize = formatFileSize(file.length())
                        val formattedDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))

                        val displayName = when {
                            file.name.startsWith("compressed_") -> file.name.substringAfter("compressed_")
                            file.name.startsWith("ImgToPdf_") -> file.name.substringAfter("ImgToPdf_")
                            else -> file.name
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$formattedSize • $formattedDate",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    // Quick delete from cache
                                    IconButton(
                                        onClick = {
                                            file.delete()
                                            refreshFileList()
                                            Toast.makeText(context, "Deleted from temporary cache", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            selectedFileForExport = file
                                            showExportSheet = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Export / Save", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = if (isPdf) "application/pdf" else "image/*"
                                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(intent, "Share Media"))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error sharing file", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportSheet && selectedFileForExport != null) {
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            selectedFileForExport!!
        )
        val plainName = when {
            selectedFileForExport!!.name.startsWith("compressed_") -> selectedFileForExport!!.name.substringAfter("compressed_")
            selectedFileForExport!!.name.startsWith("ImgToPdf_") -> selectedFileForExport!!.name.substringAfter("ImgToPdf_")
            else -> selectedFileForExport!!.name
        }
        SaveDestinationBottomSheet(
            fileUri = fileUri,
            defaultFileName = plainName,
            onDismiss = { showExportSheet = false },
            onSuccess = { fullName ->
                refreshFileList()
            }
        )
    }
}
