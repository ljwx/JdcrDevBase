package com.jdcr.jdcrbase.keyboard

import android.app.Activity
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.annotation.MainThread
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.jdcr.jdcrbase.log.JdcrDevBaseLog
import kotlin.math.roundToInt

/**
 * 单个 Activity/编辑器页面的软键盘观测器，不修改宿主 softInputMode，不依赖 X5 回调。
 *
 * AUTO 模式在 Android 11+ 读取系统 IME Insets；旧系统观测独立、非焦点的 resize 窗口可见边界。
 * 旧系统无法取得有效窗口范围或系统栏基准时返回 UNKNOWN，不以输入焦点、IMM active 或固定高度兜底。
 * 厂商 ROM 仍可能提供错误的 Insets；LEGACY_WINDOW 结果是推断值，尤其不能据此保证浮动键盘
 * 是否可见。旧系统 HIDDEN 只表示没有检测到底部键盘遮挡，不排除浮动键盘。
 *
 * 在主线程成对调用 start(editorView)/stop()，建议对应页面 onResume/onPause。
 * anchor 脱离窗口时自动 stop；每个页面只创建一份，禁止多个实例反复创建测量窗口。
 * 回调包含首次状态快照；首次 HIDDEN 或 UNKNOWN 都不代表一次“键盘收起”操作。
 * 接入方只能在同一页面已确认 SHOWN 后按需处理 HIDDEN，UNKNOWN 不得转换成高度 0。
 * 此类仅提供观测结果，不发送 softKeyboardChanged，也不提交、关闭输入框。
 * Options 可调整检测路径、旧系统阈值与收起复核时间。默认收起需间隔 120ms 重新测量确认；
 * SHOWN/UNKNOWN 立即发布。不能将 source/isEstimated 当成厂商兼容性已经通过验证的证明。
 */
