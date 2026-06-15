package com.jdcr.jdcrbase

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.jdcr.jdcrbase.app.BASE_TAG
import com.jdcr.jdcrbase.app.JdcrAppUtils

class JdcrBaseAppInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        Log.i(BASE_TAG, "启动初始化")
        val app = context.applicationContext as Application
        JdcrAppUtils.onApplicationCreate(app)
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return emptyList()
    }
}