package com.voicenotes.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voicenotes.app.data.VoiceNote
import com.voicenotes.app.ui.components.RecordingButton
import com.voicenotes.app.ui.components.VoiceNoteItem
import com.voicenotes.app.viewmodel.VoiceNotesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: VoiceNotesUiState,
    voiceNotes: List<VoiceNote>,
    isPlaying: Boolean,
    currentlyPlayingId: Long?,
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
        
        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column {
                // Recording section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (uiState.isRecording) "Recording..." else "Tap to Record",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        RecordingButton(
                            isRecording = uiState.isRecording,
                            onStartRecording = onStartRecording,
                            onStopRecording = onStopRecording
                        )
                        
                        if (uiState.isProcessing) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Processing recording...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Voice notes list
                if (voiceNotes.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxWidth(),
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
                                text = "Tap the microphone button to create your first voice note",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Voice notes list
                    Text(
                        text = "Your Recordings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(voiceNotes) { voiceNote ->
                            VoiceNoteItem(
                                voiceNote = voiceNote,
                                isPlaying = isPlaying && currentlyPlayingId == voiceNote.id,
                                onPlayClick = { onPlayVoiceNote(voiceNote) },
                                onPauseClick = onPauseAudio,
                                onDeleteClick = { onDeleteVoiceNote(voiceNote) }
                            )
                        }
                    }
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
