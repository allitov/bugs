package io.allitov.bugs.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager

/**
 * Датчик наклона телефона: переводит показания гравитации в направление «низа»
 * в системе координат экрана.
 *
 * Датчик отдаёт вектор гравитации в осях устройства, где ось Y указывает на
 * верх телефона, а ось X — вправо; сам вектор направлен «вверх» (когда телефон
 * лежит на столе экраном вверх, он смотрит из экрана). Поэтому направление
 * «вниз» — это вектор с обратным знаком, пересчитанный под текущий поворот
 * экрана: наклонив телефон, игрок заставляет жуков скатываться в сторону
 * наклона.
 *
 * [onTiltChanged] получает нормализованное направление: (0; 1) — экран опущен
 * нижним краем, (1; 0) — наклонён вправо, (-1; -1) — приподнят правый нижний
 * угол, и так далее. Каждая компонента лежит в диапазоне от -1 до 1.
 */
class TiltSensor(private val context: Context) {

    /** Вызывается при каждом новом показании датчика. */
    var onTiltChanged: ((x: Float, y: Float) -> Unit)? = null

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Точность TYPE_GRAVITY выше, поэтому он предпочтителен; TYPE_ACCELEROMETER —
    // запасной вариант, он тоже содержит вектор гравитации в состоянии покоя.
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val smoothed = FloatArray(SENSOR_AXES)
    private var hasReading = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            smooth(event.values, smoothed)
            onTiltChanged?.invoke(
                screenDownX(smoothed).coerceIn(-GRAVITY, GRAVITY) / GRAVITY,
                screenDownY(smoothed).coerceIn(-GRAVITY, GRAVITY) / GRAVITY
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /** Начинает получать показания датчика. */
    fun start() {
        val sensor = sensor ?: return
        hasReading = false
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    /** Останавливает получение показаний датчика. */
    fun stop() {
        sensorManager.unregisterListener(listener)
    }

    /** Простое сглаживание показаний (низкочастотный фильтр). */
    private fun smooth(input: FloatArray, output: FloatArray) {
        if (!hasReading) {
            input.copyInto(output)
            hasReading = true
            return
        }
        for (axis in 0 until SENSOR_AXES) {
            output[axis] += SMOOTHING * (input[axis] - output[axis])
        }
    }

    // Ось экрана X (вправо) в осях устройства в зависимости от поворота экрана:
    // поворот 0 — ось X устройства; 90 — ось Y; 180 — ось -X; 270 — ось -Y.
    // Знак «минус» появляется из-за того, что вектор гравитации направлен вверх.
    private fun screenDownX(g: FloatArray): Float = when (displayRotation()) {
        Surface.ROTATION_90 -> -g[1]
        Surface.ROTATION_180 -> g[0]
        Surface.ROTATION_270 -> g[1]
        else -> -g[0]
    }

    private fun screenDownY(g: FloatArray): Float = when (displayRotation()) {
        Surface.ROTATION_90 -> -g[0]
        Surface.ROTATION_180 -> g[1]
        Surface.ROTATION_270 -> -g[0]
        else -> -g[1]
    }

    @Suppress("DEPRECATION")
    private fun displayRotation(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            val windowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.rotation
        }

    private companion object {
        /** Ускорение свободного падения, м/с². */
        const val GRAVITY = 9.81f

        /** Доля нового показания в сглаженном значении за одно событие датчика. */
        const val SMOOTHING = 0.4f

        const val SENSOR_AXES = 3
    }
}
