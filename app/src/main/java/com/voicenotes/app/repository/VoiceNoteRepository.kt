package com.voicenotes.app.repository

import com.voicenotes.app.data.VoiceNote
import com.voicenotes.app.data.VoiceNoteDao
import kotlinx.coroutines.flow.Flow

class VoiceNoteRepository(
    private val voiceNoteDao: VoiceNoteDao
) {
    
    fun getAllVoiceNotes(): Flow<List<VoiceNote>> = voiceNoteDao.getAllVoiceNotes()
    
    suspend fun getVoiceNoteById(id: Long): VoiceNote? = voiceNoteDao.getVoiceNoteById(id)
    
    suspend fun insertVoiceNote(voiceNote: VoiceNote): Long = voiceNoteDao.insertVoiceNote(voiceNote)
    
    suspend fun updateVoiceNote(voiceNote: VoiceNote) = voiceNoteDao.updateVoiceNote(voiceNote)
    
    suspend fun deleteVoiceNote(voiceNote: VoiceNote) = voiceNoteDao.deleteVoiceNote(voiceNote)
    
    suspend fun deleteVoiceNoteById(id: Long) = voiceNoteDao.deleteVoiceNoteById(id)
    
    suspend fun getVoiceNotesCount(): Int = voiceNoteDao.getVoiceNotesCount()
}
