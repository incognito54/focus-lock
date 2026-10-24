package com.incognito54.focuslock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("FocusAccessibility", "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        Log.d(
            "FocusAccessibility",
            "SCREEN: package=${event.packageName}, " +
                    "class=${event.className}, " +
                    "text=${event.text}"
        )

        val currentApp = event.packageName?.toString() ?: return

        // Never block Focus Lock itself.
        if (currentApp == packageName) return

        val preferences = getSharedPreferences(
            "focus_lock",
            MODE_PRIVATE
        )

        val endTime = preferences.getLong("end_time", 0L)
        val focusActive = endTime > System.currentTimeMillis()

        if (!focusActive) {
            Log.d(
                "FocusAccessibility",
                "No active Focus Lock. User opened: $currentApp"
            )
            return
        }

        // Detect protected Settings screens.
        val settingsClass =
            event.className?.toString() == "com.android.settings.SubSettings"

        val isDeviceAdminSettings =
            currentApp == "com.android.settings" &&
                    settingsClass &&
                    event.text.any {
                        it.toString().contains(
                            "Device admin apps",
                            ignoreCase = true
                        )
                    }

        val isAccessibilitySettings =
            currentApp == "com.android.settings" &&
                    settingsClass &&
                    event.text.any {
                        it.toString().contains(
                            "Accessibility",
                            ignoreCase = true
                        )
                    }

        val isFocusLockStorageSettings =
            currentApp == "com.android.settings" &&
                    settingsClass &&
                    event.text.any {
                        it.toString().contains(
                            "Storage",
                            ignoreCase = true
                        )
                    }

        val blockReason = when {
            isFocusLockStorageSettings -> "STORAGE"
            isDeviceAdminSettings -> "DEVICE_ADMIN"
            isAccessibilitySettings -> "ACCESSIBILITY"
            else -> null
        }

        // Keep the existing Settings protections.
        if (blockReason != null) {
            Log.d(
                "FocusAccessibility",
                "$blockReason settings blocked during Focus Session"
            )

            val intent = Intent(this, BlockActivity::class.java).apply {
                putExtra("block_reason", blockReason)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            startActivity(intent)
            return
        }

        // Apps selected manually by the user.
        val selectedApps = preferences.getStringSet(
            "selected_apps",
            emptySet()
        )?.toSet() ?: emptySet()

        // Apps installed during an active Strong Session.
        val autoBlockedApps = preferences.getStringSet(
            "auto_blocked_apps",
            emptySet()
        )?.toSet() ?: emptySet()

        val strongSession = preferences.getBoolean(
            "strong_session",
            false
        )

        val manuallySelected = selectedApps.contains(currentApp)

        val automaticallyBlocked =
            strongSession && autoBlockedApps.contains(currentApp)

        if (manuallySelected || automaticallyBlocked) {
            val reason = if (automaticallyBlocked && !manuallySelected) {
                "STRONG_SESSION_APP"
            } else {
                "SELECTED_APP"
            }

            Log.d(
                "FocusAccessibility",
                "Blocking app: $currentApp, reason=$reason"
            )

            val intent = Intent(this, BlockActivity::class.java).apply {
                putExtra("block_reason", reason)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        Log.d(
            "FocusAccessibility",
            "Accessibility Service interrupted"
        )
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}