package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class StepSensorManager(
    private val context: Context,
    private val onStepCounted: (Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    
    private var isTracking = false
    private var sensorTypeInUse = "NONE"

    // Step Counter Sensor state
    private var previousStepCount = -1f

    // Accelerometer algorithm state
    private var smoothedMagnitude = 9.8f
    private val alpha = 0.8f // smoothing factor
    private val stepThreshold = 11.5f // peak threshold for walking / shaking (~11.5 m/s²)
    private val minTimeBetweenStepsMs = 280L
    private var lastStepTime = 0L

    init {
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun startTracking() {
        if (isTracking) return
        
        val hasPermission = hasActivityRecognitionPermission()

        if (hasPermission) {
            // 1. Prioritize TYPE_STEP_DETECTOR for instant real-time UI step callbacks
            if (stepDetectorSensor != null) {
                val registered = sensorManager.registerListener(
                    this,
                    stepDetectorSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
                if (registered) {
                    sensorTypeInUse = "STEP_DETECTOR"
                    isTracking = true
                    Log.d("StepSensorManager", "Tracking started with TYPE_STEP_DETECTOR")
                    return
                }
            }

            // 2. Fallback to TYPE_STEP_COUNTER
            if (stepCounterSensor != null) {
                val registered = sensorManager.registerListener(
                    this,
                    stepCounterSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
                if (registered) {
                    sensorTypeInUse = "STEP_COUNTER"
                    isTracking = true
                    Log.d("StepSensorManager", "Tracking started with TYPE_STEP_COUNTER")
                    return
                }
            }
        }

        // 3. Fallback to Accelerometer magnitude peak algorithm
        if (accelerometerSensor != null) {
            val registered = sensorManager.registerListener(
                this,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_UI
            )
            if (registered) {
                sensorTypeInUse = "ACCELEROMETER"
                isTracking = true
                Log.d("StepSensorManager", "Tracking started using Accelerometer fallback algorithm.")
            } else {
                Log.e("StepSensorManager", "Failed to register accelerometer listener.")
            }
        } else {
            Log.e("StepSensorManager", "No hardware sensors available for step tracking.")
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        sensorManager.unregisterListener(this)
        isTracking = false
        previousStepCount = -1f
        Log.d("StepSensorManager", "Tracking stopped.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSteps = event.values[0]
                if (previousStepCount == -1f) {
                    previousStepCount = totalSteps
                } else {
                    val delta = totalSteps - previousStepCount
                    if (delta > 0) {
                        onStepCounted(delta.toInt())
                        previousStepCount = totalSteps
                    } else if (delta < 0) {
                        previousStepCount = totalSteps
                    }
                }
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values[0] == 1.0f) {
                    onStepCounted(1)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
                smoothedMagnitude = alpha * smoothedMagnitude + (1 - alpha) * magnitude
                
                val currentTime = System.currentTimeMillis()
                if (smoothedMagnitude > stepThreshold) {
                    if (currentTime - lastStepTime > minTimeBetweenStepsMs) {
                        onStepCounted(1)
                        lastStepTime = currentTime
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
