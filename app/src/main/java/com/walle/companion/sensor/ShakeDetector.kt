package com.walle.companion.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onShakeDetected: () -> Unit,
    private val onTiltChanged: (tiltX: Float, tiltY: Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime = 0L
    private val shakeThreshold = 14.0f // m/s^2 above gravity
    private val shakeCooldownMs = 1500L

    fun start() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Normalised tilt (approx -1.0 to 1.0)
        val normalizedTiltX = (-x / 9.8f).coerceIn(-1.0f, 1.0f)
        val normalizedTiltY = (y / 9.8f).coerceIn(-1.0f, 1.0f)
        onTiltChanged(normalizedTiltX, normalizedTiltY)

        val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val netForce = kotlin.math.abs(gForce - SensorManager.GRAVITY_EARTH)

        val currentTime = System.currentTimeMillis()
        if (netForce > shakeThreshold && (currentTime - lastShakeTime > shakeCooldownMs)) {
            lastShakeTime = currentTime
            onShakeDetected()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
