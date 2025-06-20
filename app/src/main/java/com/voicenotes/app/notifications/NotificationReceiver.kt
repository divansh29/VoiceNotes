package com.voicenotes.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_DAILY_REMINDER = "com.voicenotes.app.DAILY_REMINDER"
        const val ACTION_ACTION_ITEM_REMINDER = "com.voicenotes.app.ACTION_ITEM_REMINDER"
        const val EXTRA_ACTION_ITEM = "extra_action_item"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "Received intent: ${intent.action}")
        
        when (intent.action) {
            ACTION_DAILY_REMINDER -> {
                val notificationService = NotificationService()
                notificationService.showDailyReminder(context)
            }
            
            ACTION_ACTION_ITEM_REMINDER -> {
                val actionItem = intent.getStringExtra(EXTRA_ACTION_ITEM) ?: "Complete your task"
                val notificationService = NotificationService()
                notificationService.showActionItemReminder(context, actionItem)
            }
            
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule notifications after device reboot
                rescheduleNotifications(context)
            }
        }
    }
    
    private fun rescheduleNotifications(context: Context) {
        // Get user preferences for reminder time
        val sharedPrefs = context.getSharedPreferences("voice_notes_prefs", Context.MODE_PRIVATE)
        val reminderEnabled = sharedPrefs.getBoolean("daily_reminder_enabled", true)
        val reminderHour = sharedPrefs.getInt("reminder_hour", 9)
        val reminderMinute = sharedPrefs.getInt("reminder_minute", 0)
        
        if (reminderEnabled) {
            NotificationService.scheduleDailyReminder(context, reminderHour, reminderMinute)
        }
    }
}
