package com.voicenotes.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import kotlin.coroutines.resume

class SecurityManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "voice_notes_security"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SECURITY_ENABLED = "security_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PROTECTED_RECORDINGS = "protected_recordings"
        private const val KEY_FIRST_TIME_SETUP = "first_time_setup"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun isSecurityEnabled(): Boolean {
        return securePrefs.getBoolean(KEY_SECURITY_ENABLED, false)
    }

    fun isFirstTimeSetup(): Boolean {
        return securePrefs.getBoolean(KEY_FIRST_TIME_SETUP, true)
    }

    fun markFirstTimeSetupComplete() {
        securePrefs.edit()
            .putBoolean(KEY_FIRST_TIME_SETUP, false)
            .apply()
    }
    
    fun isBiometricEnabled(): Boolean {
        return securePrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false) && isBiometricAvailable()
    }
    
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    fun setupPIN(pin: String) {
        val hashedPin = hashPIN(pin)
        securePrefs.edit()
            .putString(KEY_PIN_HASH, hashedPin)
            .putBoolean(KEY_SECURITY_ENABLED, true)
            .putBoolean(KEY_FIRST_TIME_SETUP, false)
            .apply()
    }
    
    fun verifyPIN(pin: String): Boolean {
        val storedHash = securePrefs.getString(KEY_PIN_HASH, null) ?: return false
        val inputHash = hashPIN(pin)
        return storedHash == inputHash
    }
    
    fun enableBiometric(enabled: Boolean) {
        securePrefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }
    
    fun disableSecurity() {
        securePrefs.edit()
            .putBoolean(KEY_SECURITY_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .remove(KEY_PIN_HASH)
            .apply()
    }
    
    suspend fun authenticateWithBiometric(activity: FragmentActivity): Boolean {
        if (!isBiometricEnabled()) return false
        
        return suspendCancellableCoroutine { continuation ->
            val biometricPrompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        continuation.resume(true)
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        continuation.resume(false)
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        continuation.resume(false)
                    }
                }
            )
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock VoiceNotes")
                .setSubtitle("Use your biometric credential to access protected recordings")
                .setNegativeButtonText("Use PIN")
                .build()
            
            biometricPrompt.authenticate(promptInfo)
        }
    }
    
    fun markRecordingAsProtected(recordingId: Long) {
        val protectedRecordings = getProtectedRecordings().toMutableSet()
        protectedRecordings.add(recordingId)
        saveProtectedRecordings(protectedRecordings)
    }
    
    fun unmarkRecordingAsProtected(recordingId: Long) {
        val protectedRecordings = getProtectedRecordings().toMutableSet()
        protectedRecordings.remove(recordingId)
        saveProtectedRecordings(protectedRecordings)
    }
    
    fun isRecordingProtected(recordingId: Long): Boolean {
        return getProtectedRecordings().contains(recordingId)
    }
    
    fun getProtectedRecordings(): Set<Long> {
        val protectedString = securePrefs.getString(KEY_PROTECTED_RECORDINGS, "") ?: ""
        return if (protectedString.isEmpty()) {
            emptySet()
        } else {
            protectedString.split(",").mapNotNull { it.toLongOrNull() }.toSet()
        }
    }
    
    private fun saveProtectedRecordings(recordings: Set<Long>) {
        val recordingsString = recordings.joinToString(",")
        securePrefs.edit()
            .putString(KEY_PROTECTED_RECORDINGS, recordingsString)
            .apply()
    }
    
    private fun hashPIN(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

// Authentication states
sealed class AuthenticationState {
    object Unauthenticated : AuthenticationState()
    object Authenticated : AuthenticationState()
    object BiometricRequired : AuthenticationState()
    object PINRequired : AuthenticationState()
}

// Security settings data class
data class SecuritySettings(
    val isEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val protectedRecordingsCount: Int = 0
)
