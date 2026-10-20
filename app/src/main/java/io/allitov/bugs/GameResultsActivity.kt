package io.allitov.bugs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.allitov.bugs.model.GameStats

/** Экран итогов раунда: очки, попадания, промахи, точность и повторная игра. */
class GameResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_results)

        val stats = GameStats(
            score = intent.getIntExtra(EXTRA_SCORE, 0),
            hits = intent.getIntExtra(EXTRA_HITS, 0),
            misses = intent.getIntExtra(EXTRA_MISSES, 0)
        )

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

    companion object {
        private const val EXTRA_SCORE = "extra_score"
        private const val EXTRA_HITS = "extra_hits"
        private const val EXTRA_MISSES = "extra_misses"

        /** Создаёт интент экрана итогов с результатами завершённого раунда. */
        fun createIntent(context: Context, stats: GameStats): Intent =
            Intent(context, GameResultsActivity::class.java).apply {
                putExtra(EXTRA_SCORE, stats.score)
                putExtra(EXTRA_HITS, stats.hits)
                putExtra(EXTRA_MISSES, stats.misses)
            }
    }
}
