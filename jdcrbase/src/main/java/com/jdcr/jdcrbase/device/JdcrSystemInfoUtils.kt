package com.jdcr.jdcrbase.device

import android.os.Build

object JdcrSystemInfoUtils {

    fun getRomNameTry(): String? {
        return when {
            !getSystemProperty("ro.mi.os.version.name").isNullOrBlank() -> "HyperOS"
            !getSystemProperty("ro.miui.ui.version.name").isNullOrBlank() -> "MIUI"
            !getSystemProperty("ro.build.version.opporom").isNullOrBlank() -> "ColorOS"
            !getSystemProperty("ro.build.version.oplusrom").isNullOrBlank() -> "ColorOS"
            !getSystemProperty("ro.build.version.oxygen").isNullOrBlank() -> "OxygenOS"
            !getSystemProperty("ro.vivo.os.name").isNullOrBlank() -> getSystemProperty("ro.vivo.os.name")
            !getSystemProperty("ro.build.version.emui").isNullOrBlank() -> "EMUI"
            !getSystemProperty("ro.build.version.magic").isNullOrBlank() -> "MagicOS"
            isSamsungOneUi() -> "One UI"
            else -> null
        }
    }

    fun getRomVersionTry(): String? {
        return when {
            !getSystemProperty("ro.mi.os.version.name").isNullOrBlank() ->
                getSystemProperty("ro.mi.os.version.name")

            !getSystemProperty("ro.miui.ui.version.name").isNullOrBlank() ->
                getSystemProperty("ro.miui.ui.version.name")

            !getSystemProperty("ro.build.version.opporom").isNullOrBlank() ->
                getSystemProperty("ro.build.version.opporom")

            !getSystemProperty("ro.build.version.oplusrom").isNullOrBlank() ->
                getSystemProperty("ro.build.version.oplusrom")

            !getSystemProperty("ro.build.version.oxygen").isNullOrBlank() ->
                getSystemProperty("ro.build.version.oxygen")

            !getSystemProperty("ro.vivo.os.version").isNullOrBlank() ->
                getSystemProperty("ro.vivo.os.version")

            !getSystemProperty("ro.build.version.emui").isNullOrBlank() ->
                getSystemProperty("ro.build.version.emui")

            !getSystemProperty("ro.build.version.magic").isNullOrBlank() ->
                getSystemProperty("ro.build.version.magic")

            isSamsungOneUi() -> parseOneUiVersion(Build.DISPLAY)
            else -> null
        }
    }

    private fun isSamsungOneUi(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }

    private fun parseOneUiVersion(display: String): String? {
        // 例如 OneUI7.0 之类，不同机型格式不完全一致，只能尽力解析
        val regex = Regex("OneUI(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
        return regex.find(display)?.groupValues?.get(1)
    }

    @Suppress("PrivateApi")
    private fun getSystemProperty(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java, String::class.java)
            val value = method.invoke(null, key, "") as String
            value.trim().takeIf { it.isNotEmpty() }
        } catch (_: Throwable) {
            null
        }
    }

}