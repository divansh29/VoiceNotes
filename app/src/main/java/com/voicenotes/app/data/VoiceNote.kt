package com.voicenotes.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "voice_notes")
data class VoiceNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val duration: Long, // in milliseconds
    val fileSize: Long, // in bytes
    val createdAt: Date,
    val transcript: String? = null,
    val summary: String? = null,
    val keyPoints: List<String> = emptyList(),
    val isProcessing: Boolean = false
)
