package com.incognito54.focuslock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AppInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return

        // Ignore updates to apps that were already installed.
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        val preferences = context.getSharedPreferences(
            "focus_lock",
            Context.MODE_PRIVATE
        )

        val endTime = preferences.getLong("end_time", 0L)
        val strongSession = preferences.getBoolean("strong_session", false)

        if (!strongSession || endTime <= System.currentTimeMillis()) return

        val apps = preferences.getStringSet(
            "auto_blocked_apps",
            emptySet()
        )?.toMutableSet() ?: mutableSetOf()

        apps.add(packageName)

        preferences.edit()
            .putStringSet("auto_blocked_apps", apps)
            .apply()

        Log.d("AppInstallReceiver", "Added newly installed app: $packageName")
    }
}