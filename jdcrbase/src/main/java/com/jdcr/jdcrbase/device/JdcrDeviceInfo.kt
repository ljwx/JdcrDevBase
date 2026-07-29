package com.jdcr.jdcrbase.device

import android.os.Build
import android.provider.Settings
import com.jdcr.jdcrbase.app.JdcrAppUtils

data class JdcrDeviceInfo(
    val Manufacturer: String = Build.MANUFACTURER,
    val Brand: String = Build.BRAND,
    val Model: String = Build.MODEL,
    val OSVersion: String = Build.VERSION.RELEASE,
    val APIVersion: Int = Build.VERSION.SDK_INT,
    val ABI: String = Build.SUPPORTED_ABIS.joinToString(),
    val RAMTotal: Float = JdcrDeviceUtils.totalRAM
) {

    companion object {

        private val context by lazy { JdcrAppUtils.getAppContext() }

        val androidId by lazy {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }

    }

    override fun toString(): String {
        return "JdcrDeviceInfo(厂商='$Manufacturer',品牌='$Brand',机型='$Model',CPU架构='$ABI',系统='$OSVersion',API版本='$APIVersion',RAM='${RAMTotal}GB')"
    }
}