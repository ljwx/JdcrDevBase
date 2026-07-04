package com.jdcr.jdcrbase.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.jdcr.jdcrbase.app.JdcrAppUtils
import com.jdcr.jdcrbase.log.JdcrDevBaseLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val SUSPEND_TIMEOUT_MS = 5_000L
private const val SYNC_TIMEOUT_MS = 1_500L

object JdcrDataStore {

    private var storeName = "jdcr_base_store"

    private val dataStoreInstance: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { JdcrAppUtils.getAppContext().preferencesDataStoreFile(storeName) }
        )
    }

    fun initStoreName(name: String) {
        storeName = name
    }

    private fun isInvalidKey(key: String): Boolean {
        if (key.isBlank()) {
            JdcrDevBaseLog.wDS("key不能为空")
            return true
        }
        return false
    }

    private suspend fun dataStore() = withContext(Dispatchers.IO) {
        dataStoreInstance
    }

    private suspend fun <T> runSuspend(
        default: T,
        apiName: String,
        key: String? = null,
        block: suspend () -> T
    ): T {
        return runCatching {
            withTimeout(SUSPEND_TIMEOUT_MS) {
                block()
            }
        }.getOrElse {
            val suffix = key?.let { ", key=$it" } ?: ""
            JdcrDevBaseLog.eDS("$apiName 失败$suffix", it)
            default
        }
    }

    suspend fun putString(key: String, value: String?): Boolean {
        if (isInvalidKey(key)) return false
        return runSuspend(false, "putString", key) {
            val ds = dataStore()
            ds.edit { prefs ->
                val k = stringPreferencesKey(key)
                if (value == null) prefs.remove(k) else prefs[k] = value
            }
            true
        }
    }

    suspend fun putInt(key: String, value: Int): Boolean {
        if (isInvalidKey(key)) return false
        return runSuspend(false, "putInt", key) {
            val ds = dataStore()
            ds.edit { prefs -> prefs[intPreferencesKey(key)] = value }
            true
        }
    }

    suspend fun putLong(key: String, value: Long): Boolean {
        if (isInvalidKey(key)) return false
        return runSuspend(false, "putLong", key) {
            val ds = dataStore()
            ds.edit { prefs -> prefs[longPreferencesKey(key)] = value }
            true
        }
    }

    suspend fun putBoolean(key: String, value: Boolean): Boolean {
        if (isInvalidKey(key)) return false
        return runSuspend(false, "putBoolean", key) {
            val ds = dataStore()
            ds.edit { prefs -> prefs[booleanPreferencesKey(key)] = value }
            true
        }
    }

    suspend fun putFloat(key: String, value: Float): Boolean {
        if (isInvalidKey(key)) return false
        return runSuspend(false, "putFloat", key) {
            val ds = dataStore()
            ds.edit { prefs -> prefs[floatPreferencesKey(key)] = value }
            true
        }
    }

    suspend fun putDouble(key: String, value: Double): Boolean {
        return putString(key, value.toString())
    }

    suspend fun getString(key: String, default: String? = null): String? {
        if (isInvalidKey(key)) return default
        return runSuspend(default, "getString", key) {
            val ds = dataStore()
            ds.data.firstOrNullSafe()?.get(stringPreferencesKey(key)) ?: default
        }
    }

    suspend fun getInt(key: String, default: Int = 0): Int {
        if (isInvalidKey(key)) return default
        return runSuspend(default, "getInt", key) {
            val ds = dataStore()
            ds.data.firstOrNullSafe()?.get(intPreferencesKey(key)) ?: default
        }
    }

    suspend fun getLong(key: String, default: Long = 0L): Long {
        if (isInvalidKey(key)) return default
        return runSuspend(default, "getLong", key) {
            val ds = dataStore()
            ds.data.firstOrNullSafe()?.get(longPreferencesKey(key)) ?: default
        }
    }

    suspend fun getBoolean(key: String, default: Boolean = false): Boolean {
        if (isInvalidKey(key)) return default
        return runSuspend(default, "getBoolean", key) {
            val ds = dataStore()
            ds.data.firstOrNullSafe()?.get(booleanPreferencesKey(key)) ?: default
        }
    }

    suspend fun getFloat(key: String, default: Float = 0f): Float {
        if (isInvalidKey(key)) return default
        return runSuspend(default, "getFloat", key) {
            val ds = dataStore()
            ds.data.firstOrNullSafe()?.get(floatPreferencesKey(key)) ?: default
        }
    }

    suspend fun getDouble(key: String, default: Double = 0.0): Double {
        val raw = getString(key, null) ?: return default
        return raw.toDoubleOrNull() ?: default
    }

    suspend fun remove(key: String): Boolean {
        if (isInvalidKey(key)) return false
        return runSuspend(false, "remove", key) {
            val ds = dataStore()
            ds.edit { prefs ->
                prefs.remove(stringPreferencesKey(key))
                prefs.remove(intPreferencesKey(key))
                prefs.remove(longPreferencesKey(key))
                prefs.remove(booleanPreferencesKey(key))
                prefs.remove(floatPreferencesKey(key))
            }
            true
        }
    }

    suspend fun clear(): Boolean {
        return runSuspend(false, "clear") {
            val ds = dataStore()
            ds.edit { it.clear() }
            true
        }
    }

    suspend fun put(key: String, value: Any): Boolean {
        return when (value) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Boolean -> putBoolean(key, value)
            is Float -> putFloat(key, value)
            is Double -> putDouble(key, value)
            else -> {
                JdcrDevBaseLog.wDS("put暂不支持类型: ${value::class.java.simpleName}")
                false
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> get(key: String, default: T): T {
        return when (default) {
            is String -> getString(key, default) as T
            is Int -> getInt(key, default) as T
            is Long -> getLong(key, default) as T
            is Boolean -> getBoolean(key, default) as T
            is Float -> getFloat(key, default) as T
            is Double -> getDouble(key, default) as T
            else -> {
                JdcrDevBaseLog.wDS("get暂不支持类型: ${default::class.java.simpleName}")
                default
            }
        }
    }

    private fun warnSyncCall(apiName: String) {
//        if (Looper.myLooper() == Looper.getMainLooper()) {
//            Log.w(DATASTORE_TAG, "$apiName 在主线程执行，可能造成卡顿")
//        }
    }

    private fun <T> runSync(default: T, block: suspend () -> T): T {
        return runCatching {
            runBlocking(Dispatchers.IO) {
                withTimeout(SYNC_TIMEOUT_MS) {
                    block()
                }
            }
        }.getOrElse {
            JdcrDevBaseLog.eDS("同步调用失败", it)
            default
        }
    }

    fun putStringSync(key: String, value: String?): Boolean {
        warnSyncCall("putStringSync")
        return runSync(false) { putString(key, value) }
    }

    fun putIntSync(key: String, value: Int): Boolean {
        warnSyncCall("putIntSync")
        return runSync(false) { putInt(key, value) }
    }

    fun putLongSync(key: String, value: Long): Boolean {
        warnSyncCall("putLongSync")
        return runSync(false) { putLong(key, value) }
    }

    fun putBooleanSync(key: String, value: Boolean): Boolean {
        warnSyncCall("putBooleanSync")
        return runSync(false) { putBoolean(key, value) }
    }

    fun putFloatSync(key: String, value: Float): Boolean {
        warnSyncCall("putFloatSync")
        return runSync(false) { putFloat(key, value) }
    }

    fun putDoubleSync(key: String, value: Double): Boolean {
        warnSyncCall("putDoubleSync")
        return runSync(false) { putDouble(key, value) }
    }

    fun getStringSync(key: String, default: String? = null): String? {
        warnSyncCall("getStringSync")
        return runSync(default) { getString(key, default) }
    }

    fun getIntSync(key: String, default: Int = 0): Int {
        warnSyncCall("getIntSync")
        return runSync(default) { getInt(key, default) }
    }

    fun getLongSync(key: String, default: Long = 0L): Long {
        warnSyncCall("getLongSync")
        return runSync(default) { getLong(key, default) }
    }

    fun getBooleanSync(key: String, default: Boolean = false): Boolean {
        warnSyncCall("getBooleanSync")
        return runSync(default) { getBoolean(key, default) }
    }

    fun getFloatSync(key: String, default: Float = 0f): Float {
        warnSyncCall("getFloatSync")
        return runSync(default) { getFloat(key, default) }
    }

    fun getDoubleSync(key: String, default: Double = 0.0): Double {
        warnSyncCall("getDoubleSync")
        return runSync(default) { getDouble(key, default) }
    }

    fun removeSync(key: String): Boolean {
        warnSyncCall("removeSync")
        return runSync(false) { remove(key) }
    }

    fun putSync(key: String, value: Any): Boolean {
        warnSyncCall("putSync")
        return runSync(false) { put(key, value) }
    }

    fun <T : Any> getSync(key: String, default: T): T {
        warnSyncCall("getSync")
        return runSync(default) { get(key, default) }
    }
}

private suspend fun kotlinx.coroutines.flow.Flow<Preferences>.firstOrNullSafe(): Preferences? {
    return runCatching { first() }.getOrNull()
}
