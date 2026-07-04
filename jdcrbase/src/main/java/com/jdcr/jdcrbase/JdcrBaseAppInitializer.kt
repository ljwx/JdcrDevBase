package com.jdcr.jdcrbase

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.jdcr.jdcrbase.app.JdcrAppUtils
import com.jdcr.jdcrbase.log.JdcrDevBaseLog

class JdcrBaseAppInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        JdcrDevBaseLog.i("App启动器启动")
        val app = context.applicationContext as Application
        JdcrAppUtils.onApplicationCreate(app)
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return emptyList()
    }
}