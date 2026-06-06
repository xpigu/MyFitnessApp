package com.example.myfitnessapp.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class StepTrackingService(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _currentSteps = MutableLiveData<Int>(0)
    val currentSteps: LiveData<Int> = _currentSteps

    private val _currentCadence = MutableLiveData<Int>(0)
    val currentCadence: LiveData<Int> = _currentCadence

    private var isTracking = false
    private var stepsAccumulator = 0
    
    // 用于计算步频的变量
    private val stepTimestamps = mutableListOf<Long>()
    private val CADENCE_WINDOW_MS = 10000L // 计算过去10秒内的步频

    fun startTracking() {
        if (isTracking || stepSensor == null) return
        isTracking = true
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    fun pauseTracking() {
        if (!isTracking) return
        isTracking = false
        sensorManager.unregisterListener(this)
    }

    fun stopTracking() {
        pauseTracking()
        stepsAccumulator = 0
        stepTimestamps.clear()
        _currentSteps.value = 0
        _currentCadence.value = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTracking || event == null) return

        if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            // 每次检测到一步，值为 1.0
            if (event.values[0] == 1.0f) {
                stepsAccumulator++
                _currentSteps.value = stepsAccumulator

                // 记录时间戳用于计算步频
                val currentTime = System.currentTimeMillis()
                stepTimestamps.add(currentTime)
                calculateCadence(currentTime)
            }
        }
    }

    private fun calculateCadence(currentTime: Long) {
        // 移除窗口期之前的时间戳
        stepTimestamps.removeAll { it < currentTime - CADENCE_WINDOW_MS }

        // 如果窗口期内有步数，计算步频 (步/分钟)
        if (stepTimestamps.size > 1) {
            val firstStepTime = stepTimestamps.first()
            val timeDiffMs = currentTime - firstStepTime
            if (timeDiffMs > 0) {
                // 步频 = (步数 / 时间差(秒)) * 60
                val cadence = (stepTimestamps.size.toFloat() / (timeDiffMs / 1000f)) * 60f
                _currentCadence.value = cadence.toInt()
            }
        } else {
            _currentCadence.value = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 不需要处理
    }
    
    fun hasStepSensor(): Boolean {
        return stepSensor != null
    }
}
