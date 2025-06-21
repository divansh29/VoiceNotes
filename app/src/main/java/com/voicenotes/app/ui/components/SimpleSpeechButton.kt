package com.voicenotes.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicenotes.app.MainActivity

@Composable
fun SimpleSpeechButton(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(16.dp)
    ) {
        FloatingActionButton(
            onClick = {
                // Direct call to MainActivity's speech recognition
                (context as? MainActivity)?.startSpeechRecognition()
            },
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Speech to Text",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSecondary
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "🎙️ Speech to Text",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Tap to convert speech to voice note",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SimpleSpeechCard(
    modifier: Modifier = Modifier,
    lastTranscript: String? = null,
    lastSummary: String? = null,
    lastKeywords: List<String>? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            SimpleSpeechButton()

            // Show last speech recognition results
            if (lastTranscript != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "🎙️ Last Speech Recognition:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Summary
                        if (lastSummary != null) {
                            Text(
                                text = "📝 Summary: $lastSummary",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Keywords
                        if (!lastKeywords.isNullOrEmpty()) {
                            Text(
                                text = "🏷️ Keywords: ${lastKeywords.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Full transcript (truncated)
                        Text(
                            text = "💬 \"${if (lastTranscript.length > 100) lastTranscript.take(97) + "..." else lastTranscript}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}
