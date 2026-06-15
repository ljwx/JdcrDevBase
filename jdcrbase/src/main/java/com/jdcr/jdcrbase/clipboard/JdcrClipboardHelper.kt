package com.jdcr.jdcrbase.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.annotation.AnyThread
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap

class JdcrClipboardHelper private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val clipboard =
        appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    // callback -> system listener 映射，方便移除
    private val listeners =
        ConcurrentHashMap<(CharSequence?) -> Unit, ClipboardManager.OnPrimaryClipChangedListener>()
    companion object {
        @JvmStatic
        fun create(context: Context): JdcrClipboardHelper = JdcrClipboardHelper(context)
    }
    /**
     * 写入文本到剪切板。
     *
     * @param text 要写入的文本
     * @param label 标签（系统展示时可能使用）
     * @param isSensitive 是否敏感内容（如验证码、密码）
     */
    @AnyThread
    fun setText(
        text: CharSequence,
        label: CharSequence = "text",
        isSensitive: Boolean = false
    ): Boolean = runCatching {
        val clip = ClipData.newPlainText(label, text)
        // Android 13+ 可标记敏感剪切板内容；部分 ROM 在更低版本也识别该 key。
        if (isSensitive) {
            val extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                clip.description.extras = extras
            }
        }
        clipboard.setPrimaryClip(clip)
        true
    }.getOrDefault(false)
    /**
     * 读取当前剪切板文本（若非纯文本，尝试 coerceToText）。
     */
    @AnyThread
    fun getText(): CharSequence? = runCatching {
        if (!clipboard.hasPrimaryClip()) return@runCatching null
        val primary = clipboard.primaryClip ?: return@runCatching null
        if (primary.itemCount <= 0) return@runCatching null
        primary.getItemAt(0).coerceToText(appContext)
    }.getOrNull()
    /**
     * 是否包含可读文本内容。
     */
    @AnyThread
    fun hasText(): Boolean = runCatching {
        if (!clipboard.hasPrimaryClip()) return@runCatching false
        val desc = clipboard.primaryClipDescription ?: return@runCatching false
        desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) ||
                desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST)
    }.getOrDefault(false)
    /**
     * 清空剪切板。
     * API 28+ 用 clearPrimaryClip；低版本使用空文本覆盖。
     */
    @AnyThread
    fun clear(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
        true
    }.getOrDefault(false)
    /**
     * 监听剪切板变化。返回 Closeable，用完务必 close() 释放。
     */
    @AnyThread
    fun addListener(callback: (CharSequence?) -> Unit): Closeable {
        val l = ClipboardManager.OnPrimaryClipChangedListener {
            callback(getText())
        }
        listeners[callback] = l
        clipboard.addPrimaryClipChangedListener(l)
        return Closeable {
            removeListener(callback)
        }
    }
    /**
     * 移除监听。
     */
    @AnyThread
    fun removeListener(callback: (CharSequence?) -> Unit) {
        val l = listeners.remove(callback) ?: return
        runCatching { clipboard.removePrimaryClipChangedListener(l) }
    }
    /**
     * 页面/模块销毁时可主动释放所有监听。
     */
    @AnyThread
    fun release() {
        listeners.forEach { (_, l) ->
            runCatching { clipboard.removePrimaryClipChangedListener(l) }
        }
        listeners.clear()
    }
}