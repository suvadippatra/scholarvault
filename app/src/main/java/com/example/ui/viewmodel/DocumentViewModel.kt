package com.scholarvault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scholarvault.data.model.*
import com.scholarvault.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class DocumentViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    val triggerUploadChannel = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val triggerUploadFlow = triggerUploadChannel.receiveAsFlow()
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Pre-seed some default categories if empty
                val currentCategories = repository.getAllCategories().first()
                if (currentCategories.isEmpty()) {
                    repository.insertCategory(WalletCategory(title = "Id Docs", iconName = "badge"))
                    repository.insertCategory(WalletCategory(title = "Finance Docs", iconName = "account_balance"))
                    repository.insertCategory(WalletCategory(title = "Certificates & Reservations", iconName = "workspace_premium"))
                    repository.insertCategory(WalletCategory(title = "Receipts", iconName = "receipt"))
                    repository.insertCategory(WalletCategory(title = "Marksheets", iconName = "school"))
                    repository.insertCategory(WalletCategory(title = "Admits", iconName = "assignment_ind"))
                    repository.insertCategory(WalletCategory(title = "Caste", iconName = "people"))
                }
            } catch(e: Exception) {
                _errorState.value = "Failed to initialize db state: ${e.message}"
            }
        }
    }

    // --- Files ---
    val rootFiles: StateFlow<List<DocumentFile>> = repository.getRootFiles()
        .catch { e -> _errorState.value = "Failed to load files: ${e.message}" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allFolders: StateFlow<List<DocumentFile>> = repository.getAllFolders()
        .catch { e -> _errorState.value = "Failed to load folders: ${e.message}" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allNonFolderFiles: StateFlow<List<DocumentFile>> = repository.getAllNonFolderFiles()
        .catch { e -> _errorState.value = "Failed to load all non-folder files: ${e.message}" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun autoOrganizeSubjectFolders() {
        viewModelScope.launch {
            try {
                val subjects = listOf("Mathematics", "Physics", "Computer Science", "Literature", "History", "Assignments")
                val existingFolders = repository.getAllFolders().first()
                
                subjects.forEach { subject ->
                    if (existingFolders.none { it.name.equals(subject, ignoreCase = true) && it.parentFolderId == null }) {
                        repository.insertFile(DocumentFile(
                            name = subject,
                            isFolder = true,
                            parentFolderId = null,
                            filePath = ""
                        ))
                    }
                }
            } catch (e: Exception) {
                _errorState.value = "Failed to auto organize: ${e.message}"
            }
        }
    }

    fun getFilesByFolder(folderId: Int): StateFlow<List<DocumentFile>> = repository.getFilesByFolder(folderId)
        .catch { e -> _errorState.value = "Failed to load files for folder: ${e.message}" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertFile(context: android.content.Context, file: DocumentFile, uri: android.net.Uri? = null) {
        viewModelScope.launch {
            try {
                var calculatedSize = file.sizeBytes
                val localPath = if (!file.isFolder) {
                    val appDir = context.filesDir
                    val sandboxedFile = java.io.File(appDir, "${System.currentTimeMillis()}_${file.name}")
                    
                    if (uri != null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                    val vault = com.scholarvault.util.SecurityVault(context)
                                    vault.saveEncryptedFileFromStream(sandboxedFile.name, inputStream)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else {
                        // Create empty file
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val vault = com.scholarvault.util.SecurityVault(context)
                            vault.saveEncryptedFile(sandboxedFile.name, ByteArray(0))
                        }
                    }
                    val physicalFile = java.io.File(context.filesDir, sandboxedFile.name)
                    if (physicalFile.exists()) {
                        calculatedSize = physicalFile.length()
                    }
                    sandboxedFile.absolutePath
                } else {
                    file.filePath
                }

                val finalFile = file.copy(filePath = localPath, sizeBytes = calculatedSize)
                repository.insertFile(finalFile)
            } catch (e: Exception) {
                _errorState.value = "Failed to create file: ${e.message}"
            }
        }
    }

    val trashedFiles: StateFlow<List<DocumentFile>> = repository.getTrashedFiles()
        .catch { e -> _errorState.value = "Failed to load trashed files: ${e.message}" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun trashFile(id: Int) {
        viewModelScope.launch { repository.moveToTrash(id) }
    }

    fun restoreFile(id: Int) {
        viewModelScope.launch { repository.restoreFromTrash(id) }
    }

    suspend fun importFiles(context: android.content.Context, uris: List<android.net.Uri>, parentFolderId: Int?) {
        uris.forEach { uri ->
            var fileName = "New_File_${System.currentTimeMillis()}"
            var finalExt = ""
            var finalSize = 0L
            
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                        if (fileName.contains('.')) {
                            finalExt = fileName.substringAfterLast(".", "")
                            fileName = fileName.substringBeforeLast(".")
                        }
                    }
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        finalSize = it.getLong(sizeIndex)
                    }
                }
            }
            
            val docFile = DocumentFile(
                name = if (finalExt.isNotEmpty()) "$fileName.$finalExt" else fileName,
                isFolder = false,
                parentFolderId = parentFolderId,
                filePath = "",
                extension = finalExt,
                sizeBytes = finalSize,
                tags = emptyList()
            )
            insertAttachmentFile(context, docFile, uri)
        }
    }
    
    suspend fun insertAttachmentFile(context: android.content.Context, file: DocumentFile, uri: android.net.Uri?): Long {
        return try {
            var calculatedSize = file.sizeBytes
            val localPath = if (!file.isFolder) {
                val appDir = if (file.isEncrypted) java.io.File(context.filesDir, "wallet_secure") else context.filesDir
                if (file.isEncrypted && !appDir.exists()) appDir.mkdirs()
                val sandboxedFile = java.io.File(appDir, "${System.currentTimeMillis()}_${file.name}")
                
                if (uri != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                if (file.isEncrypted) {
                                    val vault = com.scholarvault.util.SecurityVault(context)
                                    // Use absolute path for sandboxedFile as name if needed, but saveEncryptedFileFromStream takes fileName and implies context.filesDir.
                                    // So let's build the EncryptedFile properly. Actually SecurityVault uses `File(context.filesDir, fileName)`.
                                    val relativeName = "wallet_secure/${sandboxedFile.name}"
                                    vault.saveEncryptedFileFromStream(relativeName, inputStream)
                                } else {
                                    java.io.FileOutputStream(sandboxedFile).use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        if (!file.isEncrypted) sandboxedFile.createNewFile()
                    }
                }
                if (file.isEncrypted) {
                    val physicalFile = java.io.File(context.filesDir, "wallet_secure/${sandboxedFile.name}")
                    if (physicalFile.exists()) {
                        calculatedSize = physicalFile.length()
                    }
                    "wallet_secure/${sandboxedFile.name}"
                } else {
                    if (sandboxedFile.exists()) {
                        calculatedSize = sandboxedFile.length()
                    }
                    sandboxedFile.absolutePath
                }
            } else {
                file.filePath
            }

            val finalFile = file.copy(filePath = localPath, sizeBytes = if (!file.isFolder) calculatedSize else 0L)
            repository.insertFile(finalFile)
        } catch (e: Exception) {
            _errorState.value = "Failed to create attachment: ${e.message}"
            -1L
        }
    }

    fun updateFile(file: DocumentFile) {
        viewModelScope.launch {
            try {
                repository.updateFile(file)
            } catch (e: Exception) {
                _errorState.value = "Failed to update: ${e.message}"
            }
        }
    }

    fun deleteFile(context: android.content.Context, file: DocumentFile) {
        viewModelScope.launch {
            try {
                if (file.isFolder) {
                    deleteFolderContentsFromDiskRecursive(context, file.id)
                } else {
                    deleteSingleFileFromDisk(context, file)
                }
                repository.deleteFile(file)
            } catch (e: Exception) {
                _errorState.value = "Failed to delete file: ${e.message}"
            }
        }
    }

    private suspend fun deleteFolderContentsFromDiskRecursive(context: android.content.Context, folderId: Int) {
        val children = repository.getFilesByFolderForDeletion(folderId)
        children.forEach { child ->
            if (child.isFolder) {
                deleteFolderContentsFromDiskRecursive(context, child.id)
            } else {
                deleteSingleFileFromDisk(context, child)
            }
            repository.deleteFile(child)
        }
    }

    private fun deleteSingleFileFromDisk(context: android.content.Context, file: DocumentFile) {
        var path = file.filePath.trim()
        if (path.startsWith("file://")) {
            path = path.substring(7)
        } else if (path.startsWith("file:")) {
            path = path.substring(5)
        }
        if (path.isNotBlank()) {
            val localFile = if (path.startsWith("/")) {
                java.io.File(path)
            } else {
                java.io.File(context.filesDir, path)
            }
            if (localFile.exists()) {
                localFile.delete()
            }
        }
    }
    
    fun moveFiles(files: List<DocumentFile>, toFolderId: Int?) {
        viewModelScope.launch {
            files.forEach { file ->
                repository.updateFile(file.copy(parentFolderId = toFolderId))
            }
        }
    }


    // --- Wallet ---
    val allCategories: StateFlow<List<WalletCategory>> = repository.getAllCategories()
        .catch { e -> _errorState.value = "Failed to load wallet categories: ${e.message}" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCards: StateFlow<List<WalletCard>> = repository.getAllCards()
        .catch { e -> _errorState.value = "Failed to load cards: ${e.message}" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertCategory(category: WalletCategory) {
        viewModelScope.launch {
            try {
                repository.insertCategory(category)
            } catch (e: Exception) {
                _errorState.value = "Failed to create Category: ${e.message}"
            }
        }
    }

    fun deleteCategory(category: WalletCategory) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(category)
            } catch (e: Exception) {
                _errorState.value = "Failed to delete Category: ${e.message}"
            }
        }
    }

    fun parseFieldsOfCard(card: WalletCard?): List<WalletCardCustomField> {
        val json = card?.customFieldsJson ?: return emptyList()
        val list = mutableListOf<WalletCardCustomField>()
        try {
            val array = org.json.JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(WalletCardCustomField(
                    label = obj.optString("label"),
                    value = obj.optString("value"),
                    orderIndex = obj.optInt("orderIndex")
                ))
            }
        } catch (e: Exception) {}
        return list
    }

    fun insertCardWithFields(card: WalletCard, fields: List<WalletCardCustomField>) {
        viewModelScope.launch {
            try {
                repository.insertCardWithFields(card, fields)
            } catch (e: Exception) {
                _errorState.value = "Failed to create card: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }

    fun deleteCard(card: WalletCard) {
        viewModelScope.launch {
            try {
                repository.deleteCard(card)
            } catch (e: Exception) {
                _errorState.value = "Failed to delete card: ${e.message}"
            }
        }
    }

    class Factory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DocumentViewModel::class.java)) {
                return DocumentViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
