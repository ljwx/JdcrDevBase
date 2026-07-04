package com.jdcr.jdcrbase.page

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import com.jdcr.jdcrbase.log.JdcrDevBaseLog
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

fun Context?.toActivity(): Activity? {
    var currentContext = this

    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        val baseContext = currentContext.baseContext
        if (currentContext === baseContext) {
            break
        }
        currentContext = baseContext
    }
    return null
}

fun Activity?.isAlive(): Boolean {
    val alive = this != null && !this.isFinishing && !this.isDestroyed
    JdcrDevBaseLog.i("Activity是否还存活:$alive,${this?.javaClass?.simpleName}")
    return alive
}


object JdcrActivityUtils : Application.ActivityLifecycleCallbacks {
    private val lock = Any()
    private val inited = AtomicBoolean(false)

    @Volatile
    private var resumedActivityRef: WeakReference<Activity>? = null

    // key: identityHashCode(activity), value: weak ref
    // 按插入顺序保存 started activity，最后一个通常是最近可见的
    private val startedMap = LinkedHashMap<Int, WeakReference<Activity>>()
    fun init(application: Application) {
        if (inited.compareAndSet(false, true)) {
            JdcrDevBaseLog.i("注册监听Activity变化")
            application.registerActivityLifecycleCallbacks(this)
        }
    }

    /** 当前可交互 Activity（可能为 null，调用方需容错） */
    fun currentActivity(): Activity? = resumedActivityRef?.get()

    /** 当前仍 visible 的 Activity 列表（按可见进入顺序） */
    fun visibleActivities(): List<Activity> = synchronized(lock) {
        compactLocked()
        startedMap.values.mapNotNull { it.get() }
    }

    /** 栈顶 visible 候选（多窗口时仅代表“最近 started 的可见页”） */
    fun topVisibleActivity(): Activity? = synchronized(lock) {
        compactLocked()
        startedMap.values.lastOrNull()?.get()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        JdcrDevBaseLog.i("Activity创建:${activity::class.java.simpleName}")
    }

    override fun onActivityStarted(activity: Activity) {
        synchronized(lock) {
            compactLocked()
            startedMap[System.identityHashCode(activity)] = WeakReference(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        resumedActivityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumedActivityRef?.get() === activity) {
            resumedActivityRef = null
        }
    }

    override fun onActivityStopped(activity: Activity) {
        synchronized(lock) {
            startedMap.remove(System.identityHashCode(activity))
            compactLocked()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        JdcrDevBaseLog.i("Activity销毁:${activity::class.java.simpleName}")
        synchronized(lock) {
            startedMap.remove(System.identityHashCode(activity))
            compactLocked()
        }
        if (resumedActivityRef?.get() === activity) {
            resumedActivityRef = null
        }
    }

    private fun compactLocked() {
        val it = startedMap.entries.iterator()
        while (it.hasNext()) {
            if (it.next().value.get() == null) it.remove()
        }
    }

}