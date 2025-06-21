package com.voicenotes.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.*
import kotlin.coroutines.resume

class TTSHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "TTSHelper"
    }
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    
    /**
     * Initialize Text-to-Speech engine
     */
    suspend fun initialize(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            textToSpeech = TextToSpeech(context) { status ->
                isInitialized = status == TextToSpeech.SUCCESS
                if (isInitialized) {
                    textToSpeech?.language = Locale.US
                    textToSpeech?.setSpeechRate(1.0f)
                    textToSpeech?.setPitch(1.0f)
                }
                continuation.resume(isInitialized)
            }
            
            continuation.invokeOnCancellation {
                textToSpeech?.shutdown()
            }
        }
    }
    
    /**
     * Synthesize text to audio file
     */
    suspend fun synthesizeToFile(text: String, outputPath: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            if (!isInitialized) {
                Log.e(TAG, "TTS not initialized")
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            
            val utteranceId = "speech_synthesis_${System.currentTimeMillis()}"
            
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS synthesis started")
                }
                
                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS synthesis completed")
                    continuation.resume(true)
                }
                
                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS synthesis error")
                    continuation.resume(false)
                }
            })
            
            // Create output file
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            // Synthesize to file
            val result = textToSpeech?.synthesizeToFile(
                text,
                null,
                outputFile,
                utteranceId
            )
            
            if (result != TextToSpeech.SUCCESS) {
                Log.e(TAG, "Failed to start TTS synthesis")
                continuation.resume(false)
            }
            
            continuation.invokeOnCancellation {
                textToSpeech?.stop()
            }
        }
    }
    
    /**
     * Speak text directly (for testing)
     */
    fun speak(text: String) {
        if (isInitialized) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak_${System.currentTimeMillis()}")
        }
    }
    
    /**
     * Check if TTS is available
     */
    fun isAvailable(): Boolean {
        return isInitialized && textToSpeech != null
    }
    
    /**
     * Clean up resources
     */
    fun shutdown() {
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
}

/**
 * Simple TTS implementation that creates audio files from text
 */
class SimpleTTSHelper(private val context: Context) {
    
    /**
     * Create a simple audio file from text using TTS
     * This is a simplified version that works reliably
     */
    suspend fun createAudioFromText(text: String, outputPath: String): Boolean {
        return try {
            // Initialize TTS
            val ttsHelper = TTSHelper(context)
            val initialized = ttsHelper.initialize()
            
            if (initialized) {
                // Generate audio file
                val success = ttsHelper.synthesizeToFile(text, outputPath)
                ttsHelper.shutdown()
                success
            } else {
                Log.e("SimpleTTS", "Failed to initialize TTS")
                // Create a dummy audio file as fallback
                createDummyAudioFile(outputPath)
                false
            }
        } catch (e: Exception) {
            Log.e("SimpleTTS", "Error creating audio from text", e)
            // Create a dummy audio file as fallback
            createDummyAudioFile(outputPath)
            false
        }
    }
    
    /**
     * Create a dummy audio file as fallback when TTS fails
     */
    private fun createDummyAudioFile(outputPath: String): Boolean {
        return try {
            val file = File(outputPath)
            file.parentFile?.mkdirs()
            
            // Create a minimal WAV file header (silent audio)
            val wavHeader = createWavHeader(1000) // 1 second of silence
            file.writeBytes(wavHeader)
            
            Log.d("SimpleTTS", "Created dummy audio file: $outputPath")
            true
        } catch (e: Exception) {
            Log.e("SimpleTTS", "Failed to create dummy audio file", e)
            false
        }
    }
    
    /**
     * Create a minimal WAV file header for silent audio
     */
    private fun createWavHeader(durationMs: Int): ByteArray {
        val sampleRate = 44100
        val channels = 1
        val bitsPerSample = 16
        val samples = (sampleRate * durationMs / 1000)
        val dataSize = samples * channels * bitsPerSample / 8
        val fileSize = dataSize + 36
        
        return byteArrayOf(
            // RIFF header
            0x52, 0x49, 0x46, 0x46, // "RIFF"
            (fileSize and 0xff).toByte(),
            ((fileSize shr 8) and 0xff).toByte(),
            ((fileSize shr 16) and 0xff).toByte(),
            ((fileSize shr 24) and 0xff).toByte(),
            0x57, 0x41, 0x56, 0x45, // "WAVE"
            
            // Format chunk
            0x66, 0x6d, 0x74, 0x20, // "fmt "
            0x10, 0x00, 0x00, 0x00, // Chunk size (16)
            0x01, 0x00, // Audio format (PCM)
            channels.toByte(), 0x00, // Number of channels
            (sampleRate and 0xff).toByte(),
            ((sampleRate shr 8) and 0xff).toByte(),
            ((sampleRate shr 16) and 0xff).toByte(),
            ((sampleRate shr 24) and 0xff).toByte(),
            0x00, 0x00, 0x00, 0x00, // Byte rate
            0x02, 0x00, // Block align
            bitsPerSample.toByte(), 0x00, // Bits per sample
            
            // Data chunk
            0x64, 0x61, 0x74, 0x61, // "data"
            (dataSize and 0xff).toByte(),
            ((dataSize shr 8) and 0xff).toByte(),
            ((dataSize shr 16) and 0xff).toByte(),
            ((dataSize shr 24) and 0xff).toByte()
        ) + ByteArray(dataSize) // Silent audio data (all zeros)
    }
}
