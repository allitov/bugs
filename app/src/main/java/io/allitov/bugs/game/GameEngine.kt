package io.allitov.bugs.game

import io.allitov.bugs.R
import io.allitov.bugs.model.Bonus
import io.allitov.bugs.model.Bug
import io.allitov.bugs.model.BugType
import io.allitov.bugs.model.GameSettings
import io.allitov.bugs.model.GameStats
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Логика раунда игры «Жуки»: создание и перемещение жуков, обработка касаний,
 * подсчёт очков и статистики.
 *
 * Помимо жуков движок ведёт бонус наклона: раз в [TILT_BONUS_INTERVAL_SECONDS]
 * секунд на поле появляется бонус, при нажатии на который на
 * [TILT_EFFECT_DURATION_SECONDS] секунд включается режим наклона — жуки
 * скатываются в наклонённую сторону экрана (см. [setTilt]).
 *
 * Все вычисления выполняются в логической системе координат игрового поля:
 * ширина поля всегда равна [LOGICAL_FIELD_WIDTH] условных единиц, а высота
 * подбирается под соотношение сторон экрана, поэтому геометрия игры одинакова
 * на устройствах с любым разрешением.
 */
class GameEngine(
    private val settings: GameSettings,
    private val random: Random = Random.Default
) {

    companion object {
        /** Ширина игрового поля в логических единицах. */
        const val LOGICAL_FIELD_WIDTH = 100f

        /** Штраф за промах в очках. */
        const val MISS_PENALTY = 10

        /** Интервал появления бонуса наклона, в секундах. */
        const val TILT_BONUS_INTERVAL_SECONDS = 15

        /** Длительность действия бонуса наклона после нажатия, в секундах. */
        const val TILT_EFFECT_DURATION_SECONDS = 10

        private const val SPAWN_TICK_SECONDS = 0.8f
        private const val MAX_DELTA_SECONDS = 0.1f
        private const val INITIAL_BUGS = 5
        private const val TILT_BONUS_SIZE = 7f

        /** Скорость скатывания жуков при полном наклоне телефона, ед/с. */
        private const val TILT_FORCE = 65f
    }

    var logicalWidth = LOGICAL_FIELD_WIDTH
        private set
    var logicalHeight = LOGICAL_FIELD_WIDTH
        private set

    private var nextBugId = 1L
    private val activeBugs = mutableListOf<Bug>()

    /** Жуки, находящиеся на поле. */
    val bugs: List<Bug>
        get() = activeBugs

    private var nextBonusId = 1L

    /** Бонус наклона, лежащий на поле, или null, если бонуса сейчас нет. */
    var tiltBonus: Bonus? = null
        private set

    private var tiltBonusCooldown = TILT_BONUS_INTERVAL_SECONDS.toFloat()
    private var tiltEffectRemainingSeconds = 0f
    private var tiltX = 0f
    private var tiltY = 0f

    /** Активен ли сейчас режим учёта наклона телефона. */
    val isTiltActive: Boolean
        get() = tiltEffectRemainingSeconds > 0f

    /** Сколько целых секунд осталось действовать режиму наклона. */
    val tiltBonusSecondsLeft: Int
        get() = ceil(tiltEffectRemainingSeconds).toInt()

    var score = 0
        private set
    var hits = 0
        private set
    var misses = 0
        private set
    var elapsedSeconds = 0f
        private set

    private var started = false
    private var spawnCooldown = 0f
    private var rareBugCooldown = 0f

    /** Осталось до конца раунда, в целых секундах. */
    val timeLeftSeconds: Int
        get() {
            val remaining = settings.roundDurationSeconds - elapsedSeconds
            return if (remaining <= 0f) 0 else ceil(remaining).toInt()
        }

    /** Завершён ли раунд по истечении таймера. */
    val isFinished: Boolean
        get() = elapsedSeconds >= settings.roundDurationSeconds

    /** Точность игрока в процентах. */
    val accuracyPercent: Int
        get() = toStats().accuracyPercent

    /** Итоги раунда. */
    fun toStats(): GameStats = GameStats(score = score, hits = hits, misses = misses)

    /**
     * Задаёт размер игрового поля в логических единицах.
     * Вызывается представлением при изменении размеров экрана.
     */
    fun setFieldSize(width: Float, height: Float) {
        logicalWidth = width
        logicalHeight = height
        activeBugs.forEach { clampInsideField(it) }
    }

    /** Запускает раунд: создаёт первых жуков. Повторный вызов игнорируется. */
    fun start() {
        if (started) return
        started = true
        rareBugCooldown = settings.bonusIntervalSeconds.toFloat()
        repeat(settings.maxBugs.coerceAtMost(INITIAL_BUGS)) { spawnBug(rare = false) }
    }

    /**
     * Обновляет состояние раунда: перемещает жуков, отражает их от границ поля,
     * добавляет новых жуков и ведёт отсчёт времени до конца раунда.
     *
     * @param deltaSeconds время, прошедшее с предыдущего обновления, в секундах
     */
    fun update(deltaSeconds: Float) {
        if (!started || isFinished) return
        val delta = deltaSeconds.coerceIn(0f, MAX_DELTA_SECONDS)
        elapsedSeconds += delta

        moveBugs(delta)

        spawnCooldown -= delta
        if (spawnCooldown <= 0f && activeBugs.size < settings.maxBugs) {
            spawnCooldown = SPAWN_TICK_SECONDS
            spawnBug(rare = false)
        }
        rareBugCooldown -= delta
        if (rareBugCooldown <= 0f) {
            rareBugCooldown = settings.bonusIntervalSeconds.toFloat()
            spawnBug(rare = true)
        }

        updateTiltBonus(delta)
    }

    /**
     * Сохраняет текущий наклон телефона в экранных координатах (см. [TiltSensor]).
     * Учитывается в движении жуков, только пока активен режим наклона.
     */
    fun setTilt(x: Float, y: Float) {
        tiltX = x
        tiltY = y
    }

    private fun updateTiltBonus(delta: Float) {
        if (isTiltActive) {
            tiltEffectRemainingSeconds = (tiltEffectRemainingSeconds - delta).coerceAtLeast(0f)
            return
        }
        tiltBonusCooldown -= delta
        if (tiltBonusCooldown <= 0f) {
            tiltBonusCooldown = TILT_BONUS_INTERVAL_SECONDS.toFloat()
            spawnTiltBonus()
        }
    }

    private fun spawnTiltBonus() {
        val margin = TILT_BONUS_SIZE + 2f
        val x = if (margin * 2 >= logicalWidth) {
            logicalWidth / 2f
        } else {
            margin + random.nextFloat() * (logicalWidth - margin * 2)
        }
        val y = if (margin * 2 >= logicalHeight) {
            logicalHeight / 2f
        } else {
            margin + random.nextFloat() * (logicalHeight - margin * 2)
        }
        tiltBonus = Bonus(
            id = nextBonusId++,
            x = x,
            y = y,
            size = TILT_BONUS_SIZE,
            imageRes = R.drawable.bonus_tilt
        )
    }

    /** Результат касания игрока по полю. */
    enum class TapResult {
        /** Промах мимо жуков и бонусов. */
        MISS,

        /** Попадание в жука. */
        BUG,

        /** Нажатие на бонус наклона: режим наклона активирован. */
        TILT_BONUS
    }

    /**
     * Обрабатывает касание игрока в логических координатах поля.
     * Нажатие на бонус наклона включает режим наклона, попадание в жука
     * начисляет его стоимость в очках, промах — штраф [MISS_PENALTY].
     *
     * @return что произошло: промах, попадание в жука или взятый бонус
     */
    fun onTap(x: Float, y: Float): TapResult {
        val bonus = tiltBonus
        if (bonus != null) {
            val dx = x - bonus.x
            val dy = y - bonus.y
            if (dx * dx + dy * dy <= bonus.size * bonus.size) {
                tiltBonus = null
                tiltEffectRemainingSeconds = TILT_EFFECT_DURATION_SECONDS.toFloat()
                return TapResult.TILT_BONUS
            }
        }
        val bug = activeBugs.lastOrNull { bug ->
            val dx = x - bug.x
            val dy = y - bug.y
            dx * dx + dy * dy <= bug.size * bug.size
        }
        if (bug == null) {
            misses++
            score -= MISS_PENALTY
            return TapResult.MISS
        }
        activeBugs.remove(bug)
        hits++
        score += bug.points
        return TapResult.BUG
    }

    private fun moveBugs(delta: Float) {
        // В режиме наклона жуки скатываются в наклонённую сторону экрана
        val tiltDx = tiltX * delta
        val tiltDy = tiltY * delta
        val tiltForce = if (isTiltActive) TILT_FORCE else 0f
        activeBugs.forEach { bug ->
            val direction = Math.toRadians(bug.angle.toDouble())
            bug.x += (sin(direction) * bug.speed * delta).toFloat() + tiltDx * tiltForce
            bug.y -= (cos(direction) * bug.speed * delta).toFloat() + tiltDy * tiltForce
            bounceOffWalls(bug)
        }
    }

    private fun bounceOffWalls(bug: Bug) {
        if (bug.x < bug.size) {
            bug.x = bug.size + (bug.size - bug.x)
            bug.angle = -bug.angle
        } else if (bug.x > logicalWidth - bug.size) {
            bug.x = (logicalWidth - bug.size) - (bug.x - (logicalWidth - bug.size))
            bug.angle = -bug.angle
        }
        if (bug.y < bug.size) {
            bug.y = bug.size + (bug.size - bug.y)
            bug.angle = 180f - bug.angle
        } else if (bug.y > logicalHeight - bug.size) {
            bug.y = (logicalHeight - bug.size) - (bug.y - (logicalHeight - bug.size))
            bug.angle = 180f - bug.angle
        }
        bug.angle = (bug.angle % 360f + 360f) % 360f
        clampInsideField(bug)
    }

    private fun clampInsideField(bug: Bug) {
        if (logicalWidth <= 0f || logicalHeight <= 0f) return
        bug.x = if (bug.size * 2 >= logicalWidth) {
            logicalWidth / 2f
        } else {
            bug.x.coerceIn(bug.size, logicalWidth - bug.size)
        }
        bug.y = if (bug.size * 2 >= logicalHeight) {
            logicalHeight / 2f
        } else {
            bug.y.coerceIn(bug.size, logicalHeight - bug.size)
        }
    }

    private fun spawnBug(rare: Boolean) {
        val type = if (rare) BugType.RARE else BugType.random(random)
        val margin = type.size + 2f
        val x = if (margin * 2 >= logicalWidth) {
            logicalWidth / 2f
        } else {
            margin + random.nextFloat() * (logicalWidth - margin * 2)
        }
        val y = if (margin * 2 >= logicalHeight) {
            logicalHeight / 2f
        } else {
            margin + random.nextFloat() * (logicalHeight - margin * 2)
        }
        activeBugs += Bug(
            id = nextBugId++,
            type = type,
            x = x,
            y = y,
            angle = random.nextFloat() * 360f,
            speed = type.speed * settings.speed,
            size = type.size,
            points = type.points,
            imageRes = type.imageRes
        )
    }
}