class SoftKeyboardMonitor(
    private val activity: Activity,
    private val options: Options = Options(),
    private val onChanged: (State) -> Unit,
) {
    /** 强制路径用于同一设备对照；SYSTEM_INSETS 要求 Android 11+，不自动降级。 */
    enum class DetectionMode { AUTO, SYSTEM_INSETS, LEGACY_WINDOW }

    data class Options(
        /** AUTO 为日常默认；强制路径只用于验证目标设备。 */
        val detectionMode: DetectionMode = DetectionMode.AUTO,
        /** 底部遮挡超过稳定导航栏区域的最小差值，单位 dp。 */
        val minimumKeyboardExtraDp: Int = 80,
        /** 0 表示禁用复核；非 0 时到期重新测量，SHOWN/UNKNOWN 会取消待确认的收起。 */
        val hiddenConfirmationMillis: Long = 120,
        /** 调试时记录变化后的旧版测量数据；不改变检测规则或回调时机。 */
        val debugLogging: Boolean = false,
    ) {
        init {
            require(minimumKeyboardExtraDp > 0) { "minimumKeyboardExtraDp 必须大于 0" }
            require(hiddenConfirmationMillis >= 0) { "hiddenConfirmationMillis 不能为负数" }
        }
    }

    enum class Visibility { UNKNOWN, SHOWN, HIDDEN }

    enum class Source { SYSTEM_IME, LEGACY_WINDOW, UNAVAILABLE }

    /** heightPx 是底部遮挡高度（含重叠的导航栏区域），UNKNOWN 时为 null。
     * 系统可能报告 SHOWN 且高度为 0，例如没有底部遮挡的浮动输入法。
     */
    data class State(
        val visibility: Visibility,
        val heightPx: Int?,
        val source: Source,
    ) {
        val isEstimated: Boolean get() = source == Source.LEGACY_WINDOW
    }

    /** 最近发布的状态；收起复核或动画期间保留上次结果。stop 将其重置，不触发回调。 */
    var state = State(Visibility.UNKNOWN, null, Source.UNAVAILABLE)
        private set

    /** 是否仍有监听意图；失败后可能没有有效窗口，此时 state 为 UNKNOWN，可调用 refresh 重试。 */
    val isStarted: Boolean get() = started

    private val useSystemInsets = usesSystemKeyboardInsets(options.detectionMode, Build.VERSION.SDK_INT)
    private val stateFilter = KeyboardStateFilter(options.hiddenConfirmationMillis)
    private var started = false
    private var anchor: View? = null
    private var probe: View? = null
    private var popup: PopupWindow? = null
    private var observedTree: ViewTreeObserver? = null
    private var latestInsets: WindowInsetsCompat? = null
    private var lastLegacyFrame: LegacyKeyboardFrame? = null
    private var readPosted = false
    private val imeAnimations = mutableSetOf<WindowInsetsAnimationCompat>()
    private val confirmHiddenRunnable = Runnable { if (started) readState() }
    private val animationTimeoutRunnable = Runnable {
        if (started && imeAnimations.isNotEmpty()) {
            imeAnimations.clear()
            latestInsets = null
            // ROM 漏掉结束回调时，先报告未知，等新的 Insets/refresh 恢复，不猜测动画结果。
            publishUnknown()
        }
    }

    private val animationCallback = object : WindowInsetsAnimationCompat.Callback(
        DISPATCH_MODE_CONTINUE_ON_SUBTREE
    ) {
        override fun onPrepare(animation: WindowInsetsAnimationCompat) {
            if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                imeAnimations.add(animation)
                cancelHiddenConfirmation()
                anchor?.let {
                    it.removeCallbacks(animationTimeoutRunnable)
                    it.postDelayed(animationTimeoutRunnable, 2_000)
                }
            }
        }

        override fun onProgress(
            insets: WindowInsetsCompat,
            runningAnimations: MutableList<WindowInsetsAnimationCompat>,
        ): WindowInsetsCompat = insets

        override fun onEnd(animation: WindowInsetsAnimationCompat) {
            imeAnimations.remove(animation)
            if (imeAnimations.isEmpty()) anchor?.removeCallbacks(animationTimeoutRunnable)
            scheduleRead()
        }
    }

    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            view.post(startRunnable)
        }

        override fun onViewDetachedFromWindow(view: View) {
            stop()
        }
    }

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { scheduleRead() }
    private val probeLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        scheduleRead()
    }
    private val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
        if (!started) return@OnWindowFocusChangeListener
        // 失焦可能是另一个 Activity/原生输入弹窗接管窗口，不能当作键盘收起。
        latestInsets = null
        lastLegacyFrame = null
        imeAnimations.clear()
        anchor?.removeCallbacks(animationTimeoutRunnable)
        if (focused) {
            probe?.let { ViewCompat.requestApplyInsets(it) }
            scheduleRead()
        } else {
            publishUnknown()
        }
    }

    private val startRunnable = Runnable {
        if (started && probe == null) attachProbe()
    }

    private val readRunnable = Runnable {
        readPosted = false
        if (started) readState()
    }

    @MainThread
    fun start(anchor: View = activity.window.decorView) {
        checkMainThread()
        if (started && this.anchor === anchor) return
        stop()
        // 厂商窗口管理器拒绝移除旧测量窗口时，不继续叠加创建新窗口。
        if (popup != null) {
            publishUnknown()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return
        started = true
        this.anchor = anchor
        anchor.addOnAttachStateChangeListener(attachListener)
        if (ViewCompat.isAttachedToWindow(anchor)) anchor.post(startRunnable)
    }

    /** 请求重新读取；启动失败/检测窗口被关闭后可用它重试，不创建第二个窗口。 */
    @MainThread
    fun refresh() {
        checkMainThread()
        if (!started) return
        if (probe == null) {
            anchor?.let {
                it.removeCallbacks(startRunnable)
                it.post(startRunnable)
            }
        } else {
            probe?.let { ViewCompat.requestApplyInsets(it) }
            scheduleRead()
        }
    }

    @MainThread
    fun stop() {
        checkMainThread()
        started = false
        anchor?.let {
            it.removeCallbacks(startRunnable)
            it.removeCallbacks(readRunnable)
            it.removeCallbacks(confirmHiddenRunnable)
            it.removeCallbacks(animationTimeoutRunnable)
            it.removeOnAttachStateChangeListener(attachListener)
        }
        observedTree?.takeIf { it.isAlive }?.let {
            it.removeOnGlobalLayoutListener(layoutListener)
            it.removeOnWindowFocusChangeListener(focusListener)
        }
        observedTree = null
        probe?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it, null)
            ViewCompat.setWindowInsetsAnimationCallback(it, null)
            it.removeOnLayoutChangeListener(probeLayoutListener)
        }
        val measurementWindow = popup
        measurementWindow?.let {
            it.setOnDismissListener(null)
            try {
                it.dismiss()
                popup = null
            } catch (error: RuntimeException) {
                JdcrDevBaseLog.iKeyboard("SoftKeyboardMonitor 关闭测量窗口失败:${error.javaClass.simpleName}")
            }
        }
        if (measurementWindow == null) probe?.let { (it.parent as? ViewGroup)?.removeView(it) }
        probe = null
        anchor = null
        latestInsets = null
        lastLegacyFrame = null
        imeAnimations.clear()
        readPosted = false
        stateFilter.reset()
        state = State(Visibility.UNKNOWN, null, Source.UNAVAILABLE)
    }

    @Suppress("DEPRECATION")
    private fun attachProbe() {
        val target = anchor ?: return
        if (!ViewCompat.isAttachedToWindow(target) || activity.isFinishing || activity.isDestroyed) return
        val decor = activity.window.decorView as? ViewGroup
        if (decor == null || target.applicationWindowToken != decor.applicationWindowToken) {
            publishUnknown()
            return
        }
        val view = View(activity).apply {
            isFocusable = false
            isClickable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setBackgroundColor(0)
        }
        probe = view
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            latestInsets = insets
            scheduleRead()
            insets
        }
        try {
            if (useSystemInsets) {
                // 使用自己的 View，避免覆盖 Activity/WebView 已注册的 Insets 监听。
                ViewCompat.setWindowInsetsAnimationCallback(view, animationCallback)
                decor.addView(view, ViewGroup.LayoutParams(1, 1))
            } else {
                val measurementWindow = PopupWindow(view, 0, WindowManager.LayoutParams.MATCH_PARENT, false).apply {
                    setBackgroundDrawable(ColorDrawable(0))
                    isTouchable = false
                    inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
                    softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    animationStyle = 0
                    setOnDismissListener {
                        if (started) {
                            // 厂商窗口管理器可能主动关闭探测窗口；明确标记失效，不猜测隐藏。
                            handleProbeFailure()
                        }
                    }
                }
                popup = measurementWindow
                measurementWindow.showAtLocation(target, Gravity.TOP or Gravity.START, 0, 0)
            }
            observedTree = decor.viewTreeObserver.also {
                it.addOnGlobalLayoutListener(layoutListener)
                it.addOnWindowFocusChangeListener(focusListener)
            }
            // 旧系统 resize 的变化发生在独立窗口上，监听它自身的布局。
            if (!useSystemInsets) {
                view.addOnLayoutChangeListener(probeLayoutListener)
            }
            ViewCompat.requestApplyInsets(view)
            scheduleRead()
        } catch (error: RuntimeException) {
            JdcrDevBaseLog.iKeyboard("SoftKeyboardMonitor 启动失败:${error.javaClass.simpleName}")
            handleProbeFailure()
        }
    }

    private fun handleProbeFailure() {
        val restartAnchor = anchor
        stop()
        // 保留启动意图；不自动循环重建窗口，refresh 可显式重试。
        if (popup == null && restartAnchor != null && ViewCompat.isAttachedToWindow(restartAnchor)) {
            started = true
            anchor = restartAnchor
            restartAnchor.addOnAttachStateChangeListener(attachListener)
        }
        publishUnknown()
    }

    private fun scheduleRead() {
        if (!started || readPosted) return
        val target = anchor ?: return
        readPosted = true
        // 合并同帧回调；执行时读取窗口当前状态，避免使用排队前的过期状态。
        ViewCompat.postOnAnimation(target, readRunnable)
    }

    private fun readState() {
        val view = probe ?: return
        if (!activity.hasWindowFocus() || !ViewCompat.isAttachedToWindow(view)) {
            publishUnknown()
            return
        }
        // 系统在动画开始时即可下发目标可见性；等动画结束再发布最终快照。
        if (imeAnimations.isNotEmpty()) return
        val result = try {
            measureState(view)
        } catch (error: RuntimeException) {
            JdcrDevBaseLog.iKeyboard("SoftKeyboardMonitor 读取失败:${error.javaClass.simpleName}")
            State(Visibility.UNKNOWN, null, Source.UNAVAILABLE)
        }
        // 业务回调异常不应被误当作 ROM 测量失败并再次回调。
        publish(result)
    }

    private fun measureState(view: View): State {
        if (!useSystemInsets) return measureLegacyState(view)
        val insets = ViewCompat.getRootWindowInsets(view) ?: latestInsets
        if (insets == null) return State(Visibility.UNKNOWN, null, Source.UNAVAILABLE)
        val visible = insets.isVisible(WindowInsetsCompat.Type.ime())
        return State(
            if (visible) Visibility.SHOWN else Visibility.HIDDEN,
            if (visible) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0,
            Source.SYSTEM_IME,
        )
    }

    @Suppress("DEPRECATION")
    private fun measureLegacyState(view: View): State {
        val decor = activity.window.decorView
        val hostInsets = ViewCompat.getRootWindowInsets(decor)
        if (hostInsets == null || hostInsets.isConsumed || view.height <= 0) {
            return State(Visibility.UNKNOWN, null, Source.UNAVAILABLE)
        }
        val location = IntArray(2)
        decor.getLocationOnScreen(location)
        val windowBounds = Rect(location[0], location[1], location[0] + decor.width, location[1] + decor.height)
        val isMultiWindow = Build.VERSION.SDK_INT >= 24 && activity.isInMultiWindowMode
        if (!activity.window.isFloating && !isMultiWindow) {
            // 普通 Activity 用显示区域作基准，不使用可能已被 IME 缩小的宿主高度。
            val displaySize = Point()
            activity.windowManager.defaultDisplay.getRealSize(displaySize)
            // 旋转布局尚未同步或窗口被系统限制时，不套用另一套坐标范围。
            if (decor.width != displaySize.x) return State(Visibility.UNKNOWN, null, Source.UNAVAILABLE)
            windowBounds.set(0, 0, displaySize.x, displaySize.y)
        }
        val rect = Rect()
        view.getWindowVisibleDisplayFrame(rect)
        if (rect.height() <= 0) return State(Visibility.UNKNOWN, null, Source.UNAVAILABLE)
        view.getLocationOnScreen(location)
        // resize 后 Popup 内部 Insets 可以仍为 0；实际窗口底边才保留了被缩掉的高度。
        val frame = LegacyKeyboardFrame(
            windowTopPx = windowBounds.top,
            windowBottomPx = windowBounds.bottom,
            visibleBottomPx = minOf(location[1] + view.height, rect.bottom),
            navigationBarBottomPx = hostInsets.stableInsetBottom,
        )
        if (options.debugLogging && frame != lastLegacyFrame) {
            JdcrDevBaseLog.iKeyboard("SoftKeyboardMonitor 旧版测量:$frame")
            lastLegacyFrame = frame
        }
        val minimumImeExtra = (options.minimumKeyboardExtraDp * activity.resources.displayMetrics.density)
            .roundToInt().coerceAtLeast(1)
        return classifyLegacyKeyboardFrame(frame, minimumImeExtra)
    }

    private fun publishUnknown() {
        publish(State(Visibility.UNKNOWN, null, Source.UNAVAILABLE))
    }

    private fun publish(value: State) {
        val result = stateFilter.accept(value, SystemClock.uptimeMillis())
        anchor?.removeCallbacks(confirmHiddenRunnable)
        result.retryAfterMillis?.let { anchor?.postDelayed(confirmHiddenRunnable, it) }
        result.state?.let {
            state = it
            onChanged(it)
        }
    }

    private fun cancelHiddenConfirmation() {
        anchor?.removeCallbacks(confirmHiddenRunnable)
        stateFilter.cancelPendingHidden()
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) { "SoftKeyboardMonitor 必须在主线程调用" }
    }
}

