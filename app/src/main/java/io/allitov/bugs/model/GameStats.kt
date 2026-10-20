package io.allitov.bugs.model

/** Итоги игрового раунда. */
data class GameStats(
    val score: Int,
    val hits: Int,
    val misses: Int
) {

    /** Точность в процентах: доля попаданий среди всех касаний поля. */
    val accuracyPercent: Int
        get() {
            val taps = hits + misses
            return if (taps == 0) 0 else hits * 100 / taps
        }
}
