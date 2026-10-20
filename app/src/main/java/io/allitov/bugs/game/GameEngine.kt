package io.allitov.bugs.game

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

        private const val SPAWN_TICK_SECONDS = 0.8f
        private const val MAX_DELTA_SECONDS = 0.1f
        private const val INITIAL_BUGS = 5
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
    }

    /**
     * Обрабатывает касание игрока в логических координатах поля.
     * Попадание в жука начисляет его стоимость в очках, промах — штраф [MISS_PENALTY].
     *
     * @return true, если касание попало в жука, false — это промах
     */
    fun onTap(x: Float, y: Float): Boolean {
        val bug = activeBugs.lastOrNull { bug ->
            val dx = x - bug.x
            val dy = y - bug.y
            dx * dx + dy * dy <= bug.size * bug.size
        }
        if (bug == null) {
            misses++
            score -= MISS_PENALTY
            return false
        }
        activeBugs.remove(bug)
        hits++
        score += bug.points
        return true
    }

    private fun moveBugs(delta: Float) {
        activeBugs.forEach { bug ->
            val direction = Math.toRadians(bug.angle.toDouble())
            bug.x += (sin(direction) * bug.speed * delta).toFloat()
            bug.y -= (cos(direction) * bug.speed * delta).toFloat()
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
