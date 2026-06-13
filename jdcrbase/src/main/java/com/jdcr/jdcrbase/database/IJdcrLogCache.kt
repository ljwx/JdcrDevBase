package com.jdcr.jdcrbase.database

import com.jdcr.jdcrbase.log.JdcrLogData

interface IJdcrLogCache {

    suspend fun write(log: JdcrLogData): Result<Unit>

    suspend fun write(logs: List<JdcrLogData>): Result<Unit>

    suspend fun read(startTs: Long, endTs: Long): Result<List<JdcrLogData>>

    suspend fun readTag(tag: String, startTs: Long, endTs: Long): Result<List<JdcrLogData>>

    suspend fun readFeature(feat: String, startTs: Long, endTs: Long): Result<List<JdcrLogData>>

    suspend fun removeOlder(latestTs: Long): Result<Unit>

}