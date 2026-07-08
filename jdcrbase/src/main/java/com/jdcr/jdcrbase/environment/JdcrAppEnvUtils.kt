package com.jdcr.jdcrbase.environment

import com.jdcr.jdcrbase.coroutine.JdcrSafeCoroutineScope
import com.jdcr.jdcrbase.datastore.JdcrDataStore
import com.jdcr.jdcrbase.log.JdcrDevBaseLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

interface EnvHostData {
    val first: String
    val second: String
    val third: String
    val fourth: String
        get() = ""
    val fifth: String
        get() = ""
    val sixth: String
        get() = ""
    val seventh: String
        get() = ""
    val eighth: String
        get() = ""
    val ninth: String
        get() = ""
    val tenth: String
        get() = ""
}

sealed class JdcrAppEnv(
    open val type: Type,
    open val host: EnvHostData,
    open val webHost: EnvHostData
) {

    enum class Type(val type: String) {
        Release("Release"),
        Staging("Staging"),
        Test("Test"),
        Dev("Dev"),
    }

    data class Release(
        override val host: EnvHostData,
        override val webHost: EnvHostData
    ) : JdcrAppEnv(Type.Release, host, webHost)

    sealed class Internal(
        override val type: Type,
        override val host: EnvHostData,
        override val webHost: EnvHostData
    ) : JdcrAppEnv(type, host, webHost) {

        data class Dev(
            override val host: EnvHostData,
            override val webHost: EnvHostData
        ) :
            Internal(Type.Dev, host, webHost)

        data class Test(
            override val host: EnvHostData,
            override val webHost: EnvHostData,
            var extension: String? = null,
        ) :
            Internal(Type.Test, host, webHost)

        data class Staging(
            override val host: EnvHostData,
            override val webHost: EnvHostData
        ) :
            Internal(Type.Staging, host, webHost)
    }

}

object JdcrAppEnvUtils {

    private val coroutine = JdcrSafeCoroutineScope()
    private val mutex = Mutex()
    private var changeJob: Job? = null
    private val envKey = "jdcr_app_env"

    private val isInitialized = AtomicBoolean(false)
    private lateinit var releaseInfo: JdcrAppEnv.Release
    private lateinit var stagingInfo: JdcrAppEnv.Internal.Staging

    private lateinit var testInfo: JdcrAppEnv.Internal.Test
    private lateinit var devInfo: JdcrAppEnv.Internal.Dev

    private lateinit var _env: MutableStateFlow<JdcrAppEnv>
    val env: StateFlow<JdcrAppEnv> get() = _env

    fun initInfo(
        release: JdcrAppEnv.Release,
        staging: JdcrAppEnv.Internal.Staging,
        test: JdcrAppEnv.Internal.Test,
        dev: JdcrAppEnv.Internal.Dev,
        initFromCache: Boolean = false
    ) {
        if (!isInitialized.compareAndSet(false, true)) {
            JdcrDevBaseLog.w("App环境已经初始化，忽略重复调用")
            return
        }
        this.releaseInfo = release
        this.stagingInfo = staging
        this.testInfo = test
        this.devInfo = dev

        _env = MutableStateFlow(releaseInfo)

        if (initFromCache) {
            coroutine.launch {
                val cachedType = getTypeCache()
                _env.value = envByType(cachedType)
            }
        }
    }

    private fun envByType(type: JdcrAppEnv.Type): JdcrAppEnv {
        return when (type) {
            JdcrAppEnv.Type.Release -> releaseInfo
            JdcrAppEnv.Type.Staging -> stagingInfo
            JdcrAppEnv.Type.Test -> testInfo
            JdcrAppEnv.Type.Dev -> devInfo
        }
    }

    fun changeEnv(env: JdcrAppEnv.Type, success: ((Boolean) -> Unit)?) {
        JdcrDevBaseLog.iEnv("触发切换环境:${env.type}")
        changeJob?.cancel()
        changeJob = coroutine.launch {
            try {
                val result = mutex.withLock {
                    JdcrDataStore.putString(envKey, env.type)
                }
                if (result) {
                    _env.value = envByType(env)
                    JdcrDevBaseLog.iEnv("切换环境成功:${env.type}")
                } else {
                    JdcrDevBaseLog.iEnv("切换环境失败:${env.type}")
                }
                withContext(Dispatchers.Main) {
                    success?.invoke(result)
                }
            } catch (e: CancellationException) {
                JdcrDevBaseLog.iEnv("切换环境被取消:${env.type}")
            } finally {
                changeJob = null
            }

        }
    }

    fun getCurrent(): JdcrAppEnv {
        return _env.value
    }

    suspend fun getTypeCache(): JdcrAppEnv.Type {
        return when (JdcrDataStore.getString(envKey, JdcrAppEnv.Type.Release.type)) {
            JdcrAppEnv.Type.Staging.type -> JdcrAppEnv.Type.Staging
            JdcrAppEnv.Type.Test.type -> JdcrAppEnv.Type.Test
            JdcrAppEnv.Type.Dev.type -> JdcrAppEnv.Type.Dev
            else -> JdcrAppEnv.Type.Release
        }.also {
            JdcrDevBaseLog.iEnv("缓存里的环境:${it.type}")
        }
    }

    fun isCurrentRelease(): Boolean {
        return _env.value is JdcrAppEnv.Release
    }

    fun destroy() {
        changeJob?.cancel()
        coroutine.destroy()
    }

}