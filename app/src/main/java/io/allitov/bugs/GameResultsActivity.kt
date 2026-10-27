package io.allitov.bugs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.allitov.bugs.data.AppDatabase
import io.allitov.bugs.data.PlayerEntity
import io.allitov.bugs.data.PlayerSession
import io.allitov.bugs.data.ScoreEntity
import io.allitov.bugs.model.GameSettingsStore
import io.allitov.bugs.model.GameStats
import kotlinx.coroutines.launch

/** Экран итогов раунда: очки, попадания, промахи, точность и повторная игра. Результат сохраняется в базу. */
class GameResultsActivity : AppCompatActivity() {

    private var scoreSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_results)

        scoreSaved = savedInstanceState?.getBoolean(KEY_SCORE_SAVED) ?: false

        val stats = GameStats(
            score = intent.getIntExtra(EXTRA_SCORE, 0),
            hits = intent.getIntExtra(EXTRA_HITS, 0),
            misses = intent.getIntExtra(EXTRA_MISSES, 0)
        )

        val player = PlayerSession.current
        val playerView = findViewById<TextView>(R.id.tvResultPlayer)
        if (player != null) {
            playerView.visibility = View.VISIBLE
            playerView.text = getString(R.string.results_player, player.fullName)
            saveScore(stats, player)
        }

        findViewById<TextView>(R.id.tvResultScore).text = getString(R.string.results_score, stats.score)
        findViewById<TextView>(R.id.tvResultHits).text = getString(R.string.results_hits, stats.hits)
        findViewById<TextView>(R.id.tvResultMisses).text = getString(R.string.results_misses, stats.misses)
        findViewById<TextView>(R.id.tvResultAccuracy).text =
            getString(R.string.results_accuracy, stats.accuracyPercent)

        findViewById<Button>(R.id.btnReplay).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.btnToMenu).setOnClickListener {
            startActivity(
                Intent(this, GameMenuActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SCORE_SAVED, scoreSaved)
    }

    /** Сохраняет результат раунда в базу данных один раз на экземпляр экрана. */
    private fun saveScore(stats: GameStats, player: PlayerEntity) {
        if (scoreSaved) return
        scoreSaved = true

        val record = ScoreEntity(
            playerId = player.id,
            score = stats.score,
            hits = stats.hits,
            misses = stats.misses,
            difficulty = GameSettingsStore.current.difficulty,
            roundDurationSeconds = GameSettingsStore.current.roundDurationSeconds
        )
        lifecycleScope.launch {
            AppDatabase.get(applicationContext).scoreDao().insert(record)
        }
    }

    companion object {
        private const val EXTRA_SCORE = "extra_score"
        private const val EXTRA_HITS = "extra_hits"
        private const val EXTRA_MISSES = "extra_misses"
        private const val KEY_SCORE_SAVED = "key_score_saved"

        /** Создаёт интент экрана итогов с результатами завершённого раунда. */
        fun createIntent(context: Context, stats: GameStats): Intent =
            Intent(context, GameResultsActivity::class.java).apply {
                putExtra(EXTRA_SCORE, stats.score)
                putExtra(EXTRA_HITS, stats.hits)
                putExtra(EXTRA_MISSES, stats.misses)
            }
    }
}
