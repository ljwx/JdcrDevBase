package com.jdcr.jdcrbase.log

import android.util.Log

internal object JdcrDevBaseLog {

    internal const val BASE_TAG = "jdcr_base"
    internal const val DS = BASE_TAG + "_ds"

    fun v(message: String) {
        Log.v(BASE_TAG, message)
    }

    fun d(message: String, throwable: Throwable? = null) {
        Log.d(BASE_TAG, message, throwable)
    }

    fun i(message: String, throwable: Throwable? = null) {
        Log.i(BASE_TAG, message, throwable)
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(BASE_TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable?) {
        Log.e(BASE_TAG, message, throwable)
    }

    fun wDS(message: String, throwable: Throwable? = null) {
        Log.w(DS, message, throwable)
    }

    fun eDS(message: String, throwable: Throwable?) {
        Log.e(DS, message, throwable)
    }

}