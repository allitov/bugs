package io.allitov.bugs

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.allitov.bugs.model.GameSettingsStore
import io.allitov.bugs.view.GameFieldView

/** Экран раунда: игровое поле с бегающими жуками и индикаторы очков и времени. */
class GameActivity : AppCompatActivity() {

    private lateinit var gameField: GameFieldView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameField = findViewById(R.id.gameField)
        val scoreView = findViewById<TextView>(R.id.tvScore)
        val timeView = findViewById<TextView>(R.id.tvTimeLeft)
        val tiltBonusView = findViewById<TextView>(R.id.tvTiltBonus)

        scoreView.text = getString(R.string.score_value, 0)
        timeView.text = getString(R.string.time_left_value, GameSettingsStore.current.roundDurationSeconds)

        gameField.onStateChanged = { score, timeLeft, _, _, tiltSecondsLeft ->
            scoreView.text = getString(R.string.score_value, score)
            timeView.text = getString(R.string.time_left_value, timeLeft)
            if (tiltSecondsLeft > 0) {
                tiltBonusView.text = getString(R.string.tilt_bonus_active, tiltSecondsLeft)
                tiltBonusView.visibility = View.VISIBLE
            } else {
                tiltBonusView.visibility = View.GONE
            }
        }
        gameField.onRoundFinished = {
            val intent = GameResultsActivity.createIntent(this, gameField.currentStats)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        gameField.start()
    }

    override fun onPause() {
        gameField.pause()
        super.onPause()
    }
}
