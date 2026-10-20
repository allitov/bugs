package io.allitov.bugs.model

import androidx.annotation.DrawableRes
import io.allitov.bugs.R
import kotlin.random.Random

/**
 * Типы жуков. Типы отличаются скоростью, размером, стоимостью в очках и внешним видом.
 *
 * @property title       название типа
 * @property speed       базовая скорость в логических единицах в секунду
 * @property size        радиус жука в логических единицах
 * @property points      стоимость жука в очках
 * @property imageRes    визуальный ресурс жука
 * @property spawnWeight относительный вес появления при создании нового жука;
 *                       вес 0 означает, что тип не выпадает при обычном спавне
 */
enum class BugType(
    val title: String,
    val speed: Float,
    val size: Float,
    val points: Int,
    @DrawableRes val imageRes: Int,
    val spawnWeight: Int
) {
    NORMAL("Обычный жук", speed = 12f, size = 9f, points = 10, imageRes = R.drawable.bug_normal, spawnWeight = 60),
    FAST("Быстрый жук", speed = 26f, size = 6f, points = 25, imageRes = R.drawable.bug_fast, spawnWeight = 30),

    // Редкий жук не участвует в обычном спавне (вес 0): он выходит на поле
    // только по таймеру бонусов — раз в bonusIntervalSeconds (см. GameEngine).
    RARE("Редкий жук", speed = 18f, size = 5f, points = 50, imageRes = R.drawable.bug_rare, spawnWeight = 0);

    companion object {

        /** Возвращает случайный тип с учётом веса появления [BugType.spawnWeight].
         *  Типы с нулевым весом при обычном спавне не выпадают. */
        fun random(random: Random = Random.Default): BugType {
            var roll = random.nextInt(entries.sumOf { it.spawnWeight })
            entries.forEach { type ->
                roll -= type.spawnWeight
                if (roll < 0) return type
            }
            return NORMAL
        }
    }
}
