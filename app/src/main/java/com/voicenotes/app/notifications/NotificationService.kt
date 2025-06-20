package com.voicenotes.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.voicenotes.app.MainActivity
import com.voicenotes.app.R
import java.util.concurrent.TimeUnit

class NotificationService : Service() {
    
    companion object {
        const val CHANNEL_ID_REMINDERS = "voice_reminders"
        const val CHANNEL_ID_ACTION_ITEMS = "action_items"
        const val NOTIFICATION_ID_DAILY_REMINDER = 1001
        const val NOTIFICATION_ID_ACTION_ITEM = 1002
        
        fun scheduleDailyReminder(context: Context, hour: Int = 9, minute: Int = 0) {
            val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calculateInitialDelay(hour, minute), TimeUnit.MILLISECONDS)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_reminder",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
        
        fun scheduleActionItemReminder(context: Context, actionItem: String, delayHours: Int = 24) {
            val workRequest = OneTimeWorkRequestBuilder<ActionItemReminderWorker>()
                .setInitialDelay(delayHours.toLong(), TimeUnit.HOURS)
                .setInputData(workDataOf("action_item" to actionItem))
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
        }
        
        private fun calculateInitialDelay(hour: Int, minute: Int): Long {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                if (timeInMillis <= now) {
                    add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
            }
            return calendar.timeInMillis - now
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily recording reminders to build habits"
            }
            
            val actionItemChannel = NotificationChannel(
                CHANNEL_ID_ACTION_ITEMS,
                "Action Items",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for action items extracted from recordings"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(actionItemChannel)
        }
    }
    
    fun showDailyReminder(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_mic_24)
            .setContentTitle("Time for your daily voice note!")
            .setContentText("Capture your thoughts and ideas. Build your recording habit.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_mic_24,
                "Record Now",
                pendingIntent
            )
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_DAILY_REMINDER, notification)
    }
    
    fun showActionItemReminder(context: Context, actionItem: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ACTION_ITEMS)
            .setSmallIcon(R.drawable.ic_task_24)
            .setContentTitle("Action Item Reminder")
            .setContentText("Don't forget: $actionItem")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_check_24,
                "Mark Done",
                pendingIntent
            )
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_ACTION_ITEM, notification)
    }
}

// Worker classes for background notifications
class DailyReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val notificationService = NotificationService()
        notificationService.showDailyReminder(applicationContext)
        return Result.success()
    }
}

class ActionItemReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val actionItem = inputData.getString("action_item") ?: "Complete your task"
        val notificationService = NotificationService()
        notificationService.showActionItemReminder(applicationContext, actionItem)
        return Result.success()
    }
}
