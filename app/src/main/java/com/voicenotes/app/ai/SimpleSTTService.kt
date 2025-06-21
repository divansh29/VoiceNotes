package com.voicenotes.app.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log

/**
 * Simplified Speech-to-Text service that focuses on core functionality
 * without complex error handling or confidence scores
 */
class SimpleSTTService(private val context: Context) {
    
    companion object {
        private const val TAG = "SimpleSTTService"
        const val REQUEST_CODE_SPEECH = 1001
    }
    
    /**
     * Create a simple speech recognition intent
     */
    fun createSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // Use free-form speech recognition
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            
            // Set language (can be changed to other languages)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            
            // Prompt text shown to user
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            
            // Request only one result for simplicity
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }
    
    /**
     * Handle speech recognition result - simplified version
     */
    fun handleResult(resultCode: Int, data: Intent?): String? {
        return when (resultCode) {
            Activity.RESULT_OK -> {
                // Get the recognized text
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                
                if (!results.isNullOrEmpty()) {
                    val transcript = results[0]
                    Log.d(TAG, "Speech recognized: $transcript")
                    transcript
                } else {
                    Log.w(TAG, "No speech results found")
                    null
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Speech recognition cancelled by user")
                null
            }
            else -> {
                Log.e(TAG, "Speech recognition failed with code: $resultCode")
                null
            }
        }
    }
    
    /**
     * Check if speech recognition is available
     */
    fun isAvailable(): Boolean {
        val intent = createSpeechIntent()
        val activities = context.packageManager.queryIntentActivities(intent, 0)
        return activities.isNotEmpty()
    }
}

/**
 * Simple result class for speech recognition
 */
data class SimpleSpeechResult(
    val transcript: String,
    val success: Boolean = true
) {
    companion object {
        fun success(transcript: String) = SimpleSpeechResult(transcript, true)
        fun failure() = SimpleSpeechResult("", false)
    }
}
