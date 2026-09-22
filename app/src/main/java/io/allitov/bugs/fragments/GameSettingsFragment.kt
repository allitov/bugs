package io.allitov.bugs.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.allitov.bugs.GameSettings
import io.allitov.bugs.GameSettingsStore
import io.allitov.bugs.R

class GameSettingsFragment : Fragment() {

    private val speeds = listOf(0.5F, 1.0F, 1.5F, 2.0F, 3.0F)
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
        speedSeekBar.progress = speeds.indexOf(gameSettings.speed)
        speedText.text = formatSpeed(gameSettings.speed)
        speedSeekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            updateSettings { it.copy(speed = speeds[progress]) }
            speedText.text = formatSpeed(gameSettings.speed)
        })

        val maxCockroachesSeekBar = view.findViewById<SeekBar>(R.id.sbMaxCockroaches)
        val maxCockroachesText = view.findViewById<TextView>(R.id.tvMaxCockroaches)
        maxCockroachesSeekBar.progress = gameSettings.maxCockroaches - 5
        maxCockroachesText.text = gameSettings.maxCockroaches.toString()
        maxCockroachesSeekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            updateSettings { it.copy(maxCockroaches = progress + 5) }
            maxCockroachesText.text = gameSettings.maxCockroaches.toString()
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
        roundDurationSeekBar.progress = gameSettings.roundDurationSeconds - 30
        roundDurationText.text = formatDuration(gameSettings.roundDurationSeconds)
        roundDurationSeekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            updateSettings { it.copy(roundDurationSeconds = progress + 30) }
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

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes == 0) {
            getString(R.string.seconds_value, seconds)
        } else {
            getString(R.string.minutes_seconds_value, minutes, remainingSeconds)
        }
    }
}
