
package com.incognito54.focuslock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Phone has finished restarting.
            // We'll add the Focus Lock restart logic here next.
        }
    }
}
