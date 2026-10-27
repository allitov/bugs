package io.allitov.bugs.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.allitov.bugs.model.GameSettings
import io.allitov.bugs.model.GameSettingsStore
import io.allitov.bugs.R

class GameSettingsFragment : Fragment() {

    private var gameSettings = GameSettingsStore.current

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.view_game_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val speedSeekBar = view.findViewById<SeekBar>(R.id.sbGameSpeed)
        val speedText = view.findViewById<TextView>(R.id.tvGameSpeed)
        val difficultyText = view.findViewById<TextView>(R.id.tvDifficulty)
        speedSeekBar.progress = GameSettings.SPEEDS.indexOf(gameSettings.speed)
        speedText.text = formatSpeed(gameSettings.speed)
        difficultyText.text = formatDifficulty(gameSettings)
        speedSeekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            updateSettings { it.copy(speed = GameSettings.SPEEDS[progress]) }
            speedText.text = formatSpeed(gameSettings.speed)
            difficultyText.text = formatDifficulty(gameSettings)
        })

        val maxBugsSeekBar = view.findViewById<SeekBar>(R.id.sbMaxBugs)
        val maxBugsText = view.findViewById<TextView>(R.id.tvMaxBugs)
        maxBugsSeekBar.progress = gameSettings.maxBugs - 5
        maxBugsText.text = gameSettings.maxBugs.toString()
        maxBugsSeekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            updateSettings { it.copy(maxBugs = progress + 5) }
            maxBugsText.text = gameSettings.maxBugs.toString()
        })

        val bonusIntervalSeekBar = view.findViewById<SeekBar>(R.id.sbBonusInterval)
        val bonusIntervalText = view.findViewById<TextView>(R.id.tvBonusInterval)
        bonusIntervalSeekBar.progress = gameSettings.bonusIntervalSeconds - 5
        bonusIntervalText.text = getString(R.string.seconds_value, gameSettings.bonusIntervalSeconds)
        bonusIntervalSeekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            updateSettings { it.copy(bonusIntervalSeconds = progress + 5) }
            bonusIntervalText.text = getString(R.string.seconds_value, gameSettings.bonusIntervalSeconds)
        })

        val roundDurationSeekBar = view.findViewById<SeekBar>(R.id.sbRoundDuration)
        val roundDurationText = view.findViewById<TextView>(R.id.tvRoundDuration)
        roundDurationSeekBar.progress = gameSettings.roundDurationSeconds / ROUND_DURATION_STEP - 1
        roundDurationText.text = formatDuration(gameSettings.roundDurationSeconds)
        roundDurationSeekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            updateSettings { it.copy(roundDurationSeconds = progress * ROUND_DURATION_STEP + ROUND_DURATION_STEP) }
            roundDurationText.text = formatDuration(gameSettings.roundDurationSeconds)
        })
    }

    private fun updateSettings(update: (GameSettings) -> GameSettings) {
        gameSettings = update(gameSettings)
        GameSettingsStore.current = gameSettings
    }

    private fun simpleSeekBarListener(onProgress: (Int) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                onProgress(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }

    private fun formatSpeed(speed: Float): String = "${speed}×"

    private fun formatDifficulty(settings: GameSettings): String =
        getString(R.string.settings_difficulty_value, settings.difficulty)

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes == 0) {
            getString(R.string.seconds_value, seconds)
        } else {
            getString(R.string.minutes_seconds_value, minutes, remainingSeconds)
        }
    }

    private companion object {
        const val ROUND_DURATION_STEP = GameSettings.ROUND_DURATION_STEP
    }
}
