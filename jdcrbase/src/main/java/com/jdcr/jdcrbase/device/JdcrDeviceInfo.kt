package com.jdcr.jdcrbase.device

import android.adservices.appsetid.AppSetId
import android.adservices.appsetid.AppSetIdManager
import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.OutcomeReceiver
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.jdcr.jdcrbase.app.JdcrAppUtils
import com.jdcr.jdcrbase.datastore.JdcrDataStore
import com.jdcr.jdcrbase.log.JdcrDevBaseLog
import com.jdcr.jdcrbase.number.to2Decimal
import java.util.UUID
import kotlin.math.roundToInt

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

        suspend fun getAppUniqueIdCache(): String {
            var uniqueId = JdcrDataStore.getString("unique_id_cache")
            if (uniqueId == null) {
                uniqueId = UUID.randomUUID().toString()
                JdcrDataStore.putString("unique_id_cache", uniqueId)
            }
            return uniqueId
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) // API 34
        suspend fun getAppSetIdNative() {
            val manager = AppSetIdManager.get(context)
            val executor =
                ContextCompat.getMainExecutor(context) // 或者使用 ContextCompat.getMainExecutor(context)
            manager.getAppSetId(executor, object : OutcomeReceiver<AppSetId, Exception> {
                override fun onResult(result: AppSetId) {
                    val id = result.id
                    val scope = result.scope
                    JdcrDevBaseLog.d("AppSetId: $id, Scope: $scope")
                }

                override fun onError(error: Exception) {
                    error.printStackTrace()
                }
            })
        }

    }

    override fun toString(): String {
        return "JdcrDeviceInfo(厂商='$Manufacturer',品牌='$Brand',机型='$Model',CPU架构='$ABI',系统='$OSVersion',API版本='$APIVersion',RAM='${RAMTotal}GB')"
    }
}

object JdcrDeviceUtils {

    private val context by lazy { JdcrAppUtils.getAppContext() }

    private val gbSize = 1024 * 1024 * 1024f
    val totalRAM by lazy { (getRAMInfo().totalMem / gbSize).to2Decimal() }

    fun getRAMInfo(): ActivityManager.MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        JdcrDevBaseLog.d("RAM(${(info.availMem / gbSize).to2Decimal()}GB可用/${(info.totalMem / gbSize).to2Decimal()}GB) 低内存:${info.lowMemory}")
        return info
    }

    private fun getStorageInfoLegacy(): Pair<Long, Long> {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
        val freeBytes = statFs.availableBlocksLong * statFs.blockSizeLong
        return freeBytes to totalBytes
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getStorageInfoApi26(): Pair<Long, Long> {
        val storageStatsManager =
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        // 主存储卷（一般就是手机内置存储）
        val totalBytes = storageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
        val freeBytes = storageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
        return freeBytes to totalBytes
    }

    fun getStorageInfo(): Pair<Long, Long> {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getStorageInfoApi26()
        } else {
            getStorageInfoLegacy()
        }
        JdcrDevBaseLog.d("ROM(${(result.first / gbSize).to2Decimal()}GB可用/${(result.second / gbSize).to2Decimal()}GB)")
        return result
    }

    fun getSystemName(): String? {
        val romName = JdcrCustomSystemInfo.getCustomRomName() ?: return null
        val romVersion = JdcrCustomSystemInfo.getCustomRomVersion()
        return "$romName $romVersion"
    }


}