package com.jdcr.jdcrbase.database

import com.jdcr.jdcrbase.log.JdcrLogData

interface IDevBaseDatabaseLog {

    fun write(log: JdcrLogData)

    fun read(startTs: Long, endTs: Long): List<JdcrLogData>

    fun readTag(tag: String, startTs: Long, endTs: Long): List<JdcrLogData>

    fun readFeature(feat: String, startTs: Long, endTs: Long): List<JdcrLogData>

    fun removeOlder(latestTs: Long)

}