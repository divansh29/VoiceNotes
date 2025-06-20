package com.voicenotes.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.voicenotes.app.ai.LLMProvider
import com.voicenotes.app.ai.STTProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onBackClick: () -> Unit,
    onSaveSettings: (AISettings) -> Unit,
    currentSettings: AISettings
) {
    var useRealAI by remember { mutableStateOf(currentSettings.useRealAI) }
    var useLocalAI by remember { mutableStateOf(currentSettings.useLocalAI) }
    var sttProvider by remember { mutableStateOf(currentSettings.sttProvider) }
    var llmProvider by remember { mutableStateOf(currentSettings.llmProvider) }
    var openAIKey by remember { mutableStateOf(currentSettings.openAIKey) }
    var anthropicKey by remember { mutableStateOf(currentSettings.anthropicKey) }
    var googleKey by remember { mutableStateOf(currentSettings.googleKey) }
    var showKeys by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "AI Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    onSaveSettings(
                        AISettings(
                            useRealAI = useRealAI,
                            useLocalAI = useLocalAI,
                            sttProvider = sttProvider,
                            llmProvider = llmProvider,
                            openAIKey = openAIKey,
                            anthropicKey = anthropicKey,
                            googleKey = googleKey
                        )
                    )
                }
            ) {
                Text("Save")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Mode Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Real AI Processing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (useRealAI) "Using real AI services" else "Using mock AI (demo mode)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useRealAI,
                                onCheckedChange = { useRealAI = it }
                            )
                        }
                        
                        if (!useRealAI && !useLocalAI) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💡 Demo mode uses mock AI responses for testing. Enable local or cloud AI for real processing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Local AI Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Local AI Processing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (useLocalAI) "Using on-device AI models" else "Local AI disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useLocalAI,
                                onCheckedChange = {
                                    useLocalAI = it
                                    if (it) useRealAI = false // Disable cloud AI when local is enabled
                                }
                            )
                        }

                        if (useLocalAI) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🔒 Local AI runs entirely on your device. No internet required, complete privacy, no API costs!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Speech-to-Text Provider
            if (useRealAI) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Speech-to-Text Provider",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            STTProvider.values().filter { it != STTProvider.MOCK }.forEach { provider ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = sttProvider == provider,
                                        onClick = { sttProvider = provider }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when (provider) {
                                                STTProvider.OPENAI_WHISPER -> "OpenAI Whisper"
                                                STTProvider.GOOGLE_CLOUD -> "Google Cloud Speech"
                                                STTProvider.AZURE -> "Azure Speech Services"
                                                STTProvider.MOCK -> "Mock"
                                            },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = when (provider) {
                                                STTProvider.OPENAI_WHISPER -> "High accuracy, supports many languages"
                                                STTProvider.GOOGLE_CLOUD -> "Fast processing, good for real-time"
                                                STTProvider.AZURE -> "Enterprise-grade, good accuracy"
                                                STTProvider.MOCK -> "Demo mode"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // LLM Provider
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "AI Analysis Provider",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            LLMProvider.values().filter { it != LLMProvider.MOCK }.forEach { provider ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = llmProvider == provider,
                                        onClick = { llmProvider = provider }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when (provider) {
                                                LLMProvider.OPENAI -> "OpenAI GPT"
                                                LLMProvider.ANTHROPIC -> "Anthropic Claude"
                                                LLMProvider.GOOGLE -> "Google Gemini"
                                                LLMProvider.MOCK -> "Mock"
                                            },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = when (provider) {
                                                LLMProvider.OPENAI -> "Excellent for summaries and analysis"
                                                LLMProvider.ANTHROPIC -> "Great for detailed analysis"
                                                LLMProvider.GOOGLE -> "Fast and efficient processing"
                                                LLMProvider.MOCK -> "Demo mode"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // API Keys
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "API Keys",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(onClick = { showKeys = !showKeys }) {
                                    Icon(
                                        imageVector = if (showKeys) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showKeys) "Hide keys" else "Show keys"
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // OpenAI API Key
                            OutlinedTextField(
                                value = openAIKey,
                                onValueChange = { openAIKey = it },
                                label = { Text("OpenAI API Key") },
                                placeholder = { Text("sk-...") },
                                visualTransformation = if (showKeys) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Anthropic API Key
                            OutlinedTextField(
                                value = anthropicKey,
                                onValueChange = { anthropicKey = it },
                                label = { Text("Anthropic API Key") },
                                placeholder = { Text("sk-ant-...") },
                                visualTransformation = if (showKeys) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Google API Key
                            OutlinedTextField(
                                value = googleKey,
                                onValueChange = { googleKey = it },
                                label = { Text("Google AI API Key") },
                                placeholder = { Text("AIza...") },
                                visualTransformation = if (showKeys) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "🔒 API keys are stored securely on your device and never shared.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Instructions
                item {
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
                                text = "📋 How to get API keys:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• OpenAI: platform.openai.com → API Keys\n" +
                                        "• Anthropic: console.anthropic.com → API Keys\n" +
                                        "• Google: makersuite.google.com → Get API Key",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

data class AISettings(
    val useRealAI: Boolean = false,
    val useLocalAI: Boolean = false,
    val sttProvider: STTProvider = STTProvider.MOCK,
    val llmProvider: LLMProvider = LLMProvider.MOCK,
    val openAIKey: String = "",
    val anthropicKey: String = "",
    val googleKey: String = ""
)
