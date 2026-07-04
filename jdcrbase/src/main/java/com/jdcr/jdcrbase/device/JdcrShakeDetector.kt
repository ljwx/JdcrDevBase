package com.jdcr.jdcrbase.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.jdcr.jdcrbase.app.JdcrAppUtils
import com.jdcr.jdcrbase.log.JdcrDevBaseLog
import kotlin.math.sqrt

class JdcrShakeDetector(
    private val config: Config = Config(),
    private val onShakeNotMain: () -> Unit
) : SensorEventListener {
    data class Config(
        // 高通滤波系数，越接近 1 越平滑；0.8~0.95 常用
        val highPassAlpha: Float = 0.9f,
        // 峰值阈值（单位: g），高通过滤后的瞬时加速度模长超过该值算一次峰值
        val peakThresholdG: Float = 1.8f,
        // 多条件触发：在 windowMs 内至少出现 requiredPeaks 次峰值才触发
        val requiredPeaks: Int = 4,
        val windowMs: Long = 700L,
        // 冷却时间：触发后在 cooldownMs 内不再触发
        val cooldownMs: Long = 1500L,
        // 传感器延迟档位
        val sensorDelay: Int = SensorManager.SENSOR_DELAY_GAME
    )

    private val sensorManager =
        JdcrAppUtils.getAppContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // 用于高通滤波：估计重力分量（低频）
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f

    // 峰值统计
    private val peakTimes = ArrayDeque<Long>()

    // 冷却控制
    private var lastTriggerTime = 0L

    // 峰值去抖：避免同一次抖动在极短时间内重复记数
    private var lastPeakTime = 0L
    private val minPeakIntervalMs = 80L
    fun start(): Boolean {
        val sensor = accelerometer ?: return false
        JdcrDevBaseLog.i("注册摇一摇监听")
        sensorManager.registerListener(this, sensor, config.sensorDelay)
        return true
    }

    fun stop() {
        JdcrDevBaseLog.i("注销摇一摇监听")
        sensorManager.unregisterListener(this)
        resetInternalState()
    }

    private fun resetInternalState() {
        gravityX = 0f
        gravityY = 0f
        gravityZ = 0f
        peakTimes.clear()
        lastPeakTime = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val now = SystemClock.elapsedRealtime()
        // 冷却期内直接忽略
        if (now - lastTriggerTime < config.cooldownMs) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        // 1) 高通滤波：先更新重力估计（低通），再取线性加速度
        val alpha = config.highPassAlpha
        gravityX = alpha * gravityX + (1f - alpha) * x
        gravityY = alpha * gravityY + (1f - alpha) * y
        gravityZ = alpha * gravityZ + (1f - alpha) * z
        val linearX = x - gravityX
        val linearY = y - gravityY
        val linearZ = z - gravityZ
        // 将线性加速度转换到 g 单位
        val linearG = sqrt(
            linearX * linearX + linearY * linearY + linearZ * linearZ
        ) / SensorManager.GRAVITY_EARTH
        // 峰值判定
        if (linearG >= config.peakThresholdG) {
            // 峰值去抖
            if (now - lastPeakTime < minPeakIntervalMs) return
            lastPeakTime = now
            // 2) 多条件触发：统计窗口内峰值次数
            peakTimes.addLast(now)
            while (peakTimes.isNotEmpty() && now - peakTimes.first() > config.windowMs) {
                peakTimes.removeFirst()
            }
            if (peakTimes.size >= config.requiredPeaks) {
                // 3) 冷却时间：触发后记录触发时刻
                lastTriggerTime = now
                peakTimes.clear()
                JdcrDevBaseLog.i("触发摇一摇阈值")
                onShakeNotMain()
            }
        } else {
            // 非峰值时也持续清理过期记录，防止队列堆积
            while (peakTimes.isNotEmpty() && now - peakTimes.first() > config.windowMs) {
                peakTimes.removeFirst()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}