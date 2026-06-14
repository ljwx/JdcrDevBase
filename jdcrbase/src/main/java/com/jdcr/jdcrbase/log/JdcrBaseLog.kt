package com.jdcr.jdcrbase.log

data class JdcrLogData(
    val logTag: String,
    val message: String,
    val level: Long,
    val timestamp: Long,
    val throwable: Throwable? = null,
    val tagSplit: Triple<String?, String?, String?>?,
    val versionCode: Long,
    val versionName: String,
    val extraData: String? = null
)