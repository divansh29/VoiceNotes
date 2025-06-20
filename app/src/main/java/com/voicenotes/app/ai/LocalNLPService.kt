package com.voicenotes.app.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Removed OpenNLP imports to avoid build issues
// Using pure Kotlin/Java implementation instead
import java.io.InputStream
import java.util.*
import kotlin.collections.HashMap

class LocalNLPService(private val context: Context) {
    
    companion object {
        private const val TAG = "LocalNLPService"
    }
    
    private var isInitialized = false
    
    /**
     * Initialize NLP models (call once on app start)
     */
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing local NLP models...")

                // Using lightweight rule-based NLP for better compatibility
                // This avoids complex dependencies and keeps the app size small
                isInitialized = true
                Log.d(TAG, "Local NLP initialized successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize NLP models", e)
                false
            }
        }
    }
    
    /**
     * Process transcript with local NLP to extract insights
     */
    suspend fun processTranscript(transcript: String): LocalNLPResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!isInitialized) {
                    initialize()
                }
                
                val sentences = extractSentences(transcript)
                val tokens = extractTokens(transcript)
                val entities = extractEntities(transcript)
                val keywords = extractKeywords(transcript)
                val actionItems = extractActionItems(transcript)
                val summary = generateSummary(transcript, sentences)
                val sentiment = analyzeSentiment(transcript)
                val topics = extractTopics(transcript)
                
                LocalNLPResult.Success(
                    summary = summary,
                    actionItems = actionItems,
                    keywords = keywords,
                    entities = entities,
                    sentiment = sentiment,
                    topics = topics,
                    sentences = sentences,
                    wordCount = tokens.size,
                    readingTime = estimateReadingTime(tokens.size)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error processing transcript", e)
                LocalNLPResult.Error("Local NLP processing failed: ${e.message}")
            }
        }
    }
    
    /**
     * Extract sentences from text
     */
    private fun extractSentences(text: String): List<String> {
        return text.split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length > 3 }
    }
    
    /**
     * Extract and clean tokens
     */
    private fun extractTokens(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }
    }
    
    /**
     * Extract named entities (people, organizations, locations)
     */
    private fun extractEntities(text: String): Map<String, List<String>> {
        val entities = HashMap<String, MutableList<String>>()
        
        // Simple rule-based entity extraction
        val words = text.split(Regex("\\s+"))
        
        // Find potential person names (capitalized words)
        val personNames = mutableListOf<String>()
        val organizationNames = mutableListOf<String>()
        val locations = mutableListOf<String>()
        
        for (i in words.indices) {
            val word = words[i].replace(Regex("[^a-zA-Z]"), "")
            
            if (word.length > 2 && word[0].isUpperCase()) {
                // Check context for person indicators
                val context = words.getOrNull(i - 1)?.lowercase() ?: ""
                when {
                    context in listOf("mr", "mrs", "ms", "dr", "prof") -> personNames.add(word)
                    word.endsWith("Corp") || word.endsWith("Inc") || word.endsWith("LLC") -> organizationNames.add(word)
                    context in listOf("in", "at", "from", "to") -> locations.add(word)
                    else -> {
                        // Check if it's a common name
                        if (isCommonName(word)) personNames.add(word)
                    }
                }
            }
        }
        
        entities["PERSON"] = personNames.distinct().toMutableList()
        entities["ORGANIZATION"] = organizationNames.distinct().toMutableList()
        entities["LOCATION"] = locations.distinct().toMutableList()
        
        return entities
    }
    
    /**
     * Extract keywords using TF-IDF-like approach
     */
    private fun extractKeywords(text: String): List<String> {
        val tokens = extractTokens(text)
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
            "by", "from", "up", "about", "into", "through", "during", "before", "after",
            "above", "below", "between", "among", "this", "that", "these", "those", "i", "you",
            "he", "she", "it", "we", "they", "me", "him", "her", "us", "them", "my", "your",
            "his", "her", "its", "our", "their", "am", "is", "are", "was", "were", "be",
            "been", "being", "have", "has", "had", "do", "does", "did", "will", "would",
            "could", "should", "may", "might", "must", "can", "shall"
        )
        
        // Count word frequencies
        val wordCounts = tokens
            .filter { it !in stopWords }
            .groupingBy { it }
            .eachCount()
        
        // Return top keywords by frequency
        return wordCounts
            .toList()
            .sortedByDescending { it.second }
            .take(10)
            .map { it.first }
    }
    
    /**
     * Extract action items using pattern matching
     */
    private fun extractActionItems(text: String): List<String> {
        val actionItems = mutableListOf<String>()
        val sentences = extractSentences(text)
        
        val actionPatterns = listOf(
            Regex("(need to|should|must|have to|remember to)\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("(\\w+)\\s+(needs to|should|must|will)\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("(action item|todo|task):\\s*(.+)", RegexOption.IGNORE_CASE),
            Regex("(follow up|schedule|book|call|email|send|prepare|review|complete)\\s+(.+)", RegexOption.IGNORE_CASE)
        )
        
        for (sentence in sentences) {
            for (pattern in actionPatterns) {
                val match = pattern.find(sentence)
                if (match != null) {
                    val actionText = when (pattern) {
                        actionPatterns[0] -> match.groupValues[2] // "need to X" -> "X"
                        actionPatterns[1] -> "${match.groupValues[1]}: ${match.groupValues[3]}" // "John should X" -> "John: X"
                        actionPatterns[2] -> match.groupValues[2] // "Action item: X" -> "X"
                        actionPatterns[3] -> "${match.groupValues[1]} ${match.groupValues[2]}" // "Schedule X" -> "Schedule X"
                        else -> sentence
                    }
                    
                    if (actionText.length > 5 && actionText.length < 100) {
                        actionItems.add(actionText.trim())
                    }
                }
            }
        }
        
        return actionItems.distinct().take(5)
    }
    
    /**
     * Generate summary using extractive approach
     */
    private fun generateSummary(text: String, sentences: List<String>): String {
        if (sentences.isEmpty()) return "No content to summarize"
        
        val keywords = extractKeywords(text).take(5)
        
        // Score sentences based on keyword presence
        val sentenceScores = sentences.map { sentence ->
            val score = keywords.count { keyword ->
                sentence.lowercase().contains(keyword.lowercase())
            }
            sentence to score
        }
        
        // Select top sentences for summary
        val topSentences = sentenceScores
            .sortedByDescending { it.second }
            .take(2)
            .map { it.first }
        
        return if (topSentences.isNotEmpty()) {
            topSentences.joinToString(" ")
        } else {
            sentences.take(1).joinToString(" ")
        }
    }
    
    /**
     * Analyze sentiment using keyword-based approach
     */
    private fun analyzeSentiment(text: String): String {
        val positiveWords = setOf(
            "good", "great", "excellent", "amazing", "wonderful", "fantastic", "awesome",
            "happy", "pleased", "satisfied", "successful", "positive", "love", "like",
            "enjoy", "excited", "thrilled", "delighted", "perfect", "brilliant"
        )
        
        val negativeWords = setOf(
            "bad", "terrible", "awful", "horrible", "disappointing", "frustrated",
            "angry", "upset", "sad", "worried", "concerned", "problem", "issue",
            "difficult", "challenging", "failed", "wrong", "hate", "dislike"
        )
        
        val words = extractTokens(text)
        val positiveCount = words.count { it in positiveWords }
        val negativeCount = words.count { it in negativeWords }
        
        return when {
            positiveCount > negativeCount -> "positive"
            negativeCount > positiveCount -> "negative"
            else -> "neutral"
        }
    }
    
    /**
     * Extract topics using keyword clustering
     */
    private fun extractTopics(text: String): List<String> {
        val keywords = extractKeywords(text)
        val topics = mutableListOf<String>()
        
        // Define topic categories
        val topicKeywords = mapOf(
            "Work" to setOf("meeting", "project", "deadline", "task", "work", "office", "team", "client", "business"),
            "Personal" to setOf("family", "home", "personal", "friend", "weekend", "vacation", "hobby"),
            "Health" to setOf("doctor", "appointment", "health", "exercise", "medicine", "hospital", "diet"),
            "Finance" to setOf("money", "budget", "cost", "price", "payment", "bank", "investment", "expense"),
            "Technology" to setOf("app", "software", "computer", "phone", "internet", "website", "digital", "tech"),
            "Education" to setOf("learn", "study", "course", "school", "university", "training", "education")
        )
        
        for ((topic, topicWords) in topicKeywords) {
            val matchCount = keywords.count { it in topicWords }
            if (matchCount > 0) {
                topics.add(topic)
            }
        }
        
        return if (topics.isEmpty()) listOf("General") else topics
    }
    
    /**
     * Check if word is a common name
     */
    private fun isCommonName(word: String): Boolean {
        val commonNames = setOf(
            "John", "Jane", "Mike", "Sarah", "David", "Lisa", "Chris", "Anna",
            "Mark", "Emma", "Paul", "Maria", "James", "Linda", "Robert", "Susan"
        )
        return word in commonNames
    }
    
    /**
     * Estimate reading time in minutes
     */
    private fun estimateReadingTime(wordCount: Int): Int {
        return maxOf(1, wordCount / 200) // Average 200 words per minute
    }
}

sealed class LocalNLPResult {
    data class Success(
        val summary: String,
        val actionItems: List<String>,
        val keywords: List<String>,
        val entities: Map<String, List<String>>,
        val sentiment: String,
        val topics: List<String>,
        val sentences: List<String>,
        val wordCount: Int,
        val readingTime: Int
    ) : LocalNLPResult()
    
    data class Error(val message: String) : LocalNLPResult()
}
