package io.allitov.bugs

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.allitov.bugs.data.GoldRateRepository
import io.allitov.bugs.data.GoldRateStore
import io.allitov.bugs.game.GameEngine
import io.allitov.bugs.model.GameSettingsStore
import io.allitov.bugs.view.GameFieldView
import io.allitov.bugs.widget.GoldRateWidgetProvider
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

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

        loadGoldRate()
    }

    /**
     * Загружает курс золота ЦБ через Retrofit и передаёт его игровому полю:
     * от курса зависит стоимость золотого таракана (раз в 20 секунд).
     * Заодно обновляет виджет на рабочем столе и, если курс получен из сети,
     * показывает игроку текущую цену золота.
     */
    private fun loadGoldRate() {
        val repository = GoldRateRepository(applicationContext)
        val updatedAtBefore = GoldRateStore.updatedAt(applicationContext)
        lifecycleScope.launch {
            val pricePerGram = repository.loadGoldPricePerGram()
                ?: GameEngine.DEFAULT_GOLD_PRICE_PER_GRAM
            gameField.setGoldPricePerGram(pricePerGram)
            GoldRateWidgetProvider.refreshAll(applicationContext)
            if (GoldRateStore.updatedAt(applicationContext) != updatedAtBefore) {
                showGoldRateToast(pricePerGram)
            }
        }
    }

    /** Подсказка игроку: курс золота ЦБ и стоимость золотого таракана в этом раунде. */
    private fun showGoldRateToast(pricePerGram: Double) {
        val priceText = NumberFormat.getIntegerInstance(Locale.getDefault())
            .format(pricePerGram.roundToInt())
        val points = GameEngine.goldBugPointsFor(pricePerGram)
        val message = getString(R.string.gold_rate_toast, priceText) + "\n" +
            resources.getQuantityString(R.plurals.gold_bug_points, points, points)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
