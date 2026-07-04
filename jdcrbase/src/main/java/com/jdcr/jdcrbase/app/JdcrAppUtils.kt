package com.jdcr.jdcrbase.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.jdcr.jdcrbase.log.JdcrDevBaseLog
import com.jdcr.jdcrbase.page.JdcrActivityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object JdcrAppUtils {

    private lateinit var applicationContext: Context

    val isAppDebug by lazy { (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }

    val appName by lazy {
        try {
            val packageManager = applicationContext.packageManager
            val appInfo = applicationContext.applicationInfo
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            JdcrDevBaseLog.w("获取AppName异常:", e)
            ""
        }
    }

    val versionName: String by lazy {
        try {
            getPackageInfo()?.versionName ?: ""
        } catch (e: Exception) {
            JdcrDevBaseLog.w("获取VersionName异常:", e)
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

    internal fun onApplicationCreate(application: Application) {
        if (!isMainProcess(application)) return
        JdcrDevBaseLog.i("主进程基础功能初始化")
        applicationContext = application.applicationContext
        initAppLifecycle()
        JdcrActivityUtils.init(application)
        setupCrashHandler()
    }

    fun getAppContext(): Context = applicationContext

    fun isMainProcess(context: Context): Boolean {
        val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            val pid = android.os.Process.myPid()
            val am =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.runningAppProcesses
                ?.firstOrNull { it.pid == pid }
                ?.processName
        }
        return processName == context.packageName
    }

    private fun getPackageInfo(): PackageInfo? {
        return try {
            applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
        } catch (e: Exception) {
            JdcrDevBaseLog.w("获取PackageInfo异常:", e)
            null
        }
    }

    private fun initAppLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                JdcrDevBaseLog.i("app进入前台")
                _isForeground.value = true
            }

            override fun onStop(owner: LifecycleOwner) {
                JdcrDevBaseLog.i("app进入后台")
                _isForeground.value = false
            }
        })
    }

    fun restartApp() {
        val intent =
            applicationContext.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            } ?: return
        JdcrDevBaseLog.w("马上重启App")
        applicationContext.startActivity(intent)
        Runtime.getRuntime().exit(0)  // 或 Process.killProcess(Process.myPid())
    }

}