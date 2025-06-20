package com.voicenotes.app.audio

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.IOException

class AudioPlayer {

    companion object {
        private const val TAG = "AudioPlayer"
    }

    private var mediaPlayer: MediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration
    
    fun playAudio(filePath: String, onCompletion: (() -> Unit)? = null): Boolean {
        return try {
            Log.d(TAG, "Attempting to play audio: $filePath")

            // Check if file exists
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "Audio file does not exist: $filePath")
                return false
            }

            if (file.length() == 0L) {
                Log.e(TAG, "Audio file is empty: $filePath")
                return false
            }

            Log.d(TAG, "File exists, size: ${file.length()} bytes")

            stopAudio() // Stop any current playback

            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(filePath)
                    Log.d(TAG, "Data source set, preparing...")

                    setOnPreparedListener { mp ->
                        Log.d(TAG, "MediaPlayer prepared, duration: ${mp.duration}ms")
                        _duration.value = mp.duration
                        mp.start()
                        _isPlaying.value = true
                        Log.d(TAG, "Playback started")
                        startPositionUpdates()
                    }

                    setOnCompletionListener {
                        Log.d(TAG, "Playback completed")
                        _isPlaying.value = false
                        _currentPosition.value = 0
                        onCompletion?.invoke()
                    }

                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        _isPlaying.value = false
                        _currentPosition.value = 0
                        true
                    }

                    prepareAsync()
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up MediaPlayer", e)
                    throw e
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio", e)
            false
        }
    }
    
    fun pauseAudio() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
            }
        }
    }
    
    fun resumeAudio() {
        mediaPlayer?.let { mp ->
            if (!mp.isPlaying) {
                mp.start()
                _isPlaying.value = true
                startPositionUpdates()
            }
        }
    }
    
    fun stopAudio() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }
            mp.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0
        _duration.value = 0
    }
    
    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        _currentPosition.value = position
    }
    
    private fun startPositionUpdates() {
        // This would typically use a coroutine or handler to update position
        // For simplicity, we'll update it when needed
    }
    
    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }
    
    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }
}