internal fun usesSystemKeyboardInsets(mode: SoftKeyboardMonitor.DetectionMode, sdkInt: Int): Boolean =
    when (mode) {
        SoftKeyboardMonitor.DetectionMode.AUTO -> sdkInt >= 30
        SoftKeyboardMonitor.DetectionMode.SYSTEM_INSETS -> {
            require(sdkInt >= 30) { "SYSTEM_INSETS 模式要求 Android 11 或更高版本" }
            true
        }
        SoftKeyboardMonitor.DetectionMode.LEGACY_WINDOW -> false
    }

/** 全部使用屏幕坐标；导航栏基准来自宿主，不能读取已经缩小的 Popup 内部 Insets。 */
internal data class LegacyKeyboardFrame(
    val windowTopPx: Int,
    val windowBottomPx: Int,
    val visibleBottomPx: Int,
    val navigationBarBottomPx: Int,
)

internal fun classifyLegacyKeyboardFrame(frame: LegacyKeyboardFrame, minimumExtraPx: Int): SoftKeyboardMonitor.State {
    if (frame.windowBottomPx <= frame.windowTopPx ||
        frame.visibleBottomPx <= frame.windowTopPx || frame.visibleBottomPx > frame.windowBottomPx ||
        frame.navigationBarBottomPx !in 0 until (frame.windowBottomPx - frame.windowTopPx)) {
        return SoftKeyboardMonitor.State(SoftKeyboardMonitor.Visibility.UNKNOWN, null, SoftKeyboardMonitor.Source.UNAVAILABLE)
    }
    val bottomPx = frame.windowBottomPx - frame.visibleBottomPx
    val keyboardExtraPx = bottomPx - frame.navigationBarBottomPx
    val visibility = when {
        keyboardExtraPx >= minimumExtraPx -> SoftKeyboardMonitor.Visibility.SHOWN
        keyboardExtraPx > 0 -> SoftKeyboardMonitor.Visibility.UNKNOWN
        else -> SoftKeyboardMonitor.Visibility.HIDDEN
    }
    return SoftKeyboardMonitor.State(
        visibility,
        when (visibility) {
            SoftKeyboardMonitor.Visibility.SHOWN -> bottomPx
            SoftKeyboardMonitor.Visibility.HIDDEN -> 0
            SoftKeyboardMonitor.Visibility.UNKNOWN -> null
        },
        SoftKeyboardMonitor.Source.LEGACY_WINDOW,
    )
}

/** 只处理结果的复核和去重；重试到期必须由监测器重新测量，不能发布缓存的 HIDDEN。 */
internal class KeyboardStateFilter(private val hiddenConfirmationMillis: Long) {
    data class Result(val state: SoftKeyboardMonitor.State? = null, val retryAfterMillis: Long? = null)

    private var delivered: SoftKeyboardMonitor.State? = null
    private var hiddenSince: Long? = null

    fun accept(candidate: SoftKeyboardMonitor.State, nowMillis: Long): Result {
        if (candidate.visibility == SoftKeyboardMonitor.Visibility.HIDDEN &&
            delivered?.visibility != SoftKeyboardMonitor.Visibility.HIDDEN) {
            val since = hiddenSince ?: nowMillis.also { hiddenSince = it }
            val remaining = hiddenConfirmationMillis - (nowMillis - since)
            if (remaining > 0) return Result(retryAfterMillis = remaining)
        }
        hiddenSince = null
        if (delivered == candidate) return Result()
        delivered = candidate
        return Result(state = candidate)
    }

    fun cancelPendingHidden() {
        hiddenSince = null
    }

    fun reset() {
        delivered = null
        hiddenSince = null
    }
}
