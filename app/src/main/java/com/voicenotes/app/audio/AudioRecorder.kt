package com.voicenotes.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AudioRecorder(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorder"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var startTime: Long = 0
    
    fun startRecording(): String? {
        return try {
            Log.d(TAG, "Starting recording...")

            // Stop any existing recording
            if (isRecording) {
                stopRecording()
            }

            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) {
                val created = recordingsDir.mkdirs()
                Log.d(TAG, "Created recordings directory: $created")
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = File(recordingsDir, "recording_$timestamp.m4a")

            Log.d(TAG, "Output file: ${outputFile!!.absolutePath}")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                try {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(outputFile!!.absolutePath)

                    Log.d(TAG, "MediaRecorder configured, preparing...")
                    prepare()
                    Log.d(TAG, "MediaRecorder prepared, starting...")
                    start()
                    Log.d(TAG, "Recording started successfully!")
                } catch (e: Exception) {
                    Log.e(TAG, "Error configuring MediaRecorder", e)
                    throw e
                }
            }

            isRecording = true
            startTime = System.currentTimeMillis()
            outputFile!!.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            stopRecording()
            null
        }
    }
    
    fun stopRecording(): RecordingResult? {
        return try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                
                val duration = System.currentTimeMillis() - startTime
                val fileSize = outputFile?.length() ?: 0
                
                outputFile?.let { file ->
                    RecordingResult(
                        filePath = file.absolutePath,
                        duration = duration,
                        fileSize = fileSize
                    )
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            null
        }
    }
    
    fun isRecording(): Boolean = isRecording
    
    fun getCurrentDuration(): Long {
        return if (isRecording) {
            System.currentTimeMillis() - startTime
        } else {
            0
        }
    }

    /**
     * Test if recording is working by checking MediaRecorder state
     */
    fun testRecording(): String {
        return try {
            val testRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            testRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile("/dev/null") // Dummy output
                prepare()
                release()
            }

            "✅ Recording test passed - MediaRecorder is working"
        } catch (e: Exception) {
            "❌ Recording test failed: ${e.message}"
        }
    }
    
    data class RecordingResult(
        val filePath: String,
        val duration: Long,
        val fileSize: Long
    )
}
