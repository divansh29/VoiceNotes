package com.voicenotes.app

import com.voicenotes.app.data.VoiceNote
import org.junit.Test
import org.junit.Assert.*
import java.util.Date

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class VoiceNoteUnitTest {
    
    @Test
    fun voiceNote_creation_isCorrect() {
        val voiceNote = VoiceNote(
            id = 1,
            title = "Test Recording",
            filePath = "/path/to/file.m4a",
            duration = 30000, // 30 seconds
            fileSize = 1024, // 1KB
            createdAt = Date(),
            transcript = "This is a test transcript",
            summary = "Test summary",
            keyPoints = listOf("Point 1", "Point 2"),
            isProcessing = false
        )
        
        assertEquals("Test Recording", voiceNote.title)
        assertEquals(30000L, voiceNote.duration)
        assertEquals(2, voiceNote.keyPoints.size)
        assertFalse(voiceNote.isProcessing)
    }
    
    @Test
    fun voiceNote_defaultValues_areCorrect() {
        val voiceNote = VoiceNote(
            title = "Test",
            filePath = "/test/path",
            duration = 1000,
            fileSize = 100,
            createdAt = Date()
        )
        
        assertEquals(0L, voiceNote.id)
        assertNull(voiceNote.transcript)
        assertNull(voiceNote.summary)
        assertTrue(voiceNote.keyPoints.isEmpty())
        assertFalse(voiceNote.isProcessing)
    }
}
