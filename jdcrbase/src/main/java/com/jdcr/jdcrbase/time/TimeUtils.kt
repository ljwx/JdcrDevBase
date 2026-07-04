package com.jdcr.jdcrbase.time

import android.os.Build
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.getValue

object TimeUtils {

    private const val DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss"
    private const val DEFAULT_PATTERN_MS = "yyyy-MM-dd HH:mm:ss.SSS"

    // API 26+ 推荐：线程安全、可复用
    private val zoneId: ZoneId by lazy { ZoneId.systemDefault() }
    private val formatter26: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern(DEFAULT_PATTERN, Locale.getDefault())
    }
    private val formatter26Ms: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern(DEFAULT_PATTERN_MS, Locale.getDefault())
    }

    // 低版本 fallback：ThreadLocal 避免线程安全问题和重复创建开销
    private val formatterLegacy = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat(DEFAULT_PATTERN, Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
        }
    }

    private val formatterLegacyMs = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat(DEFAULT_PATTERN_MS, Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
        }
    }


    /** 时间戳(ms) -> 字符串 */
    fun format(timestampMillis: Long, enableMs: Boolean = false): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (if (enableMs) formatter26Ms else formatter26).format(
                Instant.ofEpochMilli(
                    timestampMillis
                ).atZone(zoneId)
            )
        } else {
            (if (enableMs) formatterLegacyMs else formatterLegacy).get()!!
                .format(Date(timestampMillis))
        }
    }

    fun format(timestampMillis: Long, pattern: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(
                Instant.ofEpochMilli(
                    timestampMillis
                ).atZone(zoneId)
            )
        } else {
            SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMillis))
        }
    }

    /** 字符串 -> 时间戳(ms)，失败返回 null */
    fun parse(text: String, enableMs: Boolean = false): Long? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ldt = java.time.LocalDateTime.parse(
                    text,
                    (if (enableMs) formatter26Ms else formatter26)
                )
                ldt.atZone(zoneId).toInstant().toEpochMilli()
            } else {
                (if (enableMs) formatterLegacyMs else formatterLegacy).get()!!.parse(text)?.time
            }
        }.getOrNull()
    }

}