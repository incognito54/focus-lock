package com.incognito54.focuslock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class FocusService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    private val checker = object : Runnable {
        override fun run() {

            val usageStatsManager =
                getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager

            val endTime = System.currentTimeMillis()
            val startTime = endTime - 5000

            val usageEvents =
                usageStatsManager.queryEvents(startTime, endTime)

            val event = UsageEvents.Event()
            var currentApp: String? = null

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)

                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    currentApp = event.packageName
                }
            }

            if (currentApp != null && currentApp != packageName) {
                Log.d(
                    "FocusService",
                    "User left Focus Lock: $currentApp"
                )
            }

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            "focus_lock_channel",
            "Focus Lock",
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationManager =
            getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(channel)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val notification = Notification.Builder(
            this,
            "focus_lock_channel"
        )
            .setContentTitle("Focus Lock is active")
            .setContentText("Your focus session is running.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()

        startForeground(1, notification)

        handler.removeCallbacks(checker)
        handler.post(checker)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        handler.removeCallbacks(checker)
        super.onDestroy()
    }
}