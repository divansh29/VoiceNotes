package com.voicenotes.app.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class SpeechToTextService(private val context: Context) {
    
    companion object {
        private const val TAG = "SpeechToTextService"
        
        // API Endpoints
        private const val OPENAI_WHISPER_URL = "https://api.openai.com/v1/audio/transcriptions"
        private const val GOOGLE_STT_URL = "https://speech.googleapis.com/v1/speech:recognize"
        private const val AZURE_STT_URL = "https://YOUR_REGION.stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1"
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    
    /**
     * Convert audio file to text using various STT providers
     */
    suspend fun transcribeAudio(
        audioFilePath: String,
        provider: STTProvider = STTProvider.OPENAI_WHISPER,
        apiKey: String,
        language: String = "en"
    ): STTResult {
        return withContext(Dispatchers.IO) {
            try {
                when (provider) {
                    STTProvider.OPENAI_WHISPER -> transcribeWithWhisper(audioFilePath, apiKey, language)
                    STTProvider.GOOGLE_CLOUD -> transcribeWithGoogle(audioFilePath, apiKey, language)
                    STTProvider.AZURE -> transcribeWithAzure(audioFilePath, apiKey, language)
                    STTProvider.MOCK -> transcribeWithMock(audioFilePath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error transcribing audio", e)
                STTResult.Error("Transcription failed: ${e.message}")
            }
        }
    }
    
    /**
     * OpenAI Whisper API Integration
     */
    private suspend fun transcribeWithWhisper(
        audioFilePath: String,
        apiKey: String,
        language: String
    ): STTResult {
        val audioFile = File(audioFilePath)
        if (!audioFile.exists()) {
            return STTResult.Error("Audio file not found")
        }
        
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/*".toMediaType())
            )
            .addFormDataPart("model", "whisper-1")
            .addFormDataPart("language", language)
            .addFormDataPart("response_format", "json")
            .build()
        
        val request = Request.Builder()
            .url(OPENAI_WHISPER_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()
        
        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseWhisperResponse(responseBody ?: "")
            } else {
                STTResult.Error("Whisper API error: ${response.code}")
            }
        } catch (e: IOException) {
            STTResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Google Cloud Speech-to-Text Integration
     */
    private suspend fun transcribeWithGoogle(
        audioFilePath: String,
        apiKey: String,
        language: String
    ): STTResult {
        // Note: This is a simplified implementation
        // For production, you'd want to use the Google Cloud Speech client library
        
        val audioFile = File(audioFilePath)
        if (!audioFile.exists()) {
            return STTResult.Error("Audio file not found")
        }
        
        // Convert audio to base64 for Google API
        val audioBytes = audioFile.readBytes()
        val audioBase64 = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)
        
        val requestBody = JSONObject().apply {
            put("config", JSONObject().apply {
                put("encoding", "WEBM_OPUS") // Adjust based on your audio format
                put("sampleRateHertz", 16000)
                put("languageCode", language)
                put("enableAutomaticPunctuation", true)
                put("enableWordTimeOffsets", true)
            })
            put("audio", JSONObject().apply {
                put("content", audioBase64)
            })
        }
        
        val request = Request.Builder()
            .url("$GOOGLE_STT_URL?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseGoogleResponse(responseBody ?: "")
            } else {
                STTResult.Error("Google STT error: ${response.code}")
            }
        } catch (e: IOException) {
            STTResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Azure Speech Services Integration
     */
    private suspend fun transcribeWithAzure(
        audioFilePath: String,
        apiKey: String,
        language: String
    ): STTResult {
        val audioFile = File(audioFilePath)
        if (!audioFile.exists()) {
            return STTResult.Error("Audio file not found")
        }
        
        val request = Request.Builder()
            .url("$AZURE_STT_URL?language=$language")
            .addHeader("Ocp-Apim-Subscription-Key", apiKey)
            .addHeader("Content-Type", "audio/wav")
            .post(audioFile.asRequestBody("audio/wav".toMediaType()))
            .build()
        
        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseAzureResponse(responseBody ?: "")
            } else {
                STTResult.Error("Azure STT error: ${response.code}")
            }
        } catch (e: IOException) {
            STTResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Mock transcription for testing
     */
    private fun transcribeWithMock(audioFilePath: String): STTResult {
        val audioFile = File(audioFilePath)
        val duration = estimateAudioDuration(audioFile)
        
        val mockTranscript = when {
            duration < 30000 -> "This is a short voice note. I wanted to quickly record some thoughts about the upcoming project meeting and make sure I don't forget to follow up with the team."
            duration < 120000 -> "This is a medium-length voice recording. Today I had a productive meeting with the client where we discussed the project requirements in detail. The main points covered were the timeline, budget constraints, and technical specifications. I need to follow up with a detailed proposal by Friday and schedule a technical review session with the development team."
            else -> "This is a longer voice recording with detailed discussions. The quarterly review meeting was comprehensive and covered multiple aspects of our business operations. We analyzed the performance metrics, discussed market trends, and identified key areas for improvement. The action items include updating our strategic plan, implementing new quality assurance processes, and expanding our customer outreach programs. I also need to coordinate with the marketing team about the upcoming product launch and ensure all stakeholders are aligned on the timeline and deliverables."
        }
        
        return STTResult.Success(
            transcript = mockTranscript,
            confidence = 0.95f,
            duration = duration,
            wordCount = mockTranscript.split(" ").size
        )
    }
    
    /**
     * Parse OpenAI Whisper response
     */
    private fun parseWhisperResponse(responseBody: String): STTResult {
        return try {
            val json = JSONObject(responseBody)
            val transcript = json.getString("text")
            
            STTResult.Success(
                transcript = transcript,
                confidence = 0.9f, // Whisper doesn't provide confidence scores
                duration = 0L, // Would need to be calculated separately
                wordCount = transcript.split(" ").size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Whisper response", e)
            STTResult.Error("Failed to parse transcription response")
        }
    }
    
    /**
     * Parse Google Cloud STT response
     */
    private fun parseGoogleResponse(responseBody: String): STTResult {
        return try {
            val json = JSONObject(responseBody)
            val results = json.getJSONArray("results")
            
            if (results.length() > 0) {
                val firstResult = results.getJSONObject(0)
                val alternatives = firstResult.getJSONArray("alternatives")
                val bestAlternative = alternatives.getJSONObject(0)
                
                val transcript = bestAlternative.getString("transcript")
                val confidence = bestAlternative.optDouble("confidence", 0.9).toFloat()
                
                STTResult.Success(
                    transcript = transcript,
                    confidence = confidence,
                    duration = 0L,
                    wordCount = transcript.split(" ").size
                )
            } else {
                STTResult.Error("No transcription results found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Google response", e)
            STTResult.Error("Failed to parse transcription response")
        }
    }
    
    /**
     * Parse Azure Speech Services response
     */
    private fun parseAzureResponse(responseBody: String): STTResult {
        return try {
            val json = JSONObject(responseBody)
            val recognitionStatus = json.getString("RecognitionStatus")
            
            if (recognitionStatus == "Success") {
                val displayText = json.getString("DisplayText")
                val confidence = json.optJSONObject("NBest")
                    ?.optJSONArray("Confidence")
                    ?.optDouble(0, 0.9)?.toFloat() ?: 0.9f
                
                STTResult.Success(
                    transcript = displayText,
                    confidence = confidence,
                    duration = json.optLong("Duration", 0L),
                    wordCount = displayText.split(" ").size
                )
            } else {
                STTResult.Error("Azure recognition failed: $recognitionStatus")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Azure response", e)
            STTResult.Error("Failed to parse transcription response")
        }
    }
    
    /**
     * Estimate audio duration from file size (rough approximation)
     */
    private fun estimateAudioDuration(audioFile: File): Long {
        // Very rough estimation: assume ~1KB per second for compressed audio
        return audioFile.length() / 1024 * 1000
    }
}

enum class STTProvider {
    OPENAI_WHISPER,
    GOOGLE_CLOUD,
    AZURE,
    MOCK
}

sealed class STTResult {
    data class Success(
        val transcript: String,
        val confidence: Float,
        val duration: Long,
        val wordCount: Int
    ) : STTResult()
    
    data class Error(val message: String) : STTResult()
}
