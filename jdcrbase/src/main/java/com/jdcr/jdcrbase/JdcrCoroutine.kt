package com.jdcr.jdcrbase

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.async as nativeAsync

suspend inline fun <T> safeSuspendCancellableCoroutine(
    crossinline onCancel: () -> Unit = {},
    crossinline register: (once: (T) -> Unit) -> Unit
): T = suspendCancellableCoroutine { cont ->
    val once = AtomicBoolean(false)
    cont.invokeOnCancellation { onCancel() }
    register { value ->
        if (once.compareAndSet(false, true) && cont.isActive) {
            cont.resume(value, null)
        }
    }
}

class JdcrSafeCoroutineScope(
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    tag: String = "jdcrSafeCoroutine",
    private val onError: ((Throwable) -> Unit)? = null
) : CoroutineScope {
    private val rootJob = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        report(throwable)
    }

    override val coroutineContext: CoroutineContext =
        rootJob + defaultDispatcher + exceptionHandler + CoroutineName(tag)

    /**
     * 专门为 async 准备的安全补丁（保留原生方法名）
     * 解决原生 async 吞异常的痛点，但保留最原汁原味的 Kotlin 语法
     */
    fun <T> async(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        val deferred = nativeAsync(context, start, block)
        deferred.invokeOnCompletion { cause ->
            if (cause != null && cause !is CancellationException) {
                report(cause)
            }
        }
        return deferred
    }

    private fun report(throwable: Throwable) {
        if (throwable is CancellationException) return
        Log.e(BASE_TAG, "协程内部发生致命崩溃", throwable)
        onError?.invoke(throwable)
    }

    fun cancelChildren() {
        coroutineContext.cancelChildren(CancellationException("全部任务取消"))
    }

    fun destroy() {
        coroutineContext.cancel(CancellationException("销毁协程"))
    }
}