package com.scholarvault.ui.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.scholarvault.data.model.DocumentFile
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.theme.LocalThemeController
import com.scholarvault.ui.viewmodel.DocumentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    onBack: () -> Unit,
    docViewModel: DocumentViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalThemeController.current.isDarkTheme

    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isConverting by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var pdfNameInput by remember { mutableStateOf("ScholarVault_Scan") }

    // Page preferences
    var pageSizeOption by remember { mutableStateOf("A4") } // "A4", "Fit Image"
    var marginOption by remember { mutableStateOf("No Margin") } // "No Margin", "Small Margin (15dp)"

    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImages = selectedImages + uris
            generatedPdfFile = null // Reset generator
        }
    }

    var showHelpDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = listOf("B", "KB", "MB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.toDouble())).toInt().coerceIn(0, units.size - 1)
        return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.toDouble(), digitGroups.toDouble()), units[digitGroups])
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("PDF Generator Guide") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("1. Pick Images: Tap card to choose assignment pages or note files from Device, My Files or Secure Wallet.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("2. Page Queue: Reorder images using up/down arrows to structure your pages precisely, or delete elements with a single click.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("3. PDF Format: Set custom PDF filename and choose between A4 and Fit Image dimension styles.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("4. Generate: Tap 'Convert to PDF Now' to instantly compile your document locally.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("5. Secure Export: Click Save popup to share your PDF, send it to Print spooler, download to Device, or lock inside Secure Vault.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Got it") } }
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Recent PDFs") },
            text = {
                val cacheDir = File(context.cacheDir.absolutePath)
                val files = cacheDir.listFiles()?.filter { it.name.startsWith("ImgToPdf_") && it.name.endsWith(".pdf") }?.sortedByDescending { it.lastModified() } ?: emptyList()
                if (files.isEmpty()) {
                    Text("No recently generated PDFs in cache.")
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(files.size) { i ->
                            val f = files[i]
                            val displaySize = formatBytes(f.length())
                            val dateStr = java.text.SimpleDateFormat("HH:mm, MMM dd", java.util.Locale.getDefault()).format(java.util.Date(f.lastModified()))
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(f.name.substringAfter("ImgToPdf_"), fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text("$displaySize • $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share"))
                                }) { Icon(Icons.Default.Share, "Share", modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistoryDialog = false }) { Text("Close") } }
        )
    }

    Scaffold(
        topBar = {
            TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Image to PDF",
                showProfileIcon = false,
                showSearchBar = false,
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, "History")
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, "Help Information")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // Header Info
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                "PDF Generator",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Convert assignment images, notes or lecture pages into structured PDFs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (selectedImages.isEmpty()) {
                item {
                    FileDropBox(
                        onUrisSelected = { uris ->
                            if (uris.isNotEmpty()) {
                                selectedImages = selectedImages + uris
                                generatedPdfFile = null
                            }
                        },
                        mimeType = "image/*"
                    )
                }
            } else {
                // PDF Options Panel
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = pdfNameInput,
                                    onValueChange = { pdfNameInput = it },
                                    label = { Text("PDF Document Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    suffix = { Text(".pdf") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Page size option
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Page Dimens:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = pageSizeOption == "A4",
                                            onClick = { pageSizeOption = "A4" },
                                            label = { Text("A4 Standard") }
                                        )
                                        FilterChip(
                                            selected = pageSizeOption == "Fit Image",
                                            onClick = { pageSizeOption = "Fit Image" },
                                            label = { Text("Fit Image") }
                                        )
                                    }
                                }

                                // Margin option
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Margin Choice:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = marginOption == "No Margin",
                                            onClick = { marginOption = "No Margin" },
                                            label = { Text("No Margin") }
                                        )
                                        FilterChip(
                                            selected = marginOption == "Border",
                                            onClick = { marginOption = "Border" },
                                            label = { Text("Border (15dp)") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Images List
                item {
                    Text(
                        "Selected Images Queue (${selectedImages.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                itemsIndexed(selectedImages) { index, uri ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = uri),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Page ${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = uri.lastPathSegment ?: "Image Source",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Movement operations
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = {
                                        val mutable = selectedImages.toMutableList()
                                        val temp = mutable[index]
                                        mutable[index] = mutable[index - 1]
                                        mutable[index - 1] = temp
                                        selectedImages = mutable
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    enabled = index < selectedImages.size - 1,
                                    onClick = {
                                        val mutable = selectedImages.toMutableList()
                                        val temp = mutable[index]
                                        mutable[index] = mutable[index + 1]
                                        mutable[index + 1] = temp
                                        selectedImages = mutable
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val mutable = selectedImages.toMutableList()
                                        mutable.removeAt(index)
                                        selectedImages = mutable
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    FileDropBox(
                        onUrisSelected = { uris ->
                            if (uris.isNotEmpty()) {
                                selectedImages = selectedImages + uris
                                generatedPdfFile = null
                            }
                        },
                        mimeType = "image/*",
                        isCompact = true
                    )
                }

                // Generative progress / trigger
                item {
                    if (generatedPdfFile == null) {
                        Button(
                            onClick = {
                                isConverting = true
                                scope.launch {
                                    generatePdf(
                                        context = context,
                                        uris = selectedImages,
                                        pageSize = pageSizeOption,
                                        hasMargin = marginOption == "Border"
                                    ) { file ->
                                        generatedPdfFile = file
                                        isConverting = false
                                        Toast.makeText(context, "Successfully compiled images into PDF!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isConverting && selectedImages.isNotEmpty()
                        ) {
                            if (isConverting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Text("Compiling Images to PDF...")
                            } else {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Convert to PDF Now")
                            }
                        }
                    } else {
                        // Actions on completed PDF
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "PDF Generation Successful!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Button(
                                    onClick = { showExportSheet = true },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export / Save Generated PDF")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportSheet && generatedPdfFile != null) {
        val resultUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            generatedPdfFile!!
        )
        val finalName = if (pdfNameInput.endsWith(".pdf", ignoreCase = true)) pdfNameInput else "$pdfNameInput.pdf"
        SaveDestinationBottomSheet(
            fileUri = resultUri,
            defaultFileName = finalName,
            onDismiss = { showExportSheet = false },
            onSuccess = { fullName ->
                Toast.makeText(context, "$fullName saved successfully!", Toast.LENGTH_SHORT).show()
                selectedImages = emptyList()
                generatedPdfFile = null
            }
        )
    }
}

private suspend fun generatePdf(
    context: Context,
    uris: List<Uri>,
    pageSize: String,
    hasMargin: Boolean,
    onComplete: (File) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val pdfDocument = PdfDocument()

        for ((index, uri) in uris.withIndex()) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val fullBitmap = BitmapFactory.decodeStream(stream)
                if (fullBitmap != null) {
                    // Standard A4 dimensions in PostScript points: 595 x 842
                    val pageW = if (pageSize == "A4") 595 else fullBitmap.width
                    val pageH = if (pageSize == "A4") 842 else fullBitmap.height

                    val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    val margin = if (hasMargin) 15f else 0f
                    val availableW = pageW - (margin * 2)
                    val availableH = pageH - (margin * 2)

                    // Rescale bitmap to fit the destination bounds gracefully maintaining aspect ratio
                    val scaleX = availableW / fullBitmap.width.toFloat()
                    val scaleY = availableH / fullBitmap.height.toFloat()
                    val scale = Math.min(scaleX, scaleY)

                    val destW = (fullBitmap.width * scale).toInt()
                    val destH = (fullBitmap.height * scale).toInt()

                    val offsetX = margin + (availableW - destW) / 2f
                    val offsetY = margin + (availableH - destH) / 2f

                    val scaledBitmap = Bitmap.createScaledBitmap(fullBitmap, destW, destH, true)
                    canvas.drawBitmap(scaledBitmap, offsetX, offsetY, null)

                    pdfDocument.finishPage(page)
                    if (scaledBitmap != fullBitmap) {
                        scaledBitmap.recycle()
                    }
                    fullBitmap.recycle()
                }
            }
        }

        val cacheFile = File(context.cacheDir, "ImgToPdf_${System.currentTimeMillis()}.pdf")
        FileOutputStream(cacheFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        onComplete(cacheFile)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun sharePdfFile(context: Context, file: File, finalTitle: String) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "$finalTitle.pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share PDF Document"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Unable to share generated doc", Toast.LENGTH_SHORT).show()
    }
}
