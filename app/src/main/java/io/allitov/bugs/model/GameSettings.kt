package io.allitov.bugs.model

data class GameSettings(
    val speed: Float = 1.0f,
    val maxBugs: Int = 20,
    val bonusIntervalSeconds: Int = 15,
    val roundDurationSeconds: Int = 120
) {

    /** Уровень сложности определяется позицией текущей скорости игры. */
    val difficultyIndex: Int
        get() = SPEEDS.indexOf(speed).coerceIn(0, DIFFICULTY_LEVELS.lastIndex)

    val difficulty: String
        get() = DIFFICULTY_LEVELS[difficultyIndex]

    companion object {
        /** Доступные скорости игры; количество уровней сложности совпадает с их числом. */
        val SPEEDS = listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f)

        /** Названия уровней сложности, по одному на каждую скорость игры. */
        val DIFFICULTY_LEVELS = listOf("Очень легкий", "Легкий", "Средний", "Сложный", "Экстрим")

        /** Шаг ползунка длительности раунда в настройках, в секундах. */
        const val ROUND_DURATION_STEP = 15

        /** Максимальная длительность раунда, в секундах. */
        const val MAX_ROUND_DURATION = 120
    }
}

object GameSettingsStore {
    var current = GameSettings()
}
