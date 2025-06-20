package com.voicenotes.app.calendar

import android.Manifest
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.IBinder
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit

class CalendarService : Service() {
    
    companion object {
        private const val TAG = "CalendarService"
        
        fun scheduleCalendarMonitoring(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<CalendarMonitorWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "calendar_monitor",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
        
        fun checkUpcomingMeetings(context: Context): List<CalendarEvent> {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) 
                != PackageManager.PERMISSION_GRANTED) {
                return emptyList()
            }
            
            val events = mutableListOf<CalendarEvent>()
            val contentResolver = context.contentResolver
            
            // Query for events in the next 2 hours
            val now = System.currentTimeMillis()
            val twoHoursLater = now + (2 * 60 * 60 * 1000)
            
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.EVENT_LOCATION
            )
            
            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
            val selectionArgs = arrayOf(now.toString(), twoHoursLater.toString())
            
            try {
                val cursor: Cursor? = contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${CalendarContract.Events.DTSTART} ASC"
                )
                
                cursor?.use {
                    while (it.moveToNext()) {
                        val id = it.getLong(0)
                        val title = it.getString(1) ?: "Untitled Event"
                        val startTime = it.getLong(2)
                        val endTime = it.getLong(3)
                        val description = it.getString(4) ?: ""
                        val location = it.getString(5) ?: ""
                        
                        // Only include events that look like meetings
                        if (isMeetingEvent(title, description)) {
                            events.add(
                                CalendarEvent(
                                    id = id,
                                    title = title,
                                    startTime = startTime,
                                    endTime = endTime,
                                    description = description,
                                    location = location
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying calendar events", e)
            }
            
            return events
        }
        
        private fun isMeetingEvent(title: String, description: String): Boolean {
            val meetingKeywords = listOf(
                "meeting", "call", "conference", "discussion", "review", 
                "standup", "sync", "interview", "presentation", "demo"
            )
            
            val titleLower = title.lowercase()
            val descriptionLower = description.lowercase()
            
            return meetingKeywords.any { keyword ->
                titleLower.contains(keyword) || descriptionLower.contains(keyword)
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // This service can be used for real-time calendar monitoring if needed
        return START_STICKY
    }
}

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val description: String,
    val location: String
)

class CalendarMonitorWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    override fun doWork(): Result {
        return try {
            val upcomingMeetings = CalendarService.checkUpcomingMeetings(applicationContext)
            
            upcomingMeetings.forEach { meeting ->
                // Check if we should auto-record this meeting
                val sharedPrefs = applicationContext.getSharedPreferences("voice_notes_prefs", Context.MODE_PRIVATE)
                val autoRecordEnabled = sharedPrefs.getBoolean("auto_record_meetings", false)
                
                if (autoRecordEnabled) {
                    scheduleAutoRecording(meeting)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("CalendarMonitorWorker", "Error monitoring calendar", e)
            Result.retry()
        }
    }
    
    private fun scheduleAutoRecording(meeting: CalendarEvent) {
        // Schedule a work request to start recording at meeting time
        val delay = meeting.startTime - System.currentTimeMillis()
        
        if (delay > 0) {
            val recordingWorkRequest = OneTimeWorkRequestBuilder<AutoRecordingWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        "meeting_title" to meeting.title,
                        "meeting_id" to meeting.id,
                        "meeting_duration" to (meeting.endTime - meeting.startTime)
                    )
                )
                .build()
            
            WorkManager.getInstance(applicationContext).enqueue(recordingWorkRequest)
            
            Log.d("CalendarMonitorWorker", "Scheduled auto-recording for: ${meeting.title}")
        }
    }
}

class AutoRecordingWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    override fun doWork(): Result {
        return try {
            val meetingTitle = inputData.getString("meeting_title") ?: "Meeting"
            val meetingId = inputData.getLong("meeting_id", 0)
            val duration = inputData.getLong("meeting_duration", 3600000) // Default 1 hour
            
            // Send broadcast to start recording
            val intent = Intent("com.voicenotes.app.AUTO_RECORD_MEETING").apply {
                putExtra("meeting_title", meetingTitle)
                putExtra("meeting_id", meetingId)
                putExtra("duration", duration)
            }
            
            applicationContext.sendBroadcast(intent)
            
            Log.d("AutoRecordingWorker", "Triggered auto-recording for: $meetingTitle")
            Result.success()
        } catch (e: Exception) {
            Log.e("AutoRecordingWorker", "Error starting auto-recording", e)
            Result.failure()
        }
    }
}
