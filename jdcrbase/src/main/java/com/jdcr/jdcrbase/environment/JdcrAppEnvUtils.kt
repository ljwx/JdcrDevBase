package com.jdcr.jdcrbase.environment

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class JdcrAppEnv(
    open val type: String,
    open val host: String,
    open val secondaryHost: String,
    open val webHost: String
) {

    data class Release(
        override val host: String,
        override val secondaryHost: String,
        override val webHost: String
    ) : JdcrAppEnv("Release", host, secondaryHost, webHost)

    sealed class Internal(
        override val type: String,
        override val host: String,
        override val secondaryHost: String,
        override val webHost: String
    ) : JdcrAppEnv(type, host, secondaryHost, webHost) {

        data class Dev(
            override val host: String,
            override val secondaryHost: String,
            override val webHost: String
        ) :
            Internal("Dev", host, secondaryHost, webHost)

        data class Test(
            override val host: String,
            override val secondaryHost: String,
            override val webHost: String
        ) :
            Internal("Test", host, secondaryHost, webHost)

        data class Staging(
            override val host: String,
            override val secondaryHost: String,
            override val webHost: String
        ) :
            Internal("Staging", host, secondaryHost, webHost)
    }

}

object JdcrAppEnvUtils {

    private val _env = MutableStateFlow<JdcrAppEnv>(JdcrAppEnv.Internal.Dev("", "", ""))

    val env: StateFlow<JdcrAppEnv> = _env

    fun changeEnv(env: JdcrAppEnv) {
        _env.value = env
    }

    fun getCurrent(): JdcrAppEnv {
        return _env.value
    }

}