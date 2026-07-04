package com.jdcr.jdcrbase.environment

import com.jdcr.jdcrbase.coroutine.JdcrSafeCoroutineScope
import com.jdcr.jdcrbase.datastore.JdcrDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val RELEASE_TYPE = "Release"

interface EnvHostData {
    open val host: String
    open val secondaryHost: String
    open val tertiaryHost: String
}

internal class PlaceholderDev : EnvHostData {
    override val host: String = ""
    override val secondaryHost: String = ""
    override val tertiaryHost: String = ""
}

sealed class JdcrAppEnv(
    open val type: String,
    open val host: EnvHostData,
    open val webHost: EnvHostData?
) {

    data class Release(
        override val host: EnvHostData,
        override val webHost: EnvHostData?
    ) : JdcrAppEnv(RELEASE_TYPE, host, webHost)

    sealed class Internal(
        override val type: String,
        override val host: EnvHostData,
        override val webHost: EnvHostData?
    ) : JdcrAppEnv(type, host, webHost) {

        data class Dev(
            override val host: EnvHostData,
            override val webHost: EnvHostData?
        ) :
            Internal("Dev", host, webHost)

        data class Test(
            override val host: EnvHostData,
            override val webHost: EnvHostData?
        ) :
            Internal("Test", host, webHost)

        data class Staging(
            override val host: EnvHostData,
            override val webHost: EnvHostData?
        ) :
            Internal("Staging", host, webHost)
    }

}

object JdcrAppEnvUtils {

    private val coroutine = JdcrSafeCoroutineScope()
    private val mutex = Mutex()
    private val envKey = "jdcr_app_env"

    private val _env =
        MutableStateFlow<JdcrAppEnv>(JdcrAppEnv.Internal.Dev(PlaceholderDev(), null))

    val env: StateFlow<JdcrAppEnv> = _env

    fun changeEnv(env: JdcrAppEnv) {
        _env.value = env
        coroutine.launch {
            mutex.withLock {
                JdcrDataStore.putString(envKey, env.type)
            }
        }
    }

    fun getCurrent(): JdcrAppEnv {
        return _env.value
    }

    fun getEnvCache(): String {
        return JdcrDataStore.getStringSync(envKey, RELEASE_TYPE) ?: RELEASE_TYPE
    }

}