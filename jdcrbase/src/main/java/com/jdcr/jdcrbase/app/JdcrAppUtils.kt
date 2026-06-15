package com.jdcr.jdcrbase.app

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val BASE_TAG = "jdcr_base"

object JdcrAppUtils {

    private lateinit var applicationContext: Context

    val isAppDebug by lazy { (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }

    val appName by lazy {
        try {
            val packageManager = applicationContext.packageManager
            val appInfo = applicationContext.applicationInfo
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Log.w(BASE_TAG, "获取AppName异常:", e)
            ""
        }
    }

    val versionName: String by lazy {
        try {
            getPackageInfo()?.versionName ?: ""
        } catch (e: Exception) {
            Log.w(BASE_TAG, "获取VersionName异常:", e)
            ""
        }
    }

    val versionCode: Long by lazy {
        try {
            val packageInfo = getPackageInfo()
            if (packageInfo != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
            } else {
                0L
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    val packageName: String by lazy { applicationContext.packageName }

    private val _isForeground = MutableStateFlow(false)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    internal fun setApplicationContext(context: Context) {
        Log.i(BASE_TAG, "设置ApplicationContext")
        applicationContext = context.applicationContext
        initAppLifecycle()
    }

    fun getAppContext(): Context = applicationContext

    private fun getPackageInfo(): PackageInfo? {
        return try {
            applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
        } catch (e: Exception) {
            Log.w(BASE_TAG, "获取PackageInfo异常:", e)
            null
        }
    }

    private fun initAppLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                Log.i(BASE_TAG, "app进入前台")
                _isForeground.value = true
            }

            override fun onStop(owner: LifecycleOwner) {
                Log.i(BASE_TAG, "app进入后台")
                _isForeground.value = false
            }
        })
    }

}