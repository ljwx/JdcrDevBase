package com.jdcr.jdcrbase.app

import android.os.Process
import com.jdcr.jdcrbase.device.JdcrDeviceInfo
import com.jdcr.jdcrbase.log.JdcrDevBaseLog
import com.jdcr.jdcrbase.time.TimeUtils
import java.io.File
import java.io.FileOutputStream
import kotlin.system.exitProcess

@Volatile
private var register = false

internal fun setupCrashHandler() {
    if (register) return
    register = true
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            val appMessage =
                "App版本: ${JdcrAppUtils.versionName}/${JdcrAppUtils.versionCode}。进/线程: ${JdcrAppUtils.getProcessName()}/${thread.name}"
            val deviceMessage = JdcrDeviceInfo().toString()
            val crashInfo = buildString {
                appendLine(appMessage)
                appendLine(deviceMessage)
                appendLine(truncate(throwable.stackTraceToString(), 8191))
            }
            saveCrashToFile(crashInfo)
            JdcrDevBaseLog.e("App发生了崩溃,$appMessage \n$deviceMessage", throwable)
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            if (defaultHandler == null) {
                JdcrDevBaseLog.w("杀掉App")
                Process.killProcess(Process.myPid())
                exitProcess(10)
            } else {
                JdcrDevBaseLog.w("交给其他处理者")
                defaultHandler.uncaughtException(thread, throwable)
            }
        }
    }
}

private fun truncate(text: String, maxLength: Int): String {
    if (text.length <= maxLength) return text
    return text.take(maxLength) + "\n... [stack trace truncated]"
}

private fun saveCrashToFile(content: String) {
    try {
        val dir = File(JdcrAppUtils.getAppContext().cacheDir, "crash/app/jdcr")
        dir.mkdirs()
        val time = TimeUtils.format(System.currentTimeMillis(), "yy-MM-dd_HH_mm_ss_SSS")
        val file = File(dir, "${time}.txt")
        FileOutputStream(file).use { fos ->
            fos.write(content.toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.fd.sync() // 尽量强制刷盘
        }
    } catch (e: Exception) {
        JdcrDevBaseLog.e("保存崩溃日志时发生异常", e)
    }
}