package com.jdcr.jdcrbase.app

import android.util.Log

fun Throwable?.toStackTraceText(): String? {
    if (this == null) return null
    return Log.getStackTraceString(this)
}