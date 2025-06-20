package com.voicenotes.app.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

@Database(
    entities = [VoiceNote::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VoiceNoteDatabase : RoomDatabase() {
    
    abstract fun voiceNoteDao(): VoiceNoteDao
    
    companion object {
        @Volatile
        private var INSTANCE: VoiceNoteDatabase? = null
        
        fun getDatabase(context: Context): VoiceNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceNoteDatabase::class.java,
                    "voice_note_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
