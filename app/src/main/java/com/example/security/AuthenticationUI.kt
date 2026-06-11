package com.scholarvault.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun WalletAuthenticationWrapper(
    securityManager: WalletSecurityManager,
    onUnlockSuccess: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isSecurityEnabled by securityManager.isSecurityEnabled.collectAsState()
    val lockType by securityManager.lockType.collectAsState()
    val isBiometricEnabled by securityManager.isBiometricEnabled.collectAsState()
    
    var isUnlocked by remember { mutableStateOf(!isSecurityEnabled || securityManager.isSessionUnlocked) }
    var inputData by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var inResetMode by remember { mutableStateOf(false) }

    if (!isSecurityEnabled) {
        content()
        return
    }

    if (isUnlocked || securityManager.isSessionUnlocked) {
        content()
        return
    }

    LaunchedEffect(Unit) {
        if (isBiometricEnabled) {
            triggerBiometricUnlock(context, false) { success ->
                if (success) {
                    isUnlocked = true
                    securityManager.isSessionUnlocked = true
                    onUnlockSuccess()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (inResetMode) "Reset Wallet Security" else "Unlock Wallet",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (!inResetMode) {
                // PIN input display
                if (lockType == LockType.PIN) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        repeat(inputData.length) {
                            Box(modifier = Modifier.padding(4.dp).size(16.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                    if (isError) Text("Incorrect PIN", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(48.dp))
                    CustomPinPad(
                        onNumberClick = { num ->
                            inputData += num
                            isError = false
                            // Auto check if pin is 4 to 6 chars? Let's check whenever changed.
                            if (securityManager.verifyLockData(inputData)) {
                                isUnlocked = true
                                securityManager.isSessionUnlocked = true
                                onUnlockSuccess()
                            } else if (inputData.length >= 6) {
                                isError = true
                                inputData = ""
                            }
                        },
                        onDeleteClick = { if (inputData.isNotEmpty()) inputData = inputData.dropLast(1) }
                    )
                } else if (lockType == LockType.PATTERN) {
                    com.scholarvault.ui.components.PatternLock(
                        onPatternComplete = { pattern ->
                            if (securityManager.verifyLockData(pattern)) {
                                isUnlocked = true
                                securityManager.isSessionUnlocked = true
                                onUnlockSuccess()
                            } else {
                                isError = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isError) Text("Incorrect Pattern", color = MaterialTheme.colorScheme.error)
                } else {
                // Password fallback
                OutlinedTextField(
                    value = inputData,
                    onValueChange = { inputData = it; isError = false },
                    label = { Text("Enter Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            if (securityManager.verifyLockData(inputData)) {
                                isUnlocked = true
                                securityManager.isSessionUnlocked = true
                                onUnlockSuccess()
                            } else {
                                isError = true
                            }
                        }
                    )
                )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (securityManager.verifyLockData(inputData)) {
                            isUnlocked = true
                            securityManager.isSessionUnlocked = true
                            onUnlockSuccess()
                        } else {
                            isError = true
                        }
                    }) {
                        Text("Unlock")
                    }
                    if (isError) Text("Incorrect ${lockType.name.lowercase()}", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(48.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    TextButton(onClick = { 
                        // Start reset flow
                        triggerBiometricUnlock(context, true) {
                            inResetMode = true 
                        }
                    }) {
                        Text("forgot key ?", fontSize = 12.sp)
                    }
                }
            } else {
                // Reset Mode
                val q = securityManager.getSecurityQuestion()
                if (q != null) {
                    Text("Security Question: $q", fontWeight = FontWeight.SemiBold)
                    var qAns by remember { mutableStateOf("") }
                    var qErr by remember { mutableStateOf(false) }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = qAns,
                        onValueChange = { qAns = it; qErr = false },
                        label = { Text("Answer") }
                    )
                    if (qErr) Text("Incorrect Answer", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (securityManager.verifySecurityAnswer(qAns)) {
                            securityManager.disableSecurity()
                            isUnlocked = true
                            onCancel() // Back to settings to setup again
                        } else {
                            qErr = true
                        }
                    }) { Text("Confirm Reset") }
                } else {
                    Text("No security question set. Cannot reset.")
                }
                
                TextButton(onClick = { inResetMode = false }) { Text("Back") }
            }
        }
    }
}

@Composable
fun CustomPinPad(onNumberClick: (String) -> Unit, onDeleteClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "\u232B") // Delete symbol
        )
        for (row in keys) {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth(0.8f)) {
                for (key in row) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .padding(8.dp)
                            .background(
                                color = if (key.isNotEmpty() && key != "\u232B") MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent, 
                                shape = CircleShape
                            )
                            .clickable(enabled = key.isNotEmpty()) {
                                if (key == "\u232B") onDeleteClick() else onNumberClick(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun triggerBiometricUnlock(context: android.content.Context, requireStrong: Boolean, onResult: (Boolean) -> Unit) {
    val biometricManager = BiometricManager.from(context)
    val authenticators = if (requireStrong) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
    
    if (biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            context as FragmentActivity, 
            executor, 
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(true)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (requireStrong) "Reset Wallet Security" else "Unlock Secure Wallet")
            .setSubtitle("Verify identity")
            .setAllowedAuthenticators(authenticators)
            .build()
            
        biometricPrompt.authenticate(promptInfo)
    }
}
