package com.incognito54.focuslock

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView

class BlockActivity : Activity() {

    private lateinit var countdownText: TextView

    private val handler = Handler(Looper.getMainLooper())

    private val countdownRunnable = object : Runnable {

        override fun run() {

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

            val remainingMillis =
                endTime - System.currentTimeMillis()

            if (remainingMillis <= 0) {
                finish()
                return
            }

            val totalSeconds =
                remainingMillis / 1000

            val days =
                totalSeconds / (24 * 60 * 60)

            val hours =
                (totalSeconds % (24 * 60 * 60)) / (60 * 60)

            val minutes =
                (totalSeconds % (60 * 60)) / 60

            val seconds =
                totalSeconds % 60

            countdownText.text =
                when {
                    days > 0 ->
                        String.format(
                            "%dd %02dh %02dm",
                            days,
                            hours,
                            minutes
                        )

                    hours > 0 ->
                        String.format(
                            "%dh %02dm %02ds",
                            hours,
                            minutes,
                            seconds
                        )

                    else ->
                        String.format(
                            "%02d:%02d",
                            minutes,
                            seconds
                        )
                }

            handler.postDelayed(
                this,
                1000
            )
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_block
        )

        countdownText =
            findViewById(
                R.id.countdownText
            )

        val blockMessage =
            findViewById<TextView>(
                R.id.blockMessage
            )

        blockMessage.text =
            "This app is blocked while your focus session is active."

        handler.post(
            countdownRunnable
        )
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            countdownRunnable
        )

        super.onDestroy()
    }
}