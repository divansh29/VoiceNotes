package com.voicenotes.app.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidSTTService(private val context: Context) {
    
    companion object {
        private const val TAG = "AndroidSTTService"
        const val REQUEST_CODE_SPEECH = 1001
    }
    
    /**
     * Method 1: Using Intent-based Speech Recognition (Easiest)
     * This opens Google's speech recognition dialog
     */
    fun createSpeechRecognitionIntent(): Intent {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // Language model for free-form speech recognition
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            
            // Language preference
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            
            // Prompt text shown to user
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now for VoiceNotes...")
            
            // Maximum number of results to return
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            
            // Enable partial results
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            
            // Request higher quality audio
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }
        return intent
    }
    
    /**
     * Handle the result from speech recognition intent
     * Call this from onActivityResult() in your Activity
     */
    fun handleSpeechResult(resultCode: Int, data: Intent?): AndroidSTTResult {
        return when (resultCode) {
            Activity.RESULT_OK -> {
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                if (!results.isNullOrEmpty()) {
                    val transcript = results[0]
                    val confidence = 0.9f // Default confidence since scores may not be available
                    
                    AndroidSTTResult.Success(
                        transcript = transcript,
                        confidence = confidence,
                        allResults = results
                    )
                } else {
                    AndroidSTTResult.Error("No speech recognized")
                }
            }
            Activity.RESULT_CANCELED -> {
                AndroidSTTResult.Error("Speech recognition cancelled by user")
            }
            else -> {
                AndroidSTTResult.Error("Speech recognition failed with code: $resultCode")
            }
        }
    }
    
    /**
     * Method 2: Using SpeechRecognizer directly (More control)
     * This doesn't show a dialog, works in background
     */
    suspend fun recognizeSpeechDirectly(language: String = "en-US"): AndroidSTTResult {
        return suspendCancellableCoroutine { continuation ->
            
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                continuation.resume(AndroidSTTResult.Error("Speech recognition not available on this device"))
                return@suspendCancellableCoroutine
            }
            
            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Ready for speech")
                }
                
                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Speech started")
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed - can be used for UI feedback
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {
                    // Audio buffer received
                }
                
                override fun onEndOfSpeech() {
                    Log.d(TAG, "Speech ended")
                }
                
                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech input"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error: $error"
                    }
                    
                    Log.e(TAG, "Speech recognition error: $errorMessage")
                    continuation.resume(AndroidSTTResult.Error(errorMessage))
                    speechRecognizer.destroy()
                }
                
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(RecognizerIntent.EXTRA_RESULTS)

                    if (!matches.isNullOrEmpty()) {
                        val transcript = matches[0]
                        val confidence = 0.8f // Default confidence
                        
                        Log.d(TAG, "Speech recognition successful: $transcript")
                        continuation.resume(
                            AndroidSTTResult.Success(
                                transcript = transcript,
                                confidence = confidence,
                                allResults = matches
                            )
                        )
                    } else {
                        continuation.resume(AndroidSTTResult.Error("No speech recognized"))
                    }
                    speechRecognizer.destroy()
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(RecognizerIntent.EXTRA_RESULTS)
                    if (!matches.isNullOrEmpty()) {
                        Log.d(TAG, "Partial result: ${matches[0]}")
                        // Can be used for real-time transcription display
                    }
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {
                    Log.d(TAG, "Speech event: $eventType")
                }
            })
            
            continuation.invokeOnCancellation {
                speechRecognizer.destroy()
            }
            
            try {
                speechRecognizer.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognition", e)
                continuation.resume(AndroidSTTResult.Error("Failed to start speech recognition: ${e.message}"))
                speechRecognizer.destroy()
            }
        }
    }
    
    /**
     * Check if speech recognition is available on the device
     */
    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * Get supported languages for speech recognition
     */
    fun getSupportedLanguages(): Intent {
        return Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
    }
}

sealed class AndroidSTTResult {
    data class Success(
        val transcript: String,
        val confidence: Float,
        val allResults: List<String>
    ) : AndroidSTTResult()
    
    data class Error(val message: String) : AndroidSTTResult()
}

/**
 * Extension function to handle speech recognition in Activity
 */
fun Activity.startSpeechRecognition(launcher: ActivityResultLauncher<Intent>) {
    val sttService = AndroidSTTService(this)
    if (sttService.isSpeechRecognitionAvailable()) {
        val intent = sttService.createSpeechRecognitionIntent()
        launcher.launch(intent)
    } else {
        Log.e("STT", "Speech recognition not available")
    }
}
