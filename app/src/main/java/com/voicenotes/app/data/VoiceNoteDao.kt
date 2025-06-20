package com.voicenotes.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNoteDao {
    
    @Query("SELECT * FROM voice_notes ORDER BY createdAt DESC")
    fun getAllVoiceNotes(): Flow<List<VoiceNote>>
    
    @Query("SELECT * FROM voice_notes WHERE id = :id")
    suspend fun getVoiceNoteById(id: Long): VoiceNote?
    
    @Insert
    suspend fun insertVoiceNote(voiceNote: VoiceNote): Long
    
    @Update
    suspend fun updateVoiceNote(voiceNote: VoiceNote)
    
    @Delete
    suspend fun deleteVoiceNote(voiceNote: VoiceNote)
    
    @Query("DELETE FROM voice_notes WHERE id = :id")
    suspend fun deleteVoiceNoteById(id: Long)
    
    @Query("SELECT COUNT(*) FROM voice_notes")
    suspend fun getVoiceNotesCount(): Int
}
