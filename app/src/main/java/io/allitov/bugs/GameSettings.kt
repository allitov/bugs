package io.allitov.bugs

data class GameSettings(
    val speed: Float = 1.0f,
    val maxCockroaches: Int = 20,
    val bonusIntervalSeconds: Int = 15,
    val roundDurationSeconds: Int = 120
)

object GameSettingsStore {
    var current = GameSettings()
}
