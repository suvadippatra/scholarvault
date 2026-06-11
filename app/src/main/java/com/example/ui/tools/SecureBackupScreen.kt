package com.scholarvault.ui.tools

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.theme.LocalThemeController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureBackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalThemeController.current.isDarkTheme

    var activeTab by remember { mutableStateOf("Backup") } // "Backup" or "Restore"

    // Backup state
    var backupPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }
    var backupFileByScope by remember { mutableStateOf<File?>(null) }

    // Restore state
    var restorePassword by remember { mutableStateOf("") }
    var restorePasswordVisible by remember { mutableStateOf(false) }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedRestoreUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Secure Backup",
                showProfileIcon = false,
                showSearchBar = false
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Description
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
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
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            "Offline AES-256 Vault Backup",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Encrypt your database and documents locally. Move safely to other devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tab toggler
            TabRow(
                selectedTabIndex = if (activeTab == "Backup") 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = activeTab == "Backup",
                    onClick = { activeTab = "Backup" },
                    text = { Text("Create Backup", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Backup, contentDescription = null) }
                )
                Tab(
                    selected = activeTab == "Restore",
                    onClick = { activeTab = "Restore" },
                    text = { Text("Restore Vault", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Restore, contentDescription = null) }
                )
            }

            if (activeTab == "Backup") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Set AES-256 Encryption Password",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "CRITICAL: ScholarVault is offline-first. There is NO forgot password mechanism. If you lose this password, you can never restore your files.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            lineHeight = 18.sp
                        )

                        // Password fields
                        OutlinedTextField(
                            value = backupPassword,
                            onValueChange = { backupPassword = it },
                            label = { Text("Encryption Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Encryption Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Backup Action button
                        Button(
                            onClick = {
                                if (backupPassword.length < 6) {
                                    Toast.makeText(context, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (backupPassword != confirmPassword) {
                                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isBackingUp = true
                                scope.launch {
                                    createEncryptedBackup(context, backupPassword) { file ->
                                        backupFileByScope = file
                                        isBackingUp = false
                                        Toast.makeText(context, "Encrypted backup file compiled successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isBackingUp && backupPassword.isNotEmpty()
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Text("Compiling Secure Backup...")
                            } else {
                                Icon(Icons.Default.Backup, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Backup File")
                            }
                        }
                    }
                }

                if (backupFileByScope != null) {
                    // Quick share panel
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Backup Ready!",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                "File size: " + formatBytes(backupFileByScope!!.length()) + "\nPath: " + backupFileByScope!!.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Button(
                                onClick = {
                                    shareBackupFile(context, backupFileByScope!!)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share / Export Backup File")
                            }
                        }
                    }
                }
            } else {
                // Restore Panel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Select and Decrypt Backup",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { filePickerLauncher.launch("*/*") }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedRestoreUri == null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Tap to pick .svb file", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Selected Backup File:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        selectedRestoreUri!!.lastPathSegment ?: "backup.svb",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (selectedRestoreUri != null) {
                            OutlinedTextField(
                                value = restorePassword,
                                onValueChange = { restorePassword = it },
                                label = { Text("Backup Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { restorePasswordVisible = !restorePasswordVisible }) {
                                        Icon(
                                            imageVector = if (restorePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (restorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    isRestoring = true
                                    scope.launch {
                                        restoreEncryptedBackup(context, selectedRestoreUri!!, restorePassword) { success, errorMsg ->
                                            isRestoring = false
                                            if (success) {
                                                Toast.makeText(context, "VAULT RESTORED SUCCESSFULLY! Direct changes will load on restart.", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Restore failed: $errorMsg", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isRestoring && restorePassword.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                if (isRestoring) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    Text("Decrypting & Unzipping...")
                                } else {
                                    Icon(Icons.Default.Restore, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Perform Decryption & Restore")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun createEncryptedBackup(
    context: Context,
    passwordStr: String,
    onComplete: (File) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val tempZip = File(context.cacheDir, "backup_temp.zip")
        if (tempZip.exists()) tempZip.delete()

        val dbFile = context.getDatabasePath("scholar-vault-db")
        val dbShm = context.getDatabasePath("scholar-vault-db-shm")
        val dbWal = context.getDatabasePath("scholar-vault-db-wal")

        // 1. Compile databases and files into temporary ZIP file
        ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZip))).use { zos ->
            // Add database files
            addFileToZip(dbFile, "db/scholar-vault-db", zos)
            if (dbShm.exists()) addFileToZip(dbShm, "db/scholar-vault-db-shm", zos)
            if (dbWal.exists()) addFileToZip(dbWal, "db/scholar-vault-db-wal", zos)

            // Add all attachments and materials inside filesDir
            val filesDir = context.filesDir
            zipDirectory(filesDir, "files/", zos)
        }

        // 2. Encrypt ZIP using Password-based AES-256-GCM
        val finalBackupFile = File(context.cacheDir, "ScholarVault_Backup_${System.currentTimeMillis()}.svb")

        val salt = ByteArray(16)
        val iv = ByteArray(12)
        val random = SecureRandom()
        random.nextBytes(salt)
        random.nextBytes(iv)

        val secretKey = deriveSecretKey(passwordStr, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val zipBytes = tempZip.readBytes()
        val encryptedData = cipher.doFinal(zipBytes)

        FileOutputStream(finalBackupFile).use { fos ->
            fos.write(salt)
            fos.write(iv)
            fos.write(encryptedData)
            fos.flush()
        }

        tempZip.delete() // Cleanup temporary unencrypted zip
        onComplete(finalBackupFile)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private suspend fun restoreEncryptedBackup(
    context: Context,
    backupUri: Uri,
    passwordStr: String,
    onComplete: (Boolean, String?) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        // Read header info
        val inputStream = context.contentResolver.openInputStream(backupUri)
        if (inputStream == null) {
            onComplete(false, "Cannot read selected file")
            return@withContext
        }

        val allBytes = inputStream.use { it.readBytes() }
        if (allBytes.size < 28) {
            onComplete(false, "Selected file is corrupted or incomplete")
            return@withContext
        }

        val salt = allBytes.copyOfRange(0, 16)
        val iv = allBytes.copyOfRange(16, 28)
        val cipherBytes = allBytes.copyOfRange(28, allBytes.size)

        // Derivate secretKey and Decrypt
        val secretKey = deriveSecretKey(passwordStr, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedZipBytes: ByteArray
        try {
            decryptedZipBytes = cipher.doFinal(cipherBytes)
        } catch (e: Exception) {
            onComplete(false, "Incorrect password or decryption failed")
            return@withContext
        }

        val tempDecryptedZip = File(context.cacheDir, "decrypted_temp.zip")
        tempDecryptedZip.writeBytes(decryptedZipBytes)

        // Unzip and restore files and DBS
        val filesDir = context.filesDir
        val dbFile = context.getDatabasePath("scholar-vault-db")
        val dbShm = context.getDatabasePath("scholar-vault-db-shm")
        val dbWal = context.getDatabasePath("scholar-vault-db-wal")

        // Stop core connection to DB if writing directly
        ZipInputStream(BufferedInputStream(FileInputStream(tempDecryptedZip))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryName = entry.name
                    if (entryName.startsWith("db/")) {
                        val finalDbFile = when (entryName) {
                            "db/scholar-vault-db" -> dbFile
                            "db/scholar-vault-db-shm" -> dbShm
                            "db/scholar-vault-db-wal" -> dbWal
                            else -> null
                        }
                        if (finalDbFile != null) {
                            if (!finalDbFile.parentFile.exists()) finalDbFile.parentFile.mkdirs()
                            FileOutputStream(finalDbFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                    } else if (entryName.startsWith("files/")) {
                        val subPath = entryName.substring(6)
                        val targetFile = File(filesDir, subPath)
                        if (!targetFile.parentFile.exists()) targetFile.parentFile.mkdirs()
                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        tempDecryptedZip.delete()
        onComplete(true, null)
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(false, e.localizedMessage ?: "Unknown error")
    }
}

private fun deriveSecretKey(password: String, salt: ByteArray): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
    val tmp = factory.generateSecret(spec)
    return SecretKeySpec(tmp.encoded, "AES")
}

private fun addFileToZip(file: File, pathInZip: String, zos: ZipOutputStream) {
    if (!file.exists()) return
    val entry = ZipEntry(pathInZip)
    zos.putNextEntry(entry)
    FileInputStream(file).use { fis ->
        fis.copyTo(zos)
    }
    zos.closeEntry()
}

private fun zipDirectory(directory: File, pathInZip: String, zos: ZipOutputStream) {
    directory.listFiles()?.forEach { file ->
        val childPath = pathInZip + file.name
        if (file.isDirectory) {
            zipDirectory(file, "$childPath/", zos)
        } else {
            addFileToZip(file, childPath, zos)
        }
    }
}

private fun shareBackupFile(context: Context, file: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Export Encrypted Backup"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error sharing backup", Toast.LENGTH_SHORT).show()
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.toDouble())).toInt().coerceIn(0, units.size - 1)
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.toDouble(), digitGroups.toDouble()), units[digitGroups])
}
