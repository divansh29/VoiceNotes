package com.voicenotes.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.voicenotes.app.security.SecurityManager
import kotlinx.coroutines.launch

@Composable
fun AuthenticationScreen(
    securityManager: SecurityManager,
    onAuthenticated: () -> Unit,
    onSetupSecurity: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }
    
    // Check if this is first time setup or if PIN is configured
    val isFirstTimeSetup = securityManager.isFirstTimeSetup()
    val isSecurityEnabled = securityManager.isSecurityEnabled()
    val isBiometricEnabled = securityManager.isBiometricEnabled()

    if (isFirstTimeSetup || !isSecurityEnabled) {
        // Show setup screen for first time users or if PIN not configured
        SecuritySetupScreen(
            onSetupPin = onSetupSecurity,
            onSkip = {
                // Mark setup as complete even if skipped
                securityManager.markFirstTimeSetupComplete()
                onAuthenticated()
            }
        )
    } else {
        // Show authentication screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo/Icon
            Card(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "VoiceNotes",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "VoiceNotes",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Enter your PIN to continue",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // PIN Input
            OutlinedTextField(
                value = pin,
                onValueChange = { 
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        pin = it
                        error = ""
                    }
                },
                label = { Text("PIN") },
                visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                trailingIcon = {
                    IconButton(onClick = { showPin = !showPin }) {
                        Icon(
                            imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPin) "Hide PIN" else "Show PIN"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isError = error.isNotEmpty(),
                enabled = !isAuthenticating
            )
            
            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Unlock Button
            Button(
                onClick = {
                    if (pin.length >= 4) {
                        isAuthenticating = true
                        if (securityManager.verifyPIN(pin)) {
                            onAuthenticated()
                        } else {
                            error = "Incorrect PIN. Please try again."
                            pin = ""
                            isAuthenticating = false
                        }
                    } else {
                        error = "Please enter your PIN"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = pin.isNotEmpty() && !isAuthenticating
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Unlock", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            // Biometric Authentication Button
            if (isBiometricEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                val success = securityManager.authenticateWithBiometric(context as FragmentActivity)
                                if (success) {
                                    onAuthenticated()
                                } else {
                                    error = "Biometric authentication failed"
                                }
                            } catch (e: Exception) {
                                error = "Biometric authentication error"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Biometric")
                }
            }
        }
    }
}

@Composable
fun SecuritySetupScreen(
    onSetupPin: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Security",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Secure Your Voice Notes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "For your privacy and security, please set up a PIN to protect your voice recordings.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSetupPin,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Set Up PIN Protection", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue Without PIN")
        }
    }
}
