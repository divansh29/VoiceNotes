package com.voicenotes.app.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FileProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "FileProcessor"
    }
    
    /**
     * Copy uploaded file to app's internal storage and return the local file path
     */
    fun processUploadedFile(uri: Uri, fileName: String): String? {
        return try {
            Log.d(TAG, "Processing uploaded file: $fileName")
            
            // Create uploads directory
            val uploadsDir = File(context.filesDir, "uploads")
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs()
            }
            
            // Generate unique filename to avoid conflicts
            val timestamp = System.currentTimeMillis()
            val fileExtension = getFileExtension(fileName)
            val uniqueFileName = "upload_${timestamp}$fileExtension"
            val localFile = File(uploadsDir, uniqueFileName)
            
            // Copy file from URI to local storage
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(localFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            Log.d(TAG, "File copied to: ${localFile.absolutePath}")
            Log.d(TAG, "File size: ${localFile.length()} bytes")
            
            if (localFile.exists() && localFile.length() > 0) {
                localFile.absolutePath
            } else {
                Log.e(TAG, "File copy failed or file is empty")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing uploaded file", e)
            null
        }
    }
    
    /**
     * Get file extension from filename
     */
    private fun getFileExtension(fileName: String): String {
        return if (fileName.contains(".")) {
            "." + fileName.substringAfterLast(".")
        } else {
            ".audio" // Default extension
        }
    }
    
    /**
     * Check if file is a supported audio format
     */
    fun isSupportedAudioFile(fileName: String): Boolean {
        val supportedExtensions = listOf(
            ".mp3", ".m4a", ".wav", ".aac", ".ogg", ".flac", ".3gp", ".amr"
        )
        val extension = getFileExtension(fileName).lowercase()
        return supportedExtensions.contains(extension)
    }
    
    /**
     * Get file size in a human-readable format
     */
    fun getFileSizeString(filePath: String): String {
        return try {
            val file = File(filePath)
            val bytes = file.length()
            
            when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        } catch (e: Exception) {
            "Unknown size"
        }
    }
    
    /**
     * Delete uploaded file from internal storage
     */
    fun deleteUploadedFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists() && file.absolutePath.contains("uploads")) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting uploaded file", e)
            false
        }
    }
    
    /**
     * Get all uploaded files
     */
    fun getUploadedFiles(): List<File> {
        return try {
            val uploadsDir = File(context.filesDir, "uploads")
            if (uploadsDir.exists()) {
                uploadsDir.listFiles()?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting uploaded files", e)
            emptyList()
        }
    }
    
    /**
     * Clean up old uploaded files (older than 30 days)
     */
    fun cleanupOldFiles() {
        try {
            val uploadsDir = File(context.filesDir, "uploads")
            if (uploadsDir.exists()) {
                val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
                
                uploadsDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < thirtyDaysAgo) {
                        file.delete()
                        Log.d(TAG, "Deleted old file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old files", e)
        }
    }
}
