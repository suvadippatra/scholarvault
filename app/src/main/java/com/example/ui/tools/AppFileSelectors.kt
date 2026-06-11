package com.scholarvault.ui.tools

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scholarvault.MainApplication
import com.scholarvault.data.model.DocumentFile
import com.scholarvault.data.repository.DocumentRepository
import com.scholarvault.security.WalletAuthenticationWrapper
import com.scholarvault.security.WalletSecurityManager
import com.scholarvault.ui.viewmodel.DocumentViewModel
import java.io.File
import android.widget.Toast
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Print
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFilesSelectorDialog(
    onUrisSelected: (List<Uri>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MainApplication
    val docRepository = remember { DocumentRepository(app.database.documentDao(), app.database.walletDao()) }
    val docViewModel: DocumentViewModel = viewModel(factory = DocumentViewModel.Factory(docRepository))

    var currentFolderId by remember { mutableStateOf<Int?>(null) }
    
    val filesFlow = remember(currentFolderId) {
        if (currentFolderId == null) docViewModel.rootFiles else docViewModel.getFilesByFolder(currentFolderId!!)
    }
    val files by filesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.8f) // Takes 80% of screen height
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (currentFolderId == null) "My Files" else "Select File", style = MaterialTheme.typography.titleLarge)
                if (currentFolderId != null) {
                    TextButton(onClick = { currentFolderId = null }) {
                        Text("Back to Root")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No files found.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(files) { file ->
                        ListItem(
                            headlineContent = { Text(file.name) },
                            modifier = Modifier.clickable {
                                if (file.isFolder) {
                                    currentFolderId = file.id
                                } else {
                                    // Generate URI and return
                                    var path = file.filePath
                                    if (path.startsWith("file://")) {
                                        path = path.substring(7)
                                    } else if (path.startsWith("file:")) {
                                        path = path.substring(5)
                                    }
                                    if (path.isNotEmpty() && !path.startsWith("/")) {
                                        path = File(context.filesDir, path).absolutePath
                                    }
                                    val uri = Uri.fromFile(File(path))
                                    onUrisSelected(listOf(uri))
                                }
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (file.isFolder) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (file.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletFilesSelectorDialog(
    onUrisSelected: (List<Uri>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MainApplication
    val docRepository = remember { DocumentRepository(app.database.documentDao(), app.database.walletDao()) }
    val docViewModel: DocumentViewModel = viewModel(factory = DocumentViewModel.Factory(docRepository))
    val securityManager = remember { WalletSecurityManager(context) }

    var isUnlocked by remember { mutableStateOf(false) }
    
    var currentFolderId by remember { mutableStateOf<Int?>(null) }
    
    val filesFlow = remember(currentFolderId) {
        if (currentFolderId == null) docViewModel.rootFiles else docViewModel.getFilesByFolder(currentFolderId!!)
    }
    val allFiles by filesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    // For wallet, we show folders (so we can navigate) AND encrypted files in this folder.
    val walletFiles = allFiles.filter { it.isFolder || it.isEncrypted }

    if (!isUnlocked) {
        Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize()) {
                WalletAuthenticationWrapper(
                    securityManager = securityManager,
                    onUnlockSuccess = { isUnlocked = true },
                    onCancel = onDismiss,
                ) {
                    // Fallback content before unlock if any
                }
            }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = Modifier.fillMaxHeight(0.8f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (currentFolderId == null) "Wallet Vault Files" else "Select File", style = MaterialTheme.typography.titleLarge)
                    if (currentFolderId != null) {
                        TextButton(onClick = { currentFolderId = null }) {
                            Text("Back to Root")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (walletFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No encrypted files in this folder.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(walletFiles) { file ->
                            ListItem(
                                headlineContent = { Text(file.name) },
                                modifier = Modifier.clickable {
                                    if (file.isFolder) {
                                        currentFolderId = file.id
                                    } else {
                                        var path = file.filePath
                                        if (path.isEmpty() || path == "wallet_secure/${file.name}") {
                                            path = "wallet_secure/${file.name}"
                                        } else {
                                            if (path.startsWith("file://")) path = path.substring(7)
                                        }
                                        
                                        val isFileEncrypted = path.startsWith("wallet_secure/")
                                        val absolutePath = if (isFileEncrypted) {
                                            val vault = com.scholarvault.util.SecurityVault(context)
                                            val decryptedTemp = vault.getFileForViewing(File(context.filesDir, path), context.cacheDir)
                                            decryptedTemp?.absolutePath ?: File(context.filesDir, path).absolutePath
                                        } else {
                                            if (!path.startsWith("/")) File(context.filesDir, path).absolutePath else path
                                        }
                                        
                                        val uri = Uri.fromFile(File(absolutePath))
                                        onUrisSelected(listOf(uri))
                                    }
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = if (file.isFolder) Icons.Default.Folder else Icons.Default.Security, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddFilesMenuButton(
    onDeviceClick: () -> Unit,
    onUrisSelected: (List<Uri>) -> Unit,
    onLongPress: () -> Unit,
    label: String = "Add Files"
) {
    var showAddMenu by remember { mutableStateOf(false) }
    var showMyFilesSelector by remember { mutableStateOf(false) }
    var showWalletSelector by remember { mutableStateOf(false) }

    if (showMyFilesSelector) {
        MyFilesSelectorDialog(
            onUrisSelected = { 
                showMyFilesSelector = false
                onUrisSelected(it) 
            },
            onDismiss = { showMyFilesSelector = false }
        )
    }
    if (showWalletSelector) {
        WalletFilesSelectorDialog(
            onUrisSelected = {
                showWalletSelector = false
                onUrisSelected(it)
            },
            onDismiss = { showWalletSelector = false }
        )
    }

    Box {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showAddMenu = true },
                    onLongPress = { onLongPress() }
                )
            }
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, label)
                Spacer(Modifier.width(8.dp))
                Text(label)
            }
        }
        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
            DropdownMenuItem(text = { Text("From Device") }, onClick = { showAddMenu = false; onDeviceClick() })
            DropdownMenuItem(text = { Text("From My Files") }, onClick = { showAddMenu = false; showMyFilesSelector = true })
            DropdownMenuItem(text = { Text("From Wallet") }, onClick = { showAddMenu = false; showWalletSelector = true })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveDestinationBottomSheet(
    fileUri: Uri,
    defaultFileName: String,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as MainApplication
    val docRepository = remember { DocumentRepository(app.database.documentDao(), app.database.walletDao()) }
    val docViewModel: DocumentViewModel = viewModel(factory = DocumentViewModel.Factory(docRepository))
    val coroutineScope = rememberCoroutineScope()

    var fileNameInput by remember { mutableStateOf(defaultFileName.replace(".pdf", "", ignoreCase = true)) }
    var fileSizeString by remember { mutableStateOf("Calculating...") }
    var fileSizeInBytes by remember { mutableStateOf(0L) }

    LaunchedEffect(fileUri) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(fileUri, "r")?.use { fd ->
                    val len = fd.statSize
                    fileSizeInBytes = len
                    val kb = len / 1024.0
                    fileSizeString = if (kb > 1024) String.format("%.2f MB", kb / 1024.0) else String.format("%.1f KB", kb)
                }
            } catch (e: Exception) {
                fileSizeString = "Unknown size"
            }
        }
    }

    val finalExtension = remember(fileUri) {
        val path = fileUri.path ?: ""
        var ext = File(path).extension.lowercase()
        if (ext.isEmpty()) {
            val type = context.contentResolver.getType(fileUri)
            ext = type?.substringAfterLast("/")?.lowercase() ?: "pdf"
        }
        ext
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.85f),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Export & Save",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = "Choose a destination below to save or share your document. You can rename the file first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // File Info Frame
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = defaultFileName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Size: $fileSizeString • Format: ${finalExtension.uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Rename Section
            OutlinedTextField(
                value = fileNameInput,
                onValueChange = { fileNameInput = it },
                label = { Text("Filename") },
                suffix = { Text(".$finalExtension") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Text(
                text = "Destinations",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Save destinations
            val saveOptions = listOf(
                SaveOptionItem(
                    title = "Save to Downloads",
                    description = "Save directly inside your device's Downloads folder",
                    icon = Icons.Default.Download,
                    color = MaterialTheme.colorScheme.primary,
                    bgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    onClick = {
                        val fullName = "$fileNameInput.$finalExtension"
                        val resultUri = saveFileToDownloads(context, fileUri, fullName)
                        if (resultUri != null) {
                            Toast.makeText(context, "Saved successfully to Downloads!", Toast.LENGTH_SHORT).show()
                            onSuccess(fullName)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Failed to save file", Toast.LENGTH_LONG).show()
                        }
                    }
                ),
                SaveOptionItem(
                    title = "Encrypt & Lock in Secure Vault",
                    description = "Encrypt heavily and save inside the lockable Vault",
                    icon = Icons.Default.Security,
                    color = MaterialTheme.colorScheme.error,
                    bgColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    onClick = {
                        val fullName = "$fileNameInput.$finalExtension"
                        coroutineScope.launch {
                            val docFile = DocumentFile(
                                name = fullName,
                                isFolder = false,
                                isEncrypted = true,
                                parentFolderId = null,
                                extension = finalExtension,
                                sizeBytes = fileSizeInBytes,
                                filePath = ""
                            )
                            val insertedId = docViewModel.insertAttachmentFile(context, docFile, fileUri)
                            if (insertedId >= 0) {
                                Toast.makeText(context, "Encrypted and saved inside Secure Vault!", Toast.LENGTH_SHORT).show()
                                onSuccess(fullName)
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Failed to encrypt/save in Vault", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ),
                SaveOptionItem(
                    title = "Save to Local My Files",
                    description = "Save standard unencrypted inside this app's documents folder",
                    icon = Icons.Default.Folder,
                    color = MaterialTheme.colorScheme.secondary,
                    bgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    onClick = {
                        val fullName = "$fileNameInput.$finalExtension"
                        coroutineScope.launch {
                            val docFile = DocumentFile(
                                name = fullName,
                                isFolder = false,
                                isEncrypted = false,
                                parentFolderId = null,
                                extension = finalExtension,
                                sizeBytes = fileSizeInBytes,
                                filePath = ""
                            )
                            val insertedId = docViewModel.insertAttachmentFile(context, docFile, fileUri)
                            if (insertedId >= 0) {
                                Toast.makeText(context, "Saved under app My Files!", Toast.LENGTH_SHORT).show()
                                onSuccess(fullName)
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Failed to copy to My Files", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ),
                SaveOptionItem(
                    title = "Share Document",
                    description = "Send this file to external apps using system sharesheet",
                    icon = Icons.Default.Share,
                    color = MaterialTheme.colorScheme.tertiary,
                    bgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    onClick = {
                        val mimeType = when (finalExtension) {
                            "pdf" -> "application/pdf"
                            "jpg", "jpeg" -> "image/jpeg"
                            "png" -> "image/png"
                            else -> "*/*"
                        }
                        shareFile(context, fileUri, mimeType)
                        onDismiss()
                    }
                ),
                SaveOptionItem(
                    title = "Print Document",
                    description = "Open Android Print Spooler to print or save to system PDF",
                    icon = Icons.Default.Print,
                    color = MaterialTheme.colorScheme.onSurface,
                    bgColor = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = {
                        val fullName = "$fileNameInput.$finalExtension"
                        printFile(context, fileUri, fullName)
                        onDismiss()
                    }
                )
            )

            saveOptions.forEach { opt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { opt.onClick() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = opt.bgColor,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = null,
                                    tint = opt.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = opt.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = opt.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class SaveOptionItem(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val bgColor: Color,
    val onClick: () -> Unit
)

private fun saveFileToDownloads(context: android.content.Context, sourceUri: Uri, defaultFileName: String): Uri? {
    val resolver = context.contentResolver
    val cleanName = defaultFileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    val extension = File(cleanName).extension.lowercase()
    val mimeType = when (extension) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "mp3" -> "audio/mpeg"
        "mp4" -> "video/mp4"
        "wav" -> "audio/wav"
        "txt" -> "text/plain"
        else -> "*/*"
    }
    
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
    }
    
    val uriToSave = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    } else {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val targetFile = File(downloadsDir, cleanName)
        Uri.fromFile(targetFile)
    }
    
    if (uriToSave != null) {
        try {
            resolver.openInputStream(sourceUri)?.use { input ->
                resolver.openOutputStream(uriToSave)?.use { output ->
                    input.copyTo(output)
                }
            }
            return uriToSave
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}

private fun shareFile(context: android.content.Context, uri: Uri, mimeType: String = "*/*") {
    try {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Document"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to share file", Toast.LENGTH_SHORT).show()
    }
}

private fun printFile(context: android.content.Context, uri: Uri, docName: String) {
    try {
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager == null) {
            Toast.makeText(context, "System Printing Service not available", Toast.LENGTH_SHORT).show()
            return
        }
        val printAdapter = object : android.print.PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: android.print.PrintAttributes?,
                newAttributes: android.print.PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = android.print.PrintDocumentInfo.Builder(docName)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }
            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        java.io.FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback?.onWriteFailed(e.localizedMessage)
                }
            }
        }
        printManager.print(docName, printAdapter, android.print.PrintAttributes.Builder().build())
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to start print job", Toast.LENGTH_SHORT).show()
    }
}
