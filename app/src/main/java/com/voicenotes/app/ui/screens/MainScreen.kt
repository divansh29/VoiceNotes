package com.voicenotes.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.voicenotes.app.MainActivity
import com.voicenotes.app.ui.components.SpeechRecognitionCard
import com.voicenotes.app.ui.components.SimpleSpeechCard
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voicenotes.app.data.VoiceNote
import com.voicenotes.app.ui.components.RecordingButton
import com.voicenotes.app.ui.components.VoiceNoteItem
import com.voicenotes.app.ui.components.TTSQuickActions
import com.voicenotes.app.ui.components.TTSStatusIndicator
import com.voicenotes.app.viewmodel.VoiceNotesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: VoiceNotesUiState,
    voiceNotes: List<VoiceNote>,
    isPlaying: Boolean,
    currentlyPlayingId: Long?,
    isTTSSpeaking: Boolean = false,
    currentTTSText: String = "",
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayVoiceNote: (VoiceNote) -> Unit,
    onPauseAudio: () -> Unit,
    onDeleteVoiceNote: (VoiceNote) -> Unit,
    onDismissError: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onUploadFile: () -> Unit = {},
    onOpenDrive: () -> Unit = {},
    onSpeechRecognition: () -> Unit = {},
    onReadTranscript: (VoiceNote) -> Unit = {},
    onReadSummary: (VoiceNote) -> Unit = {},
    onStopTTS: () -> Unit = {},
    onOpenTTSSettings: () -> Unit = {},
    onSpeakCustomText: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "VoiceNotes",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = onUploadFile) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Upload Audio"
                    )
                }
                IconButton(onClick = onOpenDrive) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Google Drive"
                    )
                }
                IconButton(onClick = onNavigateToAnalytics) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analytics"
                    )
                }
                IconButton(onClick = onSpeechRecognition) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speech to Text"
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        
        // Main content - Everything scrollable
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TTS Status Indicator
            if (isTTSSpeaking) {
                item {
                    TTSStatusIndicator(
                        isSpeaking = isTTSSpeaking,
                        currentText = currentTTSText,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Speech Recognition section
            item {
                SimpleSpeechCard()
            }

            // Text to Speech Input section
            item {
                TextToSpeechCard(
                    isSpeaking = isTTSSpeaking,
                    onSpeakText = onSpeakCustomText,
                    onStopTTS = onStopTTS
                )
            }

            // Voice notes list header
            if (voiceNotes.isNotEmpty()) {
                item {
                    Text(
                        text = "Your Recordings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
            }

            // Voice notes list or empty state
            if (voiceNotes.isEmpty()) {
                item {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No recordings yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Use Speech to Text or Type to Speech above to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Voice notes items
                items(voiceNotes) { voiceNote ->
                    VoiceNoteItem(
                        voiceNote = voiceNote,
                        isPlaying = isPlaying && currentlyPlayingId == voiceNote.id,
                        onPlayClick = { onPlayVoiceNote(voiceNote) },
                        onPauseClick = onPauseAudio,
                        onDeleteClick = { onDeleteVoiceNote(voiceNote) },
                        onReadTranscriptClick = { onReadTranscript(voiceNote) },
                        onReadSummaryClick = { onReadSummary(voiceNote) }
                    )
                }

                // Bottom padding for last item
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    
    // Error handling
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
            onDismissError()
        }
    }
}

@Composable
fun TextToSpeechCard(
    isSpeaking: Boolean = false,
    onSpeakText: (String) -> Unit,
    onStopTTS: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✍️",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Type to Speech",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Type text and convert to speech",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text input field
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("Enter text to speak") },
                placeholder = { Text("Type your message here...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = !isSpeaking,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSpeaking) {
                    // Stop button when speaking
                    Button(
                        onClick = onStopTTS,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                } else {
                    // Speak button
                    Button(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onSpeakText(textInput)
                            }
                        },
                        enabled = textInput.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Speak",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Speak Text")
                    }
                }

                // Clear button
                OutlinedButton(
                    onClick = { textInput = "" },
                    enabled = textInput.isNotBlank() && !isSpeaking,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }

            // Speaking indicator
            if (isSpeaking) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Speaking...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
