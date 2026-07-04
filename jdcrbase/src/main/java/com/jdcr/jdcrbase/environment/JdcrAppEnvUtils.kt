package com.jdcr.jdcrbase.environment

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class JdcrAppEnv(val type: String) {
    DEV("Dev"),
    TEST("Test"),
    STAGING("Staging"),
    RELEASE("Release"),
}

object JdcrAppEnvUtils {

    private val _env = MutableStateFlow(JdcrAppEnv.RELEASE)

    val env: StateFlow<JdcrAppEnv> = _env

    fun changeEnv(env: JdcrAppEnv) {
        _env.value = env
    }

    fun getEnv(): JdcrAppEnv {
        return _env.value
    }

}