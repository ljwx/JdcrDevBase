package com.jdcr.jdcrbase.log

data class JdcrLogData(
    val tag: String?,
    val message: String,
    val level: Int,
    val timestamp: Long,
    val throwable: Throwable?,
    val tagSplit: Triple<String?, String?, String?>?,
    val versionCode: Long,
    val versionName: String
)

interface IDevBaseLog {

    fun enable(debugMode: Boolean)

    fun v(msg: String?)

    fun vF(feature: String, msg: String?)

    fun vT(tag: String, msg: String?)

    fun d(msg: String?, t: Throwable? = null)

    fun dF(feature: String, msg: String?, t: Throwable? = null)

    fun dT(tag: String, msg: String?, t: Throwable? = null)

    fun i(msg: String?, t: Throwable? = null)

    fun iF(feature: String, msg: String?, t: Throwable? = null)

    fun iT(tag: String, msg: String?, t: Throwable? = null)

    fun w(msg: String?, t: Throwable? = null)

    fun wF(feature: String, msg: String?, t: Throwable? = null)

    fun wT(tag: String, msg: String?, t: Throwable? = null)

    fun e(msg: String?, t: Throwable?)

    fun eF(feature: String, msg: String?, t: Throwable?)

    fun eT(tag: String, msg: String?, t: Throwable?)
}