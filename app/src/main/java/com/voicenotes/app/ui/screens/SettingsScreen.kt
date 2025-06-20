package com.voicenotes.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicenotes.app.naming.NamingManager
import com.voicenotes.app.notifications.NotificationService
import com.voicenotes.app.security.SecurityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val securityManager = remember { SecurityManager(context) }
    val namingManager = remember { NamingManager(context) }
    
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showNamingDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Recording & AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.DriveFileRenameOutline,
                    title = "Recording Names",
                    subtitle = "Customize how recordings are named",
                    onClick = { showNamingDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Analytics,
                    title = "Speaking Analytics",
                    subtitle = "View your speaking patterns and statistics",
                    onClick = { /* Navigate to analytics */ }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Security & Privacy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Security,
                    title = "PIN Protection",
                    subtitle = if (securityManager.isSecurityEnabled()) "Active - Required on app start" else "Change PIN or setup security",
                    onClick = { showSecurityDialog = true }
                )
            }

            item {
                SettingsCard(
                    icon = Icons.Default.SmartToy,
                    title = "AI Settings",
                    subtitle = "Configure speech-to-text and AI analysis",
                    onClick = { /* Navigate to AI Settings */ }
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Authentication",
                    subtitle = if (securityManager.isBiometricEnabled()) "Enabled" else "Disabled",
                    enabled = securityManager.isBiometricAvailable(),
                    onClick = { showSecurityDialog = true }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Notifications & Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Notifications,
                    title = "Daily Reminders",
                    subtitle = "Get reminded to record daily voice notes",
                    onClick = { showNotificationDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Task,
                    title = "Action Item Reminders",
                    subtitle = "Get notified about extracted action items",
                    onClick = { showNotificationDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.CalendarToday,
                    title = "Calendar Integration",
                    subtitle = "Auto-record meetings from calendar",
                    onClick = { /* Navigate to calendar settings */ }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Info,
                    title = "App Version",
                    subtitle = "VoiceNotes 1.0.0",
                    onClick = { }
                )
            }
        }
    }
    
    // Dialogs
    if (showSecurityDialog) {
        SecuritySettingsDialog(
            securityManager = securityManager,
            onDismiss = { showSecurityDialog = false }
        )
    }
    
    if (showNamingDialog) {
        NamingSettingsDialog(
            namingManager = namingManager,
            onDismiss = { showNamingDialog = false }
        )
    }
    
    if (showNotificationDialog) {
        NotificationSettingsDialog(
            onDismiss = { showNotificationDialog = false }
        )
    }
}

@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SecuritySettingsDialog(
    securityManager: SecurityManager,
    onDismiss: () -> Unit
) {
    var biometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled()) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Security Settings") },
        text = {
            Column {
                // PIN Status (Read-only)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "PIN Protection Status",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (securityManager.isSecurityEnabled())
                                "✓ PIN is active and required on every app start"
                            else
                                "⚠ No PIN configured - App is not secured",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (securityManager.isSecurityEnabled())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Change PIN Button
                Button(
                    onClick = { showChangePinDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (securityManager.isSecurityEnabled()) "Change PIN" else "Set Up PIN")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Biometric Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric Authentication")
                        Text(
                            text = "Use fingerprint/face unlock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = biometricEnabled,
                        enabled = securityManager.isSecurityEnabled() && securityManager.isBiometricAvailable(),
                        onCheckedChange = {
                            biometricEnabled = it
                            securityManager.enableBiometric(it)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )

    // Change PIN Dialog
    if (showChangePinDialog) {
        com.voicenotes.app.ui.components.PINSetupDialog(
            onDismiss = { showChangePinDialog = false },
            onPINSet = { pin ->
                securityManager.setupPIN(pin)
                showChangePinDialog = false
            }
        )
    }
}

@Composable
fun NamingSettingsDialog(
    namingManager: NamingManager,
    onDismiss: () -> Unit
) {
    var selectedPattern by remember { mutableStateOf(namingManager.getNamingPattern()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recording Names") },
        text = {
            Column {
                Text("Choose how recordings are named:")
                Spacer(modifier = Modifier.height(16.dp))
                
                listOf(
                    "smart" to "Smart Naming",
                    "date_time" to "Date & Time",
                    "sequential" to "Sequential Numbers"
                ).forEach { (pattern, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPattern == pattern,
                            onClick = { selectedPattern = pattern }
                        )
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    namingManager.setNamingPattern(selectedPattern)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NotificationSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var dailyReminders by remember { mutableStateOf(true) }
    var actionItemReminders by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Settings") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Reminders")
                    Switch(
                        checked = dailyReminders,
                        onCheckedChange = { 
                            dailyReminders = it
                            if (it) {
                                NotificationService.scheduleDailyReminder(context)
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Action Item Reminders")
                    Switch(
                        checked = actionItemReminders,
                        onCheckedChange = { actionItemReminders = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
