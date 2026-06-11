package com.scholarvault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scholarvault.security.LockType
import com.scholarvault.security.WalletSecurityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSecurityScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val securityManager = remember { WalletSecurityManager(context) }
    val isSecurityEnabled by securityManager.isSecurityEnabled.collectAsState()
    
    var isSettingUp by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Wallet Security",
                showProfileIcon = false,
                showSearchBar = false
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isSettingUp) {
                WalletSecuritySetup(securityManager) { isSettingUp = false }
            } else {
                WalletSecurityMenu(securityManager, onSetupRequested = { isSettingUp = true })
            }
        }
    }
}

@Composable
fun WalletSecuritySetup(securityManager: WalletSecurityManager, onComplete: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    var selectedType by remember { mutableStateOf(LockType.PIN) }
    var lockData by remember { mutableStateOf("") }
    var allowBio by remember { mutableStateOf(false) }
    var secQuestion by remember { mutableStateOf("") }
    var secAnswer by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Setup Wallet Security", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (step == 1) {
            Text("Select Lock Method:")
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                FilterChip(selected = selectedType == LockType.PIN, onClick = { selectedType = LockType.PIN }, label = { Text("PIN") })
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(selected = selectedType == LockType.PASSWORD, onClick = { selectedType = LockType.PASSWORD }, label = { Text("Password") })
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(selected = selectedType == LockType.PATTERN, onClick = { selectedType = LockType.PATTERN }, label = { Text("Pattern") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (selectedType == LockType.PATTERN) {
                Text("Draw your pattern to set it:")
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    com.scholarvault.ui.components.PatternLock(
                        onPatternComplete = { pattern ->
                            if (pattern.length >= 4) {
                                lockData = pattern
                                step = 2
                            }
                        }
                    )
                }
                if (lockData.length < 4) {
                    Text("Connect at least 4 dots to proceed.", color = MaterialTheme.colorScheme.primary)
                }
            } else {
                OutlinedTextField(
                    value = lockData,
                    onValueChange = { lockData = it },
                    label = { Text("Enter ${selectedType.name}") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = if (selectedType == LockType.PIN) androidx.compose.ui.text.input.KeyboardType.NumberPassword else androidx.compose.ui.text.input.KeyboardType.Password
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { if (lockData.isNotEmpty()) step = 2 }) {
                    Text("Next")
                }
            }
        } else if (step == 2) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = allowBio, onCheckedChange = { allowBio = it })
                Text("Allow Biometric Unlock")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Security Question (Required for Reset):")
            OutlinedTextField(value = secQuestion, onValueChange = { secQuestion = it }, label = { Text("Question (e.g. Pet's name)") })
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = secAnswer, onValueChange = { secAnswer = it }, label = { Text("Answer") })
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { 
                if (secQuestion.isNotEmpty() && secAnswer.isNotEmpty()) {
                    securityManager.setSecurity(selectedType, lockData, secQuestion, secAnswer, allowBio)
                    onComplete()
                }
            }) {
                Text("Finish Setup")
            }
        }
    }
}

@Composable
fun WalletSecurityMenu(securityManager: WalletSecurityManager, onSetupRequested: () -> Unit) {
    var showVerifyDialog by remember { mutableStateOf(false) }
    var verifyAction by remember { mutableStateOf("") }
    
    val isSecurityEnabled by securityManager.isSecurityEnabled.collectAsState()
    val bioEnabled by securityManager.isBiometricEnabled.collectAsState()

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        SettingToggleItem(
            title = "Wallet Security",
            subtitle = if (isSecurityEnabled) "ON - Secured" else "OFF - Unsecured",
            checked = isSecurityEnabled,
            onCheckedChange = { 
                if (it) {
                    onSetupRequested()
                } else {
                    verifyAction = "turn_off"
                    showVerifyDialog = true
                }
            }
        )
        
        if (isSecurityEnabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            SettingClickItem(
                title = "Change Lock Method",
                subtitle = "Change PIN/Password",
                onClick = { verifyAction = "change_lock"; showVerifyDialog = true }
            )
            
            SettingClickItem(
                title = "Change Unlock Key",
                subtitle = "Update your current lock data",
                onClick = { verifyAction = "change_key"; showVerifyDialog = true }
            )
            
            SettingToggleItem(
                title = "Allow Biometric",
                subtitle = "Use fingerprint/face to unlock",
                checked = bioEnabled,
                onCheckedChange = { securityManager.updateBiometric(it) }
            )
            
            SettingClickItem(
                title = "Change Security Question",
                subtitle = "Update recovery details",
                onClick = { verifyAction = "change_question"; showVerifyDialog = true }
            )
        }
    }

    if (showVerifyDialog) {
        var verifyInput by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        var isVerified by remember { mutableStateOf(false) }
        
        if (!isVerified) {
            AlertDialog(
                onDismissRequest = { showVerifyDialog = false },
                title = { Text("Verification Required") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = verifyInput,
                            onValueChange = { verifyInput = it; isError = false },
                            label = { Text("Enter current lock key") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )
                        if (isError) {
                            Text("Incorrect key", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (securityManager.verifyLockData(verifyInput)) {
                            when (verifyAction) {
                                "turn_off" -> {
                                    securityManager.disableSecurity()
                                    showVerifyDialog = false
                                }
                                else -> isVerified = true
                            }
                        } else {
                            isError = true
                        }
                    }) { Text("Verify") }
                },
                dismissButton = {
                    TextButton(onClick = { showVerifyDialog = false }) { Text("Cancel") }
                }
            )
        } else {
            // Handle updates after verification
            when (verifyAction) {
                "change_lock", "change_key" -> {
                    var newKey by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showVerifyDialog = false },
                        title = { Text("Enter New Key") },
                        text = {
                            OutlinedTextField(
                                value = newKey,
                                onValueChange = { newKey = it },
                                label = { Text("New Key") }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (newKey.isNotEmpty()) {
                                    securityManager.updateLockData(newKey)
                                    showVerifyDialog = false
                                }
                            }) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showVerifyDialog = false }) { Text("Cancel") }
                        }
                    )
                }
                "change_question" -> {
                    var newQ by remember { mutableStateOf("") }
                    var newA by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showVerifyDialog = false },
                        title = { Text("New Security Question") },
                        text = {
                            Column {
                                OutlinedTextField(value = newQ, onValueChange = { newQ = it }, label = { Text("Question") })
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = newA, onValueChange = { newA = it }, label = { Text("Answer") })
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (newQ.isNotEmpty() && newA.isNotEmpty()) {
                                    securityManager.updateSecurityQuestion(newQ, newA)
                                    showVerifyDialog = false
                                }
                            }) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showVerifyDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }
}
