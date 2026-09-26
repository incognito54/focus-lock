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

        Log.d(
            "FocusAccessibility",
            "Accessibility Service connected"
        )
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {

        if (
            event?.eventType ==
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {

            val currentApp =
                event.packageName?.toString()

            if (
                currentApp != null &&
                currentApp != packageName
            ) {

                val preferences =
                    getSharedPreferences(
                        "focus_lock",
                        MODE_PRIVATE
                    )

                val endTime =
                    preferences.getLong(
                        "end_time",
                        0L
                    )

                val currentTime =
                    System.currentTimeMillis()

                val focusActive =
                    endTime > currentTime

                /*
                 * DEVICE ADMIN PROTECTION
                 *
                 * This is always checked while
                 * a Focus Session is active.
                 */
                val isDeviceAdminSettings =
                    currentApp == "com.android.settings" &&
                            event.className?.toString() ==
                            "com.android.settings.SubSettings" &&
                            event.text.any {
                                it.toString().contains(
                                    "Device admin apps",
                                    ignoreCase = true
                                )
                            }
                val isAccessibilitySettings =
                    currentApp == "com.android.settings" &&
                            event.className?.toString() ==
                            "com.android.settings.SubSettings" &&
                            event.text.any {
                                it.toString().contains(
                                    "Accessibility",
                                    ignoreCase = true
                                )
                            }
                if (focusActive) {

                    /*
                     * DEVICE ADMIN
                     */
                    if (isDeviceAdminSettings) {

                        Log.d(
                            "FocusAccessibility",
                            "Device Admin settings blocked during Focus Session"
                        )

                        val intent =
                            Intent(
                                this,
                                BlockActivity::class.java
                            )

                        intent.putExtra(
                            "block_reason",
                            "DEVICE_ADMIN"
                        )

                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )

                        startActivity(intent)

                        return
                    }
                    if (isAccessibilitySettings) {

                        Log.d(
                            "FocusAccessibility",
                            "Accessibility settings blocked during Focus Session"
                        )

                        val intent =
                            Intent(
                                this,
                                BlockActivity::class.java
                            )

                        intent.putExtra(
                            "block_reason",
                            "ACCESSIBILITY"
                        )

                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )

                        startActivity(intent)

                        return
                    }
                    /*
                     * SELECTED APP PROTECTION
                     *
                     * No Emergency Access.
                     *
                     * If the app is selected,
                     * it stays blocked until
                     * the Focus Session ends.
                     */
                    Log.d(
                        "FocusAccessibility",
                        "Focus Lock active. User opened: $currentApp"
                    )

                    val selectedApps =
                        preferences
                            .getStringSet(
                                "selected_apps",
                                emptySet()
                            )
                            ?: emptySet()

                    if (
                        selectedApps.contains(currentApp)
                    ) {

                        val intent =
                            Intent(
                                this,
                                BlockActivity::class.java
                            )

                        intent.putExtra(
                            "block_reason",
                            "SELECTED_APP"
                        )

                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )

                        startActivity(intent)
                    }

                } else {

                    Log.d(
                        "FocusAccessibility",
                        "No active Focus Lock. User opened: $currentApp"
                    )
                }
            }
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