package com.voicenotes.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import com.voicenotes.app.ui.screens.MainScreen
import com.voicenotes.app.ui.screens.AuthenticationScreen
import com.voicenotes.app.ui.components.PINSetupDialog
import com.voicenotes.app.ui.components.FileUploadDialog
import com.voicenotes.app.ui.theme.VoiceNotesTheme
import com.voicenotes.app.viewmodel.VoiceNotesViewModel
import com.voicenotes.app.security.SecurityManager
import com.voicenotes.app.ai.AndroidSTTService
import com.voicenotes.app.ai.AndroidSTTResult
import com.voicenotes.app.ai.SimpleSTTService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import android.content.Intent
import android.util.Log

class MainActivity : ComponentActivity() {

    private val viewModel: VoiceNotesViewModel by viewModels()
    private var hasAudioPermission by mutableStateOf(false)
    private var isAuthenticated by mutableStateOf(false)
    private lateinit var securityManager: SecurityManager
    private lateinit var androidSTTService: AndroidSTTService
    private lateinit var simpleSTTService: SimpleSTTService
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    // Speech recognition launcher - simplified version
    private val speechRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val transcript = simpleSTTService.handleResult(result.resultCode, result.data)
        if (transcript != null) {
            Log.d("STT", "Speech recognized: $transcript")
            viewModel.handleSpeechRecognitionResult(transcript)
        } else {
            Log.e("STT", "Speech recognition failed or cancelled")
            viewModel.handleSpeechRecognitionError("Speech recognition failed")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize security manager
        securityManager = SecurityManager(this)

        // Initialize Android STT service
        androidSTTService = AndroidSTTService(this)
        simpleSTTService = SimpleSTTService(this)

        // Always require authentication on app start
        // User is never authenticated initially - must always enter PIN
        isAuthenticated = false

        // Check for audio permission
        hasAudioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        setContent {
            VoiceNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceNotesApp(
                        viewModel = viewModel,
                        hasAudioPermission = hasAudioPermission,
                        isAuthenticated = isAuthenticated,
                        securityManager = securityManager,
                        onRequestPermission = {
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onAuthenticated = {
                            isAuthenticated = true
                        },
                        onSpeechRecognition = {
                            startSpeechRecognition()
                        }
                    )
                }
            }
        }
    }

    /**
     * Start Android's built-in speech recognition - simplified version
     */
    fun startSpeechRecognition() {
        if (simpleSTTService.isAvailable()) {
            val intent = simpleSTTService.createSpeechIntent()
            speechRecognitionLauncher.launch(intent)
        } else {
            Log.e("STT", "Speech recognition not available on this device")
        }
    }
}

@Composable
fun VoiceNotesApp(
    viewModel: VoiceNotesViewModel,
    hasAudioPermission: Boolean,
    isAuthenticated: Boolean,
    securityManager: SecurityManager,
    onRequestPermission: () -> Unit,
    onAuthenticated: () -> Unit,
    onSpeechRecognition: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val voiceNotes by viewModel.voiceNotes.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    var currentlyPlayingId by remember { mutableStateOf<Long?>(null) }
    var currentScreen by remember { mutableStateOf("main") }
    var showPinSetup by remember { mutableStateOf(false) }
    var showFileUpload by remember { mutableStateOf(false) }
    var showDriveDialog by remember { mutableStateOf(false) }

    // Google Drive sign-in launcher
    val driveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            viewModel.handleGoogleSignInResult(account)
        } catch (e: ApiException) {
            viewModel.handleGoogleSignInResult(null)
        }
    }

    // Check Drive sign-in status on startup
    LaunchedEffect(Unit) {
        viewModel.checkDriveSignInStatus()
    }

    when {
        !isAuthenticated -> {
            AuthenticationScreen(
                securityManager = securityManager,
                onAuthenticated = onAuthenticated,
                onSetupSecurity = { showPinSetup = true }
            )
        }
        !hasAudioPermission -> {
            PermissionScreen(onRequestPermission = onRequestPermission)
        }
        else -> {
            when (currentScreen) {
                "main" -> MainScreen(
                uiState = uiState,
                voiceNotes = voiceNotes,
                isPlaying = isPlaying,
                currentlyPlayingId = currentlyPlayingId,
                onStartRecording = {
                    viewModel.startRecording()
                },
                onStopRecording = {
                    viewModel.stopRecording()
                },
                onPlayVoiceNote = { voiceNote ->
                    currentlyPlayingId = voiceNote.id
                    viewModel.playAudio(voiceNote)
                },
                onPauseAudio = {
                    viewModel.pauseAudio()
                },
                onDeleteVoiceNote = { voiceNote ->
                    if (currentlyPlayingId == voiceNote.id) {
                        viewModel.stopAudio()
                        currentlyPlayingId = null
                    }
                    viewModel.deleteVoiceNote(voiceNote)
                },
                onDismissError = {
                    viewModel.clearError()
                },
                onNavigateToAnalytics = {
                    currentScreen = "analytics"
                },
                onNavigateToSettings = {
                    currentScreen = "settings"
                },
                onUploadFile = {
                    showFileUpload = true
                },
                onOpenDrive = {
                    showDriveDialog = true
                },
                onSpeechRecognition = onSpeechRecognition
                )

                "analytics" -> {
                    com.voicenotes.app.ui.screens.AnalyticsScreen(
                        voiceNotes = voiceNotes,
                        onBackClick = { currentScreen = "main" }
                    )
                }

                "settings" -> {
                    com.voicenotes.app.ui.screens.SettingsScreen(
                        onBackClick = { currentScreen = "main" }
                    )
                }
            }
        }
    }

    // PIN Setup Dialog
    if (showPinSetup) {
        PINSetupDialog(
            onDismiss = {
                showPinSetup = false
                // If user cancels PIN setup, still mark first time setup as complete
                securityManager.markFirstTimeSetupComplete()
            },
            onPINSet = { pin ->
                securityManager.setupPIN(pin)
                showPinSetup = false
                onAuthenticated()
            }
        )
    }

    // File Upload Dialog
    if (showFileUpload) {
        FileUploadDialog(
            onDismiss = { showFileUpload = false },
            onFileSelected = { uri, fileName ->
                viewModel.processUploadedFile(uri, fileName)
                showFileUpload = false
            }
        )
    }

    // Google Drive Dialog
    if (showDriveDialog) {
        com.voicenotes.app.ui.components.DriveIntegrationDialog(
            isSignedIn = uiState.isDriveSignedIn,
            driveFiles = uiState.driveFiles,
            isLoading = uiState.isDriveLoading,
            uploadProgress = uiState.driveUploadProgress,
            onDismiss = { showDriveDialog = false },
            onSignIn = {
                driveSignInLauncher.launch(viewModel.getGoogleSignInIntent())
            },
            onSignOut = {
                viewModel.signOutFromDrive()
            },
            onUploadFile = {
                showFileUpload = true
                showDriveDialog = false
            },
            onRefreshFiles = {
                viewModel.loadDriveFiles()
            },
            onDeleteFile = { fileId ->
                viewModel.deleteDriveFile(fileId)
            }
        )
    }
}

@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Audio Recording Permission Required",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "VoiceNotes needs access to your microphone to record voice notes.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission
        ) {
            Text("Grant Permission")
        }
    }
}
