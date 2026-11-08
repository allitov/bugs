package io.allitov.bugs.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import io.allitov.bugs.game.GameEngine
import io.allitov.bugs.game.GameEngine.TapResult
import io.allitov.bugs.game.TiltBonusSoundPlayer
import io.allitov.bugs.game.TiltSensor
import io.allitov.bugs.model.Bonus
import io.allitov.bugs.model.Bug
import io.allitov.bugs.model.GameSettingsStore
import io.allitov.bugs.model.GameStats
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Игровое поле: рисует жуков и бонус наклона, переводя логические координаты
 * в пиксели, обрабатывает касания игрока и крутит игровой цикл.
 */
class GameFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val engine = GameEngine(GameSettingsStore.current)

    /** Вызывается на каждом кадре: очки, оставшееся время, попадания, промахи
     *  и секунды до конца действия бонуса наклона. */
    var onStateChanged: ((score: Int, timeLeftSeconds: Int, hits: Int, misses: Int, tiltSecondsLeft: Int) -> Unit)? =
        null

    /** Вызывается один раз, когда истекает таймер раунда. */
    var onRoundFinished: (() -> Unit)? = null

    /** Итоги текущего раунда. */
    val currentStats: GameStats
        get() = engine.toStats()

    private val drawables = mutableMapOf<Int, Drawable>()
    private val effects = mutableListOf<TapEffect>()
    private val effectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    private val tiltSensor = TiltSensor(context).apply {
        onTiltChanged = { x, y -> engine.setTilt(x, y) }
    }

    private val tiltSounds = TiltBonusSoundPlayer(context)

    private var running = false
    private var roundFinishedNotified = false
    private var lastFrameTimeNanos = 0L

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            val now = System.nanoTime()
            val deltaSeconds = if (lastFrameTimeNanos == 0L) {
                0f
            } else {
                (now - lastFrameTimeNanos) / NANOS_PER_SECOND
            }
            lastFrameTimeNanos = now

            engine.update(deltaSeconds)
            effects.removeAll { now - it.startedAtNanos > EFFECT_DURATION_NANOS }
            // Звук действия бонуса: играет, пока активен режим наклона
            if (engine.isTiltActive) {
                tiltSounds.startLoop()
            } else {
                tiltSounds.stopLoop()
            }
            onStateChanged?.invoke(
                engine.score,
                engine.timeLeftSeconds,
                engine.hits,
                engine.misses,
                engine.tiltBonusSecondsLeft
            )

            if (engine.isFinished && !roundFinishedNotified) {
                roundFinishedNotified = true
                onRoundFinished?.invoke()
            }
            invalidate()
            postOnAnimation(this)
        }
    }

    /** Возобновляет игровой цикл. */
    fun start() {
        if (running) return
        running = true
        lastFrameTimeNanos = 0L
        tiltSensor.start()
        postOnAnimation(frameRunnable)
    }

    /** Приостанавливает игровой цикл (например, при уходе с экрана). */
    fun pause() {
        running = false
        tiltSensor.stop()
        tiltSounds.stopLoop()
        removeCallbacks(frameRunnable)
    }

    override fun onDetachedFromWindow() {
        tiltSounds.release()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width <= 0 || height <= 0) return
        val logicalHeight = GameEngine.LOGICAL_FIELD_WIDTH * height / width
        engine.setFieldSize(GameEngine.LOGICAL_FIELD_WIDTH, logicalHeight)
        engine.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val horizontalScale = width / engine.logicalWidth
        val verticalScale = height / engine.logicalHeight
        engine.bugs.forEach { bug -> drawBug(canvas, bug, horizontalScale, verticalScale) }
        engine.tiltBonus?.let { bonus -> drawBonus(canvas, bonus, horizontalScale) }
        drawEffects(canvas, horizontalScale, verticalScale)
    }

    private fun drawBug(canvas: Canvas, bug: Bug, horizontalScale: Float, verticalScale: Float) {
        val drawable = drawableFor(bug.imageRes)
        drawCentered(
            canvas = canvas,
            drawable = drawable,
            centerX = bug.x * horizontalScale,
            centerY = bug.y * verticalScale,
            radius = bug.size * horizontalScale,
            rotationDegrees = bug.angle
        )
    }

    private fun drawBonus(canvas: Canvas, bonus: Bonus, horizontalScale: Float) {
        val drawable = drawableFor(bonus.imageRes)
        val now = System.nanoTime()
        // Бонус «дышит»: слегка пульсирует и покачивается, чтобы привлечь внимание
        val pulse = 1f + BONUS_PULSE_AMPLITUDE * sin(now * BONUS_PULSE_SPEED_NANOS)
        val wobble = BONUS_WOBBLE_DEGREES * sin(now * BONUS_PULSE_SPEED_NANOS * 0.6f)
        drawCentered(
            canvas = canvas,
            drawable = drawable,
            centerX = bonus.x * horizontalScale,
            centerY = bonus.y * horizontalScale,
            radius = bonus.size * horizontalScale * pulse,
            rotationDegrees = wobble
        )
    }

    private fun drawableFor(imageRes: Int): Drawable =
        drawables.getOrPut(imageRes) {
            requireNotNull(AppCompatResources.getDrawable(context, imageRes)) {
                "Не найден ресурс игрового объекта: $imageRes"
            }
        }

    private fun drawCentered(
        canvas: Canvas,
        drawable: Drawable,
        centerX: Float,
        centerY: Float,
        radius: Float,
        rotationDegrees: Float
    ) {
        canvas.save()
        canvas.rotate(rotationDegrees, centerX, centerY)
        drawable.setBounds(
            (centerX - radius).roundToInt(),
            (centerY - radius).roundToInt(),
            (centerX + radius).roundToInt(),
            (centerY + radius).roundToInt()
        )
        drawable.draw(canvas)
        canvas.restore()
    }

    private fun drawEffects(canvas: Canvas, horizontalScale: Float, verticalScale: Float) {
        if (effects.isEmpty()) return
        val now = System.nanoTime()
        effects.forEach { effect ->
            val progress = ((now - effect.startedAtNanos).toFloat() / EFFECT_DURATION_NANOS)
                .coerceIn(0f, 1f)
            effectPaint.color = effect.color
            effectPaint.alpha = ((1f - progress) * 255f).roundToInt().coerceIn(0, 255)
            effectPaint.strokeWidth = horizontalScale * 1.2f
            canvas.drawCircle(
                effect.x * horizontalScale,
                effect.y * verticalScale,
                horizontalScale * (1.5f + 4.5f * progress),
                effectPaint
            )
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            handleTap(event.x, event.y)
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun handleTap(pixelX: Float, pixelY: Float) {
        if (engine.isFinished || width == 0 || height == 0) return
        val logicalX = pixelX * engine.logicalWidth / width
        val logicalY = pixelY * engine.logicalHeight / height
        val color = when (val result = engine.onTap(logicalX, logicalY)) {
            TapResult.TILT_BONUS -> {
                // Сигнал подбора бонуса и запуск «ветра» на время действия
                tiltSounds.playPickup()
                tiltSounds.startLoop()
                BONUS_COLOR
            }
            TapResult.BUG -> HIT_COLOR
            TapResult.MISS -> MISS_COLOR
        }
        effects += TapEffect(x = logicalX, y = logicalY, color = color, startedAtNanos = System.nanoTime())
        invalidate()
    }

    /** Визуальный отклик на касание: круг вокруг места тапа. */
    private data class TapEffect(
        val x: Float,
        val y: Float,
        val color: Int,
        val startedAtNanos: Long
    )

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000f
        const val EFFECT_DURATION_NANOS = 350L * 1_000_000
        val HIT_COLOR = Color.rgb(46, 125, 50)
        val MISS_COLOR = Color.rgb(198, 40, 40)
        val BONUS_COLOR = Color.rgb(255, 143, 0)

        /** Амплитуда пульсации бонуса, доля радиуса. */
        const val BONUS_PULSE_AMPLITUDE = 0.12f

        /** Скорость пульсации бонуса (2π), рад/наносекунду — полный цикл за 1,2 с. */
        const val BONUS_PULSE_SPEED_NANOS = 6.2831855f / 1_200_000_000f

        /** Амплитуда покачивания бонуса, в градусах. */
        const val BONUS_WOBBLE_DEGREES = 15f
    }
}
