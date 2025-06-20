package com.voicenotes.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicenotes.app.ai.AIService
import com.voicenotes.app.data.VoiceNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    voiceNotes: List<VoiceNote>,
    onBackClick: () -> Unit
) {
    val analytics = remember(voiceNotes) {
        calculateAnalytics(voiceNotes)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speaking Analytics") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OverallStatsCard(analytics.overallStats)
            }
            
            item {
                SpeakingSpeedCard(analytics.averageWPM, analytics.wpmTrend)
            }
            
            item {
                RecordingHabitsCard(analytics.recordingFrequency)
            }
            
            item {
                Text(
                    text = "Recent Recordings Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(analytics.recentAnalysis) { analysis ->
                RecordingAnalysisCard(analysis)
            }
        }
    }
}

@Composable
fun OverallStatsCard(stats: OverallStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Overall Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.BarChart,
                    label = "Total Recordings",
                    value = stats.totalRecordings.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    icon = Icons.Default.Timer,
                    label = "Total Time",
                    value = formatDuration(stats.totalDuration),
                    color = MaterialTheme.colorScheme.secondary
                )
                
                StatItem(
                    icon = Icons.Default.Speed,
                    label = "Avg WPM",
                    value = stats.averageWPM.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SpeakingSpeedCard(averageWPM: Int, trend: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Speaking Speed Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$averageWPM WPM",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Average Speed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = trend,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            trend.contains("↑") -> Color.Green
                            trend.contains("↓") -> Color.Red
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "Trend",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Speed interpretation
            val interpretation = when {
                averageWPM > 180 -> "Fast speaker - Great for quick idea capture"
                averageWPM > 120 -> "Normal pace - Well-balanced speaking speed"
                averageWPM > 80 -> "Thoughtful pace - Good for detailed explanations"
                else -> "Slow pace - Take your time, that's perfectly fine"
            }
            
            Text(
                text = interpretation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecordingHabitsCard(frequency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recording Habits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = frequency,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val encouragement = when {
                frequency.contains("daily") -> "Excellent! You're building a great habit 🎉"
                frequency.contains("weekly") -> "Good consistency! Try recording more often 📈"
                frequency.contains("monthly") -> "You're getting started! Consider daily recordings 🚀"
                else -> "Start building your recording habit today! 💪"
            }
            
            Text(
                text = encouragement,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecordingAnalysisCard(analysis: RecordingAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = analysis.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${analysis.wpm} WPM",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = analysis.confidence,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

// Data classes for analytics
data class AnalyticsData(
    val overallStats: OverallStats,
    val averageWPM: Int,
    val wpmTrend: String,
    val recordingFrequency: String,
    val recentAnalysis: List<RecordingAnalysis>
)

data class OverallStats(
    val totalRecordings: Int,
    val totalDuration: Long,
    val averageWPM: Int
)

data class RecordingAnalysis(
    val title: String,
    val wpm: Int,
    val confidence: String
)

// Helper function to calculate analytics from voice notes
fun calculateAnalytics(voiceNotes: List<VoiceNote>): AnalyticsData {
    val totalRecordings = voiceNotes.size
    val totalDuration = voiceNotes.sumOf { it.duration }
    
    // Mock WPM calculation (in real app, this would come from AI analysis)
    val averageWPM = if (totalRecordings > 0) {
        (120..180).random() // Mock average
    } else 0
    
    val wpmTrend = listOf("↑ Improving", "↓ Slower", "→ Stable").random()
    
    val recordingFrequency = when {
        totalRecordings >= 30 -> "Daily recordings"
        totalRecordings >= 10 -> "Weekly recordings"
        totalRecordings >= 3 -> "Monthly recordings"
        else -> "Getting started"
    }
    
    val recentAnalysis = voiceNotes.take(5).map { note ->
        RecordingAnalysis(
            title = note.title,
            wpm = (100..200).random(),
            confidence = listOf("High", "Medium", "Low").random()
        )
    }
    
    return AnalyticsData(
        overallStats = OverallStats(totalRecordings, totalDuration, averageWPM),
        averageWPM = averageWPM,
        wpmTrend = wpmTrend,
        recordingFrequency = recordingFrequency,
        recentAnalysis = recentAnalysis
    )
}

private fun formatDuration(durationMs: Long): String {
    val hours = durationMs / (1000 * 60 * 60)
    val minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60)
    
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}
