package com.voicenotes.app.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class LLMService(private val context: Context) {
    
    companion object {
        private const val TAG = "LLMService"
        
        // API Endpoints
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages"
        private const val GOOGLE_AI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
        
        // Model Names
        private const val OPENAI_MODEL = "gpt-3.5-turbo"
        private const val ANTHROPIC_MODEL = "claude-3-haiku-20240307"
        private const val GOOGLE_MODEL = "gemini-pro"
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * Process transcript with LLM to generate summary, action items, and insights
     */
    suspend fun processTranscript(
        transcript: String,
        provider: LLMProvider = LLMProvider.OPENAI,
        apiKey: String
    ): LLMResult {
        return withContext(Dispatchers.IO) {
            try {
                when (provider) {
                    LLMProvider.OPENAI -> processWithOpenAI(transcript, apiKey)
                    LLMProvider.ANTHROPIC -> processWithAnthropic(transcript, apiKey)
                    LLMProvider.GOOGLE -> processWithGoogle(transcript, apiKey)
                    LLMProvider.MOCK -> processWithMock(transcript)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing transcript with LLM", e)
                LLMResult.Error("Failed to process with AI: ${e.message}")
            }
        }
    }
    
    /**
     * OpenAI GPT Integration
     */
    private suspend fun processWithOpenAI(transcript: String, apiKey: String): LLMResult {
        val prompt = createAnalysisPrompt(transcript)
        
        val requestBody = JSONObject().apply {
            put("model", OPENAI_MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an AI assistant that analyzes voice recordings and provides structured summaries.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 1000)
            put("temperature", 0.7)
        }
        
        val request = Request.Builder()
            .url(OPENAI_API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseOpenAIResponse(responseBody ?: "")
            } else {
                LLMResult.Error("OpenAI API error: ${response.code}")
            }
        } catch (e: IOException) {
            LLMResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Anthropic Claude Integration
     */
    private suspend fun processWithAnthropic(transcript: String, apiKey: String): LLMResult {
        val prompt = createAnalysisPrompt(transcript)
        
        val requestBody = JSONObject().apply {
            put("model", ANTHROPIC_MODEL)
            put("max_tokens", 1000)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }
        
        val request = Request.Builder()
            .url(ANTHROPIC_API_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("anthropic-version", "2023-06-01")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseAnthropicResponse(responseBody ?: "")
            } else {
                LLMResult.Error("Anthropic API error: ${response.code}")
            }
        } catch (e: IOException) {
            LLMResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Google Gemini Integration
     */
    private suspend fun processWithGoogle(transcript: String, apiKey: String): LLMResult {
        val prompt = createAnalysisPrompt(transcript)
        
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }
        
        val request = Request.Builder()
            .url("$GOOGLE_AI_URL?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseGoogleResponse(responseBody ?: "")
            } else {
                LLMResult.Error("Google AI error: ${response.code}")
            }
        } catch (e: IOException) {
            LLMResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Mock implementation for testing
     */
    private fun processWithMock(transcript: String): LLMResult {
        return LLMResult.Success(
            summary = "This is a mock AI-generated summary of the voice recording. The speaker discussed various topics including project updates, meeting schedules, and action items.",
            actionItems = listOf(
                "Follow up with team about project deadline",
                "Schedule meeting with client for next week",
                "Review and update project documentation",
                "Send summary email to stakeholders"
            ),
            keywords = listOf("project", "meeting", "deadline", "client", "team", "documentation"),
            sentiment = "neutral",
            topics = listOf("Project Management", "Team Coordination", "Client Relations"),
            insights = "The recording shows good organizational planning with clear action items and follow-up tasks."
        )
    }
    
    /**
     * Create structured prompt for LLM analysis
     */
    private fun createAnalysisPrompt(transcript: String): String {
        return """
        Please analyze the following voice recording transcript and provide a structured response in JSON format:

        TRANSCRIPT:
        "$transcript"

        Please provide your analysis in the following JSON structure:
        {
            "summary": "A concise 2-3 sentence summary of the main content",
            "action_items": ["List of specific action items or tasks mentioned"],
            "keywords": ["Key terms and topics discussed"],
            "sentiment": "positive/negative/neutral",
            "topics": ["Main topics or categories discussed"],
            "insights": "Any notable patterns, insights, or observations"
        }

        Focus on being accurate, concise, and extracting actionable information.
        """.trimIndent()
    }
    
    /**
     * Parse OpenAI API response
     */
    private fun parseOpenAIResponse(responseBody: String): LLMResult {
        return try {
            val json = JSONObject(responseBody)
            val content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            
            parseStructuredResponse(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing OpenAI response", e)
            LLMResult.Error("Failed to parse AI response")
        }
    }
    
    /**
     * Parse Anthropic API response
     */
    private fun parseAnthropicResponse(responseBody: String): LLMResult {
        return try {
            val json = JSONObject(responseBody)
            val content = json.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
            
            parseStructuredResponse(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Anthropic response", e)
            LLMResult.Error("Failed to parse AI response")
        }
    }
    
    /**
     * Parse Google AI response
     */
    private fun parseGoogleResponse(responseBody: String): LLMResult {
        return try {
            val json = JSONObject(responseBody)
            val content = json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
            
            parseStructuredResponse(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Google response", e)
            LLMResult.Error("Failed to parse AI response")
        }
    }
    
    /**
     * Parse structured JSON response from LLM
     */
    private fun parseStructuredResponse(content: String): LLMResult {
        return try {
            // Extract JSON from response (handle cases where LLM adds extra text)
            val jsonStart = content.indexOf("{")
            val jsonEnd = content.lastIndexOf("}") + 1
            val jsonContent = content.substring(jsonStart, jsonEnd)
            
            val json = JSONObject(jsonContent)
            
            val actionItems = mutableListOf<String>()
            val actionArray = json.optJSONArray("action_items")
            if (actionArray != null) {
                for (i in 0 until actionArray.length()) {
                    actionItems.add(actionArray.getString(i))
                }
            }
            
            val keywords = mutableListOf<String>()
            val keywordArray = json.optJSONArray("keywords")
            if (keywordArray != null) {
                for (i in 0 until keywordArray.length()) {
                    keywords.add(keywordArray.getString(i))
                }
            }
            
            val topics = mutableListOf<String>()
            val topicArray = json.optJSONArray("topics")
            if (topicArray != null) {
                for (i in 0 until topicArray.length()) {
                    topics.add(topicArray.getString(i))
                }
            }
            
            LLMResult.Success(
                summary = json.optString("summary", "Summary not available"),
                actionItems = actionItems,
                keywords = keywords,
                sentiment = json.optString("sentiment", "neutral"),
                topics = topics,
                insights = json.optString("insights", "No specific insights available")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing structured response", e)
            LLMResult.Error("Failed to parse structured AI response")
        }
    }
}

enum class LLMProvider {
    OPENAI,
    ANTHROPIC,
    GOOGLE,
    MOCK
}

sealed class LLMResult {
    data class Success(
        val summary: String,
        val actionItems: List<String>,
        val keywords: List<String>,
        val sentiment: String,
        val topics: List<String>,
        val insights: String
    ) : LLMResult()
    
    data class Error(val message: String) : LLMResult()
}
