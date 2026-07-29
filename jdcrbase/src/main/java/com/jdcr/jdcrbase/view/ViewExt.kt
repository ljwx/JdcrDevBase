package com.jdcr.jdcrbase.view

import android.os.SystemClock
import android.view.View

fun View.setThrottleClickListener(
    intervalMillis: Long = 300L,
    onClick: (View) -> Unit
) {
    require(intervalMillis >= 0L)

    var lastAcceptedTime: Long? = null

    setOnClickListener { view ->
        val currentTime = SystemClock.elapsedRealtime()
        val lastTime = lastAcceptedTime

        if (lastTime != null &&
            currentTime - lastTime < intervalMillis
        ) {
            return@setOnClickListener
        }

        lastAcceptedTime = currentTime
        onClick(view)
    }
}