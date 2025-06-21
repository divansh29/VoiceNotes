package com.voicenotes.app.ai

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.File

class AIService(private val context: Context) {

    companion object {
        private const val TAG = "AIService"
        private const val PREFS_NAME = "ai_settings"
        private const val KEY_STT_PROVIDER = "stt_provider"
        private const val KEY_LLM_PROVIDER = "llm_provider"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_ANTHROPIC_API_KEY = "anthropic_api_key"
        private const val KEY_GOOGLE_API_KEY = "google_api_key"
        private const val KEY_USE_REAL_AI = "use_real_ai"
        private const val KEY_USE_LOCAL_AI = "use_local_ai"
    }

    private val speechToTextService = SpeechToTextService(context)
    private val llmService = LLMService(context)
    private val localSTTService = LocalSTTService(context)
    private val localNLPService = LocalNLPService(context)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Transcribe audio using local, cloud, or mock STT
     */
    suspend fun transcribeAudio(audioFilePath: String): String {
        return try {
            when {
                isLocalAIEnabled() -> {
                    // Use local STT (no API key needed)
                    when (val result = localSTTService.transcribeAudioFile(audioFilePath)) {
                        is LocalSTTResult.Success -> result.transcript
                        is LocalSTTResult.Error -> {
                            Log.e(TAG, "Local STT failed: ${result.message}")
                            // Fallback to mock
                            generateMockTranscript(getAudioDuration(audioFilePath))
                        }
                    }
                }
                isRealAIEnabled() -> {
                    // Use cloud STT (requires API key)
                    val sttProvider = getSTTProvider()
                    val apiKey = getAPIKey(sttProvider)

                    if (apiKey.isNotEmpty()) {
                        when (val result = speechToTextService.transcribeAudio(audioFilePath, sttProvider, apiKey)) {
                            is STTResult.Success -> result.transcript
                            is STTResult.Error -> {
                                Log.e(TAG, "Cloud STT failed: ${result.message}")
                                // Fallback to local or mock
                                if (isLocalAIEnabled()) {
                                    transcribeWithLocal(audioFilePath)
                                } else {
                                    generateMockTranscript(getAudioDuration(audioFilePath))
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "No API key configured, falling back to local STT")
                        transcribeWithLocal(audioFilePath)
                    }
                }
                else -> {
                    // Use mock transcription
                    generateMockTranscript(getAudioDuration(audioFilePath))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in transcribeAudio", e)
            generateMockTranscript(getAudioDuration(audioFilePath))
        }
    }

    private suspend fun transcribeWithLocal(audioFilePath: String): String {
        return when (val result = localSTTService.transcribeAudioFile(audioFilePath)) {
            is LocalSTTResult.Success -> result.transcript
            is LocalSTTResult.Error -> generateMockTranscript(getAudioDuration(audioFilePath))
        }
    }
    
    /**
     * Generate summary using local, cloud, or mock NLP
     */
    suspend fun generateSummary(transcript: String, audioFilePath: String): AIResult {
        return try {
            when {
                isLocalAIEnabled() -> {
                    // Use local NLP (no API key needed)
                    when (val result = localNLPService.processTranscript(transcript)) {
                        is LocalNLPResult.Success -> {
                            val actionItems = result.actionItems.map { actionItemText ->
                                ActionItem(
                                    task = actionItemText,
                                    priority = determinePriority(actionItemText),
                                    category = determineCategory(actionItemText)
                                )
                            }

                            val speakingPatterns = analyzeSpeakingPatterns(transcript, audioFilePath)

                            // Schedule notifications for action items
                            scheduleActionItemReminders(actionItems)

                            AIResult(
                                title = generateTitle(transcript),
                                summary = result.summary,
                                keyPoints = result.keywords,
                                actionItems = actionItems,
                                speakingPatterns = speakingPatterns
                            )
                        }
                        is LocalNLPResult.Error -> {
                            Log.e(TAG, "Local NLP failed: ${result.message}")
                            generateMockSummary(transcript, audioFilePath)
                        }
                    }
                }
                isRealAIEnabled() -> {
                    // Use cloud LLM (requires API key)
                    val llmProvider = getLLMProvider()
                    val apiKey = getAPIKey(llmProvider)

                    if (apiKey.isNotEmpty()) {
                        when (val result = llmService.processTranscript(transcript, llmProvider, apiKey)) {
                            is LLMResult.Success -> {
                                val actionItems = result.actionItems.map { actionItemText ->
                                    ActionItem(
                                        task = actionItemText,
                                        priority = determinePriority(actionItemText),
                                        category = determineCategory(actionItemText)
                                    )
                                }

                                val speakingPatterns = analyzeSpeakingPatterns(transcript, audioFilePath)

                                // Schedule notifications for action items
                                scheduleActionItemReminders(actionItems)

                                AIResult(
                                    title = generateTitle(transcript),
                                    summary = result.summary,
                                    keyPoints = result.keywords,
                                    actionItems = actionItems,
                                    speakingPatterns = speakingPatterns
                                )
                            }
                            is LLMResult.Error -> {
                                Log.e(TAG, "Cloud LLM failed: ${result.message}")
                                // Fallback to local or mock
                                if (isLocalAIEnabled()) {
                                    generateSummary(transcript, audioFilePath) // Retry with local
                                } else {
                                    generateMockSummary(transcript, audioFilePath)
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "No API key configured, falling back to local NLP")
                        if (isLocalAIEnabled()) {
                            generateSummary(transcript, audioFilePath) // Use local
                        } else {
                            generateMockSummary(transcript, audioFilePath)
                        }
                    }
                }
                else -> {
                    // Use mock processing
                    generateMockSummary(transcript, audioFilePath)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateSummary", e)
            generateMockSummary(transcript, audioFilePath)
        }
    }

    /**
     * Mock summary generation (fallback)
     */
    private fun generateMockSummary(transcript: String, audioFilePath: String): AIResult {
        val title = generateTitle(transcript)
        val summary = generateSummaryText(transcript)
        val keyPoints = generateKeyPoints(transcript)
        val actionItems = extractActionItems(transcript)
        val speakingPatterns = analyzeSpeakingPatterns(transcript, audioFilePath)

        // Schedule notifications for action items
        scheduleActionItemReminders(actionItems)

        return AIResult(
            title = title,
            summary = summary,
            keyPoints = keyPoints,
            actionItems = actionItems,
            speakingPatterns = speakingPatterns
        )
    }
    
    private fun getAudioDuration(filePath: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            retriever.release()
            duration
        } catch (e: Exception) {
            0
        }
    }

    private fun generateMockTranscript(duration: Long): String {
        // Generate mock transcript based on duration
        val minutes = duration / 60000
        return when {
            minutes < 1 -> "This is a short voice note recording. The content discusses quick thoughts and ideas. I need to remember to call John about the project meeting tomorrow. Also, don't forget to buy groceries on the way home."
            minutes < 3 -> "This is a medium-length voice note. The speaker discusses various topics including work tasks, personal reminders, and important points to remember. The meeting with the client went well, and we should follow up with the proposal by Friday. I also need to schedule a dentist appointment and review the quarterly reports."
            minutes < 5 -> "This is a longer voice note recording. The content covers multiple topics in detail, including project discussions, meeting notes, and comprehensive thoughts on various subjects. Today's brainstorming session was productive - we came up with several innovative ideas for the new product launch. I must remember to email the team about the deadline changes and schedule a follow-up meeting for next week."
            else -> "This is an extended voice recording with detailed discussions. The speaker covers comprehensive topics including strategic planning, detailed project analysis, and multiple action items. The quarterly review meeting highlighted several key areas for improvement. We need to implement the new workflow system, train the team on updated procedures, and ensure all documentation is current. Important tasks include contacting the vendor about pricing, scheduling team training sessions, and preparing the presentation for the board meeting next month."
        }
    }
    
    private fun generateTitle(transcript: String): String {
        // Simple title generation based on transcript length and content
        val words = transcript.split(" ")
        return when {
            words.size < 10 -> "Quick Note"
            words.size < 30 -> "Voice Memo"
            transcript.contains("meeting", ignoreCase = true) -> "Meeting Notes"
            transcript.contains("idea", ignoreCase = true) -> "Ideas & Thoughts"
            transcript.contains("task", ignoreCase = true) -> "Task Notes"
            transcript.contains("reminder", ignoreCase = true) -> "Reminders"
            else -> "Voice Recording"
        }
    }

    private fun generateSummaryText(transcript: String): String {
        // Simple summary generation
        val sentences = transcript.split(". ")
        return if (sentences.size > 2) {
            sentences.take(2).joinToString(". ") + "."
        } else {
            transcript
        }
    }

    private fun generateKeyPoints(transcript: String): List<String> {
        // Enhanced key points extraction using contextual analysis
        val keyPoints = mutableListOf<String>()
        val lowerTranscript = transcript.lowercase()

        // Extract specific, meaningful keywords
        val contextualKeywords = extractContextualKeywords(transcript)
        val actionKeywords = extractActionKeywords(transcript)
        val entityKeywords = extractEntityKeywords(transcript)

        // Prioritize specific keywords over generic ones
        keyPoints.addAll(contextualKeywords.take(3))
        keyPoints.addAll(actionKeywords.take(2))
        keyPoints.addAll(entityKeywords.take(3))

        // Add content-based keywords if we have room
        if (keyPoints.size < 5) {
            val contentKeywords = extractContentKeywords(transcript)
            keyPoints.addAll(contentKeywords.take(5 - keyPoints.size))
        }

        return keyPoints.distinct().take(8).ifEmpty { listOf("Voice note") }
    }

    /**
     * Extract contextual keywords from transcript
     */
    private fun extractContextualKeywords(transcript: String): List<String> {
        val keywords = mutableListOf<String>()
        val lowerText = transcript.lowercase()

        // Meeting/work related
        when {
            lowerText.contains("meeting") -> keywords.add("meeting")
            lowerText.contains("conference") -> keywords.add("conference")
            lowerText.contains("call") -> keywords.add("call")
            lowerText.contains("project") -> keywords.add("project")
            lowerText.contains("deadline") -> keywords.add("deadline")
            lowerText.contains("team") -> keywords.add("team")
        }

        // Personal tasks
        when {
            lowerText.contains("shopping") || lowerText.contains("buy") -> keywords.add("shopping")
            lowerText.contains("appointment") -> keywords.add("appointment")
            lowerText.contains("doctor") -> keywords.add("doctor")
            lowerText.contains("dentist") -> keywords.add("dentist")
        }

        // Time-related
        when {
            lowerText.contains("today") -> keywords.add("today")
            lowerText.contains("tomorrow") -> keywords.add("tomorrow")
            lowerText.contains("weekend") -> keywords.add("weekend")
            lowerText.contains("monday") -> keywords.add("monday")
            lowerText.contains("tuesday") -> keywords.add("tuesday")
            lowerText.contains("wednesday") -> keywords.add("wednesday")
            lowerText.contains("thursday") -> keywords.add("thursday")
            lowerText.contains("friday") -> keywords.add("friday")
        }

        return keywords
    }

    /**
     * Extract action-oriented keywords
     */
    private fun extractActionKeywords(transcript: String): List<String> {
        val keywords = mutableListOf<String>()
        val lowerText = transcript.lowercase()

        val actionMap = mapOf(
            "call" to listOf("call", "phone", "ring"),
            "email" to listOf("email", "send", "message"),
            "buy" to listOf("buy", "purchase", "get"),
            "schedule" to listOf("schedule", "book", "arrange"),
            "visit" to listOf("visit", "go to", "stop by"),
            "finish" to listOf("finish", "complete", "done"),
            "prepare" to listOf("prepare", "get ready", "set up"),
            "review" to listOf("review", "check", "look at")
        )

        actionMap.forEach { (action, patterns) ->
            if (patterns.any { pattern -> lowerText.contains(pattern) }) {
                keywords.add(action)
            }
        }

        return keywords
    }

    /**
     * Extract entity keywords (names, places, specific items)
     */
    private fun extractEntityKeywords(transcript: String): List<String> {
        val keywords = mutableListOf<String>()
        val lowerText = transcript.lowercase()

        // Common entities
        val entities = listOf(
            "john", "sarah", "mike", "lisa", "david", "anna", "chris", "maria",
            "milk", "bread", "eggs", "coffee", "groceries", "medicine",
            "bank", "store", "office", "home", "hospital", "school",
            "morning", "afternoon", "evening", "lunch", "dinner"
        )

        entities.forEach { entity ->
            if (lowerText.contains(entity)) {
                keywords.add(entity)
            }
        }

        return keywords
    }

    /**
     * Extract content-based keywords
     */
    private fun extractContentKeywords(transcript: String): List<String> {
        val keywords = mutableListOf<String>()
        val words = transcript.lowercase().split("\\s+".toRegex())

        // Filter meaningful words
        val meaningfulWords = words.filter { word ->
            word.length > 3 &&
            !isStopWord(word) &&
            !isCommonWord(word)
        }

        // Count frequency and return top words
        val wordCounts = meaningfulWords.groupingBy { it }.eachCount()
        return wordCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    /**
     * Check if word is a stop word
     */
    private fun isStopWord(word: String): Boolean {
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
            "by", "from", "up", "about", "into", "through", "during", "before", "after",
            "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they"
        )
        return stopWords.contains(word)
    }

    /**
     * Check if word is too common to be meaningful
     */
    private fun isCommonWord(word: String): Boolean {
        val commonWords = setOf(
            "said", "says", "going", "really", "think", "know", "like", "just",
            "want", "need", "make", "take", "come", "good", "great", "nice",
            "thing", "things", "stuff", "something", "anything", "everything"
        )
        return commonWords.contains(word)
    }

    private fun extractActionItems(transcript: String): List<ActionItem> {
        val lowerTranscript = transcript.lowercase()
        val actionItems = mutableListOf<ActionItem>()

        // Look for action-oriented phrases
        val actionPatterns = listOf(
            "need to" to Priority.MEDIUM,
            "have to" to Priority.HIGH,
            "must" to Priority.HIGH,
            "should" to Priority.MEDIUM,
            "remember to" to Priority.MEDIUM,
            "don't forget" to Priority.HIGH,
            "urgent" to Priority.URGENT,
            "asap" to Priority.URGENT,
            "deadline" to Priority.HIGH,
            "due" to Priority.HIGH,
            "schedule" to Priority.MEDIUM,
            "call" to Priority.MEDIUM,
            "email" to Priority.MEDIUM,
            "meeting" to Priority.MEDIUM,
            "follow up" to Priority.MEDIUM
        )

        actionPatterns.forEach { (pattern, priority) ->
            if (lowerTranscript.contains(pattern)) {
                // Extract context around the action phrase
                val index = lowerTranscript.indexOf(pattern)
                val start = maxOf(0, index - 20)
                val end = minOf(transcript.length, index + pattern.length + 40)
                val context = transcript.substring(start, end).trim()

                // Determine category
                val category = when {
                    lowerTranscript.contains("work") || lowerTranscript.contains("office") -> "Work"
                    lowerTranscript.contains("personal") || lowerTranscript.contains("family") -> "Personal"
                    lowerTranscript.contains("health") || lowerTranscript.contains("doctor") -> "Health"
                    lowerTranscript.contains("shopping") || lowerTranscript.contains("buy") -> "Shopping"
                    else -> "General"
                }

                actionItems.add(
                    ActionItem(
                        task = context,
                        priority = priority,
                        category = category
                    )
                )
            }
        }

        // If no specific action items found, create generic ones based on content
        if (actionItems.isEmpty()) {
            when {
                lowerTranscript.contains("meeting") -> {
                    actionItems.add(ActionItem("Follow up on meeting discussion", Priority.MEDIUM, category = "Work"))
                }
                lowerTranscript.contains("idea") -> {
                    actionItems.add(ActionItem("Develop the ideas mentioned", Priority.LOW, category = "Ideas"))
                }
                lowerTranscript.contains("project") -> {
                    actionItems.add(ActionItem("Continue project work", Priority.MEDIUM, category = "Work"))
                }
            }
        }

        return actionItems.take(3) // Limit to 3 action items
    }

    private fun analyzeSpeakingPatterns(transcript: String, audioFilePath: String): SpeakingPatterns {
        val duration = getAudioDuration(audioFilePath)
        val words = transcript.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val wordCount = words.size

        // Mock analysis - in real implementation, you'd analyze the actual audio
        val durationMinutes = duration / 60000.0
        val wordsPerMinute = if (durationMinutes > 0) (wordCount / durationMinutes).toInt() else 0

        // Estimate pauses based on sentence structure
        val sentences = transcript.split("[.!?]".toRegex()).filter { it.isNotBlank() }
        val pauseCount = maxOf(0, sentences.size - 1)
        val averagePauseLength = if (pauseCount > 0) (duration * 0.1) / pauseCount else 0.0

        // Determine confidence level based on speaking speed and patterns
        val confidenceLevel = when {
            wordsPerMinute > 180 -> "High (Fast speaker)"
            wordsPerMinute > 120 -> "Medium (Normal pace)"
            wordsPerMinute > 80 -> "Medium (Thoughtful pace)"
            else -> "Low (Slow/hesitant)"
        }

        return SpeakingPatterns(
            wordsPerMinute = wordsPerMinute,
            pauseCount = pauseCount,
            averagePauseLength = averagePauseLength,
            totalSpeakingTime = duration,
            confidenceLevel = confidenceLevel
        )
    }

    private fun scheduleActionItemReminders(actionItems: List<ActionItem>) {
        // Import notification service
        actionItems.forEach { actionItem ->
            val delayHours = when (actionItem.priority) {
                Priority.URGENT -> 1
                Priority.HIGH -> 4
                Priority.MEDIUM -> 24
                Priority.LOW -> 72
            }

            try {
                com.voicenotes.app.notifications.NotificationService.scheduleActionItemReminder(
                    context,
                    actionItem.task,
                    delayHours
                )
            } catch (e: Exception) {
                // Handle scheduling error gracefully
            }
        }
    }

    // AI Configuration Methods
    fun setRealAIEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_REAL_AI, enabled).apply()
    }

    fun isRealAIEnabled(): Boolean {
        return prefs.getBoolean(KEY_USE_REAL_AI, false)
    }

    fun setLocalAIEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_LOCAL_AI, enabled).apply()
    }

    fun isLocalAIEnabled(): Boolean {
        return prefs.getBoolean(KEY_USE_LOCAL_AI, false)
    }

    fun setSTTProvider(provider: STTProvider) {
        prefs.edit().putString(KEY_STT_PROVIDER, provider.name).apply()
    }

    fun getSTTProvider(): STTProvider {
        val providerName = prefs.getString(KEY_STT_PROVIDER, STTProvider.MOCK.name)
        return try {
            STTProvider.valueOf(providerName ?: STTProvider.MOCK.name)
        } catch (e: Exception) {
            STTProvider.MOCK
        }
    }

    fun setLLMProvider(provider: LLMProvider) {
        prefs.edit().putString(KEY_LLM_PROVIDER, provider.name).apply()
    }

    fun getLLMProvider(): LLMProvider {
        val providerName = prefs.getString(KEY_LLM_PROVIDER, LLMProvider.MOCK.name)
        return try {
            LLMProvider.valueOf(providerName ?: LLMProvider.MOCK.name)
        } catch (e: Exception) {
            LLMProvider.MOCK
        }
    }

    fun setAPIKey(provider: String, apiKey: String) {
        val keyName = when (provider.uppercase()) {
            "OPENAI", "OPENAI_WHISPER" -> KEY_OPENAI_API_KEY
            "ANTHROPIC" -> KEY_ANTHROPIC_API_KEY
            "GOOGLE", "GOOGLE_CLOUD" -> KEY_GOOGLE_API_KEY
            else -> return
        }
        prefs.edit().putString(keyName, apiKey).apply()
    }

    private fun getAPIKey(provider: STTProvider): String {
        return when (provider) {
            STTProvider.OPENAI_WHISPER -> prefs.getString(KEY_OPENAI_API_KEY, "") ?: ""
            STTProvider.GOOGLE_CLOUD -> prefs.getString(KEY_GOOGLE_API_KEY, "") ?: ""
            STTProvider.AZURE -> "" // Add Azure key if needed
            STTProvider.MOCK -> ""
        }
    }

    private fun getAPIKey(provider: LLMProvider): String {
        return when (provider) {
            LLMProvider.OPENAI -> prefs.getString(KEY_OPENAI_API_KEY, "") ?: ""
            LLMProvider.ANTHROPIC -> prefs.getString(KEY_ANTHROPIC_API_KEY, "") ?: ""
            LLMProvider.GOOGLE -> prefs.getString(KEY_GOOGLE_API_KEY, "") ?: ""
            LLMProvider.MOCK -> ""
        }
    }

    private fun determinePriority(actionItemText: String): Priority {
        val lowerText = actionItemText.lowercase()
        return when {
            lowerText.contains("urgent") || lowerText.contains("asap") -> Priority.URGENT
            lowerText.contains("important") || lowerText.contains("must") -> Priority.HIGH
            lowerText.contains("should") || lowerText.contains("need") -> Priority.MEDIUM
            else -> Priority.LOW
        }
    }

    private fun determineCategory(actionItemText: String): String {
        val lowerText = actionItemText.lowercase()
        return when {
            lowerText.contains("work") || lowerText.contains("office") || lowerText.contains("meeting") -> "Work"
            lowerText.contains("personal") || lowerText.contains("family") -> "Personal"
            lowerText.contains("health") || lowerText.contains("doctor") -> "Health"
            lowerText.contains("shopping") || lowerText.contains("buy") -> "Shopping"
            else -> "General"
        }
    }
}

data class AIResult(
    val title: String,
    val summary: String,
    val keyPoints: List<String>,
    val actionItems: List<ActionItem> = emptyList(),
    val speakingPatterns: SpeakingPatterns? = null
)

data class ActionItem(
    val task: String,
    val priority: Priority = Priority.MEDIUM,
    val dueDate: String? = null,
    val category: String = "General"
)

data class SpeakingPatterns(
    val wordsPerMinute: Int,
    val pauseCount: Int,
    val averagePauseLength: Double,
    val totalSpeakingTime: Long,
    val confidenceLevel: String
)

enum class Priority {
    LOW, MEDIUM, HIGH, URGENT
}
