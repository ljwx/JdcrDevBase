package com.jdcr.jdcrbase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context?.toActivity(): Activity? {
    var currentContext = this

    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        val baseContext = currentContext.baseContext
        if (currentContext === baseContext) {
            break
        }
        currentContext = baseContext
    }
    return null
}

fun Activity?.isAlive(): Boolean {
    return this != null && !this.isFinishing && !this.isDestroyed
}
