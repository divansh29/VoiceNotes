package com.voicenotes.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotes.app.ai.AIService
import com.voicenotes.app.ai.AIResult
import com.voicenotes.app.audio.AudioPlayer
import com.voicenotes.app.audio.AudioRecorder
import com.voicenotes.app.data.VoiceNote
import com.voicenotes.app.data.VoiceNoteDatabase
import com.voicenotes.app.repository.VoiceNoteRepository
import com.voicenotes.app.naming.NamingManager
import com.voicenotes.app.audio.FileProcessor
import com.voicenotes.app.cloud.GoogleDriveService
import com.voicenotes.app.cloud.DriveFile
import com.voicenotes.app.cloud.UploadResult
import com.voicenotes.app.audio.TTSHelper
import com.voicenotes.app.audio.SimpleTTSHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class VoiceNotesViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: VoiceNoteRepository
    private val audioRecorder: AudioRecorder
    private val audioPlayer: AudioPlayer
    private val aiService: AIService
    private val namingManager: NamingManager
    private val fileProcessor: FileProcessor
    private val googleDriveService: GoogleDriveService
    
    init {
        val database = VoiceNoteDatabase.getDatabase(application)
        repository = VoiceNoteRepository(database.voiceNoteDao())
        audioRecorder = AudioRecorder(application)
        audioPlayer = AudioPlayer()
        aiService = AIService(application)
        namingManager = NamingManager(application)
        fileProcessor = FileProcessor(application)
        googleDriveService = GoogleDriveService(application)
    }
    
    // UI State
    private val _uiState = MutableStateFlow(VoiceNotesUiState())
    val uiState: StateFlow<VoiceNotesUiState> = _uiState.asStateFlow()
    
    // Voice notes from database
    val voiceNotes: StateFlow<List<VoiceNote>> = repository.getAllVoiceNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Audio player state
    val isPlaying = audioPlayer.isPlaying
    val currentPosition = audioPlayer.currentPosition
    val duration = audioPlayer.duration
    
    fun startRecording() {
        viewModelScope.launch {
            try {
                // Check permissions first
                if (!hasRecordingPermission()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Microphone permission is required to record audio"
                    )
                    return@launch
                }

                val filePath = audioRecorder.startRecording()
                if (filePath != null) {
                    _uiState.value = _uiState.value.copy(
                        isRecording = true,
                        recordingFilePath = filePath,
                        errorMessage = null
                    )
                    android.util.Log.d("VoiceNotesViewModel", "Recording started: $filePath")
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to start recording. Please check microphone permissions."
                    )
                    android.util.Log.e("VoiceNotesViewModel", "Failed to start recording - null file path")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Recording error: ${e.message}"
                )
                android.util.Log.e("VoiceNotesViewModel", "Recording error", e)
            }
        }
    }

    private fun hasRecordingPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun testRecording() {
        viewModelScope.launch {
            try {
                val result = audioRecorder.testRecording()
                android.util.Log.d("VoiceNotesViewModel", "Recording test: $result")
                _uiState.value = _uiState.value.copy(
                    errorMessage = result
                )
            } catch (e: Exception) {
                android.util.Log.e("VoiceNotesViewModel", "Recording test failed", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Recording test failed: ${e.message}"
                )
            }
        }
    }
    
    fun stopRecording() {
        val result = audioRecorder.stopRecording()
        if (result != null) {
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                recordingFilePath = null,
                isProcessing = true
            )
            
            // Save to database and process with AI
            viewModelScope.launch {
                try {
                    // Create initial voice note
                    val voiceNote = VoiceNote(
                        title = "Processing...",
                        filePath = result.filePath,
                        duration = result.duration,
                        fileSize = result.fileSize,
                        createdAt = Date(),
                        isProcessing = true
                    )
                    
                    val noteId = repository.insertVoiceNote(voiceNote)
                    
                    // Process with AI
                    processVoiceNoteWithAI(noteId, result.filePath)
                    
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = "Failed to save recording: ${e.message}"
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                errorMessage = "Failed to stop recording"
            )
        }
    }
    
    private suspend fun processVoiceNoteWithAI(noteId: Long, filePath: String) {
        try {
            // Transcribe audio
            val transcript = aiService.transcribeAudio(filePath)
            
            // Generate AI summary with action items and speaking patterns
            val aiResult = aiService.generateSummary(transcript, filePath)

            // Generate custom name based on content
            val voiceNote = repository.getVoiceNoteById(noteId)
            val recordingNumber = repository.getVoiceNotesCount() + 1
            val customTitle = namingManager.generateRecordingName(
                transcript = transcript,
                duration = voiceNote?.duration ?: 0,
                recordingNumber = recordingNumber.toInt()
            )

            // Update voice note with AI results and custom name
            voiceNote?.let { note ->
                val updatedNote = note.copy(
                    title = customTitle,
                    transcript = transcript,
                    summary = aiResult.summary,
                    keyPoints = aiResult.keyPoints,
                    isProcessing = false
                )
                repository.updateVoiceNote(updatedNote)
            }
            
            _uiState.value = _uiState.value.copy(isProcessing = false)
            
        } catch (e: Exception) {
            // Update note to show processing failed
            val voiceNote = repository.getVoiceNoteById(noteId)
            voiceNote?.let { note ->
                val updatedNote = note.copy(
                    title = "Recording ${Date()}",
                    isProcessing = false
                )
                repository.updateVoiceNote(updatedNote)
            }
            
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                errorMessage = "Failed to process recording: ${e.message}"
            )
        }
    }
    
    fun playAudio(voiceNote: VoiceNote) {
        audioPlayer.playAudio(voiceNote.filePath) {
            // On completion
        }
    }
    
    fun pauseAudio() {
        audioPlayer.pauseAudio()
    }
    
    fun resumeAudio() {
        audioPlayer.resumeAudio()
    }
    
    fun stopAudio() {
        audioPlayer.stopAudio()
    }
    
    fun deleteVoiceNote(voiceNote: VoiceNote) {
        viewModelScope.launch {
            try {
                // Delete file
                val file = java.io.File(voiceNote.filePath)
                if (file.exists()) {
                    file.delete()
                }
                
                // Delete from database
                repository.deleteVoiceNote(voiceNote)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete recording: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Handle speech recognition result with audio synthesis and AI analysis
     */
    fun handleSpeechRecognitionResult(transcript: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isProcessing = true)

                // Create audio file from text using TTS
                val audioFileName = "speech_${System.currentTimeMillis()}.wav"
                val audioFilePath = getApplication<Application>().filesDir.absolutePath + "/" + audioFileName

                // Generate audio from text using Text-to-Speech
                val audioGenerated = generateAudioFromText(transcript, audioFilePath)

                if (audioGenerated) {
                    // Process with AI to extract keywords and summary
                    val aiResult = aiService.generateSummary(transcript, audioFilePath)

                    // Extract keywords (first 5 most important)
                    val keywords = aiResult.keyPoints.take(5)

                    // Generate 1-line summary using enhanced AI
                    val oneLinerSummary = if (aiResult.summary.isNotEmpty()) {
                        generateOneLinerSummary(transcript, aiResult.summary)
                    } else {
                        // Fallback to simple truncation
                        if (transcript.length <= 80) transcript else transcript.take(77) + "..."
                    }

                    // Create voice note entry
                    val audioFile = java.io.File(audioFilePath)
                    val voiceNote = VoiceNote(
                        title = "Speech: ${oneLinerSummary.take(30)}...",
                        filePath = audioFilePath,
                        duration = estimateAudioDuration(transcript),
                        fileSize = audioFile.length(),
                        createdAt = Date(),
                        transcript = transcript,
                        summary = oneLinerSummary,
                        keyPoints = keywords,
                        isProcessing = false
                    )

                    // Save to database
                    repository.insertVoiceNote(voiceNote)

                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = null
                    )

                    // Auto-play the generated audio
                    playAudio(voiceNote)

                } else {
                    // Fallback: create voice note without audio
                    createTextOnlyVoiceNote(transcript)
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to process speech: ${e.message}",
                    isProcessing = false
                )
            }
        }
    }

    /**
     * Generate audio from text using Android's Text-to-Speech
     */
    private suspend fun generateAudioFromText(text: String, outputPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Use simplified TTS helper
                val ttsHelper = SimpleTTSHelper(getApplication())
                ttsHelper.createAudioFromText(text, outputPath)
            } catch (e: Exception) {
                Log.e("TTS", "Failed to generate audio from text", e)
                false
            }
        }
    }

    /**
     * Generate a concise one-line summary
     */
    private fun generateOneLinerSummary(transcript: String, aiSummary: String): String {
        return when {
            transcript.length <= 50 -> transcript
            aiSummary.isNotEmpty() -> {
                // Take first sentence of AI summary, max 80 characters
                val firstSentence = aiSummary.split(".")[0].trim()
                if (firstSentence.length <= 80) firstSentence else firstSentence.take(77) + "..."
            }
            else -> {
                // Fallback: first 80 characters of transcript
                if (transcript.length <= 80) transcript else transcript.take(77) + "..."
            }
        }
    }

    /**
     * Estimate audio duration based on text length (average speaking speed)
     */
    private fun estimateAudioDuration(text: String): Long {
        // Average speaking speed: ~150 words per minute = 2.5 words per second
        val wordCount = text.split(" ").size
        val durationSeconds = (wordCount / 2.5).toLong()
        return durationSeconds * 1000 // Convert to milliseconds
    }

    /**
     * Create voice note without audio (fallback)
     */
    private suspend fun createTextOnlyVoiceNote(transcript: String) {
        try {
            // Process with AI
            val aiResult = aiService.generateSummary(transcript, "")
            val keywords = aiResult.keyPoints.take(5)
            val oneLinerSummary = generateOneLinerSummary(transcript, aiResult.summary)

            // Create text-only voice note
            val voiceNote = VoiceNote(
                title = "Speech: ${oneLinerSummary.take(30)}...",
                filePath = "", // No audio file
                duration = estimateAudioDuration(transcript),
                fileSize = 0L,
                createdAt = Date(),
                transcript = transcript,
                summary = oneLinerSummary,
                keyPoints = keywords,
                isProcessing = false
            )

            repository.insertVoiceNote(voiceNote)

            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                errorMessage = null
            )

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to create voice note: ${e.message}",
                isProcessing = false
            )
        }
    }

    /**
     * Handle speech recognition error
     */
    fun handleSpeechRecognitionError(errorMessage: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = "Speech recognition failed: $errorMessage",
            isRecording = false
        )
    }

    fun processUploadedFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isProcessing = true)

                // Check if file is supported
                if (!fileProcessor.isSupportedAudioFile(fileName)) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = "Unsupported file format. Please select an audio file."
                    )
                    return@launch
                }

                // Copy file to internal storage
                val localFilePath = fileProcessor.processUploadedFile(uri, fileName)
                if (localFilePath == null) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = "Failed to process uploaded file"
                    )
                    return@launch
                }

                // Create initial voice note for uploaded file
                val voiceNote = VoiceNote(
                    title = "Processing uploaded file...",
                    filePath = localFilePath,
                    duration = 0, // Will be updated after processing
                    fileSize = java.io.File(localFilePath).length(),
                    createdAt = Date(),
                    isProcessing = true
                )

                val noteId = repository.insertVoiceNote(voiceNote)

                // Process with AI
                processVoiceNoteWithAI(noteId, localFilePath)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "Failed to process uploaded file: ${e.message}"
                )
            }
        }
    }

    // Google Drive methods
    fun getGoogleSignInIntent() = googleDriveService.getSignInIntent()

    fun handleGoogleSignInResult(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            if (account != null) {
                val success = googleDriveService.initializeDriveService(account)
                _uiState.value = _uiState.value.copy(
                    isDriveSignedIn = success,
                    errorMessage = if (!success) "Failed to connect to Google Drive" else null
                )
                if (success) {
                    loadDriveFiles()
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isDriveSignedIn = false,
                    errorMessage = "Google Drive sign-in failed"
                )
            }
        }
    }

    fun signOutFromDrive() {
        viewModelScope.launch {
            googleDriveService.signOut()
            _uiState.value = _uiState.value.copy(
                isDriveSignedIn = false,
                driveFiles = emptyList()
            )
        }
    }

    fun loadDriveFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDriveLoading = true)
            try {
                val files = googleDriveService.listAudioFiles()
                _uiState.value = _uiState.value.copy(
                    driveFiles = files,
                    isDriveLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDriveLoading = false,
                    errorMessage = "Failed to load Drive files: ${e.message}"
                )
            }
        }
    }

    fun uploadToDrive(filePath: String, fileName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(driveUploadProgress = 0)

                val result = googleDriveService.uploadAudioFile(
                    localFilePath = filePath,
                    fileName = fileName
                ) { progress ->
                    _uiState.value = _uiState.value.copy(driveUploadProgress = progress)
                }

                when (result) {
                    is UploadResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            driveUploadProgress = null,
                            errorMessage = null
                        )
                        loadDriveFiles() // Refresh the list
                    }
                    is UploadResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            driveUploadProgress = null,
                            errorMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    driveUploadProgress = null,
                    errorMessage = "Upload failed: ${e.message}"
                )
            }
        }
    }

    fun deleteDriveFile(fileId: String) {
        viewModelScope.launch {
            try {
                val success = googleDriveService.deleteFile(fileId)
                if (success) {
                    loadDriveFiles() // Refresh the list
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete file"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Delete failed: ${e.message}"
                )
            }
        }
    }

    fun checkDriveSignInStatus() {
        val isSignedIn = googleDriveService.isSignedIn()
        _uiState.value = _uiState.value.copy(isDriveSignedIn = isSignedIn)
        if (isSignedIn) {
            loadDriveFiles()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stopAudio()
    }
}

data class VoiceNotesUiState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val recordingFilePath: String? = null,
    val errorMessage: String? = null,
    // Google Drive state
    val isDriveSignedIn: Boolean = false,
    val driveFiles: List<DriveFile> = emptyList(),
    val isDriveLoading: Boolean = false,
    val driveUploadProgress: Int? = null
)
