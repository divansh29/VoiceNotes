package com.voicenotes.app.naming

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

class NamingManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "naming_preferences"
        private const val KEY_NAMING_PATTERN = "naming_pattern"
        private const val KEY_USE_SMART_NAMING = "use_smart_naming"
        private const val KEY_INCLUDE_DATE = "include_date"
        private const val KEY_INCLUDE_TIME = "include_time"
        private const val KEY_DATE_FORMAT = "date_format"
        private const val KEY_TIME_FORMAT = "time_format"
        
        // Default naming patterns
        const val PATTERN_SMART = "smart"
        const val PATTERN_DATE_TIME = "date_time"
        const val PATTERN_SEQUENTIAL = "sequential"
        const val PATTERN_CUSTOM = "custom"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun generateRecordingName(
        transcript: String = "",
        duration: Long = 0,
        recordingNumber: Int = 1,
        customPrefix: String = ""
    ): String {
        val pattern = prefs.getString(KEY_NAMING_PATTERN, PATTERN_SMART) ?: PATTERN_SMART
        val useSmartNaming = prefs.getBoolean(KEY_USE_SMART_NAMING, true)
        
        return when (pattern) {
            PATTERN_SMART -> generateSmartName(transcript, duration)
            PATTERN_DATE_TIME -> generateDateTimeName()
            PATTERN_SEQUENTIAL -> generateSequentialName(recordingNumber, customPrefix)
            PATTERN_CUSTOM -> generateCustomName(transcript, duration, recordingNumber)
            else -> generateSmartName(transcript, duration)
        }
    }
    
    private fun generateSmartName(transcript: String, duration: Long): String {
        if (transcript.isBlank()) {
            return generateDateTimeName()
        }
        
        val lowerTranscript = transcript.lowercase()
        
        // Meeting detection
        if (lowerTranscript.contains("meeting") || lowerTranscript.contains("call")) {
            val participants = extractParticipants(transcript)
            val date = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
            return if (participants.isNotEmpty()) {
                "Meeting with $participants - $date"
            } else {
                "Meeting - $date"
            }
        }
        
        // Idea/brainstorming detection
        if (lowerTranscript.contains("idea") || lowerTranscript.contains("brainstorm")) {
            return "Ideas - ${getCurrentTimeString()}"
        }
        
        // Task/todo detection
        if (lowerTranscript.contains("task") || lowerTranscript.contains("todo") || lowerTranscript.contains("need to")) {
            return "Tasks - ${getCurrentDateString()}"
        }
        
        // Project detection
        if (lowerTranscript.contains("project")) {
            val projectName = extractProjectName(transcript)
            return if (projectName.isNotEmpty()) {
                "Project: $projectName"
            } else {
                "Project Discussion - ${getCurrentDateString()}"
            }
        }
        
        // Personal/diary detection
        if (lowerTranscript.contains("today") || lowerTranscript.contains("yesterday") || 
            lowerTranscript.contains("feeling") || lowerTranscript.contains("personal")) {
            return "Personal Note - ${getCurrentDateString()}"
        }
        
        // Shopping list detection
        if (lowerTranscript.contains("buy") || lowerTranscript.contains("shopping") || 
            lowerTranscript.contains("grocery")) {
            return "Shopping List - ${getCurrentDateString()}"
        }
        
        // Default: extract key topic
        val keyTopic = extractKeyTopic(transcript)
        return if (keyTopic.isNotEmpty()) {
            "$keyTopic - ${getCurrentTimeString()}"
        } else {
            generateDateTimeName()
        }
    }
    
    private fun generateDateTimeName(): String {
        val includeDate = prefs.getBoolean(KEY_INCLUDE_DATE, true)
        val includeTime = prefs.getBoolean(KEY_INCLUDE_TIME, true)
        val dateFormat = prefs.getString(KEY_DATE_FORMAT, "MMM dd, yyyy") ?: "MMM dd, yyyy"
        val timeFormat = prefs.getString(KEY_TIME_FORMAT, "HH:mm") ?: "HH:mm"
        
        val now = Date()
        val parts = mutableListOf<String>()
        
        if (includeDate) {
            parts.add(SimpleDateFormat(dateFormat, Locale.getDefault()).format(now))
        }
        
        if (includeTime) {
            parts.add(SimpleDateFormat(timeFormat, Locale.getDefault()).format(now))
        }
        
        return if (parts.isNotEmpty()) {
            "Recording ${parts.joinToString(" ")}"
        } else {
            "Voice Recording"
        }
    }
    
    private fun generateSequentialName(recordingNumber: Int, customPrefix: String): String {
        val prefix = customPrefix.ifEmpty { "Recording" }
        val paddedNumber = recordingNumber.toString().padStart(3, '0')
        return "$prefix $paddedNumber"
    }
    
    private fun generateCustomName(transcript: String, duration: Long, recordingNumber: Int): String {
        // This would use user-defined templates with placeholders
        // For now, return a combination approach
        val smartPart = extractKeyTopic(transcript).take(20)
        val datePart = getCurrentDateString()
        
        return if (smartPart.isNotEmpty()) {
            "$smartPart - $datePart"
        } else {
            "Recording ${recordingNumber.toString().padStart(3, '0')} - $datePart"
        }
    }
    
    private fun extractParticipants(transcript: String): String {
        // Simple participant extraction (in real app, this would be more sophisticated)
        val words = transcript.split(" ")
        val participants = mutableListOf<String>()
        
        for (i in words.indices) {
            val word = words[i].lowercase()
            if ((word == "with" || word == "and") && i + 1 < words.size) {
                val nextWord = words[i + 1].trim(',', '.', '!', '?')
                if (nextWord.length > 2 && nextWord[0].isUpperCase()) {
                    participants.add(nextWord)
                }
            }
        }
        
        return participants.take(2).joinToString(" & ")
    }
    
    private fun extractProjectName(transcript: String): String {
        val words = transcript.split(" ")
        
        for (i in words.indices) {
            val word = words[i].lowercase()
            if (word == "project" && i + 1 < words.size) {
                val nextWord = words[i + 1].trim(',', '.', '!', '?')
                if (nextWord.length > 2) {
                    return nextWord.replaceFirstChar { it.uppercase() }
                }
            }
        }
        
        return ""
    }
    
    private fun extractKeyTopic(transcript: String): String {
        // Extract the most important words/phrases
        val words = transcript.split(" ").filter { it.length > 3 }
        val importantWords = words.filter { word ->
            val lower = word.lowercase()
            !listOf("this", "that", "with", "have", "will", "been", "were", "they", "them", "from", "what", "when", "where").contains(lower)
        }
        
        return importantWords.take(3).joinToString(" ").take(30)
    }
    
    private fun getCurrentDateString(): String {
        return SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
    }
    
    private fun getCurrentTimeString(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
    
    // Settings management
    fun setNamingPattern(pattern: String) {
        prefs.edit().putString(KEY_NAMING_PATTERN, pattern).apply()
    }
    
    fun getNamingPattern(): String {
        return prefs.getString(KEY_NAMING_PATTERN, PATTERN_SMART) ?: PATTERN_SMART
    }
    
    fun setSmartNamingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_SMART_NAMING, enabled).apply()
    }
    
    fun isSmartNamingEnabled(): Boolean {
        return prefs.getBoolean(KEY_USE_SMART_NAMING, true)
    }
    
    fun setDateTimeSettings(includeDate: Boolean, includeTime: Boolean, dateFormat: String, timeFormat: String) {
        prefs.edit()
            .putBoolean(KEY_INCLUDE_DATE, includeDate)
            .putBoolean(KEY_INCLUDE_TIME, includeTime)
            .putString(KEY_DATE_FORMAT, dateFormat)
            .putString(KEY_TIME_FORMAT, timeFormat)
            .apply()
    }
    
    fun getDateTimeSettings(): DateTimeSettings {
        return DateTimeSettings(
            includeDate = prefs.getBoolean(KEY_INCLUDE_DATE, true),
            includeTime = prefs.getBoolean(KEY_INCLUDE_TIME, true),
            dateFormat = prefs.getString(KEY_DATE_FORMAT, "MMM dd, yyyy") ?: "MMM dd, yyyy",
            timeFormat = prefs.getString(KEY_TIME_FORMAT, "HH:mm") ?: "HH:mm"
        )
    }
}

data class DateTimeSettings(
    val includeDate: Boolean,
    val includeTime: Boolean,
    val dateFormat: String,
    val timeFormat: String
)

// Naming pattern options
enum class NamingPattern(val displayName: String, val description: String) {
    SMART("Smart Naming", "Automatically detects content type and generates relevant names"),
    DATE_TIME("Date & Time", "Uses current date and time for naming"),
    SEQUENTIAL("Sequential", "Numbers recordings sequentially (Recording 001, 002, etc.)"),
    CUSTOM("Custom Pattern", "Use custom templates with placeholders")
}
